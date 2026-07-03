package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(sessionId: String, onBack: () -> Unit) {
    var session by remember { mutableStateOf<BookingSessionResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        loading = true
        error = null
        try {
            session = RetrofitClient.apiService.getBookingSession(sessionId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load booking details"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Booking Details",
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
                                        session = RetrofitClient.apiService.getBookingSession(sessionId)
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

            session != null -> {
                BookingDetailContent(session = session!!, paddingValues = paddingValues)
            }
        }
    }
}

@Composable
fun BookingDetailContent(session: BookingSessionResponse, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SessionHeaderCard(session)
        }

        item {
            PatientInfoCard(session.patient)
        }

        item {
            Text(
                text = "Services (${session.tickets.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(session.tickets, key = { it.id }) { ticket ->
            TicketCard(ticket)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SessionHeaderCard(session: BookingSessionResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Session Status",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CoolGray,
                )
                StatusChip(status = session.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        fontSize = 12.sp,
                        color = CoolGray,
                    )
                    Text(
                        text = "৳${String.format("%.2f", session.total_amount ?: 0.0)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Booked By",
                        fontSize = 12.sp,
                        color = CoolGray,
                    )
                    Text(
                        text = session.booked_by ?: "N/A",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitleBlack,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatDateTime(session.created_at),
                fontSize = 12.sp,
                color = CoolGray.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun PatientInfoCard(patient: PatientSummary?) {
    if (patient == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = TechTeal,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Patient",
                    fontSize = 12.sp,
                    color = CoolGray,
                )
                Text(
                    text = listOfNotNull(patient.first_name_en, patient.last_name_en)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifEmpty { "N/A" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleBlack,
                )
                if (!patient.phone_number.isNullOrBlank()) {
                    Text(
                        text = patient.phone_number,
                        fontSize = 13.sp,
                        color = CoolGray,
                    )
                }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: Ticket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, iconColor) = serviceTypeIcon(ticket.service_type)
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ticket.service_type,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                    )
                }
                StatusChip(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = CoolGray,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ticket.ticket_no,
                    fontSize = 13.sp,
                    color = CoolGray,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!ticket.scheduled_date.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CoolGray,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticket.scheduled_date,
                            fontSize = 12.sp,
                            color = CoolGray,
                        )
                    }
                }
                if (!ticket.scheduled_time_slot.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = CoolGray,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticket.scheduled_time_slot,
                            fontSize = 12.sp,
                            color = CoolGray,
                        )
                    }
                }
            }

            if (ticket.price != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "৳${String.format("%.2f", ticket.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechTeal,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "ACTIVE", "ASSIGNED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PENDING" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "COMPLETED" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFF5F5F5) to CoolGray
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun serviceTypeIcon(type: String): Pair<ImageVector, Color> {
    return when (type.uppercase()) {
        "MBBS" -> Icons.Default.LocalHospital to Color(0xFF1976D2)
        "CAREGIVER" -> Icons.Default.Person to Color(0xFF43A047)
        "SPECIALIST" -> Icons.Default.MedicalServices to Color(0xFF7B1FA2)
        "NUTRITIONIST" -> Icons.Default.Schedule to Color(0xFFF57C00)
        else -> Icons.Default.MedicalServices to CoolGray
    }
}

private fun formatDateTime(dateStr: String?): String {
    if (dateStr == null) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateStr.take(19))
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (_: Exception) {
        dateStr.take(10)
    }
}
