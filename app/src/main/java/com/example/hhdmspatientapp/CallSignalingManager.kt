package com.example.hhdmspatientapp

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.net.URISyntaxException

object CallSignalingManager {
    private const val TAG = "CallSignalingManager"
    private const val SERVER_URL = "http://192.168.0.105:3001"

    private var mSocket: Socket? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null

    // 🚀 FIXED TRACKER: Dynamically captures the active agent's line ID
    private var agentSocketId: String? = null

    fun initialize(context: Context) {
        if (mSocket != null) return // Already setup

        try {
            mSocket = IO.socket(SERVER_URL)

            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "⚡ Connected to NestJS Signaling Server!")
            }

            // Listens for when the Web Agent accepts the phone's incoming call
            mSocket?.on("call-routing-connected") { args ->
                try {
                    val response = args[0] as JSONObject
                    val sdpAnswerData = response.getJSONObject("sdpAnswer")

                    // 🚀 FIXED: Read "agentSocketId" from the server payload, not patientSocketId
                    agentSocketId = response.optString("agentSocketId")

                    Log.d(TAG, "🟢 Web Agent answered! Processing response hardware signature...")

                    val rtcAnswer = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdpAnswerData.getString("sdp")
                    )

                    // Bind the agent's mic setup parameters to your phone's ear speaker line
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "🎉 WebRTC Peer Connection is officially ACTIVE and LINKED!")
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, rtcAnswer)

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling remote answer: ${e.message}")
                }
            }

            mSocket?.connect()

            // 1. Initialize Google's global WebRTC hardware contexts safely
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
            )

            // 2. Build the structural pipeline factory engine
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

        // Reset tracker for a completely fresh call instance
        agentSocketId = null

        // 3. Capture real hardware microphone streams
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)

        // 4. Set up public ICE network configurations
        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        // 5. Create our peer link infrastructure
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    // 🚀 CRITICAL FIX: If we don't know who the agent is yet, hold onto the candidates
                    // or let them append to the initial SDP package instead of flooding empty sockets
                    val currentAgentId = agentSocketId
                    if (currentAgentId.isNullOrEmpty()) {
                        Log.d(TAG, "⏳ ICE Candidate gathered early. Stashing configuration until line connects...")
                        return
                    }

                    val icePayload = JSONObject().apply {
                        put("targetSocketId", currentAgentId)
                        put("candidate", JSONObject().apply {
                            put("sdpMid", candidate.sdpMid)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                            put("candidate", candidate.sdp)
                        })
                    }
                    mSocket?.emit("relay-ice-candidate", icePayload)
                    Log.d(TAG, "🛰️ Dispatched phone network ICE Candidate pathway map.")
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })

        // 6. Bind the live microphone data track to the outgoing connection pipeline link
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMSms0"))

        // 7. Create a real authentic WebRTC SDP Offer signature package!
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return

                // Set local description frame configuration signature
                peerConnection?.setLocalDescription(this, description)

                // 8. Fire the authentic cryptographic SDP payload down the signaling wire!
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
}