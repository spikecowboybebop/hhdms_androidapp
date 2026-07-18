package com.example.hhdmspatientapp

import androidx.compose.foundation.background
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
fun MbbsReferralScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
) {
    var referrals by remember { mutableStateOf<List<MbbsReferral>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var specialtyCode by remember { mutableStateOf("") }
    var referralReason by remember { mutableStateOf("") }
    var clinicalSummary by remember { mutableStateOf("") }
    var isEmergency by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val specialties = listOf(
        "CARDIOLOGY", "PULMONOLOGY", "NEUROLOGY", "NEPHROLOGY",
        "GASTROENTEROLOGY", "ENDOCRINOLOGY", "RHEUMATOLOGY", "DERMATOLOGY",
        "PSYCHIATRY", "ONCOLOGY", "ORTHOPEDICS",
    )

    fun loadReferrals() {
        scope.launch {
            loading = true
            error = null
            try {
                referrals = RetrofitClient.apiService.getMbbsReferrals(patientId)
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadReferrals() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showForm) "New Referral" else "Referrals",
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
                            Icon(Icons.Default.Add, contentDescription = "New Referral", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFF9800), TechTeal))),
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
                            Text("Referral Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                            Spacer(modifier = Modifier.height(12.dp))

                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = true }) {
                                OutlinedTextField(
                                    value = specialtyCode,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Specialty *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    specialties.forEach { spec ->
                                        DropdownMenuItem(
                                            text = { Text(spec) },
                                            onClick = { specialtyCode = spec; expanded = false },
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = referralReason, onValueChange = { referralReason = it }, label = { Text("Referral Reason *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = clinicalSummary, onValueChange = { clinicalSummary = it }, label = { Text("Clinical Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isEmergency, onCheckedChange = { isEmergency = it })
                                Icon(Icons.Default.Warning, contentDescription = null, tint = if (isEmergency) ErrorRed else CoolGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark as Emergency", fontSize = 13.sp, color = if (isEmergency) ErrorRed else CoolGray)
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
                                    RetrofitClient.apiService.createMbbsReferral(patientId, CreateReferralRequest(
                                        specialty_code = specialtyCode,
                                        referral_reason = referralReason,
                                        clinical_summary = clinicalSummary.ifBlank { null },
                                        is_emergency = if (isEmergency) true else null,
                                    ))
                                    specialtyCode = ""; referralReason = ""; clinicalSummary = ""; isEmergency = false
                                    showForm = false; loadReferrals()
                                } catch (e: Exception) { error = e.message }
                                submitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        enabled = !submitting && specialtyCode.isNotBlank() && referralReason.isNotBlank(),
                    ) {
                        if (submitting) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                        else Text("Submit Referral", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            TextButton(onClick = { loadReferrals() }) { Text("Retry", color = TechTeal) }
                        }
                    }
                }
                referrals.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No referrals yet", fontSize = 16.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(referrals, key = { it.id }) { ref ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(ref.specialty_code ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(ref.status ?: "", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(
                                            when (ref.status) {
                                                "PENDING" -> Color(0xFFFF9800); "ACCEPTED" -> TechTeal
                                                "REJECTED" -> ErrorRed; "COMPLETED" -> Color(0xFF4CAF50)
                                                "CANCELLED" -> CoolGray; else -> CoolGray
                                            }, RoundedCornerShape(4.dp),
                                        ).padding(horizontal = 8.dp, vertical = 2.dp))
                                        if (ref.is_emergency) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ref.referral_reason ?: "", fontSize = 13.sp, color = TitleBlack)
                                    val clinicalSummary = ref.clinical_summary
                                    if (!clinicalSummary.isNullOrBlank()) {
                                        Text(clinicalSummary, fontSize = 12.sp, color = CoolGray)
                                    }
                                    Text("Created: ${ref.created_at?.take(10) ?: "—"}", fontSize = 11.sp, color = CoolGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
