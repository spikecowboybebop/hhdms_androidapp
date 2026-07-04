package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val WAYPOINTS = listOf(
    GeoPoint(23.7775, 90.4050),
    GeoPoint(23.7785, 90.4100),
    GeoPoint(23.7830, 90.4120),
    GeoPoint(23.7870, 90.4150),
    GeoPoint(23.7890, 90.4200),
    GeoPoint(23.7925, 90.4260),
)

private val PATIENT_POSITION = WAYPOINTS.last()
private val TOTAL_STEPS = WAYPOINTS.size - 1
private const val STEP_INTERVAL_MS = 3000L
private const val START_ETA_SECONDS = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorTrackingScreen(
    doctorName: String,
    onBack: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var etaSeconds by remember { mutableIntStateOf(START_ETA_SECONDS) }
    var isArrived by remember { mutableStateOf(false) }
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val doctorMarker = remember { mutableStateOf<Marker?>(null) }
    val trailPolyline = remember { mutableStateOf<Polyline?>(null) }

    LaunchedEffect(Unit) {
        for (step in 1..TOTAL_STEPS) {
            delay(STEP_INTERVAL_MS)
            currentStep = step

            val map = mapView.value ?: continue
            val marker = doctorMarker.value ?: continue
            val trail = trailPolyline.value ?: continue

            marker.setPosition(WAYPOINTS[step])
            trail.setPoints(ArrayList(WAYPOINTS.take(step + 1)))
            map.controller.animateTo(WAYPOINTS[step])
            map.invalidate()
        }
        isArrived = true
    }

    LaunchedEffect(Unit) {
        while (etaSeconds > 0 && !isArrived) {
            delay(1000L)
            if (etaSeconds > 0) etaSeconds--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Doctor Tracking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                    )
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
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(TechTeal, ClinicalNavy)),
                ),
            )
        },
        containerColor = SoftSlate,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(WAYPOINTS[0])

                        val doctor = Marker(this).apply {
                            setPosition(WAYPOINTS[0])
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = doctorName
                            icon = context.getDrawable(android.R.drawable.ic_menu_directions)
                        }
                        overlays.add(doctor)
                        doctorMarker.value = doctor

                        val patient = Marker(this).apply {
                            setPosition(PATIENT_POSITION)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Patient"
                            icon = context.getDrawable(android.R.drawable.ic_menu_myplaces)
                        }
                        overlays.add(patient)

                        val polyline = Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor("#00D4B2")
                            outlinePaint.strokeWidth = 6f
                            setPoints(ArrayList(listOf(WAYPOINTS[0])))
                        }
                        overlays.add(polyline)
                        trailPolyline.value = polyline

                        mapView.value = this
                    }
                },
                update = {},
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = TechTeal,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArrived) "$doctorName has arrived"
                                else "Dr. $doctorName is on the way",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleBlack,
                            )
                            Text(
                                text = if (isArrived) "Please be ready for your consultation"
                                else buildEtaText(etaSeconds),
                                fontSize = 13.sp,
                                color = CoolGray,
                            )
                        }
                    }

                    if (!isArrived) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = currentStep.toFloat() / TOTAL_STEPS
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = TechTeal,
                            trackColor = TechTeal.copy(alpha = 0.15f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% complete",
                            fontSize = 11.sp,
                            color = CoolGray,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
        }
    }
}

private fun buildEtaText(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "Arriving in ${mins}:${secs.toString().padStart(2, '0')}"
}
