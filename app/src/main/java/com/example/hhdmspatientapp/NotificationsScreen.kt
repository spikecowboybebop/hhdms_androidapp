package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationTap: (sessionId: String?) -> Unit = {},
) {
    var notifications by remember { mutableStateOf(NotificationStorage.getNotifications()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                        )
                        val unread = notifications.count { !it.isRead }
                        if (unread > 0) {
                            Text(
                                text = "$unread unread",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    }
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
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = {
                            NotificationStorage.markAllAsRead()
                            notifications = NotificationStorage.getNotifications()
                        }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Mark all read",
                                tint = PureWhite,
                            )
                        }
                        IconButton(onClick = {
                            NotificationStorage.clearAll()
                            notifications = emptyList()
                        }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = PureWhite,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))
                    ),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = CoolGray.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoolGray,
                    )
                    Text(
                        text = "You'll see all of your notifications here",
                        fontSize = 13.sp,
                        color = CoolGray.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onClick = {
                            if (!item.isRead) {
                                NotificationStorage.markAsRead(item.id)
                                notifications = NotificationStorage.getNotifications()
                            }
                            onNotificationTap(item.sessionId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val bgColor = if (item.isRead) PureWhite else Color(0xFFE3F2FD)
    val indicatorColor = if (item.isRead) Color.Transparent else TechTeal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 3.dp else 5.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(10.dp)
                    .background(indicatorColor, CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.body,
                    fontSize = 13.sp,
                    color = CoolGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatTimestamp(item.timestamp),
                    fontSize = 11.sp,
                    color = CoolGray.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
