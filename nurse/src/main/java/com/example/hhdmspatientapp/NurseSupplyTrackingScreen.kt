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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NurseSupplyTrackingScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var supplyRecords by remember { mutableStateOf<List<SupplyUsageRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var supplyName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }

    val commonSupplies = listOf("Gloves (pair)", "Syringe", "IV Line", "Dressing Pack", "Cotton", "Bandage", "Alcohol Swab", "Catheter")

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try {
            patients = RetrofitClient.apiService.getNursePatients()
            supplyRecords = RetrofitClient.apiService.getSupplyUsageRecords(null)
            loading = false
        } catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { supplyRecords = RetrofitClient.apiService.getSupplyUsageRecords(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Supply Tracking", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-013", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
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
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                                    Box(modifier = Modifier.size(40.dp).background(SlateGray.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = SlateGray, modifier = Modifier.size(20.dp))
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
                    Text("Log Supply Usage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Quick select common supplies:", fontSize = 12.sp, color = CoolGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick select chips
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        commonSupplies.forEach { supply ->
                            FilterChip(
                                selected = supplyName == supply,
                                onClick = { supplyName = supply },
                                label = { Text(supply, fontSize = 11.sp) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = supplyName, onValueChange = { supplyName = it }, label = { Text("Supply Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty *") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color = TitleBlack), placeholder = { Text("e.g. pcs, pairs", fontSize = 12.sp) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createSupplyUsage(
                                        NurseCreateSupplyUsageRequest(
                                            patient_id = patient.id,
                                            supply_name = supplyName,
                                            quantity_used = quantity.toIntOrNull() ?: 1,
                                            unit = unit.ifBlank { null },
                                        ),
                                    )
                                    snackbarText = "Supply usage logged!"
                                    supplyName = ""; quantity = "1"; unit = ""
                                    selectedPatient?.let { p -> supplyRecords = RetrofitClient.apiService.getSupplyUsageRecords(p.id) }
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                        enabled = !saving && supplyName.isNotBlank(),
                    ) {
                        if (saving) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text("Log Usage", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }

                    // Supply History
                    if (supplyRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Usage History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        supplyRecords.forEach { s ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = SlateGray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.supply_name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                        Text("${s.quantity_used} ${s.unit ?: "pcs"}", fontSize = 11.sp, color = CoolGray)
                                    }
                                    Text(s.recorded_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
