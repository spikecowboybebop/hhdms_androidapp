package com.example.hhdmspatientapp

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.hhdmspatientapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
) {
    var patient by remember { mutableStateOf<MbbsPatientSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isVisiting by remember { mutableStateOf(VisitStorage.getVisitingPatientId() == patientId) }
    var showVisitConfirm by remember { mutableStateOf(false) }
    var visitDoctorName by remember { mutableStateOf("") }
    var visitError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(patientId) {
        try {
            val response = RetrofitClient.apiService.getMbbsPatientProfile(patientId)
            patient = response.patient
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load patient"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)),
                    ),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TechTeal)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CoolGray.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "Something went wrong",
                            fontSize = 14.sp,
                            color = CoolGray,
                        )
                    }
                }
            }

            patient != null -> {
                val p = patient!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    // ── Profile Header ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val initials = buildString {
                                append(p.first_name_en.firstOrNull() ?: '—')
                                p.last_name_en?.firstOrNull()?.let { append(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${p.first_name_en} ${p.last_name_en ?: ""}".trim(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleBlack,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = p.mrn,
                                fontSize = 13.sp,
                                color = TechTeal,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (p.has_emergency_flag == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Emergency Flag",
                                        fontSize = 12.sp,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    // ── Error Banner ──
                    if (visitError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = visitError!!,
                                    fontSize = 13.sp,
                                    color = ErrorRed,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Visit Patient ──
                    if (!isVisiting) {
                        Button(
                            onClick = { showVisitConfirm = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechTeal,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Visit Patient", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition()
                        val blinkAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = TechTeal.copy(alpha = 0.1f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TechTeal.copy(alpha = blinkAlpha),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "You are visiting the patient",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechTeal.copy(alpha = blinkAlpha),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Patient Info ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "Patient Information",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleBlack,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailRow(icon = Icons.Default.CalendarMonth, label = "Date of Birth", value = p.date_of_birth ?: "—")
                            HorizontalDivider(color = SoftSlate, thickness = 1.dp)
                            DetailRow(icon = Icons.Default.Person, label = "Gender", value = when (p.sex) {
                                "M" -> "Male"; "F" -> "Female"; else -> p.sex ?: "—"
                            })
                            HorizontalDivider(color = SoftSlate, thickness = 1.dp)
                            DetailRow(icon = Icons.Default.Bloodtype, label = "Blood Group", value = p.blood_group ?: "—")
                            HorizontalDivider(color = SoftSlate, thickness = 1.dp)
                            DetailRow(icon = Icons.Default.Phone, label = "Phone", value = p.phone_number ?: "—")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Clinical Actions ──
                    Text(
                        text = "Clinical Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitleBlack,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalActionCard(
                            icon = Icons.Default.FavoriteBorder,
                            label = "Vitals",
                            color = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f),
                        )
                        ClinicalActionCard(
                            icon = Icons.Default.Description,
                            label = "Diagnosis",
                            color = TechTeal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalActionCard(
                            icon = Icons.Default.LocalPharmacy,
                            label = "Prescription",
                            color = ClinicalNavy,
                            modifier = Modifier.weight(1f),
                        )
                        ClinicalActionCard(
                            icon = Icons.Default.Science,
                            label = "Test Orders",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (showVisitConfirm) {
                    AlertDialog(
                        onDismissRequest = { showVisitConfirm = false },
                        containerColor = PureWhite,
                        titleContentColor = TitleBlack,
                        textContentColor = CoolGray,
                        title = { Text("Start Visit", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to start visiting this patient?") },
                        confirmButton = {
                            Button(onClick = {
                                showVisitConfirm = false
                                visitError = null
                                isVisiting = true
                                VisitStorage.saveVisitingPatientId(patientId)
                                scope.launch {
                                    try {
                                        val result = RetrofitClient.apiService.startPatientVisit(patientId)
                                        visitDoctorName = (result["doctor_name"] as? String) ?: ""
                                    } catch (e: Exception) {
                                        visitError = e.message ?: "Failed to start visit. Patient may not have a linked account."
                                    }
                                }
                            }) {
                                Text("Yes, Start Journey", color = PureWhite)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showVisitConfirm = false }) {
                                Text("Cancel", color = CoolGray)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CoolGray,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = CoolGray,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TitleBlack,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClinicalActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
            )
        }
    }
}
