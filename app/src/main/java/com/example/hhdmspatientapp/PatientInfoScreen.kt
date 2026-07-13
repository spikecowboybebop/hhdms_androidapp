package com.example.hhdmspatientapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientInfoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var formLoading by remember { mutableStateOf(false) }
    var formSuccess by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var dataLoaded by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        if (!dataLoaded) {
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Information", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite),
            )
        },
        containerColor = Color(0xFFF5F7FA),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // ── Demographics ──
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

            // ── Contact ──
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

            // ── Address ──
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
                    onClick = onBack,
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
