package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseConsultationScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var consultations by remember { mutableStateOf<List<NurseConsultationRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var concernSummary by remember { mutableStateOf("") }
    var urgencyLevel by remember { mutableStateOf("NORMAL") }

    val urgencyLevels = listOf("NORMAL", "URGENT", "EMERGENCY")

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try {
            patients = RetrofitClient.apiService.getNursePatients()
            consultations = RetrofitClient.apiService.getNurseConsultationRequests(null)
            loading = false
        } catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { consultations = RetrofitClient.apiService.getNurseConsultationRequests(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Doctor Consultation", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-012", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (showForm || selectedPatient != null) {
                        if (showForm) { showForm = false } else { selectedPatient = null }
                    } else { onBack() } }) {
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
            selectedPatient == null && !showForm -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Patient selector
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    ) {
                        item { Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                        items(patients, key = { it.id }) { patient ->
                            val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient; showForm = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(AlertAmber.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(20.dp))
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
            }
            showForm -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val patientName = selectedPatient?.let { "${it.first_name_en} ${it.last_name_en ?: ""}".trim() } ?: "Patient"
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AlertAmber.copy(alpha = 0.08f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Requesting consultation for $patientName", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = concernSummary, onValueChange = { concernSummary = it }, label = { Text("Concern Summary *") }, modifier = Modifier.fillMaxWidth(), minLines = 3, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Urgency Level", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        urgencyLevels.forEach { level ->
                            val color = when (level) { "EMERGENCY" -> ErrorRed; "URGENT" -> AlertAmber; else -> TechTeal }
                            FilterChip(
                                selected = urgencyLevel == level,
                                onClick = { urgencyLevel = level },
                                label = { Text(level, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createConsultationRequest(
                                        NurseCreateConsultationRequest(
                                            patient_id = patient.id,
                                            concern_summary = concernSummary,
                                            urgency_level = urgencyLevel,
                                        ),
                                    )
                                    snackbarText = "Consultation request sent to doctor!"
                                    showForm = false
                                    selectedPatient?.let { p -> consultations = RetrofitClient.apiService.getNurseConsultationRequests(p.id) }
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                        enabled = !saving && concernSummary.isNotBlank(),
                    ) {
                        if (saving) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text("Request Consultation", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }

                    // Consultation History
                    if (consultations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Consultation Requests", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        consultations.forEach { c ->
                            val statusColor = when (c.status) { "RESPONDED" -> Color(0xFF4CAF50); "PENDING" -> AlertAmber; else -> CoolGray }
                            val urgencyColor = when (c.urgency_level) { "EMERGENCY" -> ErrorRed; "URGENT" -> AlertAmber; else -> TechTeal }
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = urgencyColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(c.urgency_level, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = urgencyColor, modifier = Modifier.background(urgencyColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(c.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(c.concern_summary ?: "", fontSize = 12.sp, color = TitleBlack, maxLines = 3)
                                    c.doctor_response?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                                            Text("Doctor: $it", fontSize = 11.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(8.dp))
                                        }
                                    }
                                    Text(c.created_at?.take(16)?.replace("T", " ") ?: "", fontSize = 10.sp, color = CoolGray, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
