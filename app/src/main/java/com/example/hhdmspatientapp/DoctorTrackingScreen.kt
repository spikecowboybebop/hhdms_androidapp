package com.example.hhdmspatientapp

import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL

private val WAYPOINTS = listOf(
    GeoPoint(23.7775, 90.4050),
    GeoPoint(23.7785, 90.4100),
    GeoPoint(23.7830, 90.4120),
    GeoPoint(23.7870, 90.4150),
    GeoPoint(23.7890, 90.4200),
    GeoPoint(23.7925, 90.4260),
)

private val PATIENT_POSITION = WAYPOINTS.last()
private const val START_ETA_SECONDS = 60
private const val TRAIL_COLOR = 0xFFEA2E7C
private const val ANIMATION_ZOOM = 17.0

private suspend fun fetchRoadRoute(
    start: GeoPoint,
    end: GeoPoint,
): List<GeoPoint>? = withContext(Dispatchers.IO) {
    try {
        val lng1 = start.longitude
        val lat1 = start.latitude
        val lng2 = end.longitude
        val lat2 = end.latitude
        val url =
            URL("https://router.project-osrm.org/route/v1/driving/$lng1,$lat1;$lng2,$lat2?geometries=geojson&overview=full")
        val text = url.readText()
        val obj = JSONObject(text)
        val routes = obj.getJSONArray("routes")
        if (routes.length() == 0) return@withContext null
        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
        val coords = geometry.getJSONArray("coordinates")
        (0 until coords.length()).map { i ->
            val c = coords.getJSONArray(i)
            GeoPoint(c.getDouble(1), c.getDouble(0))
        }
    } catch (e: Exception) {
        Log.w("DoctorTracking", "OSRM route fetch failed", e)
        null
    }
}

private fun resampleRoute(route: List<GeoPoint>, targetCount: Int): List<GeoPoint> {
    if (route.size < 2) return route
    if (route.size == targetCount) return route.toList()

    val cumulative = mutableListOf(0.0)
    for (i in 1 until route.size) {
        cumulative.add(cumulative.last() + route[i - 1].distanceToAsDouble(route[i]))
    }
    val total = cumulative.last()
    if (total <= 0.0) return route.toList()

    val result = mutableListOf(route.first())
    val segLen = total / (targetCount - 1)
    var seg = 0
    for (i in 1 until targetCount - 1) {
        val target = i * segLen
        while (seg < cumulative.size - 1 && cumulative[seg + 1] < target) seg++
        val frac = (target - cumulative[seg]) / (cumulative[seg + 1] - cumulative[seg])
        val lat = route[seg].latitude + (route[seg + 1].latitude - route[seg].latitude) * frac
        val lng = route[seg].longitude + (route[seg + 1].longitude - route[seg].longitude) * frac
        result.add(GeoPoint(lat, lng))
    }
    result.add(route.last())
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorTrackingScreen(
    doctorName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var currentStep by remember { mutableIntStateOf(0) }
    var etaSeconds by remember { mutableIntStateOf(START_ETA_SECONDS) }
    var isArrived by remember { mutableStateOf(false) }
    var isRouteLoading by remember { mutableStateOf(true) }
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val doctorMarker = remember { mutableStateOf<Marker?>(null) }
    val trailPolyline = remember { mutableStateOf<Polyline?>(null) }

    LaunchedEffect(Unit) {
        val road = fetchRoadRoute(WAYPOINTS.first(), WAYPOINTS.last())
        Log.d("DoctorTracking", "OSRM route: ${road?.size ?: "null"} points")
        val raw = road ?: WAYPOINTS
        routePoints = resampleRoute(raw, START_ETA_SECONDS)
        isRouteLoading = false
    }

    LaunchedEffect(routePoints) {
        if (routePoints.size < 2) return@LaunchedEffect
        val total = routePoints.size - 1

        // Wait until the map view and overlays are attached by the factory block
        var map = mapView.value
        var marker = doctorMarker.value
        var trail = trailPolyline.value
        while (map == null || marker == null || trail == null) {
            delay(100L)
            map = mapView.value
            marker = doctorMarker.value
            trail = trailPolyline.value
        }

        for (step in 1..total) {
            delay(1000L)
            currentStep = step
            marker.setPosition(routePoints[step])
            trail.setPoints(ArrayList(routePoints.take(step + 1)))
            map.controller.animateTo(routePoints[step], ANIMATION_ZOOM, 1000L)
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
            if (isRouteLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TechTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading route...", fontSize = 14.sp, color = CoolGray)
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(ANIMATION_ZOOM)
                            controller.setCenter(WAYPOINTS[0])

                            val doctor = Marker(this).apply {
                                setPosition(WAYPOINTS[0])
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = doctorName
                                icon = ContextCompat.getDrawable(ctx, R.drawable.ic_emergency)
                            }
                            overlays.add(doctor)
                            doctorMarker.value = doctor

                            val patient = Marker(this).apply {
                                setPosition(PATIENT_POSITION)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Patient"
                                icon = ContextCompat.getDrawable(ctx, R.drawable.ic_person)
                            }
                            overlays.add(patient)

                            val polyline = Polyline().apply {
                                outlinePaint.color = TRAIL_COLOR.toInt()
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
            }

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
                        val totalSteps = if (routePoints.size > 1) routePoints.size - 1 else 1
                        val progress = currentStep.toFloat() / totalSteps
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(TRAIL_COLOR),
                            trackColor = Color(TRAIL_COLOR).copy(alpha = 0.15f),
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
