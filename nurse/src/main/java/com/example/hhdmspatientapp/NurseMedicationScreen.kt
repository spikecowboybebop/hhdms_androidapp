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
fun NurseMedicationScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var mar by remember { mutableStateOf<MedicationAdministrationRecord?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var drugName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("Oral") }
    var notes by remember { mutableStateOf("") }

    val routes = listOf("Oral", "IV", "IM", "SC", "Topical", "Inhaled", "Rectal", "Sublingual")

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
                mar = RetrofitClient.apiService.getMedicationAdministrations(p.id)
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medication Administration", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-005 — MAR", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
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
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF7B1FA2), TechTeal))),
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
                    item { Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
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
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFF7B1FA2).copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(name.take(2), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
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
                    Text("Log Medication Administered", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = drugName,
                        onValueChange = { drugName = it },
                        label = { Text("Drug Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = TitleBlack),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dose *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )
                        // Route dropdown
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                value = route,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Route") },
                                textStyle = TextStyle(color = TitleBlack),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                routes.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = { route = r; expanded = false },
                                    )
                                }
                            }
                        }
                    }
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
                                    RetrofitClient.apiService.createMedicationAdministration(
                                        patient.id,
                                        NurseCreateMedicationAdminRequest(
                                            patient_id = patient.id,
                                            drug_name = drugName,
                                            dosage = dosage.ifBlank { null },
                                            route = route,
                                            notes = notes.ifBlank { null },
                                        ),
                                    )
                                    snackbarText = "Medication logged successfully!"
                                    showForm = false
                                    selectedPatient?.let { p ->
                                        mar = RetrofitClient.apiService.getMedicationAdministrations(p.id)
                                    }
                                } catch (e: Exception) {
                                    snackbarText = "Error: ${e.message}"
                                }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        enabled = !saving && drugName.isNotBlank(),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Medication", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // MAR History
                    mar?.let { record ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Medication Administration Record", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Text("Visit: ${record.visit_date ?: "N/A"}", fontSize = 11.sp, color = CoolGray)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (record.entries.isEmpty()) {
                            Text("No medications logged for this visit.", fontSize = 12.sp, color = CoolGray, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                        } else {
                            record.entries.forEach { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.drug_name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                            Text("${entry.dosage ?: "-"} | ${entry.route ?: "-"}", fontSize = 11.sp, color = CoolGray)
                                        }
                                        Text(entry.administered_at?.take(16)?.replace("T", " ") ?: "", fontSize = 10.sp, color = CoolGray)
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
