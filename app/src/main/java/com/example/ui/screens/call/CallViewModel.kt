package com.example.ui.screens.call

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.IceCandidatePayload
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.UserRepository
import com.example.data.webrtc.WebRtcCallManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.PeerConnection
import java.util.UUID

enum class CallUiState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    IN_CALL,
    ENDED
}

class CallViewModel(
    private val context: Context,
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
        private const val RINGING_TIMEOUT_MS = 35000L
    }

    private val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _callUiState = MutableStateFlow(CallUiState.IDLE)
    val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    private val _activeSession = MutableStateFlow<CallSession?>(null)
    val activeSession: StateFlow<CallSession?> = _activeSession.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val _callStatusMessage = MutableStateFlow("")
    val callStatusMessage: StateFlow<String> = _callStatusMessage.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallSession>>(emptyList())
    val callHistory: StateFlow<List<CallSession>> = _callHistory.asStateFlow()

    private var webRtcManager: WebRtcCallManager? = null
    private var callSessionJob: Job? = null
    private var candidateJob: Job? = null
    private var durationTimerJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private var incomingListenerJob: Job? = null

    init {
        startIncomingCallListener()
        loadCallHistory()
    }

    private fun startIncomingCallListener() {
        val uid = currentUserId ?: return
        incomingListenerJob?.cancel()
        incomingListenerJob = viewModelScope.launch {
            callRepository.listenToIncomingCalls(uid).collect { session ->
                if (session != null && _callUiState.value == CallUiState.IDLE) {
                    _activeSession.value = session
                    _callUiState.value = CallUiState.INCOMING_RINGING
                    _callStatusMessage.value = "Incoming Voice Call..."
                    listenToSessionUpdates(session.callId)
                }
            }
        }
    }

    private fun loadCallHistory() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            callRepository.getCallHistoryStream(uid).collect { list ->
                _callHistory.value = list
            }
        }
    }

    /**
     * Start an outgoing voice call to target user.
     */
    fun initiateCall(receiverUser: FirebaseUserProfile) {
        val callerUid = currentUserId ?: return
        if (_callUiState.value != CallUiState.IDLE) return

        viewModelScope.launch {
            val checkResult = callRepository.canInitiateCall(callerUid, receiverUser.userId)
            if (checkResult.isFailure) {
                _callStatusMessage.value = checkResult.exceptionOrNull()?.message ?: "Call unavailable"
                delay(2000)
                _callStatusMessage.value = ""
                return@launch
            }

            val callerProfile = userRepository.getUserProfile(callerUid)
            val callId = "call_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            val newSession = CallSession(
                callId = callId,
                callerUid = callerUid,
                receiverUid = receiverUser.userId,
                callerPublicUserId = callerProfile?.publicUserId ?: "0000000",
                receiverPublicUserId = receiverUser.publicUserId,
                callerDisplayName = callerProfile?.displayName ?: "User",
                callerAvatarUrl = callerProfile?.avatar ?: "",
                callerFrameId = callerProfile?.profileFrameId ?: "frame_default",
                receiverDisplayName = receiverUser.displayName,
                receiverAvatarUrl = receiverUser.avatar,
                status = "ringing"
            )

            _activeSession.value = newSession
            _callUiState.value = CallUiState.OUTGOING_RINGING
            _callStatusMessage.value = "Calling ${receiverUser.displayName}..."

            val createResult = callRepository.createCallSession(newSession)
            if (createResult.isFailure) {
                _callStatusMessage.value = "Failed to establish call signal"
                cleanupCall()
                return@launch
            }

            // Initialize WebRTC
            webRtcManager = WebRtcCallManager(
                context = context,
                onIceCandidateGenerated = { candidate ->
                    viewModelScope.launch {
                        callRepository.addCallerCandidate(callId, candidate)
                    }
                },
                onConnectionStateChanged = { state ->
                    Log.d(TAG, "WebRTC PeerConnectionState: $state")
                    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                        _callStatusMessage.value = "Connected"
                    }
                }
            )

            webRtcManager?.setupCall(isCaller = true)
            webRtcManager?.createOffer { offerDesc ->
                viewModelScope.launch {
                    callRepository.setSdpOffer(callId, offerDesc.description)
                }
            }

            // Listen to receiver's ICE candidates
            candidateJob?.cancel()
            candidateJob = viewModelScope.launch {
                callRepository.listenToReceiverCandidates(callId).collect { candidates ->
                    candidates.forEach { c ->
                        webRtcManager?.addRemoteIceCandidate(c)
                    }
                }
            }

            // Ringing timeout (35 seconds)
            ringingTimeoutJob?.cancel()
            ringingTimeoutJob = viewModelScope.launch {
                delay(RINGING_TIMEOUT_MS)
                if (_callUiState.value == CallUiState.OUTGOING_RINGING) {
                    _callStatusMessage.value = "No answer"
                    callRepository.updateCallStatus(callId, "missed")
                    delay(1500)
                    cleanupCall()
                }
            }

            // Listen to call session changes (accepted, rejected, ended)
            listenToSessionUpdates(callId)
        }
    }

    /**
     * Answer incoming ringing call.
     */
    fun answerIncomingCall() {
        val session = _activeSession.value ?: return
        if (_callUiState.value != CallUiState.INCOMING_RINGING) return
        val callId = session.callId

        viewModelScope.launch {
            _callUiState.value = CallUiState.IN_CALL
            _callStatusMessage.value = "Connecting..."

            webRtcManager = WebRtcCallManager(
                context = context,
                onIceCandidateGenerated = { candidate ->
                    viewModelScope.launch {
                        callRepository.addReceiverCandidate(callId, candidate)
                    }
                },
                onConnectionStateChanged = { state ->
                    Log.d(TAG, "WebRTC Receiver ConnectionState: $state")
                    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                        _callStatusMessage.value = "Connected"
                    }
                }
            )

            webRtcManager?.setupCall(isCaller = false)

            val offer = session.sdpOffer
            if (!offer.isNullOrBlank()) {
                webRtcManager?.createAnswer(offer) { answerDesc ->
                    viewModelScope.launch {
                        callRepository.setSdpAnswer(callId, answerDesc.description)
                    }
                }
            }

            // Listen to caller ICE candidates
            candidateJob?.cancel()
            candidateJob = viewModelScope.launch {
                callRepository.listenToCallerCandidates(callId).collect { candidates ->
                    candidates.forEach { c ->
                        webRtcManager?.addRemoteIceCandidate(c)
                    }
                }
            }

            startDurationTimer()
        }
    }

    /**
     * Reject incoming call.
     */
    fun rejectIncomingCall() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            callRepository.updateCallStatus(session.callId, "rejected", endedBy = currentUserId)
            cleanupCall()
        }
    }

    /**
     * End ongoing call or cancel outgoing call.
     */
    fun endCall() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            val duration = _callDurationSeconds.value
            val finalStatus = if (_callUiState.value == CallUiState.OUTGOING_RINGING) "cancelled" else "ended"
            callRepository.updateCallStatus(session.callId, finalStatus, endedBy = currentUserId, duration = duration)
            _callStatusMessage.value = "Call Ended"
            _callUiState.value = CallUiState.ENDED
            delay(1200)
            cleanupCall()
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        webRtcManager?.setMicrophoneMuted(newMute)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        webRtcManager?.setSpeakerphoneOn(newSpeaker)
    }

    private fun listenToSessionUpdates(callId: String) {
        callSessionJob?.cancel()
        callSessionJob = viewModelScope.launch {
            callRepository.listenToCall(callId).collect { session ->
                if (session == null) return@collect
                _activeSession.value = session

                when (session.status) {
                    "accepted" -> {
                        if (_callUiState.value == CallUiState.OUTGOING_RINGING) {
                            ringingTimeoutJob?.cancel()
                            _callUiState.value = CallUiState.IN_CALL
                            _callStatusMessage.value = "Connected"
                            session.sdpAnswer?.let { answer ->
                                webRtcManager?.setRemoteAnswer(answer)
                            }
                            startDurationTimer()
                        }
                    }
                    "rejected" -> {
                        ringingTimeoutJob?.cancel()
                        _callStatusMessage.value = "Call Declined"
                        _callUiState.value = CallUiState.ENDED
                        delay(1500)
                        cleanupCall()
                    }
                    "busy" -> {
                        ringingTimeoutJob?.cancel()
                        _callStatusMessage.value = "User is busy"
                        _callUiState.value = CallUiState.ENDED
                        delay(1500)
                        cleanupCall()
                    }
                    "missed" -> {
                        _callStatusMessage.value = "Missed Call"
                        _callUiState.value = CallUiState.ENDED
                        delay(1500)
                        cleanupCall()
                    }
                    "ended", "cancelled" -> {
                        _callStatusMessage.value = "Call Ended"
                        _callUiState.value = CallUiState.ENDED
                        delay(1200)
                        cleanupCall()
                    }
                }
            }
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        _callDurationSeconds.value = 0L
        durationTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun cleanupCall() {
        ringingTimeoutJob?.cancel()
        durationTimerJob?.cancel()
        candidateJob?.cancel()
        callSessionJob?.cancel()

        webRtcManager?.close()
        webRtcManager = null

        _callUiState.value = CallUiState.IDLE
        _activeSession.value = null
        _callDurationSeconds.value = 0L
        _callStatusMessage.value = ""
        _isMuted.value = false
        _isSpeakerOn.value = true
    }

    override fun onCleared() {
        super.onCleared()
        cleanupCall()
        incomingListenerJob?.cancel()
    }
}
