package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(onBack: () -> Unit, onSessionTap: (String) -> Unit) {
    var sessions by remember { mutableStateOf<List<SessionSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        try {
            sessions = RetrofitClient.apiService.getMySessions()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load appointments"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Appointments",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TechTeal)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Something went wrong",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CoolGray,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            fontSize = 13.sp,
                            color = CoolGray.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        sessions = RetrofitClient.apiService.getMySessions()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Failed to load"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            sessions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CoolGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No appointments yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CoolGray,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(sessions, key = { it.id }) { session ->
                        AppointmentCard(session = session, onClick = { onSessionTap(session.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(session: SessionSummary, onClick: () -> Unit) {
    val firstTicket = session.tickets.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when (session.status.uppercase()) {
                            "ACTIVE", "ASSIGNED" -> Color(0xFFE8F5E9)
                            "COMPLETED" -> Color(0xFFE3F2FD)
                            "CANCELLED" -> Color(0xFFFFEBEE)
                            else -> Color(0xFFF5F5F5)
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    appointmentIcon(firstTicket?.service_type),
                    contentDescription = null,
                    tint = TechTeal,
                    modifier = Modifier.size(22.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CoolGray,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatApptDate(firstTicket!!.scheduled_date!!),
                            fontSize = 12.sp,
                            color = CoolGray,
                        )
                        val timeSlot = firstTicket.scheduled_time_slot
                        if (!timeSlot.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CoolGray,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeSlot,
                                fontSize = 12.sp,
                                color = CoolGray,
                            )
                        }
                    }
                }
                val patientName = listOfNotNull(
                    session.patient?.first_name_en,
                    session.patient?.last_name_en,
                ).filter { it.isNotBlank() }.joinToString(" ")
                if (patientName.isNotBlank()) {
                    Text(
                        text = patientName,
                        fontSize = 12.sp,
                        color = CoolGray.copy(alpha = 0.7f),
                    )
                }
            }
            StatusChip(session.status)
        }
    }
}

private fun appointmentIcon(serviceType: String?): ImageVector {
    return when (serviceType?.uppercase()) {
        "MBBS" -> Icons.Default.LocalHospital
        "CAREGIVER" -> Icons.Default.Person
        "SPECIALIST" -> Icons.Default.MedicalServices
        "NUTRITIONIST" -> Icons.Default.Schedule
        else -> Icons.Default.CalendarMonth
    }
}

private fun formatApptDate(dateStr: String): String {
    return try {
        dateStr.take(10)
    } catch (_: Exception) {
        dateStr
    }
}
