package com.example.hhdmspatientapp

import android.content.Context
import android.media.AudioManager
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.net.URISyntaxException

object CallSignalingManager {
    private const val TAG = "CallSignalingManager"
    private const val SERVER_URL = "http://192.168.0.101:3001"

    private var mSocket: Socket? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var audioManager: AudioManager? = null

    // Trackers
    private var agentSocketId: String? = null
    private var currentPatientEmail: String? = null
    private val earlyIceCandidates = ArrayList<IceCandidate>()

    // Callback for UI state updates
    var onCallStateChange: ((CallStatus) -> Unit)? = null

    // Video call callbacks
    var onIncomingVideoCall: ((specialistName: String, sessionId: String) -> Unit)? = null
    var onVideoCallReady: ((token: String, appId: String, channelName: String, uid: Int) -> Unit)? = null
    var onVideoCallEnded: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (mSocket != null) return

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            mSocket = IO.socket(SERVER_URL)

            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "=== Connected to NestJS Signaling Server! Socket ID: ${mSocket?.id()} ===")
            }

            mSocket?.on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "=== DISCONNECTED from Signaling Server ===")
            }

            mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "=== SOCKET CONNECTION ERROR: ${args.firstOrNull()} ===")
            }

            mSocket?.on("call-routing-connected") { args ->
                try {
                    val response = args[0] as JSONObject
                    val sdpAnswerData = response.getJSONObject("sdpAnswer")

                    agentSocketId = response.optString("agentSocketId")
                    val currentAgentId = agentSocketId

                    Log.d(TAG, "Web Agent answered! Processing response...")

                    configureAudioHardwareForCall(true)

                    val rtcAnswer = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdpAnswerData.getString("sdp")
                    )

                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "WebRTC Peer Connection is ACTIVE!")

                            onCallStateChange?.invoke(CallStatus.CONNECTED)

                            if (!currentAgentId.isNullOrEmpty()) {
                                synchronized(earlyIceCandidates) {
                                    Log.d(TAG, "Sending ${earlyIceCandidates.size} stashed ICE candidates to Agent...")
                                    for (candidate in earlyIceCandidates) {
                                        sendIceCandidateToAgent(currentAgentId, candidate)
                                    }
                                    earlyIceCandidates.clear()
                                }
                            }
                        }
                        override fun onCreateFailure(p0: String?) { Log.e(TAG, "Remote Description Failure: $p0") }
                        override fun onSetFailure(p0: String?) { Log.e(TAG, "Remote Description Set Failure: $p0") }
                    }, rtcAnswer)

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling remote answer: ${e.message}")
                }
            }

            mSocket?.on("remote-ice-candidate") { args ->
                try {
                    val data = args[0] as JSONObject
                    if (data.has("candidate")) {
                        val candidateObj = data.getJSONObject("candidate")
                        val iceCandidate = IceCandidate(
                            candidateObj.getString("sdpMid"),
                            candidateObj.getInt("sdpMLineIndex"),
                            candidateObj.getString("candidate")
                        )
                        peerConnection?.addIceCandidate(iceCandidate)
                        Log.d(TAG, "Successfully appended remote ICE candidate.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing incoming remote ICE candidate: ${e.message}")
                }
            }

            mSocket?.on("call-ended") {
                Log.d(TAG, "Remote party hung up the call!")
                hangUpActiveCall()
                onCallStateChange?.invoke(CallStatus.IDLE)
            }

            // Video call events
            mSocket?.on("video-call:ringing") { args ->
                try {
                    Log.d(TAG, "=== video-call:ringing RECEIVED === args.size=${args.size}")
                    val data = args[0] as JSONObject
                    Log.d(TAG, "video-call:ringing data: $data")
                    val sessionId = data.getString("sessionId")
                    val specialistName = data.optString("specialistName", "Specialist")
                    val channelName = data.optString("channelName", "")
                    Log.d(TAG, "Incoming video call: session=$sessionId from=$specialistName channel=$channelName")
                    Log.d(TAG, "onIncomingVideoCall callback is ${if (onIncomingVideoCall != null) "SET" else "NULL"}")
                    onIncomingVideoCall?.invoke(specialistName, sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing video-call:ringing: ${e.message}", e)
                }
            }

            mSocket?.on("video-call:ready") { args ->
                try {
                    Log.d(TAG, "=== video-call:ready RECEIVED ===")
                    val data = args[0] as JSONObject
                    val token = data.getString("token")
                    val appId = data.getString("appId")
                    val channelName = data.getString("channelName")
                    val uid = data.optInt("uid", 2)
                    Log.d(TAG, "Video call ready: channel=$channelName uid=$uid appId=$appId")
                    onVideoCallReady?.invoke(token, appId, channelName, uid)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing video-call:ready: ${e.message}", e)
                }
            }

            mSocket?.on("video-call:end") {
                Log.d(TAG, "Video call ended by remote")
                onVideoCallEnded?.invoke()
            }

            mSocket?.connect()

            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
            )

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()

        } catch (e: Exception) {
            Log.e(TAG, "WebRTC Initialization crash: ${e.message}")
        }
    }

    fun startEmergencyCall(patientEmail: String) {
        if (mSocket?.connected() != true) {
            Log.e(TAG, "Cannot dial out. Socket is offline!")
            return
        }

        hangUpActiveCall()
        currentPatientEmail = patientEmail

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
        }
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State Changed: ${state?.name}")
                when (state) {
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        Log.d(TAG, "ICE connection lost or failed. Cleaning up...")
                        hangUpActiveCall()
                        onCallStateChange?.invoke(CallStatus.IDLE)
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val currentAgentId = agentSocketId

                    if (currentAgentId.isNullOrEmpty()) {
                        synchronized(earlyIceCandidates) {
                            earlyIceCandidates.add(candidate)
                        }
                        Log.d(TAG, "ICE Candidate gathered early. Stashed.")
                    } else {
                        sendIceCandidateToAgent(currentAgentId, candidate)
                    }
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "Remote WebRTC Audio Stream detected. Attaching...")
            }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })

        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMSms0"))

        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description bound successfully.")
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "Failed to bind local description: $p0") }
                }, description)

                val dialPayload = JSONObject().apply {
                    put("patientEmail", patientEmail)
                    put("sdpOffer", JSONObject().apply {
                        put("type", "offer")
                        put("sdp", description.description)
                    })
                }
                mSocket?.emit("call-center-dial", dialPayload)
                Log.d(TAG, "WebRTC call offer fired down the wire!")
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { Log.e(TAG, "SDP Creation Failed: $p0") }
            override fun onSetFailure(p0: String?) {}
        }, mediaConstraints)
    }

    private fun configureAudioHardwareForCall(activate: Boolean) {
        try {
            audioManager?.let { am ->
                if (activate) {
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    am.isSpeakerphoneOn = true
                    Log.d(TAG, "Android system hardware switched to MODE_IN_COMMUNICATION.")
                } else {
                    am.mode = AudioManager.MODE_NORMAL
                    am.isSpeakerphoneOn = false
                    Log.d(TAG, "Android system hardware returned to MODE_NORMAL.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure phone audio hardware: ${e.message}")
        }
    }

    fun hangUpActiveCall() {
        try {
            mSocket?.emit("end-call", JSONObject().apply {
                put("targetSocketId", agentSocketId ?: "")
            })

            configureAudioHardwareForCall(false)

            agentSocketId = null
            currentPatientEmail = null
            synchronized(earlyIceCandidates) {
                earlyIceCandidates.clear()
            }
            peerConnection?.close()
            peerConnection = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null
            Log.d(TAG, "Call resources cleaned up.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up WebRTC resources: ${e.message}")
        }
    }

    fun destroy() {
        hangUpActiveCall()
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
    }

    // ── Video Call Methods ──

    fun startVideoCall(referralId: String, patientId: String, specialistName: String) {
        if (mSocket?.connected() != true) {
            Log.e(TAG, "Cannot start video call. Socket is offline!")
            return
        }
        mSocket?.emit("video-call:start", JSONObject().apply {
            put("referralId", referralId)
            put("patientId", patientId)
            put("specialistName", specialistName)
        })
        Log.d(TAG, "Emitted video-call:start for referral=$referralId")
    }

    fun registerPatient(patientId: String) {
        if (mSocket?.connected() != true) {
            Log.w(TAG, "Cannot register patient. Socket offline. Will retry on connect.")
            mSocket?.once(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected — registering patient $patientId")
                mSocket?.emit("register:patient", JSONObject().apply {
                    put("patientId", patientId)
                })
            }
            return
        }
        mSocket?.emit("register:patient", JSONObject().apply {
            put("patientId", patientId)
        })
        Log.d(TAG, "Registered patient $patientId with video call gateway")
    }

    fun acceptVideoCall(sessionId: String) {
        if (mSocket?.connected() != true) {
            Log.e(TAG, "Cannot accept video call. Socket is offline!")
            return
        }
        mSocket?.emit("video-call:accept", JSONObject().apply {
            put("sessionId", sessionId)
        })
        Log.d(TAG, "Emitted video-call:accept for session=$sessionId")
    }

    fun declineVideoCall(sessionId: String) {
        if (mSocket?.connected() != true) return
        mSocket?.emit("video-call:decline", JSONObject().apply {
            put("sessionId", sessionId)
        })
        Log.d(TAG, "Emitted video-call:decline for session=$sessionId")
    }

    fun endVideoCall(sessionId: String) {
        if (mSocket?.connected() != true) return
        mSocket?.emit("video-call:end", JSONObject().apply {
            put("sessionId", sessionId)
        })
        Log.d(TAG, "Emitted video-call:end for session=$sessionId")
    }

    private fun sendIceCandidateToAgent(agentId: String, candidate: IceCandidate) {
        try {
            val icePayload = JSONObject().apply {
                put("targetSocketId", agentId)
                put("candidate", JSONObject().apply {
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                })
            }
            mSocket?.emit("relay-ice-candidate", icePayload)
            Log.d(TAG, "Dispatched phone ICE candidate.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build ICE payload: ${e.message}")
        }
    }
}
