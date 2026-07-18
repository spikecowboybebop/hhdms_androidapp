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
fun NurseVitalsScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var vitals by remember { mutableStateOf<List<NurseVitalSigns>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form fields
    var sysBp by remember { mutableStateOf("") }
    var diaBp by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var spo2 by remember { mutableStateOf("") }
    var rr by remember { mutableStateOf("") }
    var glucose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(snackbarText) {
        snackbarText?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarText = null
        }
    }

    LaunchedEffect(Unit) {
        try {
            patients = RetrofitClient.apiService.getNursePatients()
            loading = false
        } catch (e: Exception) {
            snackbarText = "Failed to load patients: ${e.message}"
            loading = false
        }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try {
                vitals = RetrofitClient.apiService.getNursePatientVitals(p.id)
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vital Signs Recording", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        selectedPatient?.let {
                            Text("NS-004", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                        }
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
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFE53935), TechTeal))),
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
                // Patient selector
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                ) {
                    item {
                        Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedPatient = patient
                                showForm = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(name.take(2), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TechTeal)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Previous vitals comparison
                    val previous = vitals.firstOrNull()
                    if (previous != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = TechTeal.copy(alpha = 0.06f)),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Previous Vitals (Auto-Compare)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                Spacer(modifier = Modifier.height(4.dp))
                                val prevBp = "${previous.systolic_bp ?: "?"}/${previous.diastolic_bp ?: "?"}"
                                val prevPulse = "${previous.pulse_bpm ?: "?"}"
                                val prevTemp = "${previous.temperature_c ?: "?"}"
                                val prevSpo2 = "${previous.spo2_pct ?: "?"}%"
                                Text("BP: $prevBp | Pulse: $prevPulse | Temp: $prevTemp | SpO2: $prevSpo2", fontSize = 11.sp, color = CoolGray)
                                previous.recorded_at?.let {
                                    Text("Recorded: ${it.take(16).replace("T", " ")}", fontSize = 10.sp, color = CoolGray.copy(alpha = 0.7f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text("Record New Vitals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        VitalsInputField("Systolic BP", sysBp, { sysBp = it }, Modifier.weight(1f))
                        VitalsInputField("Diastolic BP", diaBp, { diaBp = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        VitalsInputField("Pulse (bpm)", pulse, { pulse = it }, Modifier.weight(1f))
                        VitalsInputField("Temp (C)", temp, { temp = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        VitalsInputField("SpO2 (%)", spo2, { spo2 = it }, Modifier.weight(1f))
                        VitalsInputField("Resp Rate", rr, { rr = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    VitalsInputField("Blood Glucose (mg/dL)", glucose, { glucose = it }, Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        textStyle = TextStyle(color = TitleBlack),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createNurseVitals(
                                        patient.id,
                                        NurseCreateVitalsRequest(
                                            patient_id = patient.id,
                                            systolic_bp = sysBp.toIntOrNull(),
                                            diastolic_bp = diaBp.toIntOrNull(),
                                            pulse_bpm = pulse.toIntOrNull(),
                                            temperature_c = temp.toDoubleOrNull(),
                                            spo2_pct = spo2.toIntOrNull(),
                                            respiratory_rate = rr.toIntOrNull(),
                                            blood_glucose = glucose.toDoubleOrNull(),
                                            notes = notes.ifBlank { null },
                                        ),
                                    )
                                    snackbarText = "Vitals recorded successfully!"
                                    showForm = false
                                    selectedPatient?.let { p ->
                                        vitals = RetrofitClient.apiService.getNursePatientVitals(p.id)
                                    }
                                } catch (e: Exception) {
                                    snackbarText = "Error: ${e.message}"
                                }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                        enabled = !saving,
                    ) {
                        if (saving) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Vitals", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vitals history
                    if (vitals.isNotEmpty()) {
                        Text("Vitals History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        vitals.forEach { v ->
                            val bp = "${v.systolic_bp ?: "?"}/${v.diastolic_bp ?: "?"}"
                            val abnormal = v.is_abnormal
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (abnormal) ErrorRed.copy(alpha = 0.06f) else PureWhite),
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (abnormal) {
                                        Icon(Icons.Default.Warning, contentDescription = "Abnormal", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("BP: $bp | Pulse: ${v.pulse_bpm ?: "-"} | Temp: ${v.temperature_c ?: "-"}", fontSize = 11.sp, color = TitleBlack)
                                        Text("SpO2: ${v.spo2_pct ?: "-"}% | RR: ${v.respiratory_rate ?: "-"} | Glucose: ${v.blood_glucose ?: "-"}", fontSize = 11.sp, color = CoolGray)
                                    }
                                    Text(v.recorded_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalsInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(color = TitleBlack, fontSize = 14.sp),
    )
}
