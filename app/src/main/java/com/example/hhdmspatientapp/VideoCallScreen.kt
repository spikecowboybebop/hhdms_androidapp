package com.example.hhdmspatientapp

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import android.view.SurfaceView

@Composable
fun VideoCallScreen(
    appId: String,
    channelName: String,
    token: String,
    uid: Int,
    onEndCall: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(true) }
    var isVideoEnabled by remember { mutableStateOf(true) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var engine by remember { mutableStateOf<RtcEngine?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

        if (cameraPermission == PackageManager.PERMISSION_GRANTED && audioPermission == PackageManager.PERMISSION_GRANTED) {
            hasPermissions = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) return@LaunchedEffect

        try {
            val rtcEngine = RtcEngine.create(context, appId, object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d("VideoCall", "Remote user joined: $uid")
                    remoteUid = uid
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d("VideoCall", "Remote user offline: $uid")
                    remoteUid = null
                    remoteSurfaceView = null
                }

                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d("VideoCall", "Joined channel: $channel, uid: $uid")
                    engine?.startPreview()
                }

                override fun onLeaveChannel(stats: RtcStats?) {
                    Log.d("VideoCall", "Left channel")
                }
            })

            engine = rtcEngine

            rtcEngine.enableAudio()
            rtcEngine.enableVideo()
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            // Setup local video view
            val localView = SurfaceView(context)
            localSurfaceView = localView
            rtcEngine.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            rtcEngine.startPreview()

            // Join channel
            rtcEngine.joinChannel(token, channelName, null, uid)

        } catch (e: Exception) {
            Log.e("VideoCall", "Failed to initialize Agora engine: ${e.message}")
        }
    }

    // Update remote video view when remote user joins
    LaunchedEffect(remoteUid) {
        val currentEngine = engine ?: return@LaunchedEffect
        val uid = remoteUid ?: return@LaunchedEffect

        Log.d("VideoCall", "Setting up remote video for uid: $uid")
        val remoteView = SurfaceView(context)
        remoteSurfaceView = remoteView
        currentEngine.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        currentEngine.muteRemoteAudioStream(uid, false)
        currentEngine.muteRemoteVideoStream(uid, false)
    }

    // Update local video when toggled
    LaunchedEffect(isVideoEnabled) {
        engine?.let { e ->
            if (isVideoEnabled) {
                e.enableVideo()
                e.startPreview()
            } else {
                e.stopPreview()
                e.disableVideo()
            }
        }
    }

    // Update audio when toggled
    LaunchedEffect(isAudioEnabled) {
        engine?.muteLocalAudioStream(!isAudioEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            engine?.leaveChannel()
            engine?.stopPreview()
            RtcEngine.destroy()
            engine = null
        }
    }

    if (!hasPermissions) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Camera and microphone permissions are required for video calls",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Remote video (full screen)
        remoteSurfaceView?.let { view ->
            AndroidView(
                factory = { view },
                modifier = Modifier.fillMaxSize(),
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Waiting for specialist...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
            )
        }

        // Local video (picture-in-picture)
        localSurfaceView?.let { view ->
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
            )
        }

        // Status text
        Text(
            text = if (remoteUid != null) "Connected" else "Calling...",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )

        // Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.5f),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
            ) {
                FloatingActionButton(
                    onClick = { isAudioEnabled = !isAudioEnabled },
                    containerColor = if (isAudioEnabled) Color.White.copy(alpha = 0.2f) else Color(0xFFE53935),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Audio",
                        tint = Color.White,
                    )
                }

                FloatingActionButton(
                    onClick = { isVideoEnabled = !isVideoEnabled },
                    containerColor = if (isVideoEnabled) Color.White.copy(alpha = 0.2f) else Color(0xFFE53935),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Video",
                        tint = Color.White,
                    )
                }

                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color(0xFFE53935),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
