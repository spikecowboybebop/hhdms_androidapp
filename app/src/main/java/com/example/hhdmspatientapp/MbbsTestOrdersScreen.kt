package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbbsTestOrdersScreen(
    patientId: String,
    patientName: String,
    onBack: () -> Unit,
) {
    var testOrders by remember { mutableStateOf<List<TestOrder>>(emptyList()) }
    var testCatalog by remember { mutableStateOf<List<TestCatalogItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedTestIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clinicalNotes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun loadData() {
        scope.launch {
            loading = true
            error = null
            try {
                testOrders = RetrofitClient.apiService.getTestOrders(patientId)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    fun loadCatalog() {
        scope.launch {
            try {
                testCatalog = RetrofitClient.apiService.getTestCatalog()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(patientId) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showForm) "Order Tests" else "Test Orders",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (showForm) {{ showForm = false }} else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                actions = {
                    if (!showForm) {
                        IconButton(onClick = { showForm = true; loadCatalog() }) {
                            Icon(Icons.Default.Add, contentDescription = "Order Tests", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF9C27B0), TechTeal))),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        if (showForm) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                item {
                    Text("Patient: $patientName", fontSize = 14.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                }

                item {
                    Text("Available Tests", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select tests to order", fontSize = 12.sp, color = CoolGray)
                }

                if (testCatalog.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TechTeal)
                        }
                    }
                } else {
                    items(testCatalog.filter { it.is_active }, key = { it.id }) { test ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (selectedTestIds.contains(test.id)) TechTeal.copy(alpha = 0.08f) else PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selectedTestIds.contains(test.id),
                                    onCheckedChange = { checked ->
                                        selectedTestIds = if (checked) selectedTestIds + test.id
                                        else selectedTestIds - test.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9C27B0)),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(test.test_name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                    Row {
                                        Text(test.test_code, fontSize = 11.sp, color = TechTeal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(test.category, fontSize = 11.sp, color = CoolGray)
                                    }
                                }
                                if (test.turnaround_hours != null) {
                                    Text("${test.turnaround_hours}h", fontSize = 11.sp, color = CoolGray)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = clinicalNotes, onValueChange = { clinicalNotes = it }, label = { Text("Clinical Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }

                item {
                    Button(
                        onClick = {
                            scope.launch {
                                submitting = true
                                try {
                                    RetrofitClient.apiService.orderTests(patientId, OrderTestsRequest(
                                        test_ids = selectedTestIds.toList(),
                                        clinical_notes = clinicalNotes.ifBlank { null },
                                    ))
                                    selectedTestIds = emptySet(); clinicalNotes = ""
                                    showForm = false; loadData()
                                } catch (e: Exception) { error = e.message }
                                submitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                        enabled = !submitting && selectedTestIds.isNotEmpty(),
                    ) {
                        if (submitting) CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                        else Text("Order Selected Tests (${selectedTestIds.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
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
                testOrders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoolGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No test orders yet", fontSize = 16.sp, color = CoolGray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(testOrders, key = { it.id }) { order ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(order.test?.test_name ?: "Test", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack, modifier = Modifier.weight(1f))
                                        Text(order.status ?: "", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(
                                            when (order.status) {
                                                "ORDERED" -> Color(0xFF9C27B0); "COLLECTED" -> Color(0xFFFF9800)
                                                "IN_PROGRESS" -> TechTeal; "COMPLETED" -> Color(0xFF4CAF50)
                                                "CANCELLED" -> ErrorRed; else -> CoolGray
                                            }, RoundedCornerShape(4.dp),
                                        ).padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Row {
                                        Text(order.test?.test_code ?: "", fontSize = 11.sp, color = TechTeal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(order.test?.category ?: "", fontSize = 11.sp, color = CoolGray)
                                    }
                                    Text("Ordered: ${order.ordered_at?.take(10) ?: "—"}", fontSize = 11.sp, color = CoolGray)

                                    val results = order.results
                                    if (!results.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Results:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TitleBlack)
                                        results.forEach { result ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(result.result_value ?: "", fontSize = 13.sp, color = TitleBlack)
                                                if (result.is_critical) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("CRITICAL", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.background(ErrorRed, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                                } else if (result.is_abnormal) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("ABNORMAL", fontSize = 10.sp, color = AlertAmber, fontWeight = FontWeight.Bold, modifier = Modifier.background(AlertAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (!order.clinical_notes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Notes: ${order.clinical_notes}", fontSize = 11.sp, color = CoolGray)
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
