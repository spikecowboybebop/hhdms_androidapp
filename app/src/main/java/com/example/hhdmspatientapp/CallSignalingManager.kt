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

    // 📍 TRACKERS
    private var agentSocketId: String? = null
    // Holding pen for early network pathways generated before agent accepts
    private val earlyIceCandidates = ArrayList<IceCandidate>()

    fun initialize(context: Context) {
        if (mSocket != null) return // Already setup

        // Initialize Android's native Hardware AudioManager reference container
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            mSocket = IO.socket(SERVER_URL)

            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "🔌 Connected to NestJS Signaling Server!")
            }

            // Listens for when the Web Agent accepts the phone's incoming call
            mSocket?.on("call-routing-connected") { args ->
                try {
                    val response = args[0] as JSONObject
                    val sdpAnswerData = response.getJSONObject("sdpAnswer")

                    // Capture the real agent socket ID
                    agentSocketId = response.optString("agentSocketId")
                    val currentAgentId = agentSocketId

                    Log.d(TAG, "🎙️ Web Agent answered! Processing response hardware signature...")

                    // 🚀 CRITICAL FIX: Route incoming stream packets straight to the physical speakers
                    configureAudioHardwareForCall(true)

                    val rtcAnswer = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdpAnswerData.getString("sdp")
                    )

                    // Bind the agent's mic setup parameters to your phone's ear speaker line
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "✅ WebRTC Peer Connection is officially ACTIVE and LINKED!")

                            // Flush out any stashed candidates immediately down the active line
                            if (!currentAgentId.isNullOrEmpty()) {
                                synchronized(earlyIceCandidates) {
                                    Log.d(TAG, "🛰️ Sending ${earlyIceCandidates.size} stashed ICE candidates to Agent...")
                                    for (candidate in earlyIceCandidates) {
                                        sendIceCandidateToAgent(currentAgentId, candidate)
                                    }
                                    earlyIceCandidates.clear() // Clean up memory allocation
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
                        Log.d(TAG, "🛰️ Successfully appended network pathway from Web Agent onto native hardware layout.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing incoming remote ICE candidate: ${e.message}")
                }
            }

            mSocket?.connect()

            // Initialize Google's global WebRTC hardware contexts safely
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
            )

            // Build the structural pipeline factory engine
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

        // Make sure previous connections are cleanly purged from device memory first
        hangUpActiveCall()

        // Capture real hardware microphone streams
        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        // Set up public ICE network configurations
        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        // Create our peer link infrastructure
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "📶 ICE Connection State Changed: ${state?.name}")
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val currentAgentId = agentSocketId

                    if (currentAgentId.isNullOrEmpty()) {
                        // If the agent hasn't clicked accept yet, cache it safely
                        synchronized(earlyIceCandidates) {
                            earlyIceCandidates.add(candidate)
                        }
                        Log.d(TAG, "📦 ICE Candidate gathered early. Stashed safety config framework.")
                    } else {
                        // If agent is already connected, send it over right away!
                        sendIceCandidateToAgent(currentAgentId, candidate)
                    }
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "🎵 Remote WebRTC Audio Stream detected from Agent. Attaching...")
            }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })

        // Bind the live microphone data track to the outgoing connection pipeline link
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMSms0"))

        // Create a real authentic WebRTC SDP Offer signature package!
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "📝 Local description frame signature bound successfully.")
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "Failed to bind local description: $p0") }
                }, description)

                // Fire the authentic cryptographic SDP payload down the signaling wire!
                val dialPayload = JSONObject().apply {
                    put("patientEmail", patientEmail)
                    put("sdpOffer", JSONObject().apply {
                        put("type", "offer")
                        put("sdp", description.description)
                    })
                }
                mSocket?.emit("call-center-dial", dialPayload)
                Log.d(TAG, "🚀 Real cryptographic WebRTC call offer fired down the wire!")
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { Log.e(TAG, "SDP Creation Failed: $p0") }
            override fun onSetFailure(p0: String?) {}
        }, mediaConstraints)
    }

    /**
     * 🚀 NEW: Explicit Audio Hardware Routing Manager
     * Switches the system audio layer from normal multimedia mode into high-priority VoIP mode.
     */
    private fun configureAudioHardwareForCall(activate: Boolean) {
        try {
            audioManager?.let { am ->
                if (activate) {
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    am.isSpeakerphoneOn = true // Routes to outer speaker layout for seamless testing
                    Log.d(TAG, "🔊 Android system hardware successfully hijacked into MODE_IN_COMMUNICATION.")
                } else {
                    am.mode = AudioManager.MODE_NORMAL
                    am.isSpeakerphoneOn = false
                    Log.d(TAG, "🔇 Android system hardware returned safely back to MODE_NORMAL status.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure phone device hardware channels: ${e.message}")
        }
    }

    /**
     * Dynamic Cleanup Routine
     * Ensures absolute execution hygiene so subsequent redials clear media tracks cleanly.
     */
    fun hangUpActiveCall() {
        try {
            // Restore native hardware routing controls safely
            configureAudioHardwareForCall(false)

            agentSocketId = null
            synchronized(earlyIceCandidates) {
                earlyIceCandidates.clear()
            }
            peerConnection?.close()
            peerConnection = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            audioSource?.dispose()
            audioSource = null
            Log.d(TAG, "🧼 Call structural components destroyed. Hardware paths reset.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up WebRTC resources: ${e.message}")
        }
    }

    // Helper function to format and send candidates uniformly over Socket.io
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
            Log.d(TAG, "📡 Dispatched phone network ICE Candidate pathway map.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build ICE payload: ${e.message}")
        }
    }
}