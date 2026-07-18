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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NursePatientScheduleScreen(
    onBack: () -> Unit,
    onPatientClick: (patientId: String, patientName: String) -> Unit,
) {
    var schedule by remember { mutableStateOf<List<NurseScheduleEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    LaunchedEffect(selectedDate) {
        loading = true
        error = null
        try {
            schedule = RetrofitClient.apiService.getNurseSchedule(selectedDate.format(formatter))
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load schedule"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Patient Schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        if (!loading) {
                            Text(
                                "${schedule.size} visit${if (schedule.size != 1) "s" else ""} on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF22C55E), TechTeal))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Date selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = TechTeal)
                }
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleBlack,
                )
                IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = TechTeal)
                }
            }

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TechTeal)
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Could not load schedule", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = CoolGray)
                            Text(error ?: "", fontSize = 13.sp, color = CoolGray.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }
                }
                schedule.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No visits scheduled", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = CoolGray)
                            Text("No patient visits for this date", fontSize = 13.sp, color = CoolGray.copy(alpha = 0.7f))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    ) {
                        items(schedule, key = { it.id }) { entry ->
                            ScheduleCard(entry = entry, onClick = {
                                val name = entry.patient?.let { "${it.first_name_en} ${it.last_name_en ?: ""}".trim() } ?: "Patient"
                                onPatientClick(entry.patient_id, name)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(entry: NurseScheduleEntry, onClick: () -> Unit) {
    val statusColor = when (entry.status) {
        "COMPLETED" -> Color(0xFF4CAF50)
        "IN_PROGRESS" -> AlertAmber
        "CANCELLED" -> CoolGray
        else -> TechTeal
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Time slot indicator
            Column(
                modifier = Modifier.width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    entry.scheduled_time_slot?.take(5) ?: "--:--",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TechTeal,
                )
                Text(
                    entry.scheduled_time_slot?.takeLast(2) ?: "",
                    fontSize = 10.sp,
                    color = CoolGray,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .background(statusColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.patient?.let { "${it.first_name_en} ${it.last_name_en ?: ""}".trim() } ?: "Patient",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleBlack,
                )
                entry.patient?.address_line1?.let { addr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CoolGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(addr, fontSize = 11.sp, color = CoolGray, maxLines = 1)
                    }
                }
                entry.service_requirements?.let { req ->
                    if (req.isNotBlank()) {
                        Text(req, fontSize = 11.sp, color = CoolGray, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    entry.status.replace("_", " "),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }
        }
    }
}
