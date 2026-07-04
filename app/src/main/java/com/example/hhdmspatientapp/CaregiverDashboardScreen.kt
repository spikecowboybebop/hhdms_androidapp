package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

private val ACTIVITY_TYPES = listOf(
    "HYGIENE" to "Personal Hygiene",
    "MOBILITY" to "Mobility Assistance",
    "FEEDING" to "Feeding Assistance",
    "MEDICATION" to "Oral Medication Admin",
    "COMPANIONSHIP" to "Companionship",
    "EXERCISE" to "Exercise",
)

private val REPORT_TYPES = listOf(
    "FALL" to "Patient Fall",
    "MEDICATION_REFUSAL" to "Medication Refusal",
    "BEHAVIORAL_CHANGE" to "Behavioral Change",
    "PHYSICAL_SYMPTOM" to "Physical Symptom",
)

private val SEVERITY_LEVELS = listOf("MILD" to "Mild", "MODERATE" to "Moderate", "SEVERE" to "Severe")

private val SERVICE_TYPE_LABELS = mapOf(
    "DAY_CARE" to "Day Care (7am–3pm)",
    "NIGHT_CARE" to "Night Care (10pm–6am)",
    "24_HOUR_CARE" to "24-Hour Care (rotational)",
    "RESPITE_CARE" to "Respite Care (min 4 hrs)",
)

private fun severityColor(severity: String) = when (severity) {
    "MILD" -> Color(0xFF22C55E)
    "SEVERE" -> Color(0xFFEF4444)
    else -> Color(0xFFFF9900)
}

private fun severityBg(severity: String) = when (severity) {
    "MILD" -> Color(0xFFDCFCE7)
    "SEVERE" -> Color(0xFFFEE2E2)
    else -> Color(0xFFFEF3C7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboardScreen(
    onLogout: () -> Unit,
) {
    var profile by remember { mutableStateOf<CaregiverProfileResponse?>(null) }
    var patients by remember { mutableStateOf<List<CaregiverPatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<CaregiverPatient?>(null) }
    var activityLogs by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var conditionReports by remember { mutableStateOf<List<ConditionReport>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var actType by remember { mutableStateOf("HYGIENE") }
    var actNotes by remember { mutableStateOf("") }
    var actSubmitting by remember { mutableStateOf(false) }

    var repType by remember { mutableStateOf("FALL") }
    var repDesc by remember { mutableStateOf("") }
    var repSeverity by remember { mutableStateOf("MODERATE") }
    var repSubmitting by remember { mutableStateOf(false) }

    var sendingAlert by remember { mutableStateOf<String?>(null) }

    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tabs = listOf("Overview", "Daily Activity Log", "Condition Reports")
    var activeTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(snackbarText) {
        snackbarText?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarText = null
        }
    }

    fun loadAll() {
        scope.launch {
            loading = true
            error = null
            try {
                val prof = RetrofitClient.apiService.getCaregiverProfile()
                val pats = RetrofitClient.apiService.getCaregiverPatients()
                profile = prof
                patients = pats
                if (pats.isNotEmpty() && selectedPatient == null) {
                    selectedPatient = pats[0]
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load caregiver data"
            }
            loading = false
        }
    }

    fun loadActivityLogs(patientId: String?) {
        scope.launch {
            try {
                activityLogs = RetrofitClient.apiService.getCaregiverActivities(patientId)
            } catch (_: Exception) { }
        }
    }

    fun loadConditionReports(patientId: String?) {
        scope.launch {
            try {
                conditionReports = RetrofitClient.apiService.getCaregiverConditionReports(patientId)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { loadActivityLogs(it.id) }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { loadConditionReports(it.id) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Caregiver Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = PureWhite)
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
            else -> {
                val profileName = profile?.let { "${it.user.firstNameEn} ${it.user.lastNameEn}" } ?: "Caregiver"

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Greeting
                    item {
                        Text("Welcome, $profileName", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                    }

                    // Stats cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatChip("Assigned", "${patients.size}", TechTeal, Modifier.weight(1f))
                            StatChip("Activities", "${activityLogs.size}", TechTeal, Modifier.weight(1f))
                            StatChip("Reports", "${conditionReports.size}", Color(0xFFFF9900), Modifier.weight(1f))
                        }
                    }

                    // Patient selector
                    item {
                        Column {
                            Text("Select Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoolGray, modifier = Modifier.padding(bottom = 6.dp))
                            if (patients.isEmpty()) {
                                Text("No patients assigned yet.", fontSize = 12.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            } else {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    patients.forEach { p ->
                                        val isSelected = selectedPatient?.id == p.id
                                        val chipColor = if (isSelected) TechTeal else CoolGray.copy(alpha = 0.15f)
                                        val textColor = if (isSelected) PureWhite else TitleBlack
                                        val label = SERVICE_TYPE_LABELS[p.service_type] ?: p.service_type ?: ""

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(chipColor)
                                                .clickable { selectedPatient = p }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "${p.first_name_en} ${p.last_name_en}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (label.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        label,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) PureWhite.copy(alpha = 0.8f) else TechTeal,
                                                        modifier = Modifier
                                                            .background(if (isSelected) PureWhite.copy(alpha = 0.2f) else TechTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tab row
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = activeTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = TechTeal,
                            edgePadding = 0.dp,
                        ) {
                            tabs.forEachIndexed { idx, title ->
                                Tab(
                                    selected = activeTabIndex == idx,
                                    onClick = { activeTabIndex = idx },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                )
                            }
                        }
                    }

                    // Tab content
                    item {
                        TabContent(
                            activeTabIndex = activeTabIndex,
                            selectedPatient = selectedPatient,
                            activityLogs = activityLogs,
                            conditionReports = conditionReports,
                            actType = actType,
                            actNotes = actNotes,
                            actSubmitting = actSubmitting,
                            repType = repType,
                            repDesc = repDesc,
                            repSeverity = repSeverity,
                            repSubmitting = repSubmitting,
                            sendingAlert = sendingAlert,
                            onActTypeChange = { actType = it },
                            onActNotesChange = { actNotes = it },
                            onRepTypeChange = { repType = it },
                            onRepDescChange = { repDesc = it },
                            onRepSeverityChange = { repSeverity = it },
                            onSubmitActivity = {
                                val sp = selectedPatient
                                if (sp != null) {
                                    scope.launch {
                                        actSubmitting = true
                                        try {
                                            RetrofitClient.apiService.createCaregiverActivity(
                                                CreateActivityLogRequest(
                                                    patient_id = sp.id,
                                                    activity_type = actType,
                                                    notes = actNotes.ifBlank { null },
                                                )
                                            )
                                            snackbarText = "Activity logged successfully!"
                                            actNotes = ""
                                            loadActivityLogs(sp.id)
                                        } catch (e: Exception) {
                                            snackbarText = "Error: ${e.message ?: "Failed to log activity"}"
                                        }
                                        actSubmitting = false
                                    }
                                }
                            },
                            onSubmitReport = {
                                val sp = selectedPatient
                                if (sp != null) {
                                    scope.launch {
                                        repSubmitting = true
                                        try {
                                            RetrofitClient.apiService.createConditionReport(
                                                CreateConditionReportRequest(
                                                    patient_id = sp.id,
                                                    report_type = repType,
                                                    description = repDesc,
                                                    severity = repSeverity,
                                                )
                                            )
                                            snackbarText = "Condition report submitted!"
                                            repDesc = ""
                                            loadConditionReports(sp.id)
                                        } catch (e: Exception) {
                                            snackbarText = "Error: ${e.message ?: "Failed to submit report"}"
                                        }
                                        repSubmitting = false
                                    }
                                }
                            },
                            onSendAlert = { reportId, target ->
                                scope.launch {
                                    sendingAlert = reportId
                                    try {
                                        RetrofitClient.apiService.sendCaregiverAlert(reportId, target)
                                        snackbarText = "Alert sent to $target"
                                        loadConditionReports(selectedPatient?.id)
                                    } catch (e: Exception) {
                                        snackbarText = "Failed to send alert: ${e.message ?: "Unknown error"}"
                                    }
                                    sendingAlert = null
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    activeTabIndex: Int,
    selectedPatient: CaregiverPatient?,
    activityLogs: List<ActivityLog>,
    conditionReports: List<ConditionReport>,
    actType: String,
    actNotes: String,
    actSubmitting: Boolean,
    repType: String,
    repDesc: String,
    repSeverity: String,
    repSubmitting: Boolean,
    sendingAlert: String?,
    onActTypeChange: (String) -> Unit,
    onActNotesChange: (String) -> Unit,
    onRepTypeChange: (String) -> Unit,
    onRepDescChange: (String) -> Unit,
    onRepSeverityChange: (String) -> Unit,
    onSubmitActivity: () -> Unit,
    onSubmitReport: () -> Unit,
    onSendAlert: (reportId: String, target: String) -> Unit,
) {
    when (activeTabIndex) {
        0 -> OverviewTabContent(selectedPatient, activityLogs, conditionReports)
        1 -> ActivityLogTabContent(
            selectedPatient = selectedPatient,
            actType = actType,
            actNotes = actNotes,
            actSubmitting = actSubmitting,
            activityLogs = activityLogs,
            onActTypeChange = onActTypeChange,
            onActNotesChange = onActNotesChange,
            onSubmitActivity = onSubmitActivity,
        )
        2 -> ConditionReportTabContent(
            selectedPatient = selectedPatient,
            repType = repType,
            repDesc = repDesc,
            repSeverity = repSeverity,
            repSubmitting = repSubmitting,
            conditionReports = conditionReports,
            sendingAlert = sendingAlert,
            onRepTypeChange = onRepTypeChange,
            onRepDescChange = onRepDescChange,
            onRepSeverityChange = onRepSeverityChange,
            onSubmitReport = onSubmitReport,
            onSendAlert = onSendAlert,
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CoolGray)
        }
    }
}

@Composable
private fun OverviewTabContent(
    selectedPatient: CaregiverPatient?,
    activityLogs: List<ActivityLog>,
    conditionReports: List<ConditionReport>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedPatient != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${selectedPatient.first_name_en} ${selectedPatient.last_name_en}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Text("MRN: ${selectedPatient.mrn}", fontSize = 11.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InfoBox("Sex", if (selectedPatient.sex == "M") "Male" else "Female", Modifier.weight(1f))
                        InfoBox("Blood Group", selectedPatient.blood_group ?: "N/A", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InfoBox("Phone", selectedPatient.phone_number ?: "N/A", Modifier.weight(1f))
                        InfoBox("District", selectedPatient.district ?: "N/A", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val stLabel = SERVICE_TYPE_LABELS[selectedPatient.service_type] ?: selectedPatient.service_type ?: "N/A"
                        val ptLabel = when (selectedPatient.patient_type) {
                            "ADULT" -> "Adult"; "CHILD" -> "Child"; "ELDERLY" -> "Elderly"
                            else -> selectedPatient.patient_type ?: "N/A"
                        }
                        InfoBox("Service Type", stLabel, Modifier.weight(1f))
                        InfoBox("Patient Type", ptLabel, Modifier.weight(1f))
                    }
                }
            }
        }

        // Recent Activity Logs
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Activity Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                Text("Last ${minOf(activityLogs.size, 5)} submissions", fontSize = 10.sp, color = CoolGray)
                Spacer(modifier = Modifier.height(8.dp))
                if (activityLogs.isEmpty()) {
                    Text("No daily activities logged yet.", fontSize = 12.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    activityLogs.take(5).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val actLabel = ACTIVITY_TYPES.find { it.first == log.activity_type }?.second ?: log.activity_type
                                Box(
                                    modifier = Modifier
                                        .background(TechTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(actLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                }
                                log.patient?.let {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${it.first_name_en ?: ""} ${it.last_name_en ?: ""}", fontSize = 10.sp, color = CoolGray)
                                }
                            }
                            Text(log.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 9.sp, color = CoolGray.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // Recent Condition Reports
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Condition Reports", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                Text("Last ${minOf(conditionReports.size, 5)} submissions", fontSize = 10.sp, color = CoolGray)
                Spacer(modifier = Modifier.height(8.dp))
                if (conditionReports.isEmpty()) {
                    Text("No condition changes reported yet.", fontSize = 12.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    conditionReports.take(5).forEach { report ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(severityBg(report.severity), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(report.severity, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = severityColor(report.severity))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                val repLabel = REPORT_TYPES.find { it.first == report.report_type }?.second ?: report.report_type
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF9900).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(repLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9900))
                                }
                                report.patient?.let {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${it.first_name_en ?: ""} ${it.last_name_en ?: ""}", fontSize = 10.sp, color = CoolGray)
                                }
                            }
                            Text(report.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 9.sp, color = CoolGray.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogTabContent(
    selectedPatient: CaregiverPatient?,
    actType: String,
    actNotes: String,
    actSubmitting: Boolean,
    activityLogs: List<ActivityLog>,
    onActTypeChange: (String) -> Unit,
    onActNotesChange: (String) -> Unit,
    onSubmitActivity: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Log Daily Activity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                if (selectedPatient != null) {
                    Text("Patient: ${selectedPatient.first_name_en} ${selectedPatient.last_name_en}", fontSize = 11.sp, color = CoolGray)
                } else {
                    Text("Select a patient first", fontSize = 11.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPatient == null) {
                    Text("Please select a patient from the bar above.", fontSize = 12.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    Text("Activity Type", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ACTIVITY_TYPES.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (value, label) ->
                                    val isSelected = actType == value
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) TechTeal else SoftSlate)
                                            .clickable { onActTypeChange(value) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) PureWhite else TitleBlack,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Notes (optional)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = actNotes,
                        onValueChange = onActNotesChange,
                        placeholder = { Text("Add any observations or notes…", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TechTeal, unfocusedBorderColor = CoolGray.copy(alpha = 0.3f)),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSubmitActivity,
                        enabled = !actSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                        modifier = Modifier.height(40.dp),
                    ) {
                        if (actSubmitting) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Log Activity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (activityLogs.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Activity Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    activityLogs.forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val actLabel = ACTIVITY_TYPES.find { it.first == log.activity_type }?.second ?: log.activity_type
                                    Box(
                                        modifier = Modifier
                                            .background(TechTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(actLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                    }
                                    log.patient?.let {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("— ${it.first_name_en ?: ""} ${it.last_name_en ?: ""}", fontSize = 10.sp, color = CoolGray)
                                    }
                                }
                                if (!log.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(log.notes, fontSize = 10.sp, color = CoolGray.copy(alpha = 0.7f))
                                }
                            }
                            Text(log.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 9.sp, color = CoolGray.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionReportTabContent(
    selectedPatient: CaregiverPatient?,
    repType: String,
    repDesc: String,
    repSeverity: String,
    repSubmitting: Boolean,
    conditionReports: List<ConditionReport>,
    sendingAlert: String?,
    onRepTypeChange: (String) -> Unit,
    onRepDescChange: (String) -> Unit,
    onRepSeverityChange: (String) -> Unit,
    onSubmitReport: () -> Unit,
    onSendAlert: (reportId: String, target: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Report Condition Change", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                if (selectedPatient != null) {
                    Text("Patient: ${selectedPatient.first_name_en} ${selectedPatient.last_name_en}", fontSize = 11.sp, color = CoolGray)
                } else {
                    Text("Select a patient first", fontSize = 11.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPatient == null) {
                    Text("Please select a patient from the bar above.", fontSize = 12.sp, color = CoolGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    Text("Report Type", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        REPORT_TYPES.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (value, label) ->
                                    val isSelected = repType == value
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFFF9900) else SoftSlate)
                                            .clickable { onRepTypeChange(value) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) PureWhite else TitleBlack,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Description", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = repDesc,
                        onValueChange = onRepDescChange,
                        placeholder = { Text("Describe the observed change in detail…", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9900), unfocusedBorderColor = CoolGray.copy(alpha = 0.3f)),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Severity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SEVERITY_LEVELS.forEach { (value, label) ->
                            val isSelected = repSeverity == value
                            val bg = if (isSelected) severityColor(value) else SoftSlate
                            val fg = if (isSelected) PureWhite else TitleBlack
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { onRepSeverityChange(value) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSubmitReport,
                        enabled = !repSubmitting && repDesc.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
                        modifier = Modifier.height(40.dp),
                    ) {
                        if (repSubmitting) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Submit Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (conditionReports.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Submitted Reports", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Text("Alerts can be sent to escalate", fontSize = 10.sp, color = CoolGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    conditionReports.forEach { report ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(severityBg(report.severity), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(report.severity, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = severityColor(report.severity))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val repLabel = REPORT_TYPES.find { it.first == report.report_type }?.second ?: report.report_type
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF9900).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(repLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9900))
                                    }
                                    report.patient?.let {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("— ${it.first_name_en ?: ""} ${it.last_name_en ?: ""}", fontSize = 10.sp, color = CoolGray)
                                    }
                                }
                                Text(report.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 9.sp, color = CoolGray.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(report.description, fontSize = 11.sp, color = CoolGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isNurseAlerted = report.alert_sent_to_nurse
                                val isDoctorAlerted = report.alert_sent_to_doctor
                                val isSending = sendingAlert == report.id

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isNurseAlerted) Color(0xFFDCFCE7) else Color(0xFF0A2540))
                                        .clickable(enabled = !isSending && !isNurseAlerted) { onSendAlert(report.id, "nurse") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        if (isNurseAlerted) "✓ Nurse Alerted"
                                        else if (isSending) "Sending…"
                                        else "Send Alert to Nurse",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNurseAlerted) Color(0xFF16A34A) else PureWhite,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDoctorAlerted) Color(0xFFDCFCE7) else Color(0xFFDC2626))
                                        .clickable(enabled = !isSending && !isDoctorAlerted) { onSendAlert(report.id, "doctor") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        if (isDoctorAlerted) "✓ Doctor Alerted"
                                        else if (isSending) "Sending…"
                                        else "Send Alert to MBBS Doctor",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDoctorAlerted) Color(0xFF16A34A) else PureWhite,
                                    )
                                }
                            }
                            if (report != conditionReports.last()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp, color = CoolGray.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SoftSlate, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CoolGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
    }
}
