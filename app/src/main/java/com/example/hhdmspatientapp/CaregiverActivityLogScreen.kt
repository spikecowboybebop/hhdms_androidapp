package com.example.hhdmspatientapp

// ══════════════════════════════════════════════════════════════════════════════
// NEW FILE: Caregiver activity logging screen (CG-005).
// Extracted from the monolithic CaregiverDashboardScreen. Follows the same
// list + form toggle pattern used by MbbsVitalsScreen and MbbsDiagnosisScreen.
// Supports patient selection and activity type grid layout.
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

private val ACTIVITY_TYPES = listOf(
    "HYGIENE" to "Personal Hygiene",
    "MOBILITY" to "Mobility Assistance",
    "FEEDING" to "Feeding Assistance",
    "MEDICATION" to "Oral Medication Admin",
    "COMPANIONSHIP" to "Companionship",
    "EXERCISE" to "Exercise",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverActivityLogScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<CaregiverPatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<CaregiverPatient?>(null) }
    var activityLogs by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var actType by remember { mutableStateOf("HYGIENE") }
    var actNotes by remember { mutableStateOf("") }
    var actSubmitting by remember { mutableStateOf(false) }

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

    fun loadActivities(patientId: String?) {
        scope.launch {
            try {
                activityLogs = RetrofitClient.apiService.getCaregiverActivities(patientId)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadPatients() }
    LaunchedEffect(selectedPatient) { selectedPatient?.let { loadActivities(it.id) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Daily Activity Log", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
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
                                    val chipColor = if (isSelected) TechTeal else CoolGray.copy(alpha = 0.15f)
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

                    // ── Activity Form ──
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Log Activity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                if (selectedPatient != null) {
                                    Text("Patient: ${selectedPatient!!.first_name_en} ${selectedPatient!!.last_name_en ?: ""}", fontSize = 11.sp, color = CoolGray)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (selectedPatient == null) {
                                    Text("Please select a patient first.", fontSize = 12.sp, color = CoolGray)
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
                                                            .clickable { actType = value }
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

                                    Text("Notes (optional)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = actNotes,
                                        onValueChange = { actNotes = it },
                                        placeholder = { Text("Add any observations or notes...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TechTeal, unfocusedBorderColor = CoolGray.copy(alpha = 0.3f)),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val sp = selectedPatient ?: return@Button
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
                                                    loadActivities(sp.id)
                                                } catch (e: Exception) {
                                                    snackbarText = "Error: ${e.message ?: "Failed to log activity"}"
                                                }
                                                actSubmitting = false
                                            }
                                        },
                                        enabled = !actSubmitting,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TechTeal, contentColor = PureWhite),
                                        modifier = Modifier.height(40.dp).fillMaxWidth(),
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
                    }

                    // ── Recent Activity Logs ──
                    if (activityLogs.isNotEmpty()) {
                        item {
                            Text("Recent Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        }
                        items(activityLogs, key = { it.id }) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
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
                                                Text("${it.first_name_en ?: ""} ${it.last_name_en ?: ""}", fontSize = 10.sp, color = CoolGray)
                                            }
                                        }
                                        val notes = log.notes
                                        if (!notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(notes, fontSize = 10.sp, color = CoolGray.copy(alpha = 0.7f))
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
    }
}
