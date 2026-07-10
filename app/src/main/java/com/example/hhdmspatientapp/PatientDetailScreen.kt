package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onNavigateToVitals: (patientId: String, patientName: String) -> Unit = { _, _ -> },
    onNavigateToDiagnosis: (patientId: String, patientName: String) -> Unit = { _, _ -> },
    onNavigateToPrescription: (patientId: String, patientName: String) -> Unit = { _, _ -> },
    onNavigateToTestOrders: (patientId: String, patientName: String) -> Unit = { _, _ -> },
    onNavigateToReferral: (patientId: String, patientName: String) -> Unit = { _, _ -> },
) {
    var profile by remember { mutableStateOf<MbbsPatientProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val tabs = listOf("Info", "Vitals", "Diagnosis", "Rx", "Tests", "Referrals", "Documents")
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)

    fun loadProfile() {
        scope.launch {
            loading = true
            error = null
            try {
                profile = RetrofitClient.apiService.getMbbsPatientProfile(patientId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load patient"
            }
            loading = false
        }
    }

    LaunchedEffect(patientId) { loadProfile() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Details",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TechTeal)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "Something went wrong", fontSize = 14.sp, color = CoolGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadProfile() }) { Text("Retry", color = TechTeal) }
                    }
                }
            }
            profile != null -> {
                val p = profile!!.patient
                val patientName = "${p.first_name_en} ${p.last_name_en ?: ""}".trim()
                val initials = buildString {
                    append(p.first_name_en.firstOrNull() ?: '—')
                    p.last_name_en?.firstOrNull()?.let { append(it) }
                }

                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(56.dp).background(Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(initials, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patientName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                                    Text(p.mrn, fontSize = 12.sp, color = TechTeal, fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(when (p.sex) { "M" -> "Male"; "F" -> "Female"; else -> p.sex ?: "—" }, fontSize = 12.sp, color = CoolGray)
                                        Text("  |  ", fontSize = 12.sp, color = LightGray)
                                        Text(p.blood_group ?: "—", fontSize = 12.sp, color = CoolGray)
                                        Text("  |  ", fontSize = 12.sp, color = LightGray)
                                        Text(p.date_of_birth?.take(10) ?: "—", fontSize = 12.sp, color = CoolGray)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ClinicalMiniButton(
                                    icon = Icons.Default.FavoriteBorder, label = "Vitals",
                                    color = Color(0xFFE91E63),
                                    onClick = { onNavigateToVitals(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                                ClinicalMiniButton(
                                    icon = Icons.Default.Description, label = "Diagnosis",
                                    color = TechTeal,
                                    onClick = { onNavigateToDiagnosis(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                                ClinicalMiniButton(
                                    icon = Icons.Default.LocalPharmacy, label = "Rx",
                                    color = ClinicalNavy,
                                    onClick = { onNavigateToPrescription(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                                ClinicalMiniButton(
                                    icon = Icons.Default.Science, label = "Tests",
                                    color = Color(0xFF9C27B0),
                                    onClick = { onNavigateToTestOrders(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                                ClinicalMiniButton(
                                    icon = Icons.Default.Share, label = "Refer",
                                    color = Color(0xFFFF9800),
                                    onClick = { onNavigateToReferral(patientId, patientName) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (p.has_emergency_flag != true) {
                        Spacer(modifier = Modifier.height(4.dp))

                        var isVisiting by remember { mutableStateOf(VisitStorage.getVisitingPatientId() == patientId) }
                        var visitLoading by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                scope.launch {
                                    visitLoading = true
                                    try {
                                        RetrofitClient.apiService.startPatientVisit(patientId)
                                        VisitStorage.saveVisitingPatientId(patientId)
                                        isVisiting = true
                                    } catch (_: Exception) { }
                                    visitLoading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVisiting) TechTeal.copy(alpha = 0.15f) else TechTeal,
                                contentColor = if (isVisiting) TechTeal else PureWhite,
                            ),
                            enabled = !visitLoading && !isVisiting,
                        ) {
                            if (visitLoading) {
                                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (isVisiting) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("You are visiting this patient", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Visit — Notify Patient", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = PureWhite,
                        contentColor = TechTeal,
                        edgePadding = 8.dp,
                        divider = {},
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        title, fontSize = 12.sp,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (pagerState.currentPage == index) TechTeal else CoolGray,
                                    )
                                },
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> PatientInfoTab(profile!!)
                            1 -> PatientVitalsTab(profile!!.vitals)
                            2 -> PatientDiagnosesTab(profile!!.diagnoses)
                            3 -> PatientPrescriptionsTab(profile!!.prescriptions)
                            4 -> PatientTestOrdersTab(profile!!.test_orders)
                            5 -> PatientReferralsTab(profile!!.referrals)
                            6 -> PatientDocumentsTab(profile!!.documents)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalMiniButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Tab: Info ──
@Composable
private fun PatientInfoTab(profile: MbbsPatientProfileResponse) {
    val p = profile.patient
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Contact Info", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    DetailRow2(Icons.Default.Phone, "Phone", p.phone_number ?: "—")
                    DetailRow2(Icons.Default.Email, "Email", p.email ?: "—")
                    DetailRow2(Icons.Default.LocationOn, "Address", listOfNotNull(p.address_line1, p.address_line2, p.district).joinToString(", ").ifEmpty { "—" })
                    DetailRow2(Icons.Default.PhoneInTalk, "Emergency", p.emergency_contact ?: "—")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Medical History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    DetailRow2(Icons.Default.Healing, "Allergies", p.known_allergies ?: "None reported")
                    DetailRow2(Icons.Default.Medication, "Current Meds", p.current_medications ?: "None")
                    DetailRow2(Icons.Default.History, "Past Medical", p.past_medical_history ?: "None")
                    DetailRow2(Icons.Default.People, "Family History", p.family_history ?: "None")
                }
            }
        }
        if (profile.previous_appointments.isNotEmpty()) {
            item {
                Text("Previous Appointments", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
            }
            items(profile.previous_appointments) { apt ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TechTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dr. ${apt.doctor?.user?.firstNameEn ?: ""} ${apt.doctor?.user?.lastNameEn ?: ""}".trim(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TitleBlack)
                            Text(apt.assigned_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                        }
                        Text(apt.appointment_activity ?: "", fontSize = 11.sp, color = TechTeal, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (profile.referral_chain.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Care Timeline", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
            }
            items(profile.referral_chain) { event ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(TechTeal.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Circle, contentDescription = null, tint = TechTeal, modifier = Modifier.size(8.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.step_label ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TitleBlack)
                            Text("${event.actor_role ?: ""} ${event.actor_name ?: ""}".trim(), fontSize = 11.sp, color = CoolGray)
                        }
                        Text(event.created_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DetailRow2(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CoolGray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, color = CoolGray, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, color = TitleBlack, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

// ── Tab: Vitals ──
@Composable
private fun PatientVitalsTab(vitals: List<VitalSignsRecord>) {
    if (vitals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No vitals recorded", color = CoolGray, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val latest = vitals.first()
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Latest", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MiniVitalCard("SYS", "${latest.systolic_bp ?: "—"}")
                            MiniVitalCard("DIA", "${latest.diastolic_bp ?: "—"}")
                            MiniVitalCard("HR", "${latest.pulse_bpm ?: "—"}")
                            MiniVitalCard("SpO2", "${latest.spo2_pct ?: "—"}")
                            MiniVitalCard("TEMP", "${latest.temperature_c ?: "—"}")
                        }
                    }
                }
            }
            item { Text("History", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
            items(vitals) { v ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.recorded_at?.take(16)?.replace("T", " ") ?: "", fontSize = 11.sp, color = CoolGray)
                            Text("BP: ${v.systolic_bp ?: "—"}/${v.diastolic_bp ?: "—"}  HR: ${v.pulse_bpm ?: "—"}  SpO2: ${v.spo2_pct ?: "—"}%", fontSize = 13.sp, color = TitleBlack)
                        }
                        if (v.is_abnormal) Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniVitalCard(label: String, value: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = SoftSlate)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
            Text(label, fontSize = 9.sp, color = CoolGray)
        }
    }
}

// ── Tab: Diagnoses ──
@Composable
private fun PatientDiagnosesTab(diagnoses: List<DiagnosisRecord>) {
    if (diagnoses.isEmpty()) {
        EmptyTab(Icons.Default.Description, "No diagnoses")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(diagnoses) { d ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(TechTeal.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(d.icd10_code ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TechTeal)
                            }
                            if (d.is_primary) { Spacer(Modifier.width(6.dp)); Text("PRIMARY", fontSize = 10.sp, color = AlertAmber, fontWeight = FontWeight.Bold, modifier = Modifier.background(AlertAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) }
                            Spacer(Modifier.weight(1f))
                            Text(d.diagnosed_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(d.preliminary_diagnosis ?: "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                        if (!d.chief_complaint.isNullOrBlank()) Text("CC: ${d.chief_complaint}", fontSize = 12.sp, color = CoolGray)
                    }
                }
            }
        }
    }
}

// ── Tab: Prescriptions ──
@Composable
private fun PatientPrescriptionsTab(prescriptions: List<PrescriptionRecord>) {
    if (prescriptions.isEmpty()) {
        EmptyTab(Icons.Default.LocalPharmacy, "No prescriptions")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(prescriptions) { rx ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rx.issued_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                            Spacer(Modifier.width(8.dp))
                            Text(rx.status ?: "", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(when (rx.status) { "ACTIVE" -> TechTeal; else -> CoolGray }, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            if (!rx.digital_signature_url.isNullOrBlank()) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Verified, contentDescription = null, tint = TechTeal, modifier = Modifier.size(14.dp)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        rx.medications.forEach { med ->
                            Text("${med.generic_name} - ${med.dosage} ${med.frequency} ${med.duration_days}d", fontSize = 13.sp, color = TitleBlack)
                        }
                    }
                }
            }
        }
    }
}

// ── Tab: Test Orders ──
@Composable
private fun PatientTestOrdersTab(orders: List<TestOrder>) {
    if (orders.isEmpty()) {
        EmptyTab(Icons.Default.Science, "No test orders")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders) { o ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.test?.test_name ?: "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack, modifier = Modifier.weight(1f))
                            Text(o.status ?: "", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(when (o.status) { "ORDERED" -> Color(0xFF9C27B0); "COMPLETED" -> Color(0xFF4CAF50); else -> CoolGray }, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(o.test?.test_code ?: "", fontSize = 11.sp, color = TechTeal)
                        o.results?.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Result: ${r.result_value ?: ""}", fontSize = 12.sp, color = TitleBlack)
                                if (r.is_critical) { Spacer(Modifier.width(4.dp)); Text("CRITICAL", fontSize = 9.sp, color = ErrorRed, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab: Referrals ──
@Composable
private fun PatientReferralsTab(referrals: List<MbbsReferral>) {
    if (referrals.isEmpty()) {
        EmptyTab(Icons.Default.Share, "No referrals")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(referrals) { r ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.specialty_code ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            Spacer(Modifier.width(8.dp))
                            Text(r.status ?: "", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(when (r.status) { "PENDING" -> Color(0xFFFF9800); "ACCEPTED" -> TechTeal; "REJECTED" -> ErrorRed; "COMPLETED" -> Color(0xFF4CAF50); else -> CoolGray }, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            if (r.is_emergency) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp)) }
                        }
                        Text(r.referral_reason ?: "", fontSize = 13.sp, color = TitleBlack)
                        Text(r.created_at?.take(10) ?: "", fontSize = 11.sp, color = CoolGray)
                    }
                }
            }
        }
    }
}

// ── Tab: Documents ──
@Composable
private fun PatientDocumentsTab(documents: List<PatientDocument>) {
    if (documents.isEmpty()) {
        EmptyTab(Icons.Default.Folder, "No documents")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(documents) { doc ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PureWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = TechTeal, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.file_name ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TitleBlack)
                            Text("${doc.file_type ?: ""}  ${if (doc.file_size != null) "${doc.file_size / 1024}KB" else ""}", fontSize = 11.sp, color = CoolGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTab(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = CoolGray, fontSize = 14.sp)
        }
    }
}
