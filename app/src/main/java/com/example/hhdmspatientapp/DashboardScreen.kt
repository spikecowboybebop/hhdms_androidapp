package com.example.hhdmspatientapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val MedicalOrange = Color(0xFFF57C00)
val MedicalOrangeBackground = Color(0xFFFFF3E0)

enum class CallStatus {
    IDLE, RINGING, CONNECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(userEmail: String, latestSession: SessionSummary? = null, doctorName: String? = null, onLogout: () -> Unit, onNavigateToNotifications: () -> Unit, onNavigateToAppointments: () -> Unit, onNavigateToBookingDetail: (String) -> Unit = {}, onNavigateToDoctorTracking: () -> Unit = {}, onCallEndedRefresh: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentCallStatus by remember { mutableStateOf(CallStatus.IDLE) }
    val visitDoctorName = remember { mutableStateOf(doctorName ?: VisitStorage.getVisitDoctorName()) }
    val appointmentDone = remember { mutableStateOf(VisitStorage.isAppointmentDone()) }
    var paymentLoading by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var isPaid by remember { mutableStateOf(false) }
    var currentPaymentId by remember { mutableStateOf<String?>(null) }

    val onPaymentSuccess: () -> Unit = {
        scope.launch {
            try {
                val paymentId = currentPaymentId
                if (paymentId != null) {
                    RetrofitClient.apiService.confirmPayment(
                        ConfirmPaymentRequest(payment_id = paymentId)
                    )
                }
                isPaid = true
                currentPaymentId = null
                VisitStorage.clearAppointmentDone()
                appointmentDone.value = false
                paymentLoading = false
            } catch (e: Exception) {
                paymentError = "Failed to confirm payment: ${e.message}"
                paymentLoading = false
            }
        }
    }

    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                scope.launch {
                    delay(2000)
                    onPaymentSuccess()
                }
            }
            is PaymentSheetResult.Canceled -> {
                paymentLoading = false
                paymentError = null
            }
            is PaymentSheetResult.Failed -> {
                paymentError = "Payment failed: ${result.error.message}"
                paymentLoading = false
            }
        }
    }

    // Check payment status on load
    LaunchedEffect(appointmentDone.value, latestSession?.id) {
        if (appointmentDone.value && latestSession?.id != null) {
            try {
                val status = RetrofitClient.apiService.getPaymentStatus(latestSession.id)
                isPaid = status.paid
                if (isPaid) {
                    VisitStorage.clearAppointmentDone()
                    appointmentDone.value = false
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(doctorName) {
        if (doctorName != null) {
            visitDoctorName.value = doctorName
        }
    }

    LaunchedEffect(Unit) {
        VisitStorage.onVisitInfoChanged = { name ->
            visitDoctorName.value = name
            appointmentDone.value = VisitStorage.isAppointmentDone()
        }
    }
    DisposableEffect(Unit) {
        onDispose { VisitStorage.onVisitInfoChanged = null }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasAudioPermission = isGranted }

    var wasPreviouslyOnCall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CallSignalingManager.onCallStateChange = { newStatus -> currentCallStatus = newStatus }
    }
    DisposableEffect(Unit) {
        onDispose { CallSignalingManager.onCallStateChange = null }
    }

    LaunchedEffect(currentCallStatus) {
        if (currentCallStatus == CallStatus.RINGING || currentCallStatus == CallStatus.CONNECTED) {
            wasPreviouslyOnCall = true
        } else if (wasPreviouslyOnCall && currentCallStatus == CallStatus.IDLE) {
            wasPreviouslyOnCall = false
            onCallEndedRefresh()
        }
    }

    val statusCardColor by animateColorAsState(
        targetValue = when (currentCallStatus) {
            CallStatus.IDLE -> PureWhite
            CallStatus.RINGING -> Color(0xFFFFF9C4)
            CallStatus.CONNECTED -> Color(0xFFE8F5E9)
        },
        label = "cardColorAnimation"
    )

    val patientName = userEmail.substringBefore("@")
    val displayName = patientName.replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            if (currentCallStatus == CallStatus.IDLE) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Aastha",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                            )
                            Text(
                                text = "Tele-HealthCare",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = PureWhite,
                            )
                        }
                        IconButton(onClick = {
                            if (currentCallStatus != CallStatus.IDLE) {
                                currentCallStatus = CallStatus.IDLE
                                CallSignalingManager.hangUpActiveCall()
                            }
                            onLogout()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = PureWhite,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    modifier = Modifier.background(
                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))
                    ),
                )
            }
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (currentCallStatus != CallStatus.IDLE) {
            CallScreen(
                displayName = displayName,
                callStatus = currentCallStatus,
                onEndCall = {
                    currentCallStatus = CallStatus.IDLE
                    CallSignalingManager.hangUpActiveCall()
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Patient profile card ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(TechTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.take(2),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechTeal,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Good Morning,",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolGray,
                    )
                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Doctor Visit Banner ──
            if (!visitDoctorName.value.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToDoctorTracking() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TechTeal.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = TechTeal,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Doctor Coming",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechTeal,
                            )
                            Text(
                                text = "${visitDoctorName.value} is coming to visit you.",
                                fontSize = 13.sp,
                                color = TitleBlack,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Track",
                            tint = TechTeal,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Appointment Done Banner ──
            if (appointmentDone.value && !isPaid) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Appointment Done",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                )
                                Text(
                                    text = "Your appointment is completed. Please complete your payment.",
                                    fontSize = 13.sp,
                                    color = TitleBlack,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        paymentError?.let { error ->
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Button(
                            onClick = {
                                if (latestSession == null) return@Button
                                paymentLoading = true
                                paymentError = null
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.createPaymentIntent(
                                            CreatePaymentRequest(booking_session_id = latestSession.id)
                                        )
                                        currentPaymentId = response.paymentId
                                        PaymentConfiguration.init(context, response.publishableKey)
                                        val config = PaymentSheet.Configuration(
                                            merchantDisplayName = "HHDMSPatient",
                                        )
                                        paymentSheet.presentWithPaymentIntent(
                                            response.clientSecret,
                                            config,
                                        )
                                    } catch (e: Exception) {
                                        paymentError = "Failed to start payment: ${e.message}"
                                        paymentLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechTeal),
                            enabled = !paymentLoading && !isPaid,
                        ) {
                            if (paymentLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPaid) "Paid" else "Pay Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Recent Appointment ──
            Text(
                text = "Recent Appointment",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            if (latestSession != null) {
                val s = latestSession!!
                val firstTicket = s.tickets.firstOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToBookingDetail(s.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(TechTeal.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = TechTeal,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = firstTicket?.service_type ?: "Appointment",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TitleBlack,
                            )
                            if (!firstTicket?.scheduled_date.isNullOrBlank()) {
                                Text(
                                    text = firstTicket!!.scheduled_date!!.take(10),
                                    fontSize = 12.sp,
                                    color = CoolGray,
                                )
                            }
                            if (!firstTicket?.scheduled_time_slot.isNullOrBlank()) {
                                Text(
                                    text = firstTicket!!.scheduled_time_slot!!,
                                    fontSize = 12.sp,
                                    color = CoolGray,
                                )
                            }
                        }
                        StatusChip(s.status)
                    }
                }
            } else {
                Text(
                    text = "No appointments have been booked yet.",
                    fontSize = 13.sp,
                    color = CoolGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhite, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── Voice Consultation ──
            Text(
                text = "Voice Consultation",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = statusCardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                color = when (currentCallStatus) {
                                    CallStatus.IDLE -> TechTeal.copy(alpha = 0.12f)
                                    CallStatus.RINGING -> MedicalOrangeBackground
                                    CallStatus.CONNECTED -> Color.Green.copy(alpha = 0.12f)
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = when (currentCallStatus) {
                                CallStatus.IDLE -> TechTeal
                                CallStatus.RINGING -> MedicalOrange
                                CallStatus.CONNECTED -> Color(0xFF2E7D32)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when (currentCallStatus) {
                            CallStatus.IDLE -> "Need Medical Assistance?"
                            CallStatus.RINGING -> "Calling Support Desk..."
                            CallStatus.CONNECTED -> "Voice Connection Active"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleBlack,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = when (currentCallStatus) {
                            CallStatus.IDLE -> "Connect with a medical coordinator instantly via a secure voice call."
                            CallStatus.RINGING -> "Please wait while we route your call to an available agent."
                            CallStatus.CONNECTED -> "You are connected. Your call is being handled by a support agent."
                        },
                        fontSize = 13.sp,
                        color = CoolGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                        lineHeight = 18.sp,
                    )

                    if (currentCallStatus == CallStatus.IDLE) {
                        Button(
                            onClick = {
                                if (hasAudioPermission) {
                                    currentCallStatus = CallStatus.RINGING
                                    CallSignalingManager.startEmergencyCall(userEmail)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechTeal,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Start Voice Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                currentCallStatus = CallStatus.IDLE
                                CallSignalingManager.hangUpActiveCall()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (currentCallStatus == CallStatus.RINGING) "Cancel Request" else "End Call",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Add Your Information ──
            var showInfoForm by remember { mutableStateOf(false) }
            var formLoading by remember { mutableStateOf(false) }
            var formSuccess by remember { mutableStateOf(false) }
            var formError by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            var dataLoaded by remember { mutableStateOf(false) }
            var docUploadStatus by remember { mutableStateOf<String?>(null) }
            var docUploading by remember { mutableStateOf(false) }

            val docFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                docUploading = true
                docUploadStatus = null
                scope.launch {
                    try {
                        val fileName = "document_${System.currentTimeMillis()}"
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw Exception("Failed to read file.")
                        val mediaType = mimeType.toMediaTypeOrNull()
                        val requestBody = bytes.toRequestBody(mediaType)
                        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
                        RetrofitClient.apiService.uploadPatientDocument(part)
                        docUploadStatus = "Document uploaded successfully!"
                    } catch (e: Exception) {
                        docUploadStatus = "Upload failed: ${e.message?.take(80) ?: "Unknown error"}"
                    } finally {
                        docUploading = false
                    }
                }
            }

            val fullNameEn = remember { mutableStateOf("") }
            val fullNameBn = remember { mutableStateOf("") }
            var showDatePicker by remember { mutableStateOf(false) }
            val dateOfBirth = remember { mutableStateOf("") }
            val selectedSex = remember { mutableStateOf("") }
            val selectedBloodGroup = remember { mutableStateOf("") }
            val primaryPhone = remember { mutableStateOf("") }
            val alternativePhone = remember { mutableStateOf("") }
            val emergencyContactName = remember { mutableStateOf("") }
            val emergencyContactRelation = remember { mutableStateOf("") }
            val emergencyContactPhone = remember { mutableStateOf("") }
            val division = remember { mutableStateOf("") }
            val district = remember { mutableStateOf("") }
            val thana = remember { mutableStateOf("") }
            val addressDetail = remember { mutableStateOf("") }

            LaunchedEffect(showInfoForm) {
                if (showInfoForm && !dataLoaded) {
                    try {
                        val info = RetrofitClient.apiService.getSelfPatientInfo()
                        fullNameEn.value = listOfNotNull(info.first_name_en, info.last_name_en).filter { it.isNotBlank() }.joinToString(" ")
                        fullNameBn.value = listOfNotNull(info.first_name_bn, info.last_name_bn).filter { it.isNotBlank() }.joinToString(" ")
                        if (info.date_of_birth != null && info.date_of_birth.length >= 10) {
                            dateOfBirth.value = info.date_of_birth.take(10)
                        }
                        selectedSex.value = when (info.sex) {
                            "M" -> "Male"
                            "F" -> "Female"
                            "C" -> "Child"
                            else -> info.sex ?: ""
                        }
                        selectedBloodGroup.value = info.blood_group ?: ""
                        primaryPhone.value = info.phone_number ?: ""
                        alternativePhone.value = info.alternative_phone ?: ""
                        emergencyContactName.value = info.emergency_contact_name ?: ""
                        emergencyContactRelation.value = info.emergency_contact_relation ?: ""
                        emergencyContactPhone.value = info.emergency_contact ?: ""
                        info.address_line1?.let { addr ->
                            val div = Regex("Division:\\s*([^,]+)", RegexOption.IGNORE_CASE).find(addr)?.groupValues?.getOrNull(1)?.trim()
                            val dist = Regex("District:\\s*([^,]+)", RegexOption.IGNORE_CASE).find(addr)?.groupValues?.getOrNull(1)?.trim()
                            val tha = Regex("Thana:\\s*([^,]+)", RegexOption.IGNORE_CASE).find(addr)?.groupValues?.getOrNull(1)?.trim()
                            if (!div.isNullOrBlank()) division.value = div
                            if (!dist.isNullOrBlank()) district.value = dist
                            if (!tha.isNullOrBlank()) thana.value = tha
                        }
                        addressDetail.value = info.address_line2 ?: ""
                        dataLoaded = true
                    } catch (_: Exception) {
                        // no existing data — fields stay empty
                    }
                }
                if (!showInfoForm) {
                    dataLoaded = false
                }
            }

            Text(
                text = "Add Your Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!showInfoForm) {
                        Text(
                            text = "Provide your information to help the call center serve you better.",
                            fontSize = 13.sp,
                            color = CoolGray,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        Button(
                            onClick = { showInfoForm = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClinicalNavy,
                                contentColor = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fill Your Information", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // ── Demographics Section ──
                        SectionHeader("Demographics")
                        OutlinedTextField(
                            value = fullNameEn.value,
                            onValueChange = { fullNameEn.value = it },
                            label = { Text("Full Name (English)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )
                        OutlinedTextField(
                            value = fullNameBn.value,
                            onValueChange = { fullNameBn.value = it },
                            label = { Text("Full Name (Bengali)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )

                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = dateOfBirth.value.ifBlank { null }?.let {
                                try {
                                    java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                } catch (_: Exception) { null }
                            }
                        )
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            OutlinedTextField(
                                value = dateOfBirth.value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date of Birth") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = TitleBlack),
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val localDate = java.time.Instant.ofEpochMilli(millis)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()
                                            dateOfBirth.value = localDate.toString()
                                        }
                                        showDatePicker = false
                                    }) { Text("OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                                },
                            ) { DatePicker(state = datePickerState) }
                        }

                        GenderDropdown(selectedSex.value) { selectedSex.value = it }
                        Spacer(modifier = Modifier.height(8.dp))
                        BloodGroupDropdown(selectedBloodGroup.value) { selectedBloodGroup.value = it }

                        // ── Contact Section ──
                        SectionHeader("Contact")
                        OutlinedTextField(
                            value = primaryPhone.value,
                            onValueChange = { primaryPhone.value = it },
                            label = { Text("Primary Phone Number") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )
                        OutlinedTextField(
                            value = alternativePhone.value,
                            onValueChange = { alternativePhone.value = it },
                            label = { Text("Alternative Phone Number") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = emergencyContactName.value,
                                onValueChange = { emergencyContactName.value = it },
                                label = { Text("Emergency Contact Name") },
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                singleLine = true,
                                textStyle = TextStyle(color = TitleBlack),
                            )
                            OutlinedTextField(
                                value = emergencyContactRelation.value,
                                onValueChange = { emergencyContactRelation.value = it },
                                label = { Text("Relation") },
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                singleLine = true,
                                textStyle = TextStyle(color = TitleBlack),
                            )
                        }
                        OutlinedTextField(
                            value = emergencyContactPhone.value,
                            onValueChange = { emergencyContactPhone.value = it },
                            label = { Text("Emergency Contact Phone") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = TitleBlack),
                        )

                        // ── Address Section ──
                        SectionHeader("Address")
                        DivisionDropdown(division.value) { div ->
                            division.value = div
                            district.value = ""
                            thana.value = ""
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DistrictDropdown(
                                selectedDivision = division.value,
                                selectedDistrict = district.value,
                                onDistrictSelected = { dist ->
                                    district.value = dist
                                    thana.value = ""
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ThanaDropdown(
                                selectedDivision = division.value,
                                selectedDistrict = district.value,
                                selectedThana = thana.value,
                                onThanaSelected = { thana.value = it },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = addressDetail.value,
                            onValueChange = { addressDetail.value = it },
                            label = { Text("Road / House No. / Landmark") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            minLines = 2,
                            textStyle = TextStyle(color = TitleBlack),
                        )

                        if (formError != null) {
                            Text(
                                text = formError!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        if (formSuccess) {
                            Text(
                                text = "Information saved successfully!",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showInfoForm = false },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    scope.launch {
                                        formLoading = true
                                        formError = null
                                        formSuccess = false
                                        try {
                                            RetrofitClient.apiService.submitPatientInfo(
                                                PatientInfoRequest(
                                                    full_name_en = fullNameEn.value.ifBlank { null },
                                                    full_name_bn = fullNameBn.value.ifBlank { null },
                                                    date_of_birth = dateOfBirth.value.ifBlank { null },
                                                    sex = selectedSex.value.ifBlank { null },
                                                    blood_group = selectedBloodGroup.value.ifBlank { null },
                                                    primary_phone = primaryPhone.value.ifBlank { null },
                                                    alternative_phone = alternativePhone.value.ifBlank { null },
                                                    emergency_contact_name = emergencyContactName.value.ifBlank { null },
                                                    emergency_contact_relation = emergencyContactRelation.value.ifBlank { null },
                                                    emergency_contact_phone = emergencyContactPhone.value.ifBlank { null },
                                                    division = division.value.ifBlank { null },
                                                    district = district.value.ifBlank { null },
                                                    thana = thana.value.ifBlank { null },
                                                    address_detail = addressDetail.value.ifBlank { null },
                                                )
                                            )
                                            formSuccess = true
                                            showInfoForm = false
                                        } catch (e: Exception) {
                                            formError = e.message ?: "Failed to save information."
                                        } finally {
                                            formLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !formLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TechTeal,
                                    contentColor = PureWhite,
                                ),
                            ) {
                                if (formLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = PureWhite,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("Save", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick actions grid ──
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.CalendarMonth,
                    label = "Appointments",
                    color = TechTeal,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAppointments,
                )
                QuickActionCard(
                    icon = Icons.Default.Description,
                    label = "Documents",
                    color = ClinicalNavy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        docFilePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "image/*",
                            )
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.LocalPharmacy,
                    label = "Prescriptions",
                    color = SlateGray,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
                QuickActionCard(
                    icon = Icons.Default.MedicalServices,
                    label = "Support",
                    color = AlertAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { },
                )
            }

            if (docUploading) {
                Text(
                    text = "Uploading document...",
                    fontSize = 12.sp,
                    color = TechTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }
            if (docUploadStatus != null) {
                Text(
                    text = docUploadStatus!!,
                    fontSize = 12.sp,
                    color = if (docUploadStatus!!.startsWith("Upload failed")) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Aastha Tele-HealthCare v1.0.0",
                fontSize = 11.sp,
                color = CoolGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
    }
}

@Composable
fun CallScreen(
    displayName: String,
    callStatus: CallStatus,
    onEndCall: () -> Unit,
) {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.CONNECTED) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val timerText = remember(seconds) {
        val min = seconds / 60
        val sec = seconds % 60
        "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ClinicalNavy, SlateGray),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(TechTeal.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AH",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Aastha Tele-HealthCare",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callStatus) {
                    CallStatus.RINGING -> "Calling..."
                    CallStatus.CONNECTED -> timerText
                    else -> ""
                },
                fontSize = 16.sp,
                color = PureWhite.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onEndCall,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = PureWhite,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tap to end call",
                fontSize = 13.sp,
                color = PureWhite.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(selectedSex: String, onSexSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Male", "Female", "Child")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedSex,
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender") },
            textStyle = TextStyle(color = TitleBlack),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSexSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodGroupDropdown(selectedBloodGroup: String, onBloodGroupSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedBloodGroup,
            onValueChange = {},
            readOnly = true,
            label = { Text("Blood Group") },
            textStyle = TextStyle(color = TitleBlack),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onBloodGroupSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

object BangladeshAddressData {
    data class District(val name: String, val thanas: List<String>)
    data class Division(val name: String, val districts: List<District>)

    val divisions = listOf(
        Division("Dhaka", listOf(
            District("Dhaka", listOf("Adabar", "Badda", "Cantonment", "Demra", "Dhanmondi", "Gulshan", "Hazaribagh", "Kadamtali", "Kafrul", "Kamrangirchar", "Khilgaon", "Khilkhet", "Lalbagh", "Mirpur", "Mohammadpur", "Motijheel", "New Market", "Pallabi", "Ramna", "Rampura", "Sabujbagh", "Shah Ali", "Shahbagh", "Sher-e-Bangla Nagar", "Shyampur", "Sutrapur", "Tejgaon", "Tejgaon Industrial", "Uttara", "Uttarkhan")),
            District("Gazipur", listOf("Kaliakair", "Kaliganj", "Kapasia", "Sreepur", "Gazipur Sadar")),
            District("Narayanganj", listOf("Araihazar", "Bandar", "Narayanganj Sadar", "Rupganj", "Sonargaon")),
            District("Tangail", listOf("Basail", "Bhuapur", "Delduar", "Dhanbari", "Ghatail", "Gopalpur", "Kalihati", "Madhupur", "Mirzapur", "Nagarpur", "Sakhipur", "Tangail Sadar")),
        )),
        Division("Chattogram", listOf(
            District("Chattogram", listOf("Akbar Shah", "Anwara", "Bakalia", "Bandar", "Bayezid", "Bhashan Char", "Chandgaon", "Chattogram Kotwali", "Double Mooring", "EPZ", "Halishahar", "Karnaphuli", "Khulshi", "Pahartali", "Panchlaish", "Patenga", "Sandwip", "Satkania", "Sitakunda")),
            District("Cox's Bazar", listOf("Chakaria", "Cox's Bazar Sadar", "Kutubdia", "Maheshkhali", "Pekua", "Ramu", "Teknaf", "Ukhia")),
        )),
        Division("Rajshahi", listOf(
            District("Rajshahi", listOf("Bagha", "Bagmara", "Boalia", "Charghat", "Durgapur", "Godagari", "Mohanpur", "Paba", "Putnia", "Rajshahi Sadar", "Tanore")),
            District("Bogura", listOf("Adamdighi", "Bogura Sadar", "Dhunat", "Dhupchanchia", "Gabtali", "Kahaloo", "Nandigram", "Sariakandi", "Shajahanpur", "Sherpur", "Shibganj", "Sonatala")),
        )),
        Division("Khulna", listOf(
            District("Khulna", listOf("Aranghata", "Daulatpur", "Dighalia", "Dumuria", "Khalishpur", "Khan Jahan Ali", "Khulna Sadar", "Koyra", "Paikgachha", "Phultala", "Rupsha", "Sonadanga", "Terokhada")),
            District("Jessore", listOf("Abhaynagar", "Bagherpara", "Chaugachha", "Jhikargachha", "Jessore Sadar", "Keshabpur", "Manirampur", "Sharsha")),
        )),
        Division("Barishal", listOf(
            District("Barishal", listOf("Agailjhara", "Babuganj", "Bakerganj", "Banaripara", "Barishal Sadar", "Gournadi", "Hizla", "Mehendiganj", "Muladi", "Wazirpur")),
            District("Patuakhali", listOf("Bauphal", "Dashmina", "Dumki", "Galachipa", "Kalapara", "Mirzaganj", "Patuakhali Sadar", "Rangabali")),
        )),
        Division("Sylhet", listOf(
            District("Sylhet", listOf("Balaganj", "Beanibazar", "Bishwanath", "Companiganj", "Dakshin Surma", "Fenchuganj", "Golapganj", "Gowainghat", "Jaintiapur", "Kanaighat", "Osmani Nagar", "Sylhet Sadar", "Zakiganj")),
            District("Moulvibazar", listOf("Barlekha", "Juri", "Kamalganj", "Kulaura", "Moulvibazar Sadar", "Rajnagar", "Sreemangal")),
        )),
        Division("Rangpur", listOf(
            District("Rangpur", listOf("Badarganj", "Gangachara", "Kaunia", "Mithapukur", "Pirgachha", "Pirganj", "Rangpur Sadar", "Taraganj")),
            District("Dinajpur", listOf("Birampur", "Birganj", "Biral", "Bochaganj", "Chirirbandar", "Dinajpur Sadar", "Ghoraghat", "Hakimpur", "Kaharole", "Khansama", "Nawabganj", "Parbatipur")),
        )),
        Division("Mymensingh", listOf(
            District("Mymensingh", listOf("Bhaluka", "Dhobaura", "Fulbaria", "Gaffargaon", "Gauripur", "Haluaghat", "Ishwarganj", "Muktagachha", "Mymensingh Sadar", "Nandail", "Phulpur", "Trishal")),
            District("Jamalpur", listOf("Bakshiganj", "Dewanganj", "Islampur", "Jamalpur Sadar", "Madarganj", "Melandaha", "Sarishabari")),
        )),
    )

    fun getDistricts(divisionName: String): List<District> =
        divisions.find { it.name == divisionName }?.districts ?: emptyList()

    fun getThanas(divisionName: String, districtName: String): List<String> =
        getDistricts(divisionName).find { it.name == districtName }?.thanas ?: emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionDropdown(
    selectedDivision: String,
    onDivisionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedDivision,
            onValueChange = {},
            readOnly = true,
            label = { Text("Division") },
            textStyle = TextStyle(color = TitleBlack),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BangladeshAddressData.divisions.forEach { div ->
                DropdownMenuItem(
                    text = { Text(div.name) },
                    onClick = {
                        onDivisionSelected(div.name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictDropdown(
    selectedDivision: String,
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val districts = BangladeshAddressData.getDistricts(selectedDivision)
    ExposedDropdownMenuBox(
        expanded = expanded && districts.isNotEmpty(),
        onExpandedChange = { if (districts.isNotEmpty()) expanded = !expanded },
        modifier = modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedDistrict,
            onValueChange = {},
            readOnly = true,
            label = { Text("District") },
            textStyle = TextStyle(color = TitleBlack),
            enabled = selectedDivision.isNotEmpty(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            districts.forEach { dist ->
                DropdownMenuItem(
                    text = { Text(dist.name) },
                    onClick = {
                        onDistrictSelected(dist.name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThanaDropdown(
    selectedDivision: String,
    selectedDistrict: String,
    selectedThana: String,
    onThanaSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val thanas = BangladeshAddressData.getThanas(selectedDivision, selectedDistrict)
    ExposedDropdownMenuBox(
        expanded = expanded && thanas.isNotEmpty(),
        onExpandedChange = { if (thanas.isNotEmpty()) expanded = !expanded },
        modifier = modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedThana,
            onValueChange = {},
            readOnly = true,
            label = { Text("Thana") },
            textStyle = TextStyle(color = TitleBlack),
            enabled = selectedDistrict.isNotEmpty(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            thanas.forEach { thana ->
                DropdownMenuItem(
                    text = { Text(thana) },
                    onClick = {
                        onThanaSelected(thana)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = ClinicalNavy,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitleBlack,
                textAlign = TextAlign.Center,
            )
        }
    }
}
