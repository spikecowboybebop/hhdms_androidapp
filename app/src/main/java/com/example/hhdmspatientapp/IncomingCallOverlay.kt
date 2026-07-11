package com.example.hhdmspatientapp

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.ClinicalNavy
import kotlinx.coroutines.delay

data class IncomingCallInfo(
    val sessionId: String,
    val specialistName: String = "Specialist",
)

@Composable
fun IncomingCallOverlay(
    callInfo: IncomingCallInfo?,
    onAccept: (IncomingCallInfo) -> Unit,
    onDecline: (IncomingCallInfo) -> Unit,
) {
    val context = LocalContext.current
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    val visible = callInfo != null

    LaunchedEffect(callInfo) {
        if (callInfo != null) {
            try {
                val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val player = MediaPlayer().apply {
                    setDataSource(context, uri)
                    isLooping = true
                    setVolume(0.8f, 0.8f)
                    prepare()
                    start()
                }
                mediaPlayer.value = player
            } catch (_: Exception) {
                // ringtone is optional
            }
        } else {
            mediaPlayer.value?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (_: Exception) { }
                it.release()
            }
            mediaPlayer.value = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (_: Exception) { }
                it.release()
            }
            mediaPlayer.value = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = androidx.compose.animation.core.tween(350),
        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(350)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = androidx.compose.animation.core.tween(250),
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Pulse animation ring
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Incoming Teleconsultation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClinicalNavy,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = callInfo?.specialistName ?: "Specialist",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Decline button
                        Button(
                            onClick = {
                                callInfo?.let { onDecline(it) }
                            },
                            modifier = Modifier
                                .width(130.dp)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Decline",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }

                        // Accept button
                        Button(
                            onClick = {
                                callInfo?.let { onAccept(it) }
                            },
                            modifier = Modifier
                                .width(130.dp)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Receive",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
