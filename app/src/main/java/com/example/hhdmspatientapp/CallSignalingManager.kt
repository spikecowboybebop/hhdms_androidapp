package com.example.hhdmspatientapp

import android.content.Context
import android.media.AudioManager
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.Timer
import java.util.TimerTask

object CallSignalingManager {
    private const val TAG = "CallSignalingManager"
    private const val SERVER_URL = "https://hhdms-api.onrender.com"

    private var mSocket: Socket? = null
    private var appContext: Context? = null

    // Agora voice call (call center) engine
    private var voiceRtcEngine: RtcEngine? = null
    private var activeVoiceSessionId: String? = null
    private var currentVoiceChannel: String? = null

    // Connection state
    private var isConnected = false
    private var pendingPatientId: String? = null
    private var registeredPatientId: String? = null
    private var reconnectTimer: Timer? = null
    private const val MAX_RECONNECT_DELAY_MS = 30_000L
    private var currentReconnectDelayMs = 2_000L

    // Callback for UI state updates
    var onCallStateChange: ((CallStatus) -> Unit)? = null

    // Video call callbacks
    var onIncomingVideoCall: ((specialistName: String, sessionId: String) -> Unit)? = null
    var onVideoCallReady: ((token: String, appId: String, channelName: String, uid: Int) -> Unit)? = null
    var onVideoCallEnded: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (mSocket != null) return

        appContext = context.applicationContext

        try {
            val opts = IO.Options()
            opts.reconnection = true
            opts.reconnectionAttempts = 999
            opts.reconnectionDelay = 2000
            opts.forceNew = false
            opts.multiplex = true

            mSocket = IO.socket(SERVER_URL, opts)

            mSocket?.on(Socket.EVENT_CONNECT) {
                isConnected = true
                currentReconnectDelayMs = 2000L
                reconnectTimer?.cancel()
                reconnectTimer = null
                Log.d(TAG, "=== Connected to NestJS Signaling Server! Socket ID: ${mSocket?.id()} ===")

                val pid = pendingPatientId
                if (pid != null) {
                    Log.d(TAG, "Auto-registering pending patient $pid")
                    mSocket?.emit("register:patient", JSONObject().apply {
                        put("patientId", pid)
                    })
                    pendingPatientId = null
                }
            }

            mSocket?.on(Socket.EVENT_DISCONNECT) {
                isConnected = false
                Log.w(TAG, "=== DISCONNECTED from Signaling Server ===")
                scheduleReconnect()
            }

            mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                isConnected = false
                Log.e(TAG, "=== SOCKET CONNECTION ERROR: ${args.firstOrNull()} ===")
                scheduleReconnect()
            }

            // ── Voice call (call center) events ──

            mSocket?.on("voice-call:ready") { args ->
                try {
                    Log.d(TAG, "=== voice-call:ready RECEIVED ===")
                    val data = args[0] as JSONObject
                    val token = data.getString("token")
                    val appId = data.getString("appId")
                    val channelName = data.getString("channelName")
                    val uid = data.optInt("uid", 2)
                    val sessionId = data.optString("sessionId")
                    joinVoiceCallChannel(token, appId, channelName, uid, sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing voice-call:ready: ${e.message}", e)
                }
            }

            mSocket?.on("voice-call:end") {
                Log.d(TAG, "Voice call ended by agent/server")
                teardownVoiceCall()
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
        } catch (e: Exception) {
            Log.e(TAG, "Socket initialization crash: ${e.message}")
        }
    }

    /**
     * Patient dials the call center. Media is carried over Agora; only the
     * session setup is signaled over Socket.IO.
     */
    fun startEmergencyCall(patientEmail: String) {
        if (mSocket?.connected() != true) {
            Log.e(TAG, "Cannot dial out. Socket is offline!")
            return
        }

        teardownVoiceCall()
        onCallStateChange?.invoke(CallStatus.RINGING)

        mSocket?.emit("voice-call:start", JSONObject().apply {
            put("patientId", registeredPatientId ?: "")
            put("patientEmail", patientEmail)
            put("patientName", patientEmail.substringBefore("@"))
        })
        Log.d(TAG, "Emitted voice-call:start for $patientEmail")
    }

    private fun joinVoiceCallChannel(token: String, appId: String, channelName: String, uid: Int, sessionId: String) {
        try {
            val ctx = appContext ?: return
            Log.d(TAG, "Joining Agora voice channel=$channelName uid=$uid appId=$appId")

            val engine = RtcEngine.create(ctx, appId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(TAG, "Voice call joined channel: $channel, uid: $uid")
                    onCallStateChange?.invoke(CallStatus.CONNECTED)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(TAG, "Voice call remote user offline: $uid")
                    teardownVoiceCall()
                    onCallStateChange?.invoke(CallStatus.IDLE)
                }

                override fun onLeaveChannel(stats: RtcStats?) {
                    Log.d(TAG, "Voice call left channel")
                }

                override fun onError(err: Int) {
                    Log.e(TAG, "Voice call Agora error: $err")
                }
            })
            voiceRtcEngine = engine

            engine.enableAudio()
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            // Loud, clear voice tuning for the emergency call
            engine.setAudioProfile(
                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                Constants.AUDIO_SCENARIO_GAME_STREAMING,
            )
            engine.adjustRecordingSignalVolume(400)
            engine.adjustPlaybackSignalVolume(400)
            engine.setDefaultAudioRoutetoSpeakerphone(true)

            val result = engine.joinChannel(token, channelName, null, uid)
            if (result == 0) {
                activeVoiceSessionId = sessionId
                currentVoiceChannel = channelName
            } else {
                Log.e(TAG, "Voice call joinChannel failed with code: $result")
                teardownVoiceCall()
                onCallStateChange?.invoke(CallStatus.IDLE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora voice engine: ${e.message}")
            teardownVoiceCall()
            onCallStateChange?.invoke(CallStatus.IDLE)
        }
    }

    fun hangUpActiveCall() {
        val sessionId = activeVoiceSessionId
        if (sessionId != null && mSocket?.connected() == true) {
            mSocket?.emit("voice-call:end", JSONObject().apply {
                put("sessionId", sessionId)
            })
            Log.d(TAG, "Emitted voice-call:end for session=$sessionId")
        }
        teardownVoiceCall()
        onCallStateChange?.invoke(CallStatus.IDLE)
    }

    private fun teardownVoiceCall() {
        try {
            voiceRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            voiceRtcEngine = null
            activeVoiceSessionId = null
            currentVoiceChannel = null
            Log.d(TAG, "Voice call Agora resources cleaned up.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Agora voice resources: ${e.message}")
        }
    }

    fun destroy() {
        reconnectTimer?.cancel()
        reconnectTimer = null
        teardownVoiceCall()
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
    }

    private fun scheduleReconnect() {
        if (reconnectTimer != null) return
        reconnectTimer = Timer("SocketReconnect").apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (mSocket != null && !isConnected) {
                        Log.d(TAG, "Attempting reconnect in ${currentReconnectDelayMs}ms...")
                        try {
                            mSocket?.connect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Reconnect attempt failed: ${e.message}")
                        }
                    }
                }
            }, currentReconnectDelayMs)
        }
        currentReconnectDelayMs = (currentReconnectDelayMs * 1.5).toLong().coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    fun forceReconnect() {
        Log.d(TAG, "Force reconnecting...")
        reconnectTimer?.cancel()
        reconnectTimer = null
        currentReconnectDelayMs = 2000L
        try {
            mSocket?.disconnect()
            mSocket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Force reconnect failed: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected && mSocket?.connected() == true

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
        registeredPatientId = patientId
        pendingPatientId = patientId
        if (mSocket?.connected() != true) {
            Log.w(TAG, "Socket offline. Will register patient $patientId on connect.")
            if (mSocket == null) {
                Log.e(TAG, "Socket is null — cannot register")
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
}
