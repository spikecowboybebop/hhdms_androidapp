package com.example.hhdmspatientapp

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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH, DASHBOARD, NOTIFICATIONS, BOOKING_DETAIL, APPOINTMENTS,
    MBBS_DOCTOR_DASHBOARD, MBBS_BOOKING_DETAIL, PATIENT_ASSIGNMENTS,
    PATIENT_DETAIL, DOCTOR_TRACKING,
    MBBS_VITALS, MBBS_DIAGNOSIS, MBBS_PRESCRIPTION, MBBS_TEST_ORDERS, MBBS_REFERRAL,
    CAREGIVER_DASHBOARD, CAREGIVER_PATIENT_LIST, CAREGIVER_PATIENT_DETAIL,
    CAREGIVER_ACTIVITY_LOG, CAREGIVER_CONDITION_REPORT, CAREGIVER_CHECK_IN_OUT,
    TELECONSULT, CHAT, PATIENT_INFO,
    NURSE_DASHBOARD, NURSE_SCHEDULE, NURSE_PATIENT_LIST,
    NURSE_VITALS, NURSE_MEDICATION, NURSE_IV_FLUID, NURSE_WOUND_CARE,
    NURSE_CARE_REPORT, NURSE_HANDOVER, NURSE_CONSULTATION,
    NURSE_SUPPLY_TRACKING, NURSE_PEDIATRIC_CARE,
    VIDEO_CALL,
}

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private var pendingSessionId by mutableStateOf<String?>(null)
    private var pendingConsentPatientId by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSessionId = intent.getStringExtra("session_id")
        pendingConsentPatientId = intent.getStringExtra("consent_patient_id")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingSessionId = intent.getStringExtra("session_id")
        pendingConsentPatientId = intent.getStringExtra("consent_patient_id")

        TokenManager.init(applicationContext)
        NotificationStorage.init(applicationContext)
        FcmTokenStorage.init(applicationContext)
        VisitStorage.init(applicationContext)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
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
                    var selectedPatientId by remember { mutableStateOf<String?>(null) }
                    var selectedPatientName by remember { mutableStateOf("") }
                    var latestSession by remember { mutableStateOf<SessionSummary?>(null) }
                    var latestDoctorName by remember { mutableStateOf<String?>(null) }
                    var trackingDoctorName by remember { mutableStateOf("") }
                    var trackingPatientId by remember { mutableStateOf<String?>(null) }
                    var consentRequestPatientId by remember { mutableStateOf<String?>(null) }
                    var sessionLoadKey by remember { mutableStateOf(0) }
                    var homeScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                    var bookingOrigin by remember { mutableStateOf(AppScreen.DASHBOARD) }
                    var userAuthedThisSession by remember { mutableStateOf(false) }

                    // Video call state
                    var videoCallSessionId by remember { mutableStateOf<String?>(null) }
                    var videoCallSpecialistName by remember { mutableStateOf("") }
                    var videoCallChannelName by remember { mutableStateOf("") }
                    var videoCallToken by remember { mutableStateOf("") }
                    var videoCallAppId by remember { mutableStateOf("") }
                    var videoCallUid by remember { mutableStateOf(2) }
                    var selectedTeleconsultSessionId by remember { mutableStateOf<String?>(null) }
                    var incomingCall by remember { mutableStateOf<IncomingCallInfo?>(null) }
                    var chatConversationId by remember { mutableStateOf("") }
                    var chatOtherName by remember { mutableStateOf("") }

                    // Wire video call callbacks from CallSignalingManager
                    LaunchedEffect(Unit) {
                        CallSignalingManager.onIncomingVideoCall = { specialistName, sessionId ->
                            Log.d("MainActivity", "=== onIncomingVideoCall fired: $specialistName, $sessionId ===")
                            videoCallSpecialistName = specialistName
                            videoCallSessionId = sessionId
                        }
                        CallSignalingManager.onVideoCallReady = { token, appId, channelName, uid ->
                            Log.d("MainActivity", "=== onVideoCallReady fired: channel=$channelName uid=$uid ===")
                            videoCallToken = token
                            videoCallAppId = appId
                            videoCallChannelName = channelName
                            videoCallUid = uid
                            currentScreen = AppScreen.VIDEO_CALL
                        }
                        CallSignalingManager.onVideoCallEnded = {
                            Log.d("MainActivity", "=== onVideoCallEnded fired ===")
                            videoCallSessionId = null
                            videoCallChannelName = ""
                            videoCallToken = ""
                            if (currentScreen == AppScreen.VIDEO_CALL) {
                                currentScreen = homeScreen
                            }
                        }
                    }

                    // Video call incoming overlay
                    if (videoCallSessionId != null && currentScreen != AppScreen.VIDEO_CALL) {
                        IncomingVideoCallModal(
                            specialistName = videoCallSpecialistName,
                            onAccept = {
                                val sid = videoCallSessionId
                                if (sid != null) {
                                    CallSignalingManager.acceptVideoCall(sid)
                                }
                            },
                            onDecline = {
                                val sid = videoCallSessionId
                                if (sid != null) {
                                    CallSignalingManager.declineVideoCall(sid)
                                }
                                videoCallSessionId = null
                            },
                        )
                    }

                    if (pendingConsentPatientId != null && consentRequestPatientId == null) {
                        consentRequestPatientId = pendingConsentPatientId
                        pendingConsentPatientId = null
                    }

                    LaunchedEffect(sessionLoadKey) {
                        if (sessionLoadKey > 0 && TokenManager.getToken() != null) {
                            HhdmsFirebaseMessagingService.registerCurrentToken()

                            try {
                                val sessions = RetrofitClient.apiService.getMySessions()
                                latestSession = sessions.firstOrNull()
                                val hasActiveSession = sessions.any { s ->
                                    s.status != "COMPLETED" && s.status != "CANCELLED"
                                }
                                if (!hasActiveSession) {
                                    VisitStorage.clearVisit()
                                    latestDoctorName = null
                                }
                            } catch (_: Exception) { }
                            try {
                                val pending = RetrofitClient.apiService.getPendingNotifications()
                                var foundDoctorComing = false
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
                                    showLocalNotification(n.title, n.body, n.session_id)
                                    if (n.type == "doctor_coming") {
                                        val doctorName = n.body.substringBefore(" is coming to visit you")
                                        VisitStorage.saveVisitInfo(doctorName)
                                        n.patient_id?.let { VisitStorage.saveVisitingPatientId(it) }
                                        latestDoctorName = doctorName
                                        foundDoctorComing = true
                                    }
                                    if (n.type == "appointment_done") {
                                        VisitStorage.clearVisit()
                                        VisitStorage.clearVisitingPatientId()
                                        VisitStorage.saveAppointmentDone()
                                        latestDoctorName = null
                                    }
                                    if (n.type == "provider_assigned") {
                                        Toast.makeText(
                                            this@MainActivity,
                                            n.body,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                    if (n.type == "consent_request") {
                                        n.patient_id?.let { pid ->
                                            consentRequestPatientId = pid
                                        }
                                    }
                                }

                            } catch (_: Exception) { }

                            while (true) {
                                delay(15_000)
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
                                        showLocalNotification(n.title, n.body, n.session_id)
                                        if (n.type == "doctor_coming") {
                                            val doctorName = n.body.substringBefore(" is coming to visit you")
                                            VisitStorage.saveVisitInfo(doctorName)
                                            n.patient_id?.let { VisitStorage.saveVisitingPatientId(it) }
                                            latestDoctorName = doctorName
                                        }
                                        if (n.type == "appointment_done") {
                                            VisitStorage.clearVisit()
                                            VisitStorage.clearVisitingPatientId()
                                            VisitStorage.saveAppointmentDone()
                                            latestDoctorName = null
                                        }
                                        if (n.type == "provider_assigned") {
                                            Toast.makeText(
                                                this@MainActivity,
                                                n.body,
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                        if (n.type == "consent_request") {
                                            n.patient_id?.let { pid ->
                                                consentRequestPatientId = pid
                                            }
                                        }
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                    }

                    LaunchedEffect(currentScreen) {
                        if (currentScreen == AppScreen.DASHBOARD || currentScreen == AppScreen.MBBS_DOCTOR_DASHBOARD || currentScreen == AppScreen.CAREGIVER_DASHBOARD || currentScreen == AppScreen.CAREGIVER_PATIENT_LIST || currentScreen == AppScreen.CAREGIVER_CHECK_IN_OUT || currentScreen == AppScreen.NURSE_DASHBOARD) {
                            sessionLoadKey++
                        }
                    }

                    // Consent dialog
                    if (consentRequestPatientId != null || pendingConsentPatientId != null) {
                        val patientId = consentRequestPatientId ?: pendingConsentPatientId ?: ""
                        AlertDialog(
                            onDismissRequest = {
                                consentRequestPatientId = null
                                pendingConsentPatientId = null
                            },
                            containerColor = PureWhite,
                            title = { Text("Patient Consent Required", color = TitleBlack) },
                            text = { Text("The doctor is requesting your consent to begin the consultation. Do you grant consent?", color = CoolGray) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                RetrofitClient.apiService.respondConsent(
                                                    patientId,
                                                    mapOf("answer" to "granted"),
                                                )
                                            } catch (_: Exception) { }
                                        }
                                        consentRequestPatientId = null
                                        pendingConsentPatientId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                                ) { Text("Grant Consent", color = PureWhite) }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                RetrofitClient.apiService.respondConsent(
                                                    patientId,
                                                    mapOf("answer" to "denied"),
                                                )
                                            } catch (_: Exception) { }
                                        }
                                        consentRequestPatientId = null
                                        pendingConsentPatientId = null
                                    },
                                ) { Text("Deny", color = TechTeal) }
                            },
                        )
                    }

                    if (!pendingSessionId.isNullOrBlank() &&
                        (currentScreen == AppScreen.DASHBOARD || currentScreen == AppScreen.MBBS_DOCTOR_DASHBOARD || currentScreen == AppScreen.CAREGIVER_DASHBOARD || currentScreen == AppScreen.NURSE_DASHBOARD)
                    ) {
                        bookingOrigin = homeScreen
                            selectedSessionId = pendingSessionId
                            pendingSessionId = null
                            currentScreen = if (loggedInUserRole == "MBBS_DOCTOR" || loggedInUserRole == "CAREGIVER" || loggedInUserRole == "NURSE")
                                AppScreen.MBBS_BOOKING_DETAIL
                            else
                                AppScreen.BOOKING_DETAIL
                    }

                    BackHandler(enabled = true) {
                        when (currentScreen) {
                            AppScreen.AUTH,
                            AppScreen.DASHBOARD,
                            AppScreen.MBBS_DOCTOR_DASHBOARD,
                            AppScreen.CAREGIVER_DASHBOARD -> {
                                finishAffinity()
                            }
                            AppScreen.CAREGIVER_PATIENT_LIST -> currentScreen = AppScreen.CAREGIVER_DASHBOARD
                            AppScreen.CAREGIVER_CHECK_IN_OUT -> currentScreen = AppScreen.CAREGIVER_DASHBOARD
                            AppScreen.CAREGIVER_ACTIVITY_LOG -> currentScreen = AppScreen.CAREGIVER_DASHBOARD
                            AppScreen.CAREGIVER_CONDITION_REPORT -> currentScreen = AppScreen.CAREGIVER_DASHBOARD
                            AppScreen.CAREGIVER_PATIENT_DETAIL -> currentScreen = AppScreen.CAREGIVER_PATIENT_LIST
                            AppScreen.PATIENT_ASSIGNMENTS -> currentScreen = AppScreen.MBBS_DOCTOR_DASHBOARD
                            AppScreen.PATIENT_DETAIL -> currentScreen = AppScreen.PATIENT_ASSIGNMENTS
                            AppScreen.CHAT -> currentScreen = homeScreen
                            AppScreen.MBBS_VITALS,
                            AppScreen.MBBS_DIAGNOSIS,
                            AppScreen.MBBS_PRESCRIPTION,
                            AppScreen.MBBS_TEST_ORDERS,
                            AppScreen.MBBS_REFERRAL -> currentScreen = AppScreen.PATIENT_DETAIL
                            AppScreen.DOCTOR_TRACKING -> currentScreen = homeScreen
                            AppScreen.APPOINTMENTS -> currentScreen = homeScreen
                            AppScreen.NOTIFICATIONS -> currentScreen = homeScreen
                            AppScreen.PATIENT_INFO -> currentScreen = AppScreen.DASHBOARD
                            AppScreen.BOOKING_DETAIL -> currentScreen = bookingOrigin
                            AppScreen.MBBS_BOOKING_DETAIL -> currentScreen = bookingOrigin
                            AppScreen.VIDEO_CALL -> {
                                val sid = videoCallSessionId
                                if (sid != null) {
                                    CallSignalingManager.endVideoCall(sid)
                                }
                                videoCallSessionId = null
                                videoCallChannelName = ""
                                videoCallToken = ""
                                currentScreen = homeScreen
                            }
                            else -> finishAffinity()
                        }
                    }

                    when (currentScreen) {
                        AppScreen.AUTH -> {
                            AuthScreen(onAuthSuccess = { verifiedEmail, role ->
                                Toast.makeText(this@MainActivity, "Logged in as $verifiedEmail", Toast.LENGTH_LONG).show()
                                NotificationStorage.setCurrentUser(verifiedEmail)
                                loggedInUserEmail = verifiedEmail
                                loggedInUserRole = role
                                userAuthedThisSession = true
                                HhdmsFirebaseMessagingService.registerCurrentToken()

                                // Fetch patient ID and register with video call gateway
                                if (role == "MOBILE_USER") {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val patientInfo = RetrofitClient.apiService.getSelfPatientInfo()
                                            val patientId = patientInfo.id
                                            if (!patientId.isNullOrEmpty()) {
                                                Log.d("MainActivity", "Fetched patient ID: $patientId — registering with video call gateway")
                                                CallSignalingManager.registerPatient(patientId)
                                            } else {
                                                Log.w("MainActivity", "Patient ID is null/empty from /patients/self")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Failed to fetch patient ID: ${e.message}")
                                        }
                                    }
                                }

                                val pendingId = pendingSessionId
                                pendingSessionId = null
                                if (!pendingId.isNullOrBlank()) {
                                    homeScreen = when (role) {
                                        "MBBS_DOCTOR" -> AppScreen.MBBS_DOCTOR_DASHBOARD
                                        "CAREGIVER" -> AppScreen.CAREGIVER_DASHBOARD
                                        "NURSE" -> AppScreen.NURSE_DASHBOARD
                                        else -> AppScreen.DASHBOARD
                                    }
                                    bookingOrigin = homeScreen
                                    selectedSessionId = pendingId
                                    currentScreen = if (role == "MBBS_DOCTOR" || role == "CAREGIVER" || role == "NURSE")
                                        AppScreen.MBBS_BOOKING_DETAIL
                                    else
                                        AppScreen.BOOKING_DETAIL
                                    sessionLoadKey++
                                } else if (role == "MBBS_DOCTOR") {
                                    homeScreen = AppScreen.MBBS_DOCTOR_DASHBOARD
                                    currentScreen = AppScreen.MBBS_DOCTOR_DASHBOARD
                                } else if (role == "CAREGIVER") {
                                    homeScreen = AppScreen.CAREGIVER_DASHBOARD
                                    currentScreen = AppScreen.CAREGIVER_DASHBOARD
                                } else if (role == "NURSE") {
                                    homeScreen = AppScreen.NURSE_DASHBOARD
                                    currentScreen = AppScreen.NURSE_DASHBOARD
                                } else {
                                    homeScreen = AppScreen.DASHBOARD
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            })
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                userEmail = loggedInUserEmail,
                                latestSession = latestSession,
                                doctorName = latestDoctorName,
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
                    onNavigateToDoctorTracking = {
                        trackingDoctorName = latestDoctorName ?: ""
                        trackingPatientId = VisitStorage.getVisitingPatientId()
                        currentScreen = AppScreen.DOCTOR_TRACKING
                    },
                    onCallEndedRefresh = { sessionLoadKey++ },
                )
            }
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
                                onNavigateToDoctorTracking = {
                                    trackingDoctorName = latestDoctorName ?: ""
                                    trackingPatientId = VisitStorage.getVisitingPatientId()
                                    currentScreen = AppScreen.DOCTOR_TRACKING
                                },
                                onCallEndedRefresh = { sessionLoadKey++ },
                                onNavigateToChat = { convId, name ->
                                    chatConversationId = convId
                                    chatOtherName = name
                                    currentScreen = AppScreen.CHAT
                                },
                                onNavigateToPatientInfo = {
                                    currentScreen = AppScreen.PATIENT_INFO
                                },
                            )
                        }

                        AppScreen.PATIENT_INFO -> {
                            PatientInfoScreen(
                                onBack = { currentScreen = AppScreen.DASHBOARD },
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
                                onNavigateToPatientAssignments = {
                                    currentScreen = AppScreen.PATIENT_ASSIGNMENTS
                                },
                                onNavigateToChat = { convId, name ->
                                    chatConversationId = convId
                                    chatOtherName = name
                                    currentScreen = AppScreen.CHAT
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
                                        currentScreen = if (loggedInUserRole == "MBBS_DOCTOR" || loggedInUserRole == "NURSE")
                                            AppScreen.MBBS_BOOKING_DETAIL
                                        else
                                            AppScreen.BOOKING_DETAIL
                                    } else {
                                        currentScreen = AppScreen.APPOINTMENTS
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

                        AppScreen.PATIENT_ASSIGNMENTS -> {
                            PatientAssignmentsScreen(
                                onBack = { currentScreen = AppScreen.MBBS_DOCTOR_DASHBOARD },
                                onPatientClick = { patientId ->
                                    selectedPatientId = patientId
                                    currentScreen = AppScreen.PATIENT_DETAIL
                                },
                            )
                        }

                        AppScreen.PATIENT_DETAIL -> {
                            PatientDetailScreen(
                                patientId = selectedPatientId ?: "",
                                onBack = { currentScreen = AppScreen.PATIENT_ASSIGNMENTS },
                                onNavigateToVitals = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.MBBS_VITALS
                                },
                                onNavigateToDiagnosis = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.MBBS_DIAGNOSIS
                                },
                                onNavigateToPrescription = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.MBBS_PRESCRIPTION
                                },
                                onNavigateToTestOrders = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.MBBS_TEST_ORDERS
                                },
                                onNavigateToReferral = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.MBBS_REFERRAL
                                },
                            )
                        }

                        AppScreen.MBBS_VITALS -> {
                            MbbsVitalsScreen(
                                patientId = selectedPatientId ?: "",
                                patientName = selectedPatientName,
                                onBack = {
                                    selectedPatientId = selectedPatientId
                                    currentScreen = AppScreen.PATIENT_DETAIL
                                },
                            )
                        }

                        AppScreen.MBBS_DIAGNOSIS -> {
                            MbbsDiagnosisScreen(
                                patientId = selectedPatientId ?: "",
                                patientName = selectedPatientName,
                                onBack = {
                                    selectedPatientId = selectedPatientId
                                    currentScreen = AppScreen.PATIENT_DETAIL
                                },
                            )
                        }

                        AppScreen.MBBS_PRESCRIPTION -> {
                            MbbsPrescriptionScreen(
                                patientId = selectedPatientId ?: "",
                                patientName = selectedPatientName,
                                onBack = { currentScreen = AppScreen.PATIENT_DETAIL },
                            )
                        }

                        AppScreen.MBBS_TEST_ORDERS -> {
                            MbbsTestOrdersScreen(
                                patientId = selectedPatientId ?: "",
                                patientName = selectedPatientName,
                                onBack = { currentScreen = AppScreen.PATIENT_DETAIL },
                            )
                        }

                        AppScreen.MBBS_REFERRAL -> {
                            MbbsReferralScreen(
                                patientId = selectedPatientId ?: "",
                                patientName = selectedPatientName,
                                onBack = { currentScreen = AppScreen.PATIENT_DETAIL },
                            )
                        }

                        AppScreen.CAREGIVER_DASHBOARD -> {
                            CaregiverDashboardScreen(
                                onLogout = {
                                    TokenManager.clearToken()
                                    NotificationStorage.setCurrentUser(null)
                                    currentScreen = AppScreen.AUTH
                                },
                                onNavigateToNotifications = {
                                    currentScreen = AppScreen.NOTIFICATIONS
                                },
                                onNavigateToPatientList = {
                                    currentScreen = AppScreen.CAREGIVER_PATIENT_LIST
                                },
                                onNavigateToCheckInOut = {
                                    currentScreen = AppScreen.CAREGIVER_CHECK_IN_OUT
                                },
                                onNavigateToActivityLog = {
                                    currentScreen = AppScreen.CAREGIVER_ACTIVITY_LOG
                                },
                                onNavigateToConditionReports = {
                                    currentScreen = AppScreen.CAREGIVER_CONDITION_REPORT
                                },
                            )
                        }

                        AppScreen.CAREGIVER_PATIENT_LIST -> {
                            CaregiverPatientListScreen(
                                onBack = { currentScreen = AppScreen.CAREGIVER_DASHBOARD },
                                onPatientClick = { patientId, patientName ->
                                    selectedPatientId = patientId
                                    selectedPatientName = patientName
                                    currentScreen = AppScreen.CAREGIVER_PATIENT_DETAIL
                                },
                            )
                        }

                        AppScreen.CAREGIVER_PATIENT_DETAIL -> {
                            CaregiverPatientDetailScreen(
                                patientId = selectedPatientId ?: "",
                                onBack = { currentScreen = AppScreen.CAREGIVER_PATIENT_LIST },
                                onNavigateToActivityLog = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.CAREGIVER_ACTIVITY_LOG
                                },
                                onNavigateToConditionReport = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.CAREGIVER_CONDITION_REPORT
                                },
                            )
                        }

                        AppScreen.CAREGIVER_ACTIVITY_LOG -> {
                            CaregiverActivityLogScreen(
                                onBack = { currentScreen = AppScreen.CAREGIVER_DASHBOARD },
                            )
                        }

                        AppScreen.CAREGIVER_CONDITION_REPORT -> {
                            CaregiverConditionReportScreen(
                                onBack = { currentScreen = AppScreen.CAREGIVER_DASHBOARD },
                            )
                        }

                        AppScreen.CAREGIVER_CHECK_IN_OUT -> {
                            CaregiverCheckInOutScreen(
                                onBack = { currentScreen = AppScreen.CAREGIVER_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_DASHBOARD -> {
                            NurseDashboardScreen(
                                userEmail = loggedInUserEmail,
                                onLogout = {
                                    TokenManager.clearToken()
                                    NotificationStorage.setCurrentUser(null)
                                    currentScreen = AppScreen.AUTH
                                },
                                onNavigateToNotifications = { currentScreen = AppScreen.NOTIFICATIONS },
                                onNavigateToSchedule = { currentScreen = AppScreen.NURSE_SCHEDULE },
                                onNavigateToPatientList = { currentScreen = AppScreen.NURSE_PATIENT_LIST },
                                onNavigateToVitals = { currentScreen = AppScreen.NURSE_VITALS },
                                onNavigateToMedication = { currentScreen = AppScreen.NURSE_MEDICATION },
                                onNavigateToIVFluid = { currentScreen = AppScreen.NURSE_IV_FLUID },
                                onNavigateToWoundCare = { currentScreen = AppScreen.NURSE_WOUND_CARE },
                                onNavigateToCareReport = { currentScreen = AppScreen.NURSE_CARE_REPORT },
                                onNavigateToHandover = { currentScreen = AppScreen.NURSE_HANDOVER },
                                onNavigateToConsultation = { currentScreen = AppScreen.NURSE_CONSULTATION },
                                onNavigateToSupplyTracking = { currentScreen = AppScreen.NURSE_SUPPLY_TRACKING },
                                onNavigateToPediatricCare = { currentScreen = AppScreen.NURSE_PEDIATRIC_CARE },
                            )
                        }

                        AppScreen.NURSE_SCHEDULE -> {
                            NursePatientScheduleScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                                onPatientClick = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.NURSE_VITALS
                                },
                            )
                        }

                        AppScreen.NURSE_PATIENT_LIST -> {
                            NursePatientListScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                                onPatientClick = { id, name ->
                                    selectedPatientId = id
                                    selectedPatientName = name
                                    currentScreen = AppScreen.NURSE_VITALS
                                },
                            )
                        }

                        AppScreen.NURSE_VITALS -> {
                            NurseVitalsScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_MEDICATION -> {
                            NurseMedicationScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_IV_FLUID -> {
                            NurseIVFluidScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_WOUND_CARE -> {
                            NurseWoundCareScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_CARE_REPORT -> {
                            NurseCareReportScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_HANDOVER -> {
                            NurseHandoverScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_CONSULTATION -> {
                            NurseConsultationScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_SUPPLY_TRACKING -> {
                            NurseSupplyTrackingScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.NURSE_PEDIATRIC_CARE -> {
                            NursePediatricCareScreen(
                                onBack = { currentScreen = AppScreen.NURSE_DASHBOARD },
                            )
                        }

                        AppScreen.DOCTOR_TRACKING -> {
                            DoctorTrackingScreen(
                                doctorName = trackingDoctorName,
                                patientId = trackingPatientId ?: "",
                                onBack = { currentScreen = homeScreen },
                            )
                        }

                        AppScreen.VIDEO_CALL -> {
                            VideoCallScreen(
                                appId = videoCallAppId,
                                channelName = videoCallChannelName,
                                token = videoCallToken,
                                uid = videoCallUid,
                                onEndCall = {
                                    val sid = videoCallSessionId
                                    if (sid != null) {
                                        CallSignalingManager.endVideoCall(sid)
                                    }
                                    videoCallSessionId = null
                                    videoCallChannelName = ""
                                    videoCallToken = ""
                                    currentScreen = homeScreen
                                },
                            )
                        }
                        AppScreen.CHAT -> {
                            ChatScreen(
                                conversationId = chatConversationId,
                                otherUserName = chatOtherName,
                                onBack = { currentScreen = homeScreen },
                            )
                        }
                    }

                    }
                }
            }
        }
    }

    private fun showLocalNotification(title: String, body: String, sessionId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            sessionId?.let { putExtra("session_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, HhdmsFirebaseMessagingService.CHANNEL_ID)
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

    private fun extractSubFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE),
                    Charsets.UTF_8,
                )
                org.json.JSONObject(payload).optString("sub").ifBlank { null }
            } else null
        } catch (e: Exception) { null }
    }
}
