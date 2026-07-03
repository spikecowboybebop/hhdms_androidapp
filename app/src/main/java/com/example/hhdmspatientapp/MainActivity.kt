package com.example.hhdmspatientapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
    AUTH, DASHBOARD, NOTIFICATIONS, BOOKING_DETAIL, APPOINTMENTS, MBBS_DOCTOR_DASHBOARD, MBBS_BOOKING_DETAIL
}

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private var pendingSessionId by mutableStateOf<String?>(null)
    private var notificationIntentCount by mutableStateOf(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationIntentCount++
        pendingSessionId = intent.getStringExtra("session_id")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read session_id from launch intent (cold start from notification tap)
        pendingSessionId = intent.getStringExtra("session_id")

        TokenManager.init(applicationContext)
        NotificationStorage.init(applicationContext)
        FcmTokenStorage.init(applicationContext)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        registerExistingFcmToken()

        CallSignalingManager.initialize(applicationContext)

        setContent {
            HHDMSPatientAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SoftSlate,
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.AUTH) }
                    var loggedInUserEmail by remember { mutableStateOf("") }
                    var loggedInUserRole by remember { mutableStateOf("") }
                    var selectedSessionId by remember { mutableStateOf<String?>(null) }
                    var latestSession by remember { mutableStateOf<SessionSummary?>(null) }
                    var sessionLoadKey by remember { mutableStateOf(0) }
                    var homeScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                    var bookingOrigin by remember { mutableStateOf(AppScreen.DASHBOARD) }

                    notificationIntentCount // read to trigger recomposition on new intent

                    LaunchedEffect(sessionLoadKey) {
                        if (sessionLoadKey > 0 && TokenManager.getToken() != null) {
                            try {
                                val sessions = RetrofitClient.apiService.getMySessions()
                                latestSession = sessions.firstOrNull()
                            } catch (_: Exception) { }
                        }
                    }

                    LaunchedEffect(currentScreen) {
                        if (currentScreen == AppScreen.DASHBOARD) {
                            sessionLoadKey++
                        }
                    }

                    if (!pendingSessionId.isNullOrBlank() &&
                        (currentScreen == AppScreen.DASHBOARD || currentScreen == AppScreen.MBBS_DOCTOR_DASHBOARD)
                    ) {
                        bookingOrigin = homeScreen
                        selectedSessionId = pendingSessionId
                        pendingSessionId = null
                        currentScreen = if (loggedInUserRole == "MBBS_DOCTOR")
                            AppScreen.MBBS_BOOKING_DETAIL
                        else
                            AppScreen.BOOKING_DETAIL
                    }

                    when (currentScreen) {
                        AppScreen.AUTH -> {
                            AuthScreen(onAuthSuccess = { verifiedEmail, role ->
                                Toast.makeText(this@MainActivity, "Logged in as $verifiedEmail", Toast.LENGTH_LONG).show()
                                NotificationStorage.setCurrentUser(verifiedEmail)
                                loggedInUserEmail = verifiedEmail
                                loggedInUserRole = role

                                // Fetch pending server notifications for offline users
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val pending = RetrofitClient.apiService.getPendingNotifications()
                                        for (n in pending) {
                                            NotificationStorage.addNotification(
                                                NotificationItem(
                                                    id = NotificationStorage.nextId(),
                                                    title = n.title,
                                                    body = n.body,
                                                    timestamp = System.currentTimeMillis(),
                                                    sessionId = n.session_id,
                                                )
                                            )
                                        }
                                    } catch (_: Exception) { }
                                }

                                val pendingId = pendingSessionId
                                pendingSessionId = null
                                if (!pendingId.isNullOrBlank()) {
                                    homeScreen = if (role == "MBBS_DOCTOR") AppScreen.MBBS_DOCTOR_DASHBOARD else AppScreen.DASHBOARD
                                    bookingOrigin = homeScreen
                                    selectedSessionId = pendingId
                                    currentScreen = if (role == "MBBS_DOCTOR")
                                        AppScreen.MBBS_BOOKING_DETAIL
                                    else
                                        AppScreen.BOOKING_DETAIL
                                } else if (role == "MBBS_DOCTOR") {
                                    homeScreen = AppScreen.MBBS_DOCTOR_DASHBOARD
                                    currentScreen = AppScreen.MBBS_DOCTOR_DASHBOARD
                                } else {
                                    homeScreen = AppScreen.DASHBOARD
                                    currentScreen = AppScreen.DASHBOARD
                                }
                                sessionLoadKey++
                            })
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                userEmail = loggedInUserEmail,
                                latestSession = latestSession,
                                onLogout = {
                                    TokenManager.clearToken()
                                    NotificationStorage.setCurrentUser(null)
                                    currentScreen = AppScreen.AUTH
                                },
                                onNavigateToNotifications = {
                                    currentScreen = AppScreen.NOTIFICATIONS
                                },
                                onNavigateToAppointments = {
                                    currentScreen = AppScreen.APPOINTMENTS
                                },
                                onNavigateToBookingDetail = { sessionId ->
                                    bookingOrigin = AppScreen.DASHBOARD
                                    selectedSessionId = sessionId
                                    currentScreen = AppScreen.BOOKING_DETAIL
                                },
                                onCallEndedRefresh = { sessionLoadKey++ },
                            )
                        }

                        AppScreen.MBBS_DOCTOR_DASHBOARD -> {
                            MbbsDoctorDashboardScreen(
                                userEmail = loggedInUserEmail,
                                onLogout = {
                                    TokenManager.clearToken()
                                    NotificationStorage.setCurrentUser(null)
                                    currentScreen = AppScreen.AUTH
                                },
                                onNavigateToNotifications = {
                                    currentScreen = AppScreen.NOTIFICATIONS
                                },
                            )
                        }

                        AppScreen.APPOINTMENTS -> {
                            AppointmentsScreen(
                                onBack = { currentScreen = homeScreen },
                                onSessionTap = { sessionId ->
                                    bookingOrigin = AppScreen.APPOINTMENTS
                                    selectedSessionId = sessionId
                                    currentScreen = AppScreen.BOOKING_DETAIL
                                },
                            )
                        }

                        AppScreen.NOTIFICATIONS -> {
                            NotificationsScreen(
                                onBack = { currentScreen = homeScreen },
                                onNotificationTap = { sessionId ->
                                    if (!sessionId.isNullOrBlank()) {
                                        bookingOrigin = AppScreen.NOTIFICATIONS
                                        selectedSessionId = sessionId
                                        currentScreen = if (loggedInUserRole == "MBBS_DOCTOR")
                                            AppScreen.MBBS_BOOKING_DETAIL
                                        else
                                            AppScreen.BOOKING_DETAIL
                                    }
                                },
                            )
                        }

                        AppScreen.BOOKING_DETAIL -> {
                            BookingDetailScreen(
                                sessionId = selectedSessionId ?: "",
                                onBack = { currentScreen = bookingOrigin },
                            )
                        }

                        AppScreen.MBBS_BOOKING_DETAIL -> {
                            DoctorBookingDetailScreen(
                                sessionId = selectedSessionId ?: "",
                                onBack = { currentScreen = bookingOrigin },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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

        val storedToken = FcmTokenStorage.getToken()
        if (storedToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.apiService.registerToken(
                        RegisterTokenRequest(token = storedToken),
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to register stored FCM token: ${e.message}")
                }
            }
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FcmTokenStorage.saveToken(token)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.apiService.registerToken(
                            RegisterTokenRequest(token = token),
                        )
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to register FCM token: ${e.message}")
                    }
                }
            } else {
                Log.e("MainActivity", "Firebase token retrieval failed: ${task.exception?.message}")
            }
        }
    }
}