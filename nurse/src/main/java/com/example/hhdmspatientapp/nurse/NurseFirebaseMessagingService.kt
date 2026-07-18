package com.example.hhdmspatientapp.nurse

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.hhdmspatientapp.FcmTokenStorage
import com.example.hhdmspatientapp.RegisterTokenRequest
import com.example.hhdmspatientapp.RetrofitClient
import com.example.hhdmspatientapp.TokenManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NurseFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        FcmTokenStorage.saveToken(token)
        registerTokenWithServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.notification?.title}")

        val title = message.notification?.title ?: "Aastha Nurse"
        val body = message.notification?.body ?: "You have a new update."
        val data = message.data

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, NurseMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun registerTokenWithServer(token: String) {
        if (TokenManager.getToken() == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.apiService.registerToken(RegisterTokenRequest(token = token))
                Log.d(TAG, "FCM token registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "NurseFCM"
        const val CHANNEL_ID = "nurse_alerts"

        fun registerCurrentToken() {
            if (TokenManager.getToken() == null) return
            val storedToken = FcmTokenStorage.getToken()
            if (storedToken != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.apiService.registerToken(RegisterTokenRequest(token = storedToken))
                    } catch (_: Exception) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fcmToken = task.result
                                FcmTokenStorage.saveToken(fcmToken)
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        RetrofitClient.apiService.registerToken(RegisterTokenRequest(token = fcmToken))
                                    } catch (_: Exception) { }
                                }
                            }
                        }
                    }
                }
            } else {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fcmToken = task.result
                        FcmTokenStorage.saveToken(fcmToken)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitClient.apiService.registerToken(RegisterTokenRequest(token = fcmToken))
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }
}
