package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

data class MedicationEntry(
    val genericName: String = "",
    val brandName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val durationDays: String = "",
    val route: String = "Oral",
    val specialInstructions: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbbsPrescriptionScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
) {
    var prescriptions by remember { mutableStateOf<List<PrescriptionRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var notes by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf(listOf(MedicationEntry())) }
    var submitting by remember { mutableStateOf(false) }

    val routeOptions = listOf("Oral", "IV", "IM", "Subcutaneous", "Topical", "Inhalation", "Ophthalmic", "Otic", "Nasal", "Rectal", "Vaginal", "Sublingual", "Intraosseous")

    fun loadPrescriptions() {
        scope.launch {
            loading = true
            error = null
            try {
                prescriptions = RetrofitClient.apiService.getMbbsPatientPrescriptions(patientId)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadPrescriptions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showForm) "New Prescription" else "Prescriptions",
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
                            Icon(Icons.Default.Add, contentDescription = "Add Prescription", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ClinicalNavy, TechTeal))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (showForm) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                item {
                    Text("Patient: $patientName", fontSize = 14.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Prescription Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }
                    }
                }

                item {
                    Text("Medications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                }

                itemsIndexed(medications) { index: Int, med: MedicationEntry ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Medication #${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TechTeal)
                                Spacer(modifier = Modifier.weight(1f))
                                if (medications.size > 1) {
                                    IconButton(onClick = { medications = medications.toMutableList().apply { removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(value = med.genericName, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(genericName = v) } }, label = { Text("Generic Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(value = med.brandName, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(brandName = v) } }, label = { Text("Brand Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = med.dosage, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(dosage = v) } }, label = { Text("Dosage *") }, placeholder = { Text("e.g. 500mg") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = med.frequency, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(frequency = v) } }, label = { Text("Frequency *") }, placeholder = { Text("e.g. 1+0+1") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = med.durationDays, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(durationDays = v) } }, label = { Text("Days *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.5f), singleLine = true)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                var expanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = true }) {
                                    OutlinedTextField(
                                        value = med.route,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Route *") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier.menuAnchor().weight(1f),
                                        singleLine = true,
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        routeOptions.forEach { route ->
                                            DropdownMenuItem(
                                                text = { Text(route) },
                                                onClick = {
                                                    medications = medications.toMutableList().also { it[index] = it[index].copy(route = route) }
                                                    expanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(value = med.specialInstructions, onValueChange = { v -> medications = medications.toMutableList().also { it[index] = it[index].copy(specialInstructions = v) } }, label = { Text("Special Instructions") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                    }
                }

                item {
                    TextButton(onClick = { medications = medications + MedicationEntry() }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Medication", color = TechTeal)
                    }
                }

                item {
                    Button(
                        onClick = {
                            scope.launch {
                                submitting = true
                                try {
                                    RetrofitClient.apiService.createMbbsPrescription(patientId, CreatePrescriptionRequest(
                                        notes = notes.ifBlank { null },
                                        medications = medications.filter { it.genericName.isNotBlank() }.map {
                                            CreateMedicationRequest(
                                                generic_name = it.genericName,
                                                brand_name = it.brandName.ifBlank { null },
                                                dosage = it.dosage,
                                                frequency = it.frequency,
                                                duration_days = it.durationDays.toIntOrNull() ?: 0,
                                                route = it.route,
                                                special_instructions = it.specialInstructions.ifBlank { null },
                                            )
                                        },
                                    ))
                                    notes = ""; medications = listOf(MedicationEntry())
                                    showForm = false
                                    loadPrescriptions()
                                } catch (e: Exception) {
                                    error = e.message
                                }
                                submitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalNavy),
                        enabled = !submitting && medications.any { it.genericName.isNotBlank() && it.dosage.isNotBlank() && it.frequency.isNotBlank() },
                    ) {
                        if (submitting) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                        else Text("Issue Prescription", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            TextButton(onClick = { loadPrescriptions() }) { Text("Retry", color = TechTeal) }
                        }
                    }
                }
                prescriptions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No prescriptions yet", fontSize = 16.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(prescriptions, key = { it.id }) { rx ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(rx.issued_at?.take(10) ?: "", fontSize = 12.sp, color = CoolGray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(rx.status ?: "", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(
                                            when (rx.status) { "ACTIVE" -> TechTeal; "COMPLETED" -> CoolGray; "DISCONTINUED" -> ErrorRed; else -> CoolGray },
                                            RoundedCornerShape(4.dp),
                                        ).padding(horizontal = 8.dp, vertical = 2.dp))
                                        if (!rx.digital_signature_url.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Verified, contentDescription = "Signed", tint = TechTeal, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    rx.medications.forEachIndexed { i, med ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("${i + 1}.", fontSize = 12.sp, color = CoolGray, modifier = Modifier.width(20.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(med.generic_name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                                Text("${med.dosage} - ${med.frequency} - ${med.duration_days}d - ${med.route}", fontSize = 12.sp, color = CoolGray)
                                                if (!med.special_instructions.isNullOrBlank()) {
                                                    Text("Note: ${med.special_instructions}", fontSize = 11.sp, color = AlertAmber)
                                                }
                                            }
                                        }
                                        if (i < rx.medications.lastIndex) Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    if (!rx.notes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Notes: ${rx.notes}", fontSize = 12.sp, color = CoolGray)
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
