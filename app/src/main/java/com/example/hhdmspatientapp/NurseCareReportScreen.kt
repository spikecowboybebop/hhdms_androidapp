package com.example.hhdmspatientapp

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseCareReportScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var report by remember { mutableStateOf<NursingCareReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var generating by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try { patients = RetrofitClient.apiService.getNursePatients(); loading = false }
        catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { report = RetrofitClient.apiService.getNursingCareReport(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nursing Care Report", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-008", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectedPatient != null) selectedPatient = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ClinicalNavy, TechTeal))),
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
            selectedPatient == null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                ) {
                    item { Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(ClinicalNavy.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = ClinicalNavy, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    patient.mrn?.let { Text(it, fontSize = 11.sp, color = CoolGray) }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                            }
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val patientName = selectedPatient?.let { "${it.first_name_en} ${it.last_name_en ?: ""}".trim() } ?: "Patient"
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Patient: $patientName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                            selectedPatient?.mrn?.let { Text(it, fontSize = 12.sp, color = CoolGray) }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (report != null) {
                        // Display existing report
                        val r = report!!
                        ReportSection("Vitals Summary", r.vitals_summary)
                        ReportSection("Medications", r.medications_summary)
                        ReportSection("Procedures Performed", r.procedures_performed)
                        ReportSection("Patient Response", r.patient_response)
                        ReportSection("Handover Notes", r.handover_notes)

                        r.generated_at?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Generated: ${it.take(16).replace("T", " ")}", fontSize = 11.sp, color = CoolGray)
                        }
                        r.pdf_url?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download PDF Report", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No report generated yet", fontSize = 14.sp, color = CoolGray)
                                Text("Tap below to auto-generate a structured nursing visit report", fontSize = 12.sp, color = CoolGray.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                generating = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    report = RetrofitClient.apiService.generateNursingCareReport(patient.id)
                                    snackbarText = "Report generated!"
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                generating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalNavy),
                        enabled = !generating,
                    ) {
                        if (generating) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text(if (report != null) "Regenerate Report" else "Generate Report", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSection(title: String, content: String?) {
    if (content.isNullOrBlank()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClinicalNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(content, fontSize = 12.sp, color = TitleBlack, lineHeight = 18.sp)
        }
    }
}
