package com.example.hhdmspatientapp

// ══════════════════════════════════════════════════════════════════════════════
// NEW FILE: GPS-based Check-In/Check-Out screen (CG-006).
// Caregiver checks in when within 100m of patient address (geofence verified).
// Uses Android FusedLocationProviderClient for GPS coordinates.
// Backend validates distance against patient registered address.
// ══════════════════════════════════════════════════════════════════════════════

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverCheckInOutScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<CaregiverPatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<CaregiverPatient?>(null) }
    var checkInOuts by remember { mutableStateOf<List<CaregiverCheckInOut>>(emptyList()) }
    var todayRecord by remember { mutableStateOf<CaregiverCheckInOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionLoading by remember { mutableStateOf(false) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                    currentLocation = loc
                }
            } catch (_: Exception) { }
        } else {
            locationError = "Location permission required for GPS check-in"
        }
    }

    LaunchedEffect(snackbarText) {
        snackbarText?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarText = null
        }
    }

    fun getCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                currentLocation = loc
                locationError = null
            }.addOnFailureListener {
                locationError = "Unable to get location. Please enable GPS."
            }
        } catch (e: SecurityException) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun loadData() {
        scope.launch {
            loading = true
            error = null
            // Load each call independently — new endpoints may not exist yet
            try {
                patients = RetrofitClient.apiService.getCaregiverPatients()
                if (patients.isNotEmpty() && selectedPatient == null) {
                    selectedPatient = patients[0]
                }
            } catch (e: Exception) {
                error = "Failed to load patients: ${e.message}"
            }
            try {
                checkInOuts = RetrofitClient.apiService.getCaregiverCheckInOuts(null)
            } catch (_: Exception) { }
            try {
                todayRecord = RetrofitClient.apiService.getTodayCheckInOut()
            } catch (_: Exception) { }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
        getCurrentLocation()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Check-In / Check-Out", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF22C55E), TechTeal))),
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
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = CoolGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error ?: "Error", color = CoolGray, fontSize = 14.sp)
                        TextButton(onClick = { loadData() }) { Text("Retry", color = TechTeal) }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    // ── Current Status Card ──
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val isCheckedIn = todayRecord != null && todayRecord!!.check_out_time == null
                                val isCheckedOut = todayRecord != null && todayRecord!!.check_out_time != null

                                // Status indicator
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            when {
                                                isCheckedIn -> Color(0xFF22C55E)
                                                isCheckedOut -> CoolGray
                                                else -> TechTeal
                                            },
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        when {
                                            isCheckedIn -> Icons.Default.LocationOn
                                            isCheckedOut -> Icons.Default.CheckCircle
                                            else -> Icons.Default.AddLocationAlt
                                        },
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(40.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    when {
                                        isCheckedIn -> "Checked In"
                                        isCheckedOut -> "Shift Completed"
                                        else -> "Ready to Check In"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TitleBlack,
                                )

                                todayRecord?.let { rec ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    rec.check_in_time?.let {
                                        Text("Check-in: ${formatTime(it)}", fontSize = 12.sp, color = CoolGray)
                                    }
                                    rec.check_out_time?.let {
                                        Text("Check-out: ${formatTime(it)}", fontSize = 12.sp, color = CoolGray)
                                    }
                                    rec.distance_meters?.let { dist ->
                                        Text("Distance: ${String.format("%.0f", dist)}m from patient", fontSize = 11.sp, color = CoolGray.copy(alpha = 0.7f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Location status
                                if (locationError != null) {
                                    Text(locationError ?: "", fontSize = 11.sp, color = ErrorRed, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    }) { Text("Enable Location", color = TechTeal) }
                                } else if (currentLocation == null) {
                                    Text("Getting GPS location...", fontSize = 11.sp, color = CoolGray)
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TechTeal)
                                } else {
                                    Text(
                                        "GPS: ${String.format("%.6f", currentLocation!!.latitude)}, ${String.format("%.6f", currentLocation!!.longitude)}",
                                        fontSize = 10.sp,
                                        color = CoolGray.copy(alpha = 0.6f),
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ── Patient Selector (only if not checked in) ──
                                if (!isCheckedIn) {
                                    Text("Select Patient to Check In", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoolGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (patients.isEmpty()) {
                                        Text("No patients assigned.", fontSize = 12.sp, color = CoolGray)
                                    } else {
                                        patients.forEach { p ->
                                            val isSelected = selectedPatient?.id == p.id
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = if (isSelected) TechTeal.copy(alpha = 0.08f) else SoftSlate),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { selectedPatient = p },
                                                        colors = RadioButtonDefaults.colors(selectedColor = TechTeal),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "${p.first_name_en} ${p.last_name_en ?: ""}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TitleBlack,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // ── Check-In / Check-Out Button ──
                                Button(
                                    onClick = {
                                        scope.launch {
                                            actionLoading = true
                                            try {
                                                val loc = currentLocation
                                                if (loc == null) {
                                                    snackbarText = "Waiting for GPS location..."
                                                    actionLoading = false
                                                    return@launch
                                                }

                                                if (isCheckedIn) {
                                                    // Check Out
                                                    RetrofitClient.apiService.caregiverCheckOut(
                                                        todayRecord!!.id,
                                                        CaregiverCheckOutRequest(
                                                            latitude = loc.latitude,
                                                            longitude = loc.longitude,
                                                        )
                                                    )
                                                    snackbarText = "Checked out successfully!"
                                                } else {
                                                    // Check In
                                                    val sp = selectedPatient
                                                    if (sp == null) {
                                                        snackbarText = "Please select a patient"
                                                        actionLoading = false
                                                        return@launch
                                                    }
                                                    RetrofitClient.apiService.caregiverCheckIn(
                                                        CaregiverCheckInRequest(
                                                            patient_id = sp.id,
                                                            latitude = loc.latitude,
                                                            longitude = loc.longitude,
                                                        )
                                                    )
                                                    snackbarText = "Checked in successfully!"
                                                }
                                                loadData()
                                            } catch (e: Exception) {
                                                snackbarText = "Error: ${e.message ?: "Failed"}"
                                            }
                                            actionLoading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isCheckedIn -> ErrorRed
                                            else -> Color(0xFF22C55E)
                                        },
                                    ),
                                    enabled = !actionLoading && currentLocation != null,
                                ) {
                                    if (actionLoading) {
                                        CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            if (isCheckedIn) Icons.Default.Logout else Icons.Default.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isCheckedIn) "Check Out" else "Check In",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Recent Check-In/Out History ──
                    if (checkInOuts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recent History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        }
                        items(checkInOuts, key = { it.id }) { record ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (record.check_out_time != null) CoolGray else Color(0xFF22C55E),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            record.patient?.let { "${it.first_name_en ?: ""} ${it.last_name_en ?: ""}" } ?: "Patient",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TitleBlack,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            record.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite,
                                            modifier = Modifier
                                                .background(
                                                    if (record.check_out_time != null) CoolGray else Color(0xFF22C55E),
                                                    RoundedCornerShape(4.dp),
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("In: ${record.check_in_time?.let { formatTime(it) } ?: "—"}", fontSize = 11.sp, color = CoolGray)
                                        Text("Out: ${record.check_out_time?.let { formatTime(it) } ?: "—"}", fontSize = 11.sp, color = CoolGray)
                                    }
                                    record.distance_meters?.let { dist ->
                                        Text("Distance: ${String.format("%.0f", dist)}m", fontSize = 10.sp, color = CoolGray.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

private fun formatTime(isoTime: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(isoTime.take(19))
        date?.let { outputFormat.format(it) } ?: isoTime.take(16)
    } catch (_: Exception) {
        isoTime.take(16).replace("T", " ")
    }
}
