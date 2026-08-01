package com.example.hhdmspatientapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseDashboardScreen(
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToPatientList: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToIVFluid: () -> Unit,
    onNavigateToWoundCare: () -> Unit,
    onNavigateToCareReport: () -> Unit,
    onNavigateToHandover: () -> Unit,
    onNavigateToConsultation: () -> Unit,
    onNavigateToSupplyTracking: () -> Unit,
    onNavigateToPediatricCare: () -> Unit,
) {
    var profile by remember { mutableStateOf<NurseProfileResponse?>(null) }
    var patientCount by remember { mutableIntStateOf(0) }
    var todayScheduleCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val prof = RetrofitClient.apiService.getNurseProfile()
            profile = prof
            try {
                val pats = RetrofitClient.apiService.getNursePatients()
                patientCount = pats.size
            } catch (_: Exception) { }
            try {
                val today = java.time.LocalDate.now().toString()
                val schedule = RetrofitClient.apiService.getNurseSchedule(today)
                todayScheduleCount = schedule.size
            } catch (_: Exception) { }
        } catch (_: Exception) { }
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
                } ?: "Nurse"
                val nurseType = profile?.nurse_type
                val nurseTypeLabel = when (nurseType) {
                    "PEDIATRIC" -> "Pediatric Nurse"
                    "ADULT" -> "Adult Nurse"
                    else -> "Registered Nurse"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Card
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
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = PureWhite, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                Text(nurseTypeLabel, fontSize = 13.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                                Text("Patients: $patientCount | Today's Visits: $todayScheduleCount", fontSize = 11.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Today's Schedule Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNavigateToSchedule,
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF22C55E).copy(alpha = 0.08f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFF22C55E).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Today's Schedule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                Text("$todayScheduleCount patient visits planned", fontSize = 12.sp, color = CoolGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feature Grid
                    Text(
                        text = "Nursing Tools",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitleBlack,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NurseDashboardCard(
                            icon = Icons.Default.People,
                            label = "Patients",
                            color = TechTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPatientList,
                        )
                        NurseDashboardCard(
                            icon = Icons.Default.MonitorHeart,
                            label = "Vitals",
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToVitals,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NurseDashboardCard(
                            icon = Icons.Default.Medication,
                            label = "Medications",
                            color = Color(0xFF7B1FA2),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMedication,
                        )
                        NurseDashboardCard(
                            icon = Icons.Default.WaterDrop,
                            label = "IV Fluids",
                            color = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToIVFluid,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NurseDashboardCard(
                            icon = Icons.Default.Healing,
                            label = "Wound Care",
                            color = Color(0xFFE65100),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToWoundCare,
                        )
                        NurseDashboardCard(
                            icon = Icons.Default.Description,
                            label = "Care Report",
                            color = ClinicalNavy,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCareReport,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NurseDashboardCard(
                            icon = Icons.Default.SwapHoriz,
                            label = "Handover",
                            color = Color(0xFF00897B),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToHandover,
                        )
                        NurseDashboardCard(
                            icon = Icons.Default.ContactPhone,
                            label = "Consult",
                            color = AlertAmber,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToConsultation,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NurseDashboardCard(
                            icon = Icons.Default.Inventory2,
                            label = "Supplies",
                            color = SlateGray,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSupplyTracking,
                        )
                        NurseDashboardCard(
                            icon = Icons.Default.ChildCare,
                            label = "Pediatric",
                            color = Color(0xFFEC407A),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPediatricCare,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Aastha Tele-HealthCare v1.0.0 — Nurse Portal",
                        fontSize = 11.sp,
                        color = CoolGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NurseDashboardCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack, textAlign = TextAlign.Center)
        }
    }
}
