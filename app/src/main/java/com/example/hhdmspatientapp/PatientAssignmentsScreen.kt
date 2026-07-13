package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAssignmentsScreen(
    onBack: () -> Unit,
    onPatientClick: (patientId: String) -> Unit,
) {
    var patients by remember { mutableStateOf<List<MbbsPatientSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("active") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            patients = RetrofitClient.apiService.getMbbsPatients()
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load patients"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Patient Assignments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                        )
                        if (!loading) {
                            val activeCount = patients.count { it.appointment_activity != "done" }
                            val completedCount = patients.count { it.appointment_activity == "done" }
                            Text(
                                text = "$activeCount active • $completedCount completed",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)),
                    ),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TechTeal)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CoolGray.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Could not load patients",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CoolGray,
                        )
                        Text(
                            text = error ?: "",
                            fontSize = 13.sp,
                            color = CoolGray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }

            patients.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CoolGray.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No patients assigned yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CoolGray,
                        )
                        Text(
                            text = "Assigned patients will appear here",
                            fontSize = 13.sp,
                            color = CoolGray.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            else -> {
                val activePatients = patients.filter { it.appointment_activity != "done" }
                val completedPatients = patients.filter { it.appointment_activity == "done" }
                val baseList = if (selectedFilter == "active") activePatients else completedPatients
                val query = searchQuery.trim().lowercase()
                val filtered = if (query.isNotEmpty()) {
                    baseList.filter { p ->
                        p.first_name_en.lowercase().contains(query) ||
                        (p.last_name_en?.lowercase()?.contains(query) == true) ||
                        p.mrn.lowercase().contains(query)
                    }
                } else baseList

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .navigationBarsPadding(),
                ) {
                    // Filter buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChipItem(
                            label = "Active",
                            count = activePatients.size,
                            selected = selectedFilter == "active",
                            selectedColor = ClinicalNavy,
                            onClick = { selectedFilter = "active" },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChipItem(
                            label = "Completed",
                            count = completedPatients.size,
                            selected = selectedFilter == "completed",
                            selectedColor = TechTeal,
                            onClick = { selectedFilter = "completed" },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or MRN...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CoolGray)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedBorderColor = TechTeal,
                            unfocusedContainerColor = PureWhite,
                            focusedContainerColor = PureWhite,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = CoolGray.copy(alpha = 0.5f),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (query.isNotEmpty()) "No patients match your search" else "No ${selectedFilter} patients",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CoolGray,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        ) {
                            items(filtered, key = { it.id }) { patient ->
                                PatientCard(
                                    patient = patient,
                                    completed = patient.appointment_activity == "done",
                                    onClick = { onPatientClick(patient.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedColor else CoolGray.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) PureWhite else CoolGray,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) PureWhite.copy(alpha = 0.25f) else CoolGray.copy(alpha = 0.15f),
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) PureWhite else CoolGray,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: MbbsPatientSummary,
    completed: Boolean = false,
    onClick: () -> Unit,
) {
    val initials = buildString {
        append(patient.first_name_en.firstOrNull() ?: '—')
        patient.last_name_en?.firstOrNull()?.let { append(it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (completed) Modifier else Modifier.clickable(onClick = onClick)
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) CoolGray.copy(alpha = 0.08f) else PureWhite,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (completed) 0.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (completed) CoolGray.copy(alpha = 0.12f) else TechTeal.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) CoolGray else TechTeal,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${patient.first_name_en} ${patient.last_name_en ?: ""}".trim(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (completed) CoolGray else TitleBlack,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = patient.mrn,
                        fontSize = 12.sp,
                        color = CoolGray,
                    )
                }
            }
            if (patient.has_emergency_flag == true && !completed) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Emergency",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
