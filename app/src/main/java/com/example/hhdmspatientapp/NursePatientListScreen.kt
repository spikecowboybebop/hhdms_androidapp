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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NursePatientListScreen(
    onBack: () -> Unit,
    onPatientClick: (patientId: String, patientName: String) -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            patients = RetrofitClient.apiService.getNursePatients()
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load patients"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Patients", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        if (!loading) {
                            Text(
                                "${patients.size} patient${if (patients.size != 1) "s" else ""} assigned",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Could not load patients", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = CoolGray)
                        Text(error ?: "", fontSize = 13.sp, color = CoolGray.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
            }
            patients.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No patients assigned yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = CoolGray)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
                ) {
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        val initials = buildString {
                            append(patient.first_name_en.firstOrNull() ?: '—')
                            patient.last_name_en?.firstOrNull()?.let { append(it) }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPatientClick(patient.id, name) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(patient.mrn ?: "", fontSize = 12.sp, color = CoolGray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        patient.age_years?.let { age ->
                                            Text(
                                                "${age}y",
                                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                color = TechTeal,
                                                modifier = Modifier.background(TechTeal.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                        patient.patient_type?.let { type ->
                                            Text(
                                                type,
                                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                color = ClinicalNavy,
                                                modifier = Modifier.background(ClinicalNavy.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
