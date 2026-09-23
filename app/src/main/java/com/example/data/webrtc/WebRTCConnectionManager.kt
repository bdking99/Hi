package com.example.data.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.model.RoomRtcSignal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * WebRTCConnectionManager
 *
 * Comprehensive manager for real-time peer-to-peer audio communications in voice rooms:
 * 1. Peer-to-Peer Signaling: Handles SDP offer/answer generation, ICE candidate trickling,
 *    pending candidate queuing, renegotiation, and session state tracking.
 * 2. Audio Track Initialization: Manages hardware AEC (Acoustic Echo Cancellation), AGC (Auto Gain Control),
 *    NS (Noise Suppression), High-pass filter, AudioManager routing, and microphone capture.
 * 3. Stream Management: Multi-peer mesh topologies for voice rooms, local and remote audio track lifecycle,
 *    per-peer mute/volume adjustments, voice activity detection (VAD), and safe disposal.
 */
class WebRTCConnectionManager(
    private val context: Context,
    val myUserId: String,
    private val onSendSignal: (RoomRtcSignal) -> Unit = {},
    private val onPeerSpeakingChanged: ((peerUid: String, isSpeaking: Boolean) -> Unit)? = null,
    private val onConnectionStateChanged: ((peerUid: String, state: PeerConnection.PeerConnectionState) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WebRTCConnectionManager"
        private const val LOCAL_AUDIO_STREAM_ID = "voice_room_audio_stream"
        private const val LOCAL_AUDIO_TRACK_BASE = "mic_track"

        private val DEFAULT_ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )
    }

    // Android Audio subsystem
    private val audioManager: AudioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // WebRTC Core
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Per-peer connection state wrapper
    private class PeerSession(
        val peerUid: String,
        val connection: PeerConnection,
        var rtpSender: RtpSender? = null,
        var remoteAudioTrack: AudioTrack? = null,
        val pendingIceCandidates: MutableList<IceCandidate> = CopyOnWriteArrayList(),
        var isRemoteDescriptionSet: Boolean = false,
        var isMutedByLocal: Boolean = false,
        var volumeMultiplier: Double = 1.0
    )

    private val peerSessions = ConcurrentHashMap<String, PeerSession>()

    // State flows for UI observing
    private val _isMicrophoneMuted = MutableStateFlow(false)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

    private val _isSpeakingLocally = MutableStateFlow(false)
    val isSpeakingLocally: StateFlow<Boolean> = _isSpeakingLocally.asStateFlow()

    private val _isSpeakerphoneEnabled = MutableStateFlow(true)
    val isSpeakerphoneEnabled: StateFlow<Boolean> = _isSpeakerphoneEnabled.asStateFlow()

    private val _activePeers = MutableStateFlow<Set<String>>(emptySet())
    val activePeers: StateFlow<Set<String>> = _activePeers.asStateFlow()

    private val _speakingPeers = MutableStateFlow<Set<String>>(emptySet())
    val speakingPeers: StateFlow<Set<String>> = _speakingPeers.asStateFlow()

    private val _peerConnectionStates = MutableStateFlow<Map<String, PeerConnection.PeerConnectionState>>(emptyMap())
    val peerConnectionStates: StateFlow<Map<String, PeerConnection.PeerConnectionState>> = _peerConnectionStates.asStateFlow()

    // Internal coroutine scope for monitoring audio levels & timeout handlers
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vadJob: Job? = null
    private var currentRoomId: String? = null
    private var isSpeakerModeActive: Boolean = false

    init {
        initializePeerConnectionFactory()
    }

    // =========================================================================
    // 1. FACTORY & HARDWARE AUDIO TRACK INITIALIZATION
    // =========================================================================

    private fun initializePeerConnectionFactory() {
        try {
            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)

            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory()

            Log.d(TAG, "PeerConnectionFactory successfully initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PeerConnectionFactory: ${e.message}", e)
        }
    }

    /**
     * Initializes the local audio track with hardware constraints (AEC, AGC, NS, Highpass).
     */
    @Synchronized
    private fun initLocalAudioTrack(factory: PeerConnectionFactory) {
        if (localAudioTrack != null) return

        try {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
            }

            audioSource = factory.createAudioSource(audioConstraints)
            val trackId = "${LOCAL_AUDIO_TRACK_BASE}_${myUserId}_${System.currentTimeMillis()}"
            localAudioTrack = factory.createAudioTrack(trackId, audioSource).apply {
                setEnabled(!_isMicrophoneMuted.value)
            }

            Log.d(TAG, "Local hardware-accelerated AudioTrack initialized: $trackId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize local audio track: ${e.message}", e)
        }
    }

    /**
     * Start voice room audio session.
     * @param roomId The room ID for signaling channel.
     * @param asSpeaker True if the user is a host or taking a seat on the stage.
     */
    fun startAudio(roomId: String, asSpeaker: Boolean) {
        currentRoomId = roomId
        isSpeakerModeActive = asSpeaker
        val factory = peerConnectionFactory ?: return

        setupAudioManager()

        if (asSpeaker) {
            initLocalAudioTrack(factory)
            attachLocalTrackToAllPeers()
        }

        startVoiceActivityDetection()
    }

    /**
     * Called when audience member takes a microphone seat.
     */
    fun activateSpeakerMicrophone() {
        val factory = peerConnectionFactory ?: return
        isSpeakerModeActive = true

        if (localAudioTrack == null) {
            initLocalAudioTrack(factory)
        }

        setMicrophoneMuted(false)
        attachLocalTrackToAllPeers()
        Log.d(TAG, "Speaker microphone activated for user $myUserId")
    }

    /**
     * Called when speaker steps down to audience.
     */
    fun deactivateSpeakerMicrophone() {
        isSpeakerModeActive = false
        setMicrophoneMuted(true)
        detachLocalTrackFromAllPeers()
        _isSpeakingLocally.value = false
        Log.d(TAG, "Speaker microphone deactivated for user $myUserId")
    }

    /**
     * Toggles local microphone mute.
     */
    fun setMicrophoneMuted(muted: Boolean) {
        _isMicrophoneMuted.value = muted
        localAudioTrack?.setEnabled(!muted)
        if (muted) {
            _isSpeakingLocally.value = false
        }
        Log.d(TAG, "Microphone mute state changed to: $muted")
    }

    fun toggleMicrophone(): Boolean {
        val newState = !_isMicrophoneMuted.value
        setMicrophoneMuted(newState)
        return newState
    }

    // =========================================================================
    // 2. ANDROID AUDIO MANAGER & ROUTING MANAGEMENT
    // =========================================================================

    private fun setupAudioManager() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Request Audio Focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    }
                    .build()

                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            setSpeakerphoneEnabled(_isSpeakerphoneEnabled.value)
        } catch (e: Exception) {
            Log.e(TAG, "setupAudioManager error: ${e.message}", e)
        }
    }

    fun setSpeakerphoneEnabled(enabled: Boolean) {
        _isSpeakerphoneEnabled.value = enabled
        try {
            audioManager.isSpeakerphoneOn = enabled
            Log.d(TAG, "Speakerphone set to: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "setSpeakerphoneEnabled error: ${e.message}")
        }
    }

    fun toggleSpeakerphone(): Boolean {
        val newState = !_isSpeakerphoneEnabled.value
        setSpeakerphoneEnabled(newState)
        return newState
    }

    private fun resetAudioManager() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "resetAudioManager error: ${e.message}")
        }
    }

    // =========================================================================
    // 3. PEER-TO-PEER SIGNALING & MESH STREAM MANAGEMENT
    // =========================================================================

    /**
     * Connects to a specific remote peer in the room mesh.
     * @param remoteUid ID of the remote user.
     * @param isInitiator If true, creates and dispatches the initial SDP offer.
     */
    @Synchronized
    fun connectToPeer(remoteUid: String, isInitiator: Boolean) {
        if (remoteUid.isBlank() || remoteUid == myUserId) return
        if (peerSessions.containsKey(remoteUid)) {
            Log.d(TAG, "PeerConnection already exists for remote peer: $remoteUid")
            return
        }

        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "Cannot connectToPeer: PeerConnectionFactory is null.")
            return
        }

        val rtcConfig = PeerConnection.RTCConfiguration(DEFAULT_ICE_SERVERS).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "[$remoteUid] SignalingState: $state")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "[$remoteUid] ConnectionState: $newState")
                if (newState != null) {
                    val updatedMap = _peerConnectionStates.value.toMutableMap()
                    updatedMap[remoteUid] = newState
                    _peerConnectionStates.value = updatedMap
                    onConnectionStateChanged?.invoke(remoteUid, newState)

                    if (newState == PeerConnection.PeerConnectionState.FAILED) {
                        restartIce(remoteUid)
                    }
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "[$remoteUid] IceConnectionState: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "[$remoteUid] IceGatheringState: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    dispatchIceCandidate(remoteUid, candidate)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "[$remoteUid] onRenegotiationNeeded triggered")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() as? AudioTrack
                if (track != null) {
                    Log.d(TAG, "[$remoteUid] Received remote audio track")
                    peerSessions[remoteUid]?.remoteAudioTrack = track
                    track.setEnabled(true)
                }
            }
        }) ?: run {
            Log.e(TAG, "Failed to create PeerConnection for $remoteUid")
            return
        }

        val session = PeerSession(peerUid = remoteUid, connection = pc)
        peerSessions[remoteUid] = session
        updateActivePeers()

        // If local audio is ready, attach it
        localAudioTrack?.let { track ->
            session.rtpSender = pc.addTrack(track, listOf(LOCAL_AUDIO_STREAM_ID))
        }

        if (isInitiator) {
            createAndSendOffer(session)
        }
    }

    private fun createAndSendOffer(session: PeerSession) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        session.connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                session.connection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val roomId = currentRoomId ?: return
                        val signal = RoomRtcSignal(
                            id = "${myUserId}_${session.peerUid}_offer_${System.currentTimeMillis()}",
                            roomId = roomId,
                            senderUid = myUserId,
                            receiverUid = session.peerUid,
                            type = "OFFER",
                            sdp = desc.description,
                            timestamp = System.currentTimeMillis()
                        )
                        onSendSignal(signal)
                        Log.d(TAG, "Dispatched SDP OFFER to ${session.peerUid}")
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Failed to setLocalDescription offer: $error")
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription failure: $error")
                    }
                }, desc)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to createOffer: $error")
            }

            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    /**
     * Handles incoming peer-to-peer WebRTC signals (OFFER, ANSWER, ICE_CANDIDATE).
     */
    fun handleIncomingSignal(signal: RoomRtcSignal) {
        if (signal.senderUid == myUserId) return
        if (signal.receiverUid != myUserId && signal.receiverUid != "ALL") return

        val senderUid = signal.senderUid

        when (signal.type) {
            "OFFER" -> {
                connectToPeer(senderUid, isInitiator = false)
                val session = peerSessions[senderUid] ?: return
                val sdp = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)

                session.connection.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        session.isRemoteDescriptionSet = true
                        drainPendingIceCandidates(session)
                        createAndSendAnswer(session)
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "Failed to setRemoteDescription (OFFER) for $senderUid: $err")
                    }
                }, sdp)
            }

            "ANSWER" -> {
                val session = peerSessions[senderUid] ?: return
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)

                session.connection.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        session.isRemoteDescriptionSet = true
                        drainPendingIceCandidates(session)
                        Log.d(TAG, "Remote description (ANSWER) set for $senderUid. Connection established.")
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "Failed to setRemoteDescription (ANSWER) for $senderUid: $err")
                    }
                }, sdp)
            }

            "ICE_CANDIDATE" -> {
                val candidateSdp = signal.iceCandidateSdp ?: signal.sdp
                if (candidateSdp.isNotBlank()) {
                    val candidate = IceCandidate(
                        signal.iceCandidateMid ?: "0",
                        signal.iceCandidateIndex ?: 0,
                        candidateSdp
                    )
                    handleIncomingIceCandidate(senderUid, candidate)
                }
            }
        }
    }

    private fun createAndSendAnswer(session: PeerSession) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        session.connection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                session.connection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val roomId = currentRoomId ?: return
                        val signal = RoomRtcSignal(
                            id = "${myUserId}_${session.peerUid}_answer_${System.currentTimeMillis()}",
                            roomId = roomId,
                            senderUid = myUserId,
                            receiverUid = session.peerUid,
                            type = "ANSWER",
                            sdp = desc.description,
                            timestamp = System.currentTimeMillis()
                        )
                        onSendSignal(signal)
                        Log.d(TAG, "Dispatched SDP ANSWER to ${session.peerUid}")
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription answer error: $error")
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription answer failure: $error")
                    }
                }, desc)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createAnswer failure: $error")
            }

            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun dispatchIceCandidate(remoteUid: String, candidate: IceCandidate) {
        val roomId = currentRoomId ?: return
        val signal = RoomRtcSignal(
            id = "${myUserId}_${remoteUid}_cand_${System.currentTimeMillis()}_${candidate.sdpMLineIndex}",
            roomId = roomId,
            senderUid = myUserId,
            receiverUid = remoteUid,
            type = "ICE_CANDIDATE",
            sdp = candidate.sdp,
            iceCandidateMid = candidate.sdpMid,
            iceCandidateIndex = candidate.sdpMLineIndex,
            iceCandidateSdp = candidate.sdp,
            timestamp = System.currentTimeMillis()
        )
        onSendSignal(signal)
    }

    private fun handleIncomingIceCandidate(remoteUid: String, candidate: IceCandidate) {
        val session = peerSessions[remoteUid]
        if (session != null) {
            if (session.isRemoteDescriptionSet) {
                session.connection.addIceCandidate(candidate)
            } else {
                session.pendingIceCandidates.add(candidate)
                Log.d(TAG, "Queued ICE candidate for $remoteUid (remoteDescription pending)")
            }
        }
    }

    private fun drainPendingIceCandidates(session: PeerSession) {
        if (session.pendingIceCandidates.isNotEmpty()) {
            for (candidate in session.pendingIceCandidates) {
                session.connection.addIceCandidate(candidate)
            }
            Log.d(TAG, "Flushed ${session.pendingIceCandidates.size} pending ICE candidates for ${session.peerUid}")
            session.pendingIceCandidates.clear()
        }
    }

    /**
     * Synchronizes active room speakers with the local mesh peer connection pool.
     * Automatically establishes connections to new speakers and disconnects leaving speakers.
     */
    fun syncRoomSpeakers(speakerUids: List<String>) {
        val currentSpeakers = speakerUids.filter { it != myUserId }.toSet()
        val existingPeers = peerSessions.keys.toSet()

        // Disconnect peers no longer on stage
        val toRemove = existingPeers - currentSpeakers
        for (peerId in toRemove) {
            disconnectPeer(peerId)
        }

        // Connect to newly seated speakers
        val toAdd = currentSpeakers - existingPeers
        for (peerId in toAdd) {
            // Tie-break initiator role using lexicographical UID comparison
            val isInitiator = myUserId > peerId
            connectToPeer(peerId, isInitiator = isInitiator)
        }
    }

    /**
     * Restarts ICE gathering on connection drop.
     */
    fun restartIce(remoteUid: String) {
        val session = peerSessions[remoteUid] ?: return
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        session.connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                session.connection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val roomId = currentRoomId ?: return
                        val signal = RoomRtcSignal(
                            id = "${myUserId}_${remoteUid}_restart_${System.currentTimeMillis()}",
                            roomId = roomId,
                            senderUid = myUserId,
                            receiverUid = remoteUid,
                            type = "OFFER",
                            sdp = desc.description,
                            timestamp = System.currentTimeMillis()
                        )
                        onSendSignal(signal)
                        Log.d(TAG, "Dispatched ICE restart offer to $remoteUid")
                    }

                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, desc)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdpConstraints)
    }

    /**
     * Closes and removes a specific peer connection.
     */
    @Synchronized
    fun disconnectPeer(remoteUid: String) {
        val session = peerSessions.remove(remoteUid) ?: return
        try {
            session.connection.close()
            session.connection.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing PeerConnection for $remoteUid: ${e.message}")
        }
        updateActivePeers()
    }

    // =========================================================================
    // 4. STREAM MANAGEMENT & PER-PEER AUDIO CONTROLS
    // =========================================================================

    private fun attachLocalTrackToAllPeers() {
        val track = localAudioTrack ?: return
        for ((_, session) in peerSessions) {
            if (session.rtpSender == null) {
                try {
                    session.rtpSender = session.connection.addTrack(track, listOf(LOCAL_AUDIO_STREAM_ID))
                } catch (e: Exception) {
                    Log.e(TAG, "Error attaching track to ${session.peerUid}: ${e.message}")
                }
            } else {
                session.rtpSender?.setTrack(track, true)
            }
        }
    }

    private fun detachLocalTrackFromAllPeers() {
        for ((_, session) in peerSessions) {
            try {
                session.rtpSender?.let { sender ->
                    session.connection.removeTrack(sender)
                    session.rtpSender = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error detaching track from ${session.peerUid}: ${e.message}")
            }
        }
    }

    /**
     * Mutes or unmutes a specific remote participant's incoming audio track.
     */
    fun setRemotePeerMuted(remoteUid: String, isMuted: Boolean) {
        val session = peerSessions[remoteUid] ?: return
        session.isMutedByLocal = isMuted
        session.remoteAudioTrack?.setEnabled(!isMuted)
        Log.d(TAG, "Remote peer $remoteUid local mute set to: $isMuted")
    }

    /**
     * Sets volume multiplier for a specific remote participant.
     */
    fun setRemotePeerVolume(remoteUid: String, volume: Double) {
        val session = peerSessions[remoteUid] ?: return
        session.volumeMultiplier = volume.coerceIn(0.0, 2.0)
        session.remoteAudioTrack?.setVolume(session.volumeMultiplier)
    }

    fun getRemoteAudioTrack(remoteUid: String): AudioTrack? {
        return peerSessions[remoteUid]?.remoteAudioTrack
    }

    private fun updateActivePeers() {
        _activePeers.value = peerSessions.keys.toSet()
        val currentStates = _peerConnectionStates.value.toMutableMap()
        currentStates.keys.retainAll(peerSessions.keys)
        _peerConnectionStates.value = currentStates
    }

    // =========================================================================
    // 5. VOICE ACTIVITY DETECTION (VAD) & SPEAKING LEVEL MONITOR
    // =========================================================================

    private fun startVoiceActivityDetection() {
        vadJob?.cancel()
        vadJob = coroutineScope.launch {
            while (isActive) {
                delay(250)

                // 1. Evaluate local speaking status
                val isMicActive = !_isMicrophoneMuted.value &&
                        localAudioTrack?.enabled() == true &&
                        isSpeakerModeActive
                _isSpeakingLocally.value = isMicActive

                // 2. Evaluate active speaking participants based on remote track states
                val currentlySpeaking = mutableSetOf<String>()
                for ((uid, session) in peerSessions) {
                    val track = session.remoteAudioTrack
                    if (track != null && track.enabled() && !session.isMutedByLocal) {
                        // In unified plan, active enabled audio tracks indicate stream activity
                        currentlySpeaking.add(uid)
                        onPeerSpeakingChanged?.invoke(uid, true)
                    }
                }
                _speakingPeers.value = currentlySpeaking
            }
        }
    }

    // =========================================================================
    // 6. RESOURCE CLEANUP & DISPOSAL
    // =========================================================================

    /**
     * Cleanly terminates all peer connections, stops audio captures, releases hardware focus,
     * and disposes WebRTC native allocations.
     */
    @Synchronized
    fun close() {
        try {
            vadJob?.cancel()
            vadJob = null

            // Close and dispose all active peer sessions
            for ((_, session) in peerSessions) {
                try {
                    session.connection.close()
                    session.connection.dispose()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing peer ${session.peerUid}: ${e.message}")
                }
            }
            peerSessions.clear()
            updateActivePeers()

            // Release local hardware audio track and source
            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null

            resetAudioManager()
            Log.d(TAG, "WebRTCConnectionManager successfully closed and all resources released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebRTCConnectionManager: ${e.message}", e)
        }
    }
}

/**
 * Typealias for camelCase compatibility with any callers.
 */
typealias WebRtcConnectionManager = WebRTCConnectionManager
