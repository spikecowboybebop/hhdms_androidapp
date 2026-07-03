package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
fun DoctorBookingDetailScreen(sessionId: String, onBack: () -> Unit) {
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
                        text = "Patient Session",
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
                DoctorBookingContent(session = session!!, paddingValues = paddingValues)
            }
        }
    }
}

@Composable
fun DoctorBookingContent(session: BookingSessionResponse, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DoctorPatientHeaderCard(session)
        }

        item {
            Text(
                text = "Service Tickets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(session.tickets, key = { it.id }) { ticket ->
            DoctorTicketCard(ticket)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DoctorPatientHeaderCard(session: BookingSessionResponse) {
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
                    text = "Patient Information",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CoolGray,
                )
                DoctorStatusChip(status = session.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TechTeal,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val patientName = listOfNotNull(
                        session.patient?.first_name_en,
                        session.patient?.last_name_en,
                    ).filter { it.isNotBlank() }.joinToString(" ").ifEmpty { "Unknown" }

                    Text(
                        text = patientName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                    )
                    if (!session.patient?.phone_number.isNullOrBlank()) {
                        Text(
                            text = session.patient!!.phone_number!!,
                            fontSize = 13.sp,
                            color = CoolGray,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = SoftSlate, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = CoolGray,
                    )
                    val amount = session.total_amount?.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "৳${String.format("%.2f", amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechTeal,
                    )
                }
            }

            if (session.created_at != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Created: ${session.created_at.take(10)}",
                    fontSize = 12.sp,
                    color = CoolGray.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun DoctorTicketCard(ticket: Ticket) {
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
                    val icon = when (ticket.service_type.uppercase()) {
                        "MBBS" -> Icons.Default.LocalHospital
                        "SPECIALIST" -> Icons.Default.MedicalServices
                        "CAREGIVER" -> Icons.Default.Person
                        "NUTRITIONIST" -> Icons.Default.Schedule
                        else -> Icons.Default.MedicalServices
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = TechTeal,
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
                DoctorStatusChip(status = ticket.status)
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

            if (ticket.provider != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = listOfNotNull(
                            ticket.provider.first_name_en,
                            ticket.provider.last_name_en,
                        ).filter { it.isNotBlank() }.joinToString(" "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TitleBlack,
                    )
                    if (!ticket.provider.specialization.isNullOrBlank()) {
                        Text(
                            text = " (${ticket.provider.specialization})",
                            fontSize = 12.sp,
                            color = CoolGray,
                        )
                    }
                }
            }

            val ticketPrice = ticket.price?.toDoubleOrNull()
            if (ticketPrice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "৳${String.format("%.2f", ticketPrice)}",
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
fun DoctorStatusChip(status: String) {
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
