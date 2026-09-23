package com.example.data.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.model.IceCandidatePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*

class WebRtcCallManager(
    private val context: Context,
    private val onIceCandidateGenerated: (IceCandidatePayload) -> Unit,
    private val onConnectionStateChanged: (PeerConnection.PeerConnectionState) -> Unit = {}
) {
    companion object {
        private const val TAG = "WebRtcCallManager"
        private const val AUDIO_TRACK_ID = "voice_call_audio_track"
    }

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var isSpeakerphoneOn = false
    private var isMicrophoneMuted = false

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

            Log.d(TAG, "PeerConnectionFactory initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PeerConnectionFactory: ${e.message}", e)
        }
    }

    /**
     * Start WebRTC audio engine and create PeerConnection.
     */
    fun setupCall(isCaller: Boolean) {
        val factory = peerConnectionFactory ?: return

        // Configure audio constraints with echo cancellation & noise suppression
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(true)

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange: $state")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "onConnectionChange: $newState")
                newState?.let { onConnectionStateChanged(it) }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: $newState")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val payload = IceCandidatePayload(
                        sdpMid = candidate.sdpMid ?: "",
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        sdp = candidate.sdp,
                        timestamp = System.currentTimeMillis()
                    )
                    onIceCandidateGenerated(payload)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "onAddStream with audio tracks: ${stream?.audioTracks?.size}")
            }

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack: Remote audio stream received!")
            }
        })

        // Add local microphone audio track to peer connection
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf("stream_audio_call"))
        }

        // Setup audio routing
        enableCallAudioMode()
    }

    /**
     * Create SDP Offer (Caller side).
     */
    fun createOffer(onOfferCreated: (SessionDescription) -> Unit) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Caller local description set successfully.")
                            onOfferCreated(desc)
                        }
                        override fun onCreateFailure(err: String?) {}
                        override fun onSetFailure(err: String?) {
                            Log.e(TAG, "Caller setLocalDescription failed: $err")
                        }
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "createOffer failed: $err")
            }
            override fun onSetFailure(err: String?) {}
        }, sdpConstraints)
    }

    /**
     * Set Remote Offer and Create SDP Answer (Receiver side).
     */
    fun createAnswer(remoteSdpOffer: String, onAnswerCreated: (SessionDescription) -> Unit) {
        val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, remoteSdpOffer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Receiver remote description set successfully.")
                val sdpConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                }
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerDesc: SessionDescription?) {
                        if (answerDesc != null) {
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    Log.d(TAG, "Receiver local description set successfully.")
                                    onAnswerCreated(answerDesc)
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(err: String?) {
                                    Log.e(TAG, "Receiver setLocalDescription failed: $err")
                                }
                            }, answerDesc)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(err: String?) {
                        Log.e(TAG, "createAnswer failed: $err")
                    }
                    override fun onSetFailure(err: String?) {}
                }, sdpConstraints)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "Receiver setRemoteDescription failed: $err")
            }
        }, sessionDesc)
    }

    /**
     * Set Remote Answer (Caller side after Receiver accepts).
     */
    fun setRemoteAnswer(remoteSdpAnswer: String) {
        val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, remoteSdpAnswer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Caller remote description set successfully. Peer connection established!")
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "Caller setRemoteDescription failed: $err")
            }
        }, sessionDesc)
    }

    /**
     * Add received ICE candidate.
     */
    fun addRemoteIceCandidate(payload: IceCandidatePayload) {
        try {
            val candidate = IceCandidate(payload.sdpMid, payload.sdpMLineIndex, payload.sdp)
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding remote ice candidate: ${e.message}")
        }
    }

    /**
     * Toggle microphone mute.
     */
    fun setMicrophoneMuted(muted: Boolean) {
        isMicrophoneMuted = muted
        localAudioTrack?.setEnabled(!muted)
        Log.d(TAG, "Microphone muted: $muted")
    }

    /**
     * Toggle speakerphone vs earpiece.
     */
    fun setSpeakerphoneOn(speakerOn: Boolean) {
        isSpeakerphoneOn = speakerOn
        audioManager.isSpeakerphoneOn = speakerOn
        Log.d(TAG, "Speakerphone: $speakerOn")
    }

    private fun enableCallAudioMode() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring AudioManager: ${e.message}")
        }
    }

    private fun disableCallAudioMode() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting AudioManager: ${e.message}")
        }
    }

    /**
     * Release all WebRTC and Audio hardware resources.
     */
    fun close() {
        try {
            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            disableCallAudioMode()
            Log.d(TAG, "WebRtcCallManager closed and resources released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebRtcCallManager: ${e.message}")
        }
    }
}
