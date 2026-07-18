package com.example.hhdmspatientapp

data class NotificationItem(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: Long,
    val sessionId: String? = null,
    val isRead: Boolean = false,
)
