package com.example.hhdmspatientapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object NotificationStorage {
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val MAX_NOTIFICATIONS = 100
    private val gson = Gson()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addNotification(item: NotificationItem) {
        val list = getNotifications().toMutableList()
        list.add(0, item)
        if (list.size > MAX_NOTIFICATIONS) {
            list.removeAt(list.lastIndex)
        }
        save(list)
    }

    fun getNotifications(): List<NotificationItem> {
        val json = prefs?.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun markAsRead(notificationId: Long) {
        val list = getNotifications().map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        save(list)
    }

    fun markAllAsRead() {
        val list = getNotifications().map { it.copy(isRead = true) }
        save(list)
    }

    fun clearAll() {
        prefs?.edit()?.remove(KEY_NOTIFICATIONS)?.apply()
    }

    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }

    private fun save(list: List<NotificationItem>) {
        prefs?.edit()?.putString(KEY_NOTIFICATIONS, gson.toJson(list))?.apply()
    }

    private var lastId: Long = System.currentTimeMillis()

    fun nextId(): Long = ++lastId
}
