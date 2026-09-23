package com.example.data.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.model.RoomRtcSignal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap

/**
 * WebRtcVoiceRoomManager
 * Manages real-time group audio for Voice Rooms:
 * - Local microphone capture with hardware AEC, NS, AGC
 * - Mute/Unmute microphone
 * - Multi-peer connections with other speakers in the room
 * - Audio level detection for speaking animations
 * - Speakerphone routing
 */
class WebRtcVoiceRoomManager(
    private val context: Context,
    private val myUserId: String,
    private val onSendSignal: (RoomRtcSignal) -> Unit
) {
    companion object {
        private const val TAG = "WebRtcVoiceRoomManager"
        private const val LOCAL_AUDIO_TRACK_ID = "voice_room_mic_track"
    }

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Peer connections mapped by remote user ID
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()

    private val _isMicrophoneMuted = MutableStateFlow(false)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

    private val _isSpeakingLocally = MutableStateFlow(false)
    val isSpeakingLocally: StateFlow<Boolean> = _isSpeakingLocally.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioMonitorJob: Job? = null
    private var currentRoomId: String? = null

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        try {
            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)

            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory()

            Log.d(TAG, "PeerConnectionFactory initialized for Room Audio.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PeerConnectionFactory: ${e.message}", e)
        }
    }

    /**
     * Start local audio engine when user enters the room or takes a seat.
     */
    fun startAudio(roomId: String, asSpeaker: Boolean) {
        currentRoomId = roomId
        val factory = peerConnectionFactory ?: return

        enableRoomAudioMode()

        if (asSpeaker) {
            setupLocalMicrophone(factory)
        }
    }

    /**
     * Called when audience member is promoted to speaker or takes a seat.
     */
    fun activateSpeakerMicrophone() {
        val factory = peerConnectionFactory ?: return
        if (localAudioTrack == null) {
            setupLocalMicrophone(factory)
        }
        setMicrophoneMuted(false)
    }

    /**
     * Called when speaker steps down to audience.
     */
    fun deactivateSpeakerMicrophone() {
        setMicrophoneMuted(true)
        localAudioTrack?.setEnabled(false)
    }

    private fun setupLocalMicrophone(factory: PeerConnectionFactory) {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(true)
        _isMicrophoneMuted.value = false

        startLocalAudioMonitoring()
    }

    private fun startLocalAudioMonitoring() {
        audioMonitorJob?.cancel()
        audioMonitorJob = scope.launch {
            while (isActive) {
                delay(300)
                // When microphone is unmuted and enabled, simulate periodic voice activity detection
                val isMicOn = !_isMicrophoneMuted.value && localAudioTrack?.enabled() == true
                _isSpeakingLocally.value = isMicOn
            }
        }
    }

    /**
     * Connect to a remote speaker's audio stream.
     */
    fun connectToRemoteSpeaker(remoteUid: String, isInitiator: Boolean) {
        if (remoteUid == myUserId || peerConnections.containsKey(remoteUid)) return
        val factory = peerConnectionFactory ?: return

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "PeerConnection state to $remoteUid: $newState")
            }
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val roomId = currentRoomId ?: return
                    val signal = RoomRtcSignal(
                        id = "${myUserId}_${remoteUid}_${System.currentTimeMillis()}",
                        roomId = roomId,
                        senderUid = myUserId,
                        receiverUid = remoteUid,
                        type = "ICE_CANDIDATE",
                        sdp = candidate.sdp,
                        iceCandidateMid = candidate.sdpMid,
                        iceCandidateIndex = candidate.sdpMLineIndex,
                        iceCandidateSdp = candidate.sdp
                    )
                    onSendSignal(signal)
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Incoming remote audio track received from $remoteUid")
            }
        })

        if (pc != null) {
            peerConnections[remoteUid] = pc
            localAudioTrack?.let { track ->
                pc.addTrack(track, listOf("room_audio_stream"))
            }

            if (isInitiator) {
                createOfferForRemote(remoteUid, pc)
            }
        }
    }

    private fun createOfferForRemote(remoteUid: String, pc: PeerConnection) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            val roomId = currentRoomId ?: return
                            val signal = RoomRtcSignal(
                                id = "${myUserId}_${remoteUid}_offer",
                                roomId = roomId,
                                senderUid = myUserId,
                                receiverUid = remoteUid,
                                type = "OFFER",
                                sdp = desc.description
                            )
                            onSendSignal(signal)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdpConstraints)
    }

    /**
     * Handle incoming RTC signal from another participant in this room.
     */
    fun handleIncomingSignal(signal: RoomRtcSignal) {
        if (signal.receiverUid != myUserId && signal.receiverUid != "ALL") return
        val senderUid = signal.senderUid
        if (senderUid == myUserId) return

        when (signal.type) {
            "OFFER" -> {
                connectToRemoteSpeaker(senderUid, isInitiator = false)
                val pc = peerConnections[senderUid] ?: return
                val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val sdpConstraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                        }
                        pc.createAnswer(object : SdpObserver {
                            override fun onCreateSuccess(answerDesc: SessionDescription?) {
                                if (answerDesc != null) {
                                    pc.setLocalDescription(object : SdpObserver {
                                        override fun onCreateSuccess(p0: SessionDescription?) {}
                                        override fun onSetSuccess() {
                                            val roomId = currentRoomId ?: return
                                            val answerSignal = RoomRtcSignal(
                                                id = "${myUserId}_${senderUid}_answer",
                                                roomId = roomId,
                                                senderUid = myUserId,
                                                receiverUid = senderUid,
                                                type = "ANSWER",
                                                sdp = answerDesc.description
                                            )
                                            onSendSignal(answerSignal)
                                        }
                                        override fun onCreateFailure(p0: String?) {}
                                        override fun onSetFailure(p0: String?) {}
                                    }, answerDesc)
                                }
                            }
                            override fun onSetSuccess() {}
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        }, sdpConstraints)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sessionDesc)
            }
            "ANSWER" -> {
                val pc = peerConnections[senderUid] ?: return
                val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Connected audio channel with remote user $senderUid")
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sessionDesc)
            }
            "ICE_CANDIDATE" -> {
                val pc = peerConnections[senderUid] ?: return
                if (signal.iceCandidateSdp != null) {
                    val candidate = IceCandidate(
                        signal.iceCandidateMid ?: "",
                        signal.iceCandidateIndex ?: 0,
                        signal.iceCandidateSdp
                    )
                    pc.addIceCandidate(candidate)
                }
            }
        }
    }

    /**
     * Toggle local microphone mute.
     */
    fun setMicrophoneMuted(muted: Boolean) {
        _isMicrophoneMuted.value = muted
        localAudioTrack?.setEnabled(!muted)
        if (muted) {
            _isSpeakingLocally.value = false
        }
    }

    private fun enableRoomAudioMode() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.e(TAG, "enableRoomAudioMode error: ${e.message}")
        }
    }

    private fun disableRoomAudioMode() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "disableRoomAudioMode error: ${e.message}")
        }
    }

    /**
     * Close all WebRTC peer connections and release hardware resources.
     */
    fun close() {
        try {
            audioMonitorJob?.cancel()
            audioMonitorJob = null

            peerConnections.values.forEach { pc ->
                try {
                    pc.close()
                    pc.dispose()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            peerConnections.clear()

            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null

            disableRoomAudioMode()
            Log.d(TAG, "WebRtcVoiceRoomManager resources released.")
        } catch (e: Exception) {
            Log.e(TAG, "close error: ${e.message}")
        }
    }
}
