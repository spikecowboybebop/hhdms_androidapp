package com.example.hhdmspatientapp.nurse

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
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
import com.example.hhdmspatientapp.FcmTokenStorage
import com.example.hhdmspatientapp.NotificationStorage
import com.example.hhdmspatientapp.RetrofitClient
import com.example.hhdmspatientapp.TokenManager
import com.example.hhdmspatientapp.VisitStorage
import com.example.hhdmspatientapp.RegisterTokenRequest
import com.example.hhdmspatientapp.*
import com.example.hhdmspatientapp.ui.theme.HhdmsTheme
import com.example.hhdmspatientapp.ui.theme.SoftSlate
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class NurseScreen {
    AUTH, DASHBOARD, SCHEDULE, PATIENT_LIST,
    VITALS, MEDICATION, IV_FLUID, WOUND_CARE,
    CARE_REPORT, HANDOVER, CONSULTATION,
    SUPPLY_TRACKING, PEDIATRIC_CARE,
}

class NurseMainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(applicationContext)
        NotificationStorage.init(applicationContext)
        FcmTokenStorage.init(applicationContext)
        VisitStorage.init(applicationContext)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        registerExistingFcmToken()

        setContent {
            HhdmsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SoftSlate,
                ) {
                    var currentScreen by remember { mutableStateOf(NurseScreen.AUTH) }
                    var loggedInUserEmail by remember { mutableStateOf("") }
                    var selectedPatientId by remember { mutableStateOf<String?>(null) }
                    var selectedPatientName by remember { mutableStateOf("") }

                    when (currentScreen) {
                        NurseScreen.AUTH -> {
                            NurseAuthScreen(onAuthSuccess = { email, _ ->
                                Toast.makeText(this@NurseMainActivity, "Logged in as $email", Toast.LENGTH_LONG).show()
                                NotificationStorage.setCurrentUser(email)
                                loggedInUserEmail = email
                                currentScreen = NurseScreen.DASHBOARD
                            })
                        }

                        NurseScreen.DASHBOARD -> {
                            NurseDashboardScreen(
                                userEmail = loggedInUserEmail,
                                onLogout = {
                                    TokenManager.clearToken()
                                    NotificationStorage.setCurrentUser(null)
                                    currentScreen = NurseScreen.AUTH
                                },
                                onNavigateToSchedule = { currentScreen = NurseScreen.SCHEDULE },
                                onNavigateToPatientList = { currentScreen = NurseScreen.PATIENT_LIST },
                                onNavigateToVitals = { currentScreen = NurseScreen.VITALS },
                                onNavigateToMedication = { currentScreen = NurseScreen.MEDICATION },
                                onNavigateToIVFluid = { currentScreen = NurseScreen.IV_FLUID },
                                onNavigateToWoundCare = { currentScreen = NurseScreen.WOUND_CARE },
                                onNavigateToCareReport = { currentScreen = NurseScreen.CARE_REPORT },
                                onNavigateToHandover = { currentScreen = NurseScreen.HANDOVER },
                                onNavigateToConsultation = { currentScreen = NurseScreen.CONSULTATION },
                                onNavigateToSupplyTracking = { currentScreen = NurseScreen.SUPPLY_TRACKING },
                                onNavigateToPediatricCare = { currentScreen = NurseScreen.PEDIATRIC_CARE },
                                onNavigateToNotifications = { },
                            )
                        }

                        NurseScreen.SCHEDULE -> {
                            NursePatientScheduleScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                                onPatientClick = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = NurseScreen.VITALS
                                },
                            )
                        }

                        NurseScreen.PATIENT_LIST -> {
                            NursePatientListScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                                onPatientClick = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = NurseScreen.VITALS
                                },
                            )
                        }

                        NurseScreen.VITALS -> {
                            NurseVitalsScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.MEDICATION -> {
                            NurseMedicationScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.IV_FLUID -> {
                            NurseIVFluidScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.WOUND_CARE -> {
                            NurseWoundCareScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.CARE_REPORT -> {
                            NurseCareReportScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.HANDOVER -> {
                            NurseHandoverScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.CONSULTATION -> {
                            NurseConsultationScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.SUPPLY_TRACKING -> {
                            NurseSupplyTrackingScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
                            )
                        }

                        NurseScreen.PEDIATRIC_CARE -> {
                            NursePediatricCareScreen(
                                onBack = { currentScreen = NurseScreen.DASHBOARD },
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
            NurseFirebaseMessagingService.CHANNEL_ID,
            "Nurse Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for nurse care updates"
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
                    Log.e("NurseMainActivity", "Failed to register FCM token: ${e.message}")
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
                        Log.e("NurseMainActivity", "Failed to register FCM token: ${e.message}")
                    }
                }
            }
        }
    }
}
