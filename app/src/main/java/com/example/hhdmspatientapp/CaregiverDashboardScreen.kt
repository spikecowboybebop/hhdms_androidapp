package com.example.hhdmspatientapp

// ══════════════════════════════════════════════════════════════════════════════
// MAJOR CHANGE: Refactored from monolithic 941-line single screen into a
// hub-style dashboard (mirroring MbbsDoctorDashboardScreen pattern).
// Navigation is now callback-driven; each caregiver feature lives in its own
// dedicated screen file for maintainability and consistency with MBBS module.
// ══════════════════════════════════════════════════════════════════════════════

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPatientList: () -> Unit,
    onNavigateToCheckInOut: () -> Unit,
    onNavigateToActivityLog: () -> Unit,
    onNavigateToConditionReports: () -> Unit,
) {
    var profile by remember { mutableStateOf<CaregiverProfileResponse?>(null) }
    var patientCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val prof = RetrofitClient.apiService.getCaregiverProfile()
            val pats = RetrofitClient.apiService.getCaregiverPatients()
            profile = prof
            patientCount = pats.size
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load profile"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aastha", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("Tele-HealthCare", fontSize = 15.sp, fontWeight = FontWeight.Normal, color = PureWhite.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = PureWhite)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))),
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
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "Something went wrong", fontSize = 14.sp, color = CoolGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loading = true; error = null }) { Text("Retry", color = TechTeal) }
                    }
                }
            }
            else -> {
                val displayName = profile?.let {
                    "${it.user.firstNameEn ?: ""} ${it.user.lastNameEn ?: ""}".trim()
                } ?: "Caregiver"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Profile Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = PureWhite, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                Text("Caregiver", fontSize = 13.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                                Text("Assigned Patients: $patientCount", fontSize = 11.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── CG-006: Check-In/Out Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToCheckInOut),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFF22C55E).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Check-In / Check-Out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                Text("GPS-verified shift attendance", fontSize = 12.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Patient List Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToPatientList),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(TechTeal.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = TechTeal, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("My Patients", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                Text("View assigned patients & details", fontSize = 12.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Activity Log Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToActivityLog),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFF3B82F6).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Activity Log", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                Text("Log hygiene, mobility, feeding & more", fontSize = 12.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Condition Reports Card ──
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToConditionReports),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(AlertAmber.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Condition Reports", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                Text("Report falls, medication refusal & more", fontSize = 12.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Aastha Tele-HealthCare v1.0.0 — Caregiver Portal",
                        fontSize = 11.sp,
                        color = CoolGray.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}
