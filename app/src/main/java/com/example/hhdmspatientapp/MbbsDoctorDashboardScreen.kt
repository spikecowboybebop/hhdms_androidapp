package com.example.hhdmspatientapp

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbbsDoctorDashboardScreen(
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPatientAssignments: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
) {
    val doctorName = userEmail.substringBefore("@")
    val displayName = doctorName.replaceFirstChar { it.uppercase() }
    var conversations by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var unreadCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            conversations = RetrofitClient.apiService.getChatConversations()
        } catch (_: Exception) {}
        try {
            val counts = RetrofitClient.apiService.getChatUnreadCounts()
            unreadCounts = counts.associate { it.conversationId to it.unreadCount }
        } catch (_: Exception) {}
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

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
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = PureWhite, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Text("MBBS (General Practitioner)", fontSize = 13.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                        Text("BMDC Reg: — | Specialization: General Medicine", fontSize = 11.sp, color = CoolGray)
                    }
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToPatientAssignments),
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
                        Text("Patient Assignments", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                        Text("View your assigned patients", fontSize = 12.sp, color = CoolGray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (conversations.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = TechTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Patient Chats", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                            Spacer(modifier = Modifier.weight(1f))
                            val totalUnread = unreadCounts.values.sum()
                            if (totalUnread > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFFFF3B30), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (totalUnread > 9) "9+" else "$totalUnread",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        conversations.forEach { conv ->
                            val patientName = conv.patient?.let { "${it.first_name_en ?: ""} ${it.last_name_en ?: ""}".trim() } ?: "Patient"
                            val lastMsg = conv.messages.lastOrNull()
                            val unread = unreadCounts[conv.id] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigateToChat(conv.id, patientName)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(TechTeal.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = patientName.firstOrNull()?.uppercase() ?: "?",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TechTeal,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patientName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    Text(
                                        text = lastMsg?.content ?: "Click to start chat",
                                        fontSize = 12.sp,
                                        color = CoolGray,
                                        maxLines = 1,
                                    )
                                }
                                if (unread > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFFFF3B30), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (unread > 9) "9+" else "$unread",
                                            color = PureWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Aastha Tele-HealthCare v1.0.0 — MBBS Portal",
                fontSize = 11.sp,
                color = CoolGray.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
