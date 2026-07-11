package com.example.hhdmspatientapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.ClinicalNavy
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleconsultScreen(
    sessionId: String,
    patientEmail: String,
    onHangUp: () -> Unit,
) {
    val context = LocalContext.current
    var callState by remember { mutableStateOf(TeleconsultCallState.JOINING) }
    var cameraOn by remember { mutableStateOf(true) }
    var micOn by remember { mutableStateOf(true) }
    var callDuration by remember { mutableStateOf(0) }
    var audioOnly by remember { mutableStateOf(false) }
    val remoteRendererRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    val localRendererRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        TeleconsultSignalingManager.onStateChange = { newState ->
            callState = newState
        }

        TeleconsultSignalingManager.onRemoteStreamReady = {
            val renderer = remoteRendererRef.value
            if (renderer != null) {
                TeleconsultSignalingManager.remoteVideoTrack?.addSink(renderer)
            }
        }

        TeleconsultSignalingManager.initialize(context)
        TeleconsultSignalingManager.startCall(sessionId, context)
        audioOnly = TeleconsultSignalingManager.isAudioOnly

        while (true) {
            delay(1000)
            if (callState == TeleconsultCallState.CONNECTED) {
                callDuration++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            TeleconsultSignalingManager.hangUp()
        }
    }

    val timerText = run {
        val min = callDuration / 60
        val sec = callDuration % 60
        "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Remote video (full screen)
            if (callState == TeleconsultCallState.CONNECTED) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            setMirror(false)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        }.also { renderer ->
                            remoteRendererRef.value = renderer
                            TeleconsultSignalingManager.remoteVideoTrack?.addSink(renderer)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Connecting / waiting overlay
            if (callState == TeleconsultCallState.JOINING || callState == TeleconsultCallState.WAITING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (callState == TeleconsultCallState.JOINING)
                                "Connecting..."
                            else
                                "Waiting for specialist...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Ended overlay
            if (callState == TeleconsultCallState.ENDED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Call Ended",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                TeleconsultSignalingManager.hangUp()
                                onHangUp()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClinicalNavy
                            )
                        ) {
                            Text("Close")
                        }
                    }
                }
            }

            // Local video preview (PIP bottom-right) — hidden in audio-only
            if (callState == TeleconsultCallState.CONNECTED && !audioOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 100.dp)
                        .size(width = 140.dp, height = 100.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                setMirror(true)
                                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            }.also { renderer ->
                                localRendererRef.value = renderer
                                TeleconsultSignalingManager.localVideoTrack?.addSink(renderer)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Status bar
            if (callState == TeleconsultCallState.CONNECTED) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "Live — $timerText",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        if (audioOnly) {
                            Text(
                                text = "Audio Only",
                                color = Color(0xFFFFA726),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Control bar
            if (callState != TeleconsultCallState.ENDED) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = ClinicalNavy,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera toggle — hidden in audio-only
                        if (!audioOnly) {
                            FilledIconToggleButton(
                                checked = cameraOn,
                                onCheckedChange = {
                                    cameraOn = !cameraOn
                                    TeleconsultSignalingManager.toggleCamera()
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = Color(0xFFFF5252),
                                    checkedContainerColor = Color(0xFF4CAF50)
                                ),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = if (cameraOn) "Turn off camera" else "Turn on camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Mic toggle
                        FilledIconToggleButton(
                            checked = micOn,
                            onCheckedChange = {
                                micOn = !micOn
                                TeleconsultSignalingManager.toggleMic()
                            },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = Color(0xFFFF5252),
                                checkedContainerColor = Color(0xFF4CAF50)
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (micOn) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (micOn) "Mute" else "Unmute",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Hangup button
                        FilledIconButton(
                            onClick = {
                                TeleconsultSignalingManager.hangUp()
                                onHangUp()
                            },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFE53935)
                            ),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End call",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
