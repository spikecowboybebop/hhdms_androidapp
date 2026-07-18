package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbbsVitalsScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
) {
    var vitalsList by remember { mutableStateOf<List<VitalSignsRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var spo2 by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun loadVitals() {
        scope.launch {
            loading = true
            error = null
            try {
                vitalsList = RetrofitClient.apiService.getMbbsPatientVitals(patientId)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadVitals() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showForm) "Record Vitals" else "Vital Signs",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (showForm) {{ showForm = false }} else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                actions = {
                    if (!showForm) {
                        IconButton(onClick = { showForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Vitals", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFE91E63), TechTeal))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (showForm) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text("Patient: $patientName", fontSize = 14.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Vital Signs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = systolic, onValueChange = { systolic = it }, label = { Text("Systolic BP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = diastolic, onValueChange = { diastolic = it }, label = { Text("Diastolic BP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = pulse, onValueChange = { pulse = it }, label = { Text("Pulse (bpm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temp (°C)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = spo2, onValueChange = { spo2 = it }, label = { Text("SpO2 (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = respiratoryRate, onValueChange = { respiratoryRate = it }, label = { Text("Resp. Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Clinical Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    submitting = true
                                    try {
                                        RetrofitClient.apiService.createMbbsVitals(patientId, CreateVitalsRequest(
                                            systolic_bp = systolic.toIntOrNull(),
                                            diastolic_bp = diastolic.toIntOrNull(),
                                            pulse_bpm = pulse.toIntOrNull(),
                                            temperature_c = temperature.toDoubleOrNull(),
                                            spo2_pct = spo2.toIntOrNull(),
                                            respiratory_rate = respiratoryRate.toIntOrNull(),
                                            weight_kg = weight.toDoubleOrNull(),
                                            height_cm = height.toDoubleOrNull(),
                                            notes = notes.ifBlank { null },
                                        ))
                                        systolic = ""; diastolic = ""; pulse = ""; temperature = ""
                                        spo2 = ""; respiratoryRate = ""; weight = ""; height = ""; notes = ""
                                        showForm = false
                                        loadVitals()
                                    } catch (e: Exception) {
                                        error = e.message
                                    }
                                    submitting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            enabled = !submitting,
                        ) {
                            if (submitting) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                            else Text("Save Vitals", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
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
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { loadVitals() }) { Text("Retry", color = TechTeal) }
                        }
                    }
                }
                vitalsList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No vitals recorded yet", fontSize = 16.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    val latest = vitalsList.firstOrNull()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    ) {
                        if (latest != null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text("Latest Readings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        VitalsGrid(vitals = latest)
                                    }
                                }
                            }
                        }
                        item {
                            Text("History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        items(vitalsList, key = { it.id }) { vitals ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(vitals.recorded_at?.take(16)?.replace("T", " ") ?: "—", fontSize = 12.sp, color = CoolGray)
                                        if (vitals.is_abnormal) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ABNORMAL", fontSize = 10.sp, color = ErrorRed, fontWeight = FontWeight.Bold, modifier = Modifier.background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        VitalsChip("BP", "${vitals.systolic_bp ?: "—"}/${vitals.diastolic_bp ?: "—"}")
                                        VitalsChip("Pulse", "${vitals.pulse_bpm ?: "—"}")
                                        VitalsChip("SpO2", "${vitals.spo2_pct ?: "—"}%")
                                        VitalsChip("Temp", "${vitals.temperature_c ?: "—"}")
                                    }
                                    val notes = vitals.notes
                                    if (!notes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(notes, fontSize = 12.sp, color = CoolGray)
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

@Composable
private fun VitalsGrid(vitals: VitalSignsRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalsDisplayCard("Systolic BP", "${vitals.systolic_bp ?: "—"}", "mmHg")
            VitalsDisplayCard("Diastolic BP", "${vitals.diastolic_bp ?: "—"}", "mmHg")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalsDisplayCard("Pulse", "${vitals.pulse_bpm ?: "—"}", "bpm")
            VitalsDisplayCard("Temperature", "${vitals.temperature_c ?: "—"}", "°C")
            VitalsDisplayCard("SpO2", "${vitals.spo2_pct ?: "—"}", "%")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalsDisplayCard("Resp. Rate", "${vitals.respiratory_rate ?: "—"}", "/min")
            VitalsDisplayCard("Weight", "${vitals.weight_kg ?: "—"}", "kg")
            VitalsDisplayCard("Height", "${vitals.height_cm ?: "—"}", "cm")
        }
        if (vitals.bmi != null) {
            VitalsDisplayCard("BMI", String.format("%.1f", vitals.bmi), "kg/m²")
        }
    }
}

@Composable
private fun VitalsDisplayCard(label: String, value: String, unit: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSlate),
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
            Text("$label ($unit)", fontSize = 10.sp, color = CoolGray)
        }
    }
}

@Composable
private fun VitalsChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = CoolGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
    }
}
