package com.example.hhdmspatientapp

// ══════════════════════════════════════════════════════════════════════════════
// NEW FILE: Caregiver patient detail screen.
// Mirrors PatientDetailScreen pattern but with caregiver-specific tabs:
// Info, Activities, Reports. Uses the same MbbsPatientProfileResponse for
// patient demographics since both roles view the same patient model.
// ══════════════════════════════════════════════════════════════════════════════

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

private val SERVICE_TYPE_LABELS = mapOf(
    "DAY_CARE" to "Day Care (7am-3pm)",
    "NIGHT_CARE" to "Night Care (10pm-6am)",
    "24_HOUR_CARE" to "24-Hour Care",
    "RESPITE_CARE" to "Respite Care",
)

private val ACTIVITY_TYPES = mapOf(
    "HYGIENE" to "Personal Hygiene",
    "MOBILITY" to "Mobility Assistance",
    "FEEDING" to "Feeding Assistance",
    "MEDICATION" to "Oral Medication Admin",
    "COMPANIONSHIP" to "Companionship",
    "EXERCISE" to "Exercise",
)

private val REPORT_TYPES = mapOf(
    "FALL" to "Patient Fall",
    "MEDICATION_REFUSAL" to "Medication Refusal",
    "BEHAVIORAL_CHANGE" to "Behavioral Change",
    "PHYSICAL_SYMPTOM" to "Physical Symptom",
)

private fun severityColor(severity: String) = when (severity) {
    "MILD" -> Color(0xFF22C55E)
    "SEVERE" -> Color(0xFFEF4444)
    else -> Color(0xFFFF9900)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverPatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onNavigateToActivityLog: (patientId: String, patientName: String) -> Unit,
    onNavigateToConditionReport: (patientId: String, patientName: String) -> Unit,
) {
    var profile by remember { mutableStateOf<MbbsPatientProfileResponse?>(null) }
    var activityLogs by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var conditionReports by remember { mutableStateOf<List<ConditionReport>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val tabs = listOf("Info", "Activities", "Reports")
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

    fun loadAll() {
        scope.launch {
            loading = true
            error = null
            try {
                profile = RetrofitClient.apiService.getMbbsPatientProfile(patientId)
                activityLogs = RetrofitClient.apiService.getCaregiverActivities(patientId)
                conditionReports = RetrofitClient.apiService.getCaregiverConditionReports(patientId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load patient"
            }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadAll() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Patient Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TechTeal)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "Something went wrong", fontSize = 14.sp, color = CoolGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadAll() }) { Text("Retry", color = TechTeal) }
                    }
                }
            }
            profile != null -> {
                val p = profile!!.patient
                val patientName = "${p.first_name_en} ${p.last_name_en ?: ""}".trim()
                val initials = buildString {
                    append(p.first_name_en.firstOrNull() ?: '—')
                    p.last_name_en?.firstOrNull()?.let { append(it) }
                }

                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // ── Patient Header Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(56.dp).background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(initials, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patientName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                    Text(p.mrn, fontSize = 12.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(when (p.sex) { "M" -> "Male"; "F" -> "Female"; else -> p.sex ?: "—" }, fontSize = 12.sp, color = CoolGray)
                                        Text("  |  ", fontSize = 12.sp, color = LightGray)
                                        Text(p.blood_group ?: "—", fontSize = 12.sp, color = CoolGray)
                                        Text("  |  ", fontSize = 12.sp, color = LightGray)
                                        Text(p.date_of_birth?.take(10) ?: "—", fontSize = 12.sp, color = CoolGray)
                                    }
                                }
                            }

                            // ── Quick action buttons ──
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CaregiverMiniButton(
                                    icon = Icons.Default.EditNote,
                                    label = "Log Activity",
                                    color = Color(0xFF3B82F6),
                                    onClick = { onNavigateToActivityLog(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                                CaregiverMiniButton(
                                    icon = Icons.Default.Warning,
                                    label = "Report",
                                    color = AlertAmber,
                                    onClick = { onNavigateToConditionReport(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Tab Row ──
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = PureWhite,
                        contentColor = TechTeal,
                        edgePadding = 8.dp,
                        divider = {},
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        title, fontSize = 12.sp,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (pagerState.currentPage == index) TechTeal else CoolGray,
                                    )
                                },
                            )
                        }
                    }

                    // ── Tab Content ──
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> CaregiverPatientInfoTab(profile!!)
                            1 -> CaregiverActivityTab(activityLogs)
                            2 -> CaregiverReportsTab(conditionReports)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaregiverMiniButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Tab: Info ──
@Composable
private fun CaregiverPatientInfoTab(profile: MbbsPatientProfileResponse) {
    val p = profile.patient
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Contact Info", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    CaregiverDetailRow(Icons.Default.Phone, "Phone", p.phone_number ?: "—")
                    CaregiverDetailRow(Icons.Default.Email, "Email", p.email ?: "—")
                    CaregiverDetailRow(Icons.Default.LocationOn, "Address", listOfNotNull(p.address_line1, p.address_line2, p.district).joinToString(", ").ifEmpty { "—" })
                    CaregiverDetailRow(Icons.Default.PhoneInTalk, "Emergency", p.emergency_contact ?: "—")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Medical History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    CaregiverDetailRow(Icons.Default.Healing, "Allergies", p.known_allergies ?: "None reported")
                    CaregiverDetailRow(Icons.Default.Medication, "Current Meds", p.current_medications ?: "None")
                    CaregiverDetailRow(Icons.Default.History, "Past Medical", p.past_medical_history ?: "None")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Care Assignment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Service type and patient details are managed by the call center.", fontSize = 12.sp, color = CoolGray)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CaregiverDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CoolGray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, color = CoolGray, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, color = TitleBlack, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

// ── Tab: Activities ──
@Composable
private fun CaregiverActivityTab(activities: List<ActivityLog>) {
    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No activities logged yet", color = CoolGray, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(activities, key = { it.id }) { log ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val actLabel = ACTIVITY_TYPES[log.activity_type] ?: log.activity_type
                            Box(
                                modifier = Modifier.background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(actLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(log.created_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                        }
                        if (!log.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(log.notes, fontSize = 13.sp, color = TitleBlack)
                        }
                    }
                }
            }
        }
    }
}

// ── Tab: Reports ──
@Composable
private fun CaregiverReportsTab(reports: List<ConditionReport>) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No condition reports yet", color = CoolGray, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(reports, key = { it.id }) { report ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val repLabel = REPORT_TYPES[report.report_type] ?: report.report_type
                            Box(
                                modifier = Modifier.background(severityColor(report.severity).copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(report.severity, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = severityColor(report.severity))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(repLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(report.created_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(report.description, fontSize = 13.sp, color = CoolGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (report.alert_sent_to_nurse) {
                                Text("Nurse Alerted", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            }
                            if (report.alert_sent_to_doctor) {
                                Text("Doctor Alerted", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
