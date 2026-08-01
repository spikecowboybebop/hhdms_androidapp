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
fun NurseIVFluidScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var ivRecords by remember { mutableStateOf<List<IvFluidRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var fluidType by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var siteCondition by remember { mutableStateOf("") }

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
            snackbarText = "Failed to load: ${e.message}"
            loading = false
        }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { ivRecords = RetrofitClient.apiService.getIvFluidRecords(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("IV Fluid Monitoring", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-006", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (showForm || selectedPatient != null) {
                        if (showForm) { showForm = false; selectedPatient = null } else { selectedPatient = null }
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
                    item { Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient; showForm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFF1565C0).copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
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
                    Text("New IV Fluid Order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fluidType,
                        onValueChange = { fluidType = it },
                        label = { Text("Fluid Type * (e.g. Normal Saline, Ringer's Lactate)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = TitleBlack),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Rate (mL/hr) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = TitleBlack),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = siteCondition,
                        onValueChange = { siteCondition = it },
                        label = { Text("IV Site Condition") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = TitleBlack),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createIvFluidRecord(
                                        patient.id,
                                        NurseCreateIvFluidRequest(
                                            patient_id = patient.id,
                                            fluid_type = fluidType,
                                            rate_ml_hr = rate.toDoubleOrNull() ?: 0.0,
                                            site_condition = siteCondition.ifBlank { null },
                                        ),
                                    )
                                    snackbarText = "IV fluid order created!"
                                    showForm = false
                                    selectedPatient = null
                                    selectedPatient?.let { p -> ivRecords = RetrofitClient.apiService.getIvFluidRecords(p.id) }
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = !saving && fluidType.isNotBlank() && rate.isNotBlank(),
                    ) {
                        if (saving) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Save, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text("Start IV", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }

                    // Active IV Records
                    if (ivRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("IV Fluid Records", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        ivRecords.forEach { iv ->
                            val isActive = iv.status == "ACTIVE"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF1565C0).copy(alpha = 0.06f) else PureWhite),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = if (isActive) Color(0xFF1565C0) else CoolGray, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(iv.fluid_type ?: "Unknown", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.background(if (isActive) Color(0xFF1565C0).copy(alpha = 0.12f) else CoolGray.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(iv.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color(0xFF1565C0) else CoolGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Rate: ${iv.rate_ml_hr ?: "-"} mL/hr | Volume: ${iv.volume_given_ml ?: "-"} mL", fontSize = 11.sp, color = CoolGray)
                                    iv.site_condition?.let { Text("Site: $it", fontSize = 11.sp, color = CoolGray) }
                                    if (isActive) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            val newRate = (iv.rate_ml_hr ?: 0.0) + 10
                                                            RetrofitClient.apiService.updateIvFluidRecord(iv.id, NurseUpdateIvFluidRequest(rate_ml_hr = newRate))
                                                            selectedPatient?.let { p -> ivRecords = RetrofitClient.apiService.getIvFluidRecords(p.id) }
                                                            snackbarText = "Rate updated to $newRate mL/hr"
                                                        } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                                    }
                                                },
                                                modifier = Modifier.height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                            ) { Text("+10 mL/hr", fontSize = 11.sp) }

                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            RetrofitClient.apiService.updateIvFluidRecord(iv.id, NurseUpdateIvFluidRequest(status = "COMPLETED"))
                                                            selectedPatient?.let { p -> ivRecords = RetrofitClient.apiService.getIvFluidRecords(p.id) }
                                                            snackbarText = "IV stopped"
                                                        } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                                    }
                                                },
                                                modifier = Modifier.height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                            ) { Text("Stop IV", fontSize = 11.sp) }
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
