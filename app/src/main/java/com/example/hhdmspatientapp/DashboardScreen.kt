package com.example.hhdmspatientapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.delay

val MedicalOrange = Color(0xFFF57C00)
val MedicalOrangeBackground = Color(0xFFFFF3E0)

enum class CallStatus {
    IDLE, RINGING, CONNECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(userEmail: String, latestSession: SessionSummary? = null, onLogout: () -> Unit, onNavigateToNotifications: () -> Unit, onNavigateToAppointments: () -> Unit, onNavigateToBookingDetail: (String) -> Unit = {}, onCallEndedRefresh: () -> Unit = {}) {
    val context = LocalContext.current
    var currentCallStatus by remember { mutableStateOf(CallStatus.IDLE) }
    val visitDoctorName = remember { mutableStateOf(VisitStorage.getVisitDoctorName()) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasAudioPermission = isGranted }

    var wasPreviouslyOnCall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CallSignalingManager.onCallStateChange = { newStatus -> currentCallStatus = newStatus }
    }
    DisposableEffect(Unit) {
        onDispose { CallSignalingManager.onCallStateChange = null }
    }

    LaunchedEffect(currentCallStatus) {
        if (currentCallStatus == CallStatus.RINGING || currentCallStatus == CallStatus.CONNECTED) {
            wasPreviouslyOnCall = true
        } else if (wasPreviouslyOnCall && currentCallStatus == CallStatus.IDLE) {
            wasPreviouslyOnCall = false
            onCallEndedRefresh()
        }
    }

    val statusCardColor by animateColorAsState(
        targetValue = when (currentCallStatus) {
            CallStatus.IDLE -> PureWhite
            CallStatus.RINGING -> Color(0xFFFFF9C4)
            CallStatus.CONNECTED -> Color(0xFFE8F5E9)
        },
        label = "cardColorAnimation"
    )

    val patientName = userEmail.substringBefore("@")
    val displayName = patientName.replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            if (currentCallStatus == CallStatus.IDLE) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Aastha",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                            )
                            Text(
                                text = "Tele-HealthCare",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = PureWhite,
                            )
                        }
                        IconButton(onClick = {
                            if (currentCallStatus != CallStatus.IDLE) {
                                currentCallStatus = CallStatus.IDLE
                                CallSignalingManager.hangUpActiveCall()
                            }
                            onLogout()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = PureWhite,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    modifier = Modifier.background(
                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))
                    ),
                )
            }
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (currentCallStatus != CallStatus.IDLE) {
            CallScreen(
                displayName = displayName,
                callStatus = currentCallStatus,
                onEndCall = {
                    currentCallStatus = CallStatus.IDLE
                    CallSignalingManager.hangUpActiveCall()
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Patient profile card ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(TechTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.take(2),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechTeal,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Good Morning,",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolGray,
                    )
                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Doctor Visit Banner ──
            if (!visitDoctorName.value.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TechTeal.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = TechTeal,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Doctor Coming",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTeal,
                            )
                            Text(
                                text = "${visitDoctorName.value} is coming to visit you.",
                                fontSize = 13.sp,
                                color = TitleBlack,
                            )
                        }
                        IconButton(onClick = {
                            VisitStorage.clearVisit()
                            visitDoctorName.value = null
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = CoolGray,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Recent Appointment ──
            Text(
                text = "Recent Appointment",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            if (latestSession != null) {
                val s = latestSession!!
                val firstTicket = s.tickets.firstOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToBookingDetail(s.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(TechTeal.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = TechTeal,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = firstTicket?.service_type ?: "Appointment",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TitleBlack,
                            )
                            if (!firstTicket?.scheduled_date.isNullOrBlank()) {
                                Text(
                                    text = firstTicket!!.scheduled_date!!.take(10),
                                    fontSize = 12.sp,
                                    color = CoolGray,
                                )
                            }
                            if (!firstTicket?.scheduled_time_slot.isNullOrBlank()) {
                                Text(
                                    text = firstTicket!!.scheduled_time_slot!!,
                                    fontSize = 12.sp,
                                    color = CoolGray,
                                )
                            }
                        }
                        StatusChip(s.status)
                    }
                }
            } else {
                Text(
                    text = "No appointments have been booked yet.",
                    fontSize = 13.sp,
                    color = CoolGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── Voice Consultation ──
            Text(
                text = "Voice Consultation",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = statusCardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                color = when (currentCallStatus) {
                                    CallStatus.IDLE -> TechTeal.copy(alpha = 0.12f)
                                    CallStatus.RINGING -> MedicalOrangeBackground
                                    CallStatus.CONNECTED -> Color.Green.copy(alpha = 0.12f)
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = when (currentCallStatus) {
                                CallStatus.IDLE -> TechTeal
                                CallStatus.RINGING -> MedicalOrange
                                CallStatus.CONNECTED -> Color(0xFF2E7D32)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when (currentCallStatus) {
                            CallStatus.IDLE -> "Need Medical Assistance?"
                            CallStatus.RINGING -> "Calling Support Desk..."
                            CallStatus.CONNECTED -> "Voice Connection Active"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = when (currentCallStatus) {
                            CallStatus.IDLE -> "Connect with a medical coordinator instantly via a secure voice call."
                            CallStatus.RINGING -> "Please wait while we route your call to an available agent."
                            CallStatus.CONNECTED -> "You are connected. Your call is being handled by a support agent."
                        },
                        fontSize = 13.sp,
                        color = CoolGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                        lineHeight = 18.sp,
                    )

                    if (currentCallStatus == CallStatus.IDLE) {
                        Button(
                            onClick = {
                                if (hasAudioPermission) {
                                    currentCallStatus = CallStatus.RINGING
                                    CallSignalingManager.startEmergencyCall(userEmail)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechTeal,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Start Voice Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                currentCallStatus = CallStatus.IDLE
                                CallSignalingManager.hangUpActiveCall()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (currentCallStatus == CallStatus.RINGING) "Cancel Request" else "End Call",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick actions grid ──
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.CalendarMonth,
                    label = "Appointments",
                    color = TechTeal,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAppointments,
                )
                QuickActionCard(
                    icon = Icons.Default.Description,
                    label = "Records",
                    color = ClinicalNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.LocalPharmacy,
                    label = "Prescriptions",
                    color = SlateGray,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                QuickActionCard(
                    icon = Icons.Default.MedicalServices,
                    label = "Support",
                    color = AlertAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Aastha Tele-HealthCare v1.0.0",
                fontSize = 11.sp,
                color = CoolGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
    }
}

@Composable
fun CallScreen(
    displayName: String,
    callStatus: CallStatus,
    onEndCall: () -> Unit,
) {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.CONNECTED) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val timerText = remember(seconds) {
        val min = seconds / 60
        val sec = seconds % 60
        "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ClinicalNavy, SlateGray),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(TechTeal.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AH",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Aastha Tele-HealthCare",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callStatus) {
                    CallStatus.RINGING -> "Calling..."
                    CallStatus.CONNECTED -> timerText
                    else -> ""
                },
                fontSize = 16.sp,
                color = PureWhite.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onEndCall,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = PureWhite,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tap to end call",
                fontSize = 13.sp,
                color = PureWhite.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                textAlign = TextAlign.Center,
            )
        }
    }
}
