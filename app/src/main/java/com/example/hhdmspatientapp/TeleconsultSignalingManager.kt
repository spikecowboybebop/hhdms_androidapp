package com.example.hhdmspatientapp

import android.content.Context
import android.media.AudioManager
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.net.URISyntaxException

enum class TeleconsultCallState {
    IDLE, JOINING, WAITING, CONNECTED, ENDED
}

object TeleconsultSignalingManager {
    private const val TAG = "TeleconsultSignaling"
    private const val SERVER_URL = "http://192.168.0.105:3001"

    private var mSocket: Socket? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null

    // These are set by the UI layer
    var localVideoTrack: VideoTrack? = null
    var remoteVideoTrack: VideoTrack? = null

    private var currentSessionId: String? = null
    private var cameraEnabled = true
    private var micEnabled = true
    var isAudioOnly = false
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isInRoom = false
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var audioManager: AudioManager? = null

    var onStateChange: ((TeleconsultCallState) -> Unit)? = null
    var onRemoteStreamReady: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (mSocket != null) return

        try {
            mSocket = IO.socket("$SERVER_URL/teleconsult")

            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to teleconsult signaling server")
            }

            mSocket?.on("teleconsult:room-joined") { args ->
                try {
                    val data = args[0] as JSONObject
                    val peerCount = data.getInt("peerCount")
                    Log.d(TAG, "Joined room, peers: $peerCount")
                    onStateChange?.invoke(TeleconsultCallState.WAITING)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling room-joined: ${e.message}")
                }
            }

            mSocket?.on("teleconsult:peer-joined") {
                Log.d(TAG, "Peer joined, creating offer")
                createOffer()
            }

            mSocket?.on("teleconsult:offer-received") { args ->
                try {
                    val data = args[0] as JSONObject
                    val sdpData = data.getJSONObject("sdp")
                    val rtcOffer = SessionDescription(
                        SessionDescription.Type.OFFER,
                        sdpData.getString("sdp")
                    )
                    Log.d(TAG, "Offer received, creating answer")
                    createAnswer(rtcOffer)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling offer: ${e.message}")
                }
            }

            mSocket?.on("teleconsult:answer-received") { args ->
                try {
                    val data = args[0] as JSONObject
                    val sdpData = data.getJSONObject("sdp")
                    val rtcAnswer = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdpData.getString("sdp")
                    )
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description set successfully, flushing ICE candidates")
                            flushPendingIceCandidates()
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            Log.e(TAG, "Failed to set remote description: $p0")
                        }
                    }, rtcAnswer)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling answer: ${e.message}")
                }
            }

            mSocket?.on("teleconsult:ice-candidate-received") { args ->
                try {
                    val data = args[0] as JSONObject
                    val candidateData = data.getJSONObject("candidate")
                    val candidate = IceCandidate(
                        candidateData.getString("sdpMid"),
                        candidateData.getInt("sdpMLineIndex"),
                        candidateData.getString("candidate")
                    )
                    if (peerConnection?.remoteDescription == null) {
                        Log.d(TAG, "Queuing ICE candidate (no remote description yet)")
                        pendingIceCandidates.add(candidate)
                    } else {
                        peerConnection?.addIceCandidate(candidate)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding ICE candidate: ${e.message}")
                }
            }

            mSocket?.on("teleconsult:peer-left") {
                Log.d(TAG, "Peer left the call")
                onStateChange?.invoke(TeleconsultCallState.ENDED)
                hangUp()
            }

            mSocket?.on("teleconsult:room-error") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.e(TAG, "Room error: ${data.getString("message")}")
                } catch (e: Exception) {
                    Log.e(TAG, "Room error")
                }
                onStateChange?.invoke(TeleconsultCallState.ENDED)
            }

            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
            )

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket.IO URI error: ${e.message}")
        }
    }

    fun startCall(sessionId: String, context: Context) {
        cleanupPeerConnection()
        currentSessionId = sessionId
        onStateChange?.invoke(TeleconsultCallState.JOINING)

        if (mSocket?.connected() != true) {
            mSocket?.connect()
        }

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = true

        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        try {
            val capturer = createCameraCapturer(context)
            videoCapturer = capturer
            if (capturer != null) {
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", null)
                videoSource = peerConnectionFactory?.createVideoSource(capturer.isScreencast)
                videoSource?.let { source ->
                    capturer.initialize(surfaceTextureHelper, context, source.capturerObserver)
                }
                capturer.startCapture(1280, 720, 30)
                localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
                isAudioOnly = false
            } else {
                Log.d(TAG, "No camera available, running audio-only")
                isAudioOnly = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera setup failed, running audio-only: ${e.message}")
            videoCapturer?.dispose()
            videoCapturer = null
            videoSource?.dispose()
            videoSource = null
            localVideoTrack = null
            isAudioOnly = true
        }

        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE state: ${state?.name}")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        onStateChange?.invoke(TeleconsultCallState.CONNECTED)
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        onStateChange?.invoke(TeleconsultCallState.ENDED)
                        hangUp()
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { sendIceCandidate(it) }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "Remote stream added")
            }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                streams?.firstOrNull()?.let { stream ->
                    stream.videoTracks.firstOrNull()?.let { track ->
                        remoteVideoTrack = track
                        onRemoteStreamReady?.invoke()
                    }
                }
            }
        })

        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("ARDAMSms0"))
        }
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("ARDAMSvs0"))
        }

        if (isInRoom) {
            mSocket?.emit("teleconsult:leave-room", JSONObject().apply {
                put("sessionId", currentSessionId ?: sessionId)
            })
        }
        mSocket?.emit("teleconsult:join-room", JSONObject().apply {
            put("sessionId", sessionId)
        })
        isInRoom = true
    }

    private fun createCameraCapturer(context: Context): Camera2Capturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                val capturer = enumerator.createCapturer(name, null) as? Camera2Capturer
                if (capturer != null) return capturer
            }
        }
        for (name in deviceNames) {
            val capturer = enumerator.createCapturer(name, null) as? Camera2Capturer
            if (capturer != null) return capturer
        }
        return null
    }

    private fun createOffer() {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set, sending offer")
                        currentSessionId?.let { sessionId ->
                            mSocket?.emit("teleconsult:offer", JSONObject().apply {
                                put("sessionId", sessionId)
                                put("sdp", JSONObject().apply {
                                    put("type", description.type.canonicalForm())
                                    put("sdp", description.description)
                                })
                            })
                        }
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "Set local description failed: $p0") }
                }, description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { Log.e(TAG, "Create offer failed: $p0") }
            override fun onSetFailure(p0: String?) {}
        }, mediaConstraints)
    }

    private fun createAnswer(offer: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set, creating answer")
                flushPendingIceCandidates()
                val mediaConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription?) {
                        if (description == null) return
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d(TAG, "Answer created, sending")
                                currentSessionId?.let { sessionId ->
                                    mSocket?.emit("teleconsult:answer", JSONObject().apply {
                                        put("sessionId", sessionId)
                                        put("sdp", JSONObject().apply {
                                            put("type", description.type.canonicalForm())
                                            put("sdp", description.description)
                                        })
                                    })
                                }
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) { Log.e(TAG, "Set answer failed: $p0") }
                        }, description)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) { Log.e(TAG, "Create answer failed: $p0") }
                    override fun onSetFailure(p0: String?) {}
                }, mediaConstraints)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) { Log.e(TAG, "Set remote offer failed: $p0") }
        }, offer)
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        currentSessionId?.let { sessionId ->
            mSocket?.emit("teleconsult:ice-candidate", JSONObject().apply {
                put("sessionId", sessionId)
                put("candidate", JSONObject().apply {
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                })
            })
        }
    }

    private fun flushPendingIceCandidates() {
        val batch = pendingIceCandidates.toList()
        pendingIceCandidates.clear()
        for (candidate in batch) {
            peerConnection?.addIceCandidate(candidate)
        }
    }

    fun toggleCamera() {
        cameraEnabled = !cameraEnabled
        localVideoTrack?.let {
            (it as org.webrtc.MediaStreamTrack).setEnabled(cameraEnabled)
        }
    }

    fun toggleMic() {
        micEnabled = !micEnabled
        localAudioTrack?.let {
            (it as org.webrtc.MediaStreamTrack).setEnabled(micEnabled)
        }
    }

    private fun cleanupPeerConnection() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false
        audioManager = null

        peerConnection?.close()
        peerConnection = null

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        videoSource?.dispose()
        videoSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        remoteVideoTrack?.dispose()
        remoteVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        currentSessionId = null
        cameraEnabled = true
        micEnabled = true
        pendingIceCandidates.clear()
    }

    fun hangUp() {
        try {
            currentSessionId?.let { sessionId ->
                mSocket?.emit("teleconsult:leave-room", JSONObject().apply {
                    put("sessionId", sessionId)
                })
            }
            isInRoom = false
            cleanupPeerConnection()
            Log.d(TAG, "Teleconsult resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during hangup: ${e.message}")
        }
    }

    fun isCameraEnabled() = cameraEnabled
    fun isMicEnabled() = micEnabled
}
