package com.example.hhdmspatientapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseWoundCareScreen(
    onBack: () -> Unit,
) {
    var patients by remember { mutableStateOf<List<NursePatient>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<NursePatient?>(null) }
    var woundRecords by remember { mutableStateOf<List<WoundCareRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var woundLocation by remember { mutableStateOf("") }
    var woundMeasurements by remember { mutableStateOf("") }
    var woundCondition by remember { mutableStateOf("") }
    var dressingApplied by remember { mutableStateOf("") }
    var healingProgress by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val conditions = listOf("Clean", "Infected", "Healing", "Necrotic", "Granulating", "Epithelializing")
    val healings = listOf("Improving", "Stable", "Deteriorating", "Healed")

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> photoUri = uri }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                    loc?.let { location = Pair(it.latitude, it.longitude) }
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(snackbarText) {
        snackbarText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); snackbarText = null }
    }

    LaunchedEffect(Unit) {
        try { patients = RetrofitClient.apiService.getNursePatients(); loading = false }
        catch (e: Exception) { snackbarText = "Failed: ${e.message}"; loading = false }
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            try { woundRecords = RetrofitClient.apiService.getWoundCareRecords(p.id) } catch (_: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wound Care Documentation", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("NS-007", fontSize = 12.sp, color = PureWhite.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (showForm || selectedPatient != null) {
                        if (showForm) { showForm = false } else { selectedPatient = null }
                    } else { onBack() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFE65100), TechTeal))),
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
            selectedPatient == null && !showForm -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                ) {
                    item { Text("Select Patient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack) }
                    items(patients, key = { it.id }) { patient ->
                        val name = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim()
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPatient = patient; showForm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFFE65100).copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    patient.mrn?.let { Text(it, fontSize = 11.sp, color = CoolGray) }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoolGray)
                            }
                        }
                    }
                }
            }
            showForm -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("Record Wound Assessment", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = woundLocation, onValueChange = { woundLocation = it }, label = { Text("Wound Location *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = woundMeasurements, onValueChange = { woundMeasurements = it }, label = { Text("Measurements (L x W x D cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Condition dropdown
                    var expandedCond by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCond, onExpandedChange = { expandedCond = !expandedCond }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = woundCondition, onValueChange = {}, readOnly = true, label = { Text("Wound Condition") }, textStyle = TextStyle(color = TitleBlack), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCond) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedCond, onDismissRequest = { expandedCond = false }) {
                            conditions.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { woundCondition = c; expandedCond = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = dressingApplied, onValueChange = { dressingApplied = it }, label = { Text("Dressing Applied") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = TitleBlack))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Healing progress dropdown
                    var expandedHeal by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedHeal, onExpandedChange = { expandedHeal = !expandedHeal }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = healingProgress, onValueChange = {}, readOnly = true, label = { Text("Healing Progress") }, textStyle = TextStyle(color = TitleBlack), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHeal) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedHeal, onDismissRequest = { expandedHeal = false }) {
                            healings.forEach { h -> DropdownMenuItem(text = { Text(h) }, onClick = { healingProgress = h; expandedHeal = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo + Location
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { photoLauncher.launch("image/*") }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(if (photoUri != null) Icons.Default.Check else Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (photoUri != null) "Photo Added" else "Add Photo", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    val client = LocationServices.getFusedLocationProviderClient(context)
                                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                                        loc?.let { location = Pair(it.latitude, it.longitude) }
                                    }
                                } catch (_: Exception) { }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (location != null) "GPS Tagged" else "Tag GPS", fontSize = 12.sp)
                        }
                    }
                    if (location != null) {
                        Text("GPS: ${String.format("%.6f", location!!.first)}, ${String.format("%.6f", location!!.second)}", fontSize = 10.sp, color = CoolGray, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    val patient = selectedPatient ?: return@launch
                                    RetrofitClient.apiService.createWoundCareRecord(
                                        patient.id,
                                        NurseCreateWoundCareRequest(
                                            patient_id = patient.id,
                                            wound_location = woundLocation.ifBlank { null },
                                            wound_measurements = woundMeasurements.ifBlank { null },
                                            wound_condition = woundCondition.ifBlank { null },
                                            dressing_applied = dressingApplied.ifBlank { null },
                                            healing_progress = healingProgress.ifBlank { null },
                                            latitude = location?.first,
                                            longitude = location?.second,
                                        ),
                                    )
                                    // Upload photo if selected
                                    photoUri?.let { uri ->
                                        try {
                                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@let
                                            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                                            val part = okhttp3.MultipartBody.Part.createFormData("photo", "wound_${System.currentTimeMillis()}.jpg", body)
                                            val woundIdBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                                            RetrofitClient.apiService.uploadWoundPhoto(patient.id, part, woundIdBody)
                                        } catch (_: Exception) { }
                                    }
                                    snackbarText = "Wound care recorded!"
                                    showForm = false
                                    selectedPatient?.let { p -> woundRecords = RetrofitClient.apiService.getWoundCareRecords(p.id) }
                                } catch (e: Exception) { snackbarText = "Error: ${e.message}" }
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        enabled = !saving,
                    ) {
                        if (saving) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Save, contentDescription = null, tint = PureWhite); Spacer(modifier = Modifier.width(8.dp)); Text("Save Record", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }

                    // History
                    if (woundRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Wound Care History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        woundRecords.forEach { w ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(w.wound_location ?: "Wound", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(w.recorded_at?.take(10) ?: "", fontSize = 10.sp, color = CoolGray)
                                    }
                                    w.wound_measurements?.let { Text("Size: $it", fontSize = 11.sp, color = CoolGray) }
                                    w.wound_condition?.let { Text("Condition: $it", fontSize = 11.sp, color = CoolGray) }
                                    w.healing_progress?.let { Text("Progress: $it", fontSize = 11.sp, color = CoolGray) }
                                    if (w.photo_url != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = TechTeal, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Photo attached", fontSize = 10.sp, color = TechTeal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
