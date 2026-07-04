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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbbsDiagnosisScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
) {
    var diagnoses by remember { mutableStateOf<List<DiagnosisRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var icd10Code by remember { mutableStateOf("") }
    var icd10Description by remember { mutableStateOf("") }
    var chiefComplaint by remember { mutableStateOf("") }
    var historyOfPresentIllness by remember { mutableStateOf("") }
    var reviewOfSystems by remember { mutableStateOf("") }
    var examinationFindings by remember { mutableStateOf("") }
    var preliminaryDiagnosis by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }

    var icd10SearchQuery by remember { mutableStateOf("") }
    var icd10Results by remember { mutableStateOf<List<Icd10Code>>(emptyList()) }
    var showIcd10Search by remember { mutableStateOf(false) }
    var icd10Searching by remember { mutableStateOf(false) }

    fun loadDiagnoses() {
        scope.launch {
            loading = true
            error = null
            try {
                diagnoses = RetrofitClient.apiService.getMbbsPatientDiagnoses(patientId)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadDiagnoses() }

    LaunchedEffect(icd10SearchQuery) {
        if (icd10SearchQuery.length >= 2 && showIcd10Search) {
            icd10Searching = true
            try {
                icd10Results = RetrofitClient.apiService.searchIcd10(icd10SearchQuery)
            } catch (_: Exception) { }
            icd10Searching = false
        } else {
            icd10Results = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showForm) "Add Diagnosis" else "Diagnoses",
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
                            Icon(Icons.Default.Add, contentDescription = "Add Diagnosis", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))),
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
                            Text("ICD-10 Code", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = icd10SearchQuery,
                                onValueChange = {
                                    icd10SearchQuery = it
                                    showIcd10Search = true
                                },
                                label = { Text("Search ICD-10 codes") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (icd10Searching) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                },
                            )

                            if (showIcd10Search && icd10Results.isNotEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = SoftSlate),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                        icd10Results.take(10).forEach { code ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        icd10Code = code.code
                                                        icd10Description = code.description
                                                        icd10SearchQuery = "${code.code} - ${code.description}"
                                                        showIcd10Search = false
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(code.code, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(code.description, fontSize = 12.sp, color = CoolGray, modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (icd10Code.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Selected: $icd10Code - $icd10Description", fontSize = 13.sp, color = TechTeal, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Clinical Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(value = chiefComplaint, onValueChange = { chiefComplaint = it }, label = { Text("Chief Complaint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = historyOfPresentIllness, onValueChange = { historyOfPresentIllness = it }, label = { Text("History of Present Illness") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = reviewOfSystems, onValueChange = { reviewOfSystems = it }, label = { Text("Review of Systems") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = examinationFindings, onValueChange = { examinationFindings = it }, label = { Text("Examination Findings") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = preliminaryDiagnosis, onValueChange = { preliminaryDiagnosis = it }, label = { Text("Preliminary Diagnosis *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                                Text("Set as primary diagnosis", fontSize = 13.sp, color = CoolGray)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            scope.launch {
                                submitting = true
                                try {
                                    RetrofitClient.apiService.createMbbsDiagnosis(patientId, CreateDiagnosisRequest(
                                        icd10_code = icd10Code,
                                        chief_complaint = chiefComplaint.ifBlank { null },
                                        history_of_present_illness = historyOfPresentIllness.ifBlank { null },
                                        review_of_systems = reviewOfSystems.ifBlank { null },
                                        examination_findings = examinationFindings.ifBlank { null },
                                        preliminary_diagnosis = preliminaryDiagnosis,
                                        is_primary = isPrimary,
                                    ))
                                    icd10Code = ""; icd10Description = ""; icd10SearchQuery = ""
                                    chiefComplaint = ""; historyOfPresentIllness = ""
                                    reviewOfSystems = ""; examinationFindings = ""; preliminaryDiagnosis = ""
                                    isPrimary = true
                                    showForm = false
                                    loadDiagnoses()
                                } catch (e: Exception) {
                                    error = e.message
                                }
                                submitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                        enabled = !submitting && preliminaryDiagnosis.isNotBlank() && icd10Code.isNotBlank(),
                    ) {
                        if (submitting) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                        else Text("Save Diagnosis", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            TextButton(onClick = { loadDiagnoses() }) { Text("Retry", color = TechTeal) }
                        }
                    }
                }
                diagnoses.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No diagnoses recorded", fontSize = 16.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(diagnoses, key = { it.id }) { diagnosis ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(diagnosis.icd10_code ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                        }
                                        if (diagnosis.is_primary) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PRIMARY", fontSize = 10.sp, color = AlertAmber, fontWeight = FontWeight.Bold, modifier = Modifier.background(AlertAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(diagnosis.diagnosed_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(diagnosis.preliminary_diagnosis ?: diagnosis.icd10?.description ?: "", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    if (!diagnosis.chief_complaint.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Chief Complaint: ${diagnosis.chief_complaint}", fontSize = 12.sp, color = CoolGray)
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
