package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun MbbsDoctorDashboardScreen(
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
) {
    val doctorName = userEmail.substringBefore("@")
    val displayName = doctorName.replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MBBS Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                        )
                        Text(
                            text = "Aastha Tele-HealthCare",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = PureWhite.copy(alpha = 0.8f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = PureWhite,
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = PureWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))
                ),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Doctor Profile Card ──
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
                            .background(
                                Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dr. $displayName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TitleBlack,
                        )
                        Text(
                            text = "MBBS (General Practitioner)",
                            fontSize = 13.sp,
                            color = TechTeal,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "BMDC Reg: — | Specialization: General Medicine",
                            fontSize = 11.sp,
                            color = CoolGray,
                        )
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Stats Row ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = "—",
                    label = "Total Patients",
                    color = TechTeal,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "—",
                    label = "Today",
                    color = ClinicalNavy,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "—",
                    label = "Pending",
                    color = AlertAmber,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Actions ──
            Text(
                text = "Clinical Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DoctorActionCard(
                    icon = Icons.Default.FavoriteBorder,
                    label = "Vitals",
                    color = Color(0xFFE91E63),
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                DoctorActionCard(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Diagnosis",
                    color = TechTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DoctorActionCard(
                    icon = Icons.Default.LocalPharmacy,
                    label = "Prescription",
                    color = ClinicalNavy,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                DoctorActionCard(
                    icon = Icons.Default.Science,
                    label = "Test Orders",
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DoctorActionCard(
                    icon = Icons.Default.Share,
                    label = "Referral",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                DoctorActionCard(
                    icon = Icons.Default.Warning,
                    label = "Emergency",
                    color = ErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Recent Patients ──
            Text(
                text = "Assigned Patients",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            PatientListItem(
                name = "—",
                phone = "—",
                condition = "No data yet",
                onClick = { },
            )

            Spacer(modifier = Modifier.height(12.dp))

            PatientListItem(
                name = "—",
                phone = "—",
                condition = "Sync pending",
                onClick = { },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Recent Activity ──
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = CoolGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recent activity",
                        fontSize = 14.sp,
                        color = CoolGray,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Connect to the server to sync your patient data",
                        fontSize = 12.sp,
                        color = CoolGray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Aastha Tele-HealthCare v1.0.0 — MBBS Portal",
                fontSize = 11.sp,
                color = CoolGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = CoolGray,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun DoctorActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
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

@Composable
fun PatientListItem(
    name: String,
    phone: String,
    condition: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TechTeal.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(2).ifEmpty { "—" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TechTeal,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifEmpty { "Unknown Patient" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleBlack,
                )
                Text(
                    text = phone.ifEmpty { "No phone" },
                    fontSize = 12.sp,
                    color = CoolGray,
                )
            }
            Text(
                text = condition,
                fontSize = 11.sp,
                color = AlertAmber,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
