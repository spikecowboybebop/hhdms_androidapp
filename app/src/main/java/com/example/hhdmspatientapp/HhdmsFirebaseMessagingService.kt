package com.example.hhdmspatientapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HhdmsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        TokenManager.init(applicationContext)
        NotificationStorage.init(applicationContext)
        FcmTokenStorage.init(applicationContext)
        VisitStorage.init(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        FcmTokenStorage.saveToken(token)
        registerTokenWithServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.notification?.title}")

        val title = message.notification?.title ?: "Aastha Tele-HealthCare"
        val body = message.notification?.body ?: "You have a new update."
        val data = message.data

        NotificationStorage.addNotification(
            NotificationItem(
                id = NotificationStorage.nextId(),
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                sessionId = data["session_id"],
            ),
        )

        // Show system notification for all other types
        val isConsentRequest = data["type"] == "consent_request"
        showNotification(title, body, data["session_id"], if (isConsentRequest) data["patient_id"] else null)

        if (data["type"] == "doctor_coming") {
            val doctorName = body.substringBefore(" is coming to visit you")
            VisitStorage.saveVisitInfo(doctorName)
            data["patient_id"]?.let { pid ->
                VisitStorage.saveVisitingPatientId(pid)
            }
            Log.d(TAG, "Doctor visit saved: $doctorName (patient_id=${data["patient_id"]})")
        }

        if (data["type"] == "appointment_done") {
            VisitStorage.clearVisit()
            VisitStorage.clearVisitingPatientId()
            VisitStorage.saveAppointmentDone()
            Log.d(TAG, "Appointment done, visit cleared")
        }
    }

    private fun showNotification(title: String, body: String, sessionId: String?, consentPatientId: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            sessionId?.let { putExtra("session_id", it) }
            consentPatientId?.let { putExtra("consent_patient_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun registerTokenWithServer(token: String) {
        val existingToken = TokenManager.getToken()
        if (existingToken == null) {
            Log.d(TAG, "No auth token yet, skipping FCM registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.apiService.registerToken(
                    RegisterTokenRequest(token = token),
                )
                Log.d(TAG, "FCM token registered with server")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "HhdmsFCM"
        const val CHANNEL_ID = "service_bookings"

        fun registerCurrentToken() {
            if (TokenManager.getToken() == null) return

            val storedToken = FcmTokenStorage.getToken()
            if (storedToken != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.apiService.registerToken(
                            RegisterTokenRequest(token = storedToken),
                        )
                        Log.d(TAG, "FCM token registered from local storage")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to register stored FCM token: ${e.message}, trying Firebase fetch")
                        fetchAndRegisterFromFirebase()
                    }
                }
            } else {
                fetchAndRegisterFromFirebase()
            }
        }

        private fun fetchAndRegisterFromFirebase() {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fcmToken = task.result
                        FcmTokenStorage.saveToken(fcmToken)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitClient.apiService.registerToken(
                                    RegisterTokenRequest(token = fcmToken),
                                )
                                Log.d(TAG, "FCM token explicitly registered from Firebase")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to explicitly register FCM token: ${e.message}")
                            }
                        }
                    } else {
                        Log.e(TAG, "Firebase token retrieval failed: ${task.exception?.message}")
                    }
                }
        }
    }
}
