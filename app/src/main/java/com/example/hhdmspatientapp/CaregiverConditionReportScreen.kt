package com.example.hhdmspatientapp

// ══════════════════════════════════════════════════════════════════════════════
// NEW FILE: Caregiver condition reporting screen (CG-007).
// Extracted from the monolithic CaregiverDashboardScreen. Follows the same
// list + form toggle pattern. Includes alert escalation to nurse/doctor.
// ══════════════════════════════════════════════════════════════════════════════

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

private val REPORT_TYPES = listOf(
    "FALL" to "Patient Fall",
    "MEDICATION_REFUSAL" to "Medication Refusal",
    "BEHAVIORAL_CHANGE" to "Behavioral Change",
    "PHYSICAL_SYMPTOM" to "Physical Symptom",
)

private val SEVERITY_LEVELS = listOf("MILD" to "Mild", "MODERATE" to "Moderate", "SEVERE" to "Severe")

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
fun CaregiverConditionReportScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<CaregiverPatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<CaregiverPatient?>(null) }
    var conditionReports by remember { mutableStateOf<List<ConditionReport>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var repType by remember { mutableStateOf("FALL") }
    var repDesc by remember { mutableStateOf("") }
    var repSeverity by remember { mutableStateOf("MODERATE") }
    var repSubmitting by remember { mutableStateOf(false) }

    var sendingAlert by remember { mutableStateOf<String?>(null) }

    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackbarText) {
        snackbarText?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarText = null
        }
    }

    fun loadPatients() {
        scope.launch {
            loading = true
            error = null
            try {
                patients = RetrofitClient.apiService.getCaregiverPatients()
                if (patients.isNotEmpty() && selectedPatient == null) {
                    selectedPatient = patients[0]
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load patients"
            }
            loading = false
        }
    }

    fun loadReports(patientId: String?) {
        scope.launch {
            try {
                conditionReports = RetrofitClient.apiService.getCaregiverConditionReports(patientId)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadPatients() }
    LaunchedEffect(selectedPatient) { selectedPatient?.let { loadReports(it.id) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Condition Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(AlertAmber, Color(0xFFE65100)))),
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
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error ?: "Error", color = CoolGray, fontSize = 14.sp)
                        TextButton(onClick = { loadPatients() }) { Text("Retry", color = TechTeal) }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    // ── Patient Selector ──
                    item {
                        Text("Select Patient", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoolGray, modifier = Modifier.padding(bottom = 4.dp))
                        if (patients.isEmpty()) {
                            Text("No patients assigned.", fontSize = 12.sp, color = CoolGray)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                patients.forEach { p ->
                                    val isSelected = selectedPatient?.id == p.id
                                    val chipColor = if (isSelected) AlertAmber else CoolGray.copy(alpha = 0.15f)
                                    val textColor = if (isSelected) PureWhite else TitleBlack
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(chipColor)
                                            .clickable { selectedPatient = p }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            "${p.first_name_en} ${p.last_name_en ?: ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Report Form ──
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Report Condition Change", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                if (selectedPatient != null) {
                                    Text("Patient: ${selectedPatient!!.first_name_en} ${selectedPatient!!.last_name_en ?: ""}", fontSize = 11.sp, color = CoolGray)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (selectedPatient == null) {
                                    Text("Please select a patient first.", fontSize = 12.sp, color = CoolGray)
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
                                                            .background(if (isSelected) AlertAmber else SoftSlate)
                                                            .clickable { repType = value }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) PureWhite else TitleBlack, textAlign = TextAlign.Center)
                                                    }
                                                }
                                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Description", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = repDesc,
                                        onValueChange = { repDesc = it },
                                        placeholder = { Text("Describe the observed change in detail...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AlertAmber, unfocusedBorderColor = CoolGray.copy(alpha = 0.3f)),
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
                                                    .clickable { repSeverity = value }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val sp = selectedPatient ?: return@Button
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
                                                    loadReports(sp.id)
                                                } catch (e: Exception) {
                                                    snackbarText = "Error: ${e.message ?: "Failed to submit report"}"
                                                }
                                                repSubmitting = false
                                            }
                                        },
                                        enabled = !repSubmitting && repDesc.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                                        modifier = Modifier.height(40.dp).fillMaxWidth(),
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
                    }

                    // ── Submitted Reports with Alert Escalation ──
                    if (conditionReports.isNotEmpty()) {
                        item {
                            Text("Submitted Reports", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        }
                        items(conditionReports, key = { it.id }) { report ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
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
                                                    .background(AlertAmber.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                            ) {
                                                Text(repLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertAmber)
                                            }
                                        }
                                        Text(report.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 9.sp, color = CoolGray.copy(alpha = 0.6f))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(report.description, fontSize = 11.sp, color = CoolGray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // ── Alert buttons ──
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val isNurseAlerted = report.alert_sent_to_nurse
                                        val isDoctorAlerted = report.alert_sent_to_doctor
                                        val isSending = sendingAlert == report.id

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isNurseAlerted) Color(0xFFDCFCE7) else ClinicalNavy)
                                                .clickable(enabled = !isSending && !isNurseAlerted) {
                                                    scope.launch {
                                                        sendingAlert = report.id
                                                        try {
                                                            RetrofitClient.apiService.sendCaregiverAlert(report.id, mapOf("target" to "nurse"))
                                                            snackbarText = "Alert sent to Nurse"
                                                            loadReports(selectedPatient?.id)
                                                        } catch (e: Exception) {
                                                            snackbarText = "Failed: ${e.message ?: "Unknown error"}"
                                                        }
                                                        sendingAlert = null
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                if (isNurseAlerted) "Nurse Alerted"
                                                else if (isSending) "Sending..."
                                                else "Alert Nurse",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isNurseAlerted) Color(0xFF16A34A) else PureWhite,
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDoctorAlerted) Color(0xFFDCFCE7) else ErrorRed)
                                                .clickable(enabled = !isSending && !isDoctorAlerted) {
                                                    scope.launch {
                                                        sendingAlert = report.id
                                                        try {
                                                            RetrofitClient.apiService.sendCaregiverAlert(report.id, mapOf("target" to "doctor"))
                                                            snackbarText = "Alert sent to MBBS Doctor"
                                                            loadReports(selectedPatient?.id)
                                                        } catch (e: Exception) {
                                                            snackbarText = "Failed: ${e.message ?: "Unknown error"}"
                                                        }
                                                        sendingAlert = null
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                if (isDoctorAlerted) "Doctor Alerted"
                                                else if (isSending) "Sending..."
                                                else "Alert Doctor",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDoctorAlerted) Color(0xFF16A34A) else PureWhite,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
