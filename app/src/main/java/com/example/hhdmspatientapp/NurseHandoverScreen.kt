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
fun NurseHandoverScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var handovers by remember { mutableStateOf<List<ShiftHandover>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentStatus by remember { mutableStateOf("") }
    var activeConcerns by remember { mutableStateOf("") }
    var medsDue by remember { mutableStateOf("") }
    var physicianOrders by remember { mutableStateOf("") }
    var patientInstructions by remember { mutableStateOf("") }
    var handoverTo by remember { mutableStateOf("") }

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try { patients = RetrofitClient.apiService.getNursePatients(); loading = false }
        catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { handovers = RetrofitClient.apiService.getNurseHandovers(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Shift Handover", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-010", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                ) {
                    item { Text("Select Patient for Handover", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient; showForm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFF00897B).copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
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
            showForm -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("Create Handover Note", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = currentStatus, onValueChange = { currentStatus = it }, label = { Text("Current Status *") }, modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = activeConcerns, onValueChange = { activeConcerns = it }, label = { Text("Active Concerns") }, modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = medsDue, onValueChange = { medsDue = it }, label = { Text("Medications Due") }, modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = physicianOrders, onValueChange = { physicianOrders = it }, label = { Text("Physician Orders") }, modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = patientInstructions, onValueChange = { patientInstructions = it }, label = { Text("Patient/Family Instructions") }, modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = handoverTo, onValueChange = { handoverTo = it }, label = { Text("Hand Over To (email)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createHandover(
                                        NurseCreateHandoverRequest(
                                            patient_id = patient.id,
                                            current_status = currentStatus,
                                            active_concerns = activeConcerns.ifBlank { null },
                                            medications_due = medsDue.ifBlank { null },
                                            physician_orders = physicianOrders.ifBlank { null },
                                            patient_instructions = patientInstructions.ifBlank { null },
                                            handed_over_to_email = handoverTo.ifBlank { null },
                                        ),
                                    )
                                    snackbarText = "Handover note created!"
                                    showForm = false
                                    selectedPatient?.let { p -> handovers = RetrofitClient.apiService.getNurseHandovers(p.id) }
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                        enabled = !saving && currentStatus.isNotBlank(),
                    ) {
                        if (saving) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text("Submit Handover", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }

                    // Handover History
                    if (handovers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Handover History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        handovers.forEach { h ->
                            val statusColor = when (h.status) { "SIGNED" -> Color(0xFF4CAF50); "PENDING" -> AlertAmber; else -> CoolGray }
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Handover: ${h.handed_over_by ?: "Nurse"} -> ${h.handed_over_to ?: "Next shift"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                            Text(h.current_status ?: "", fontSize = 11.sp, color = CoolGray, maxLines = 2)
                                        }
                                        Box(modifier = Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(h.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                        }
                                    }
                                    h.signed_at?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Signed: ${it.take(16).replace("T", " ")}", fontSize = 10.sp, color = CoolGray)
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
