package com.example.hhdmspatientapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH, DASHBOARD
}

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(applicationContext)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        registerExistingFcmToken()

        // 🚀 BABY STEP: Warm up the WebRTC hardware factory and connection pipelines instantly!
        CallSignalingManager.initialize(applicationContext)

        setContent {
            HHDMSPatientAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SoftSlate,
                ) {
                    // 1. Set up the local navigation state containers
                    var currentScreen by remember { mutableStateOf(AppScreen.AUTH) }
                    var loggedInUserEmail by remember { mutableStateOf("") }

                    // 2. Conditionally switch screens based on currentScreen value
                    when (currentScreen) {
                        AppScreen.AUTH -> {
                            AuthScreen(onAuthSuccess = { verifiedEmail ->
                                // Trigger the toast notification alert
                                Toast.makeText(this@MainActivity, "Logged in as $verifiedEmail", Toast.LENGTH_LONG).show()

                                // Save the email string and flip state to navigate forward
                                loggedInUserEmail = verifiedEmail
                                currentScreen = AppScreen.DASHBOARD
                            })
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                userEmail = loggedInUserEmail,
                                onLogout = {
                                    // Reset state tracker flags to route them backward
                                    TokenManager.clearToken()
                                    currentScreen = AppScreen.AUTH
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            HhdmsFirebaseMessagingService.CHANNEL_ID,
            "Service Bookings",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for service booking updates"
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerExistingFcmToken() {
        if (TokenManager.getToken() == null) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.apiService.registerToken(
                            RegisterTokenRequest(token = token),
                        )
                    } catch (_: Exception) { }
                }
            }
        }
    }
}