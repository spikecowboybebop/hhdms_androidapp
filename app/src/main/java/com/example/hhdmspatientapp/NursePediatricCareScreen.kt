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
fun NursePediatricCareScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var loading by remember { mutableStateOf(true) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=Feeding, 1=Growth, 2=Vaccination
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Feeding
    var feedingLogs by remember { mutableStateOf<List<FeedingLog>>(emptyList()) }
    var feedingType by remember { mutableStateOf("") }
    var feedVolume by remember { mutableStateOf("") }
    var feedFrequency by remember { mutableStateOf("") }

    // Growth
    var growthRecords by remember { mutableStateOf<List<GrowthRecord>>(emptyList()) }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var headCirc by remember { mutableStateOf("") }

    // Vaccination
    var vaccinationRecords by remember { mutableStateOf<List<VaccinationRecord>>(emptyList()) }
    var vaccineName by remember { mutableStateOf("") }
    var doseNumber by remember { mutableStateOf("") }
    var nextDue by remember { mutableStateOf("") }

    val feedTypes = listOf("Breast Milk", "Formula", "Mixed", "Solid", "TPN")

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try { patients = RetrofitClient.apiService.getNursePatients(); loading = false }
        catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient, activeTab) {
        selectedPatient?.let { p ->
            try {
                when (activeTab) {
                    0 -> feedingLogs = RetrofitClient.apiService.getFeedingLogs(p.id)
                    1 -> growthRecords = RetrofitClient.apiService.getGrowthRecords(p.id)
                    2 -> vaccinationRecords = RetrofitClient.apiService.getVaccinationRecords(p.id)
                }
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pediatric Care", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-014", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectedPatient != null) selectedPatient = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFEC407A), TechTeal))),
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
                    item { Text("Select Pediatric Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFFEC407A).copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ChildCare, contentDescription = null, tint = Color(0xFFEC407A), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    Row { patient.mrn?.let { Text(it, fontSize = 11.sp, color = CoolGray) }; patient.age_years?.let { Spacer(modifier = Modifier.width(8.dp)); Text("Age: ${it}y", fontSize = 11.sp, color = Color(0xFFEC407A)) } }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                            }
                        }
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Tabs
                    TabRow(selectedTabIndex = activeTab, containerColor = PureWhite, contentColor = Color(0xFFEC407A)) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Feeding", fontSize = 12.sp) })
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Growth", fontSize = 12.sp) })
                        Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Vaccines", fontSize = 12.sp) })
                    }

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        when (activeTab) {
                            0 -> {
                                Text("Feeding Log", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                Spacer(modifier = Modifier.height(8.dp))

                                var expandedFeed by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expandedFeed, onExpandedChange = { expandedFeed = !expandedFeed }, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = feedingType, onValueChange = {}, readOnly = true, label = { Text("Feeding Type *") }, textStyle = TextStyle(color = TitleBlack), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFeed) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                                    ExposedDropdownMenu(expanded = expandedFeed, onDismissRequest = { expandedFeed = false }) {
                                        feedTypes.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { feedingType = t; expandedFeed = false }) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = feedVolume, onValueChange = { feedVolume = it }, label = { Text("Volume (mL)") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                    OutlinedTextField(value = feedFrequency, onValueChange = { feedFrequency = it }, label = { Text("Frequency") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color = TitleBlack), placeholder = { Text("e.g. 3x/day") })
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val p = selectedPatient ?: return@launch
                                                RetrofitClient.apiService.createFeedingLog(p.id, NurseCreateFeedingLogRequest(p.id, feedingType, feedVolume.toDoubleOrNull(), feedFrequency.ifBlank { null }))
                                                snackbarText = "Feeding log saved!"; feedingType = ""; feedVolume = ""; feedFrequency = ""
                                                feedingLogs = RetrofitClient.apiService.getFeedingLogs(p.id)
                                            } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC407A)),
                                    enabled = feedingType.isNotBlank(),
                                ) { Text("Log Feeding", fontWeight = FontWeight.Bold) }

                                if (feedingLogs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Feeding History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    feedingLogs.forEach { f ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(f.feeding_type ?: "-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                                    Text("Vol: ${f.volume_ml ?: "-"} mL | Freq: ${f.frequency ?: "-"}", fontSize = 11.sp, color = CoolGray)
                                                }
                                                Text(f.recorded_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                Text("Growth Monitoring", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Track weight, height, and head circumference vs WHO growth charts", fontSize = 12.sp, color = CoolGray)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = headCirc, onValueChange = { headCirc = it }, label = { Text("Head Circumference (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val p = selectedPatient ?: return@launch
                                                RetrofitClient.apiService.createGrowthRecord(p.id, NurseCreateGrowthRecordRequest(p.id, weight.toDoubleOrNull(), height.toDoubleOrNull(), headCirc.toDoubleOrNull()))
                                                snackbarText = "Growth record saved!"; weight = ""; height = ""; headCirc = ""
                                                growthRecords = RetrofitClient.apiService.getGrowthRecords(p.id)
                                            } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC407A)),
                                ) { Text("Save Growth Record", fontWeight = FontWeight.Bold) }

                                if (growthRecords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Growth History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    growthRecords.forEach { g ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("W: ${g.weight_kg ?: "-"} kg | H: ${g.height_cm ?: "-"} cm | HC: ${g.head_circumference_cm ?: "-"} cm", fontSize = 12.sp, color = TitleBlack)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        g.percentile_weight?.let { Text("Wtile: ${String.format("%.0f", it)}%", fontSize = 10.sp, color = TechTeal) }
                                                        g.percentile_height?.let { Text("Htile: ${String.format("%.0f", it)}%", fontSize = 10.sp, color = TechTeal) }
                                                    }
                                                }
                                                Text(g.recorded_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Text("Vaccination Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(value = vaccineName, onValueChange = { vaccineName = it }, label = { Text("Vaccine Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = doseNumber, onValueChange = { doseNumber = it }, label = { Text("Dose #") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                    OutlinedTextField(value = nextDue, onValueChange = { nextDue = it }, label = { Text("Next Due (YYYY-MM-DD)") }, modifier = Modifier.weight(1.5f), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val p = selectedPatient ?: return@launch
                                                RetrofitClient.apiService.createVaccinationRecord(p.id, NurseCreateVaccinationRequest(p.id, vaccineName, doseNumber.toIntOrNull(), nextDue.ifBlank { null }))
                                                snackbarText = "Vaccination recorded!"; vaccineName = ""; doseNumber = ""; nextDue = ""
                                                vaccinationRecords = RetrofitClient.apiService.getVaccinationRecords(p.id)
                                            } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC407A)),
                                    enabled = vaccineName.isNotBlank(),
                                ) { Text("Record Vaccination", fontWeight = FontWeight.Bold) }

                                if (vaccinationRecords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Vaccination History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    vaccinationRecords.forEach { v ->
                                        val statusColor = when (v.status) { "COMPLETED" -> Color(0xFF4CAF50); "SCHEDULED" -> AlertAmber; else -> CoolGray }
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Vaccines, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(v.vaccine_name ?: "-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                                    Text("Dose ${v.dose_number ?: "-"} | Administered: ${v.administered_date?.take(10) ?: "-"}", fontSize = 11.sp, color = CoolGray)
                                                }
                                                Box(modifier = Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                    Text(v.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
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
    }
}
