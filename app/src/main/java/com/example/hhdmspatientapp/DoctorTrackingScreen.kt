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
import android.view.View
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL

// French OSM mirror — same data, separate infrastructure, HTTPS/2
// supported, no registration required.  Main tile.openstreetmap.org is
// IP-blocking this network.
private val OSM_TILE_SOURCE = XYTileSource(
    "OSM-FR",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.tile.openstreetmap.fr/osmfr/",
        "https://b.tile.openstreetmap.fr/osmfr/",
        "https://c.tile.openstreetmap.fr/osmfr/",
    ),
    "© OpenStreetMap contributors",
    TileSourcePolicy(
        2,
        TileSourcePolicy.FLAG_NO_BULK
            or TileSourcePolicy.FLAG_NO_PREVENTIVE
            or TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL,
    ),
)

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

// Route each consecutive waypoint pair through OSRM and stitch the segments
// together. Segments are fetched concurrently so the worst case is a single
// segment's latency instead of the sum of all of them. Routing per-pair keeps
// the path on roads even if one segment fails. The whole thing runs on
// Dispatchers.IO so the blocking HTTP calls never touch the main thread (which
// would freeze the loading spinner / UI).
private suspend fun fetchRoadRoute(): List<GeoPoint> = withContext(Dispatchers.IO) {
    val segments = WAYPOINTS.zipWithNext().map { (a, b) -> async { fetchRoadSegment(a, b) } }
    val result = mutableListOf<GeoPoint>()
    segments.forEachIndexed { i, deferred ->
        val segment = deferred.await()
        if (segment != null && segment.size > 1) {
            if (result.isEmpty()) result.addAll(segment) else result.addAll(segment.drop(1))
        } else {
            Log.w("DoctorTracking", "OSRM failed for segment $i; using straight line")
            if (result.isEmpty()) result.add(WAYPOINTS[i])
            result.add(WAYPOINTS[i + 1])
        }
    }
    if (result.size < 2) WAYPOINTS else result
}

private fun fetchRoadSegment(
    start: GeoPoint,
    end: GeoPoint,
): List<GeoPoint>? {
    val lng1 = start.longitude
    val lat1 = start.latitude
    val lng2 = end.longitude
    val lat2 = end.latitude
    val url =
        URL("https://router.project-osrm.org/route/v1/driving/$lng1,$lat1;$lng2,$lat2?geometries=geojson&overview=full")
    var attempt = 0
    while (attempt < 3) {
        try {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                val routes = obj.getJSONArray("routes")
                if (routes.length() == 0) return null
                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coords = geometry.getJSONArray("coordinates")
                return (0 until coords.length()).map { i ->
                    val c = coords.getJSONArray(i)
                    GeoPoint(c.getDouble(1), c.getDouble(0))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            attempt++
            Log.w("DoctorTracking", "OSRM segment fetch attempt $attempt failed", e)
            if (attempt < 3) Thread.sleep(1000L * attempt)
        }
    }
    return null
}

// Convert a lat/lng to tile coordinates at a given zoom level.
private fun tileXY(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
    val n = 1 shl zoom
    val x = ((lng + 180.0) / 360.0 * n).toInt()
    val latRad = Math.toRadians(lat)
    val y =
        ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n).toInt()
    return x to y
}

// Queue downloads for every tile covering the route's bounding box, at several
// zoom levels. These are fetched in the background and written to osmdroid's
// persistent disk cache, so the map is fully painted before/while tracking and
// stays cached for future sessions.
private fun prefetchRouteTiles(provider: MapTileProviderBase, route: List<GeoPoint>) {
    if (route.size < 2) return
    val minLat = route.minOf { it.latitude }
    val maxLat = route.maxOf { it.latitude }
    val minLng = route.minOf { it.longitude }
    val maxLng = route.maxOf { it.longitude }
    for (zoom in 15..17) {
        val (x1, y1) = tileXY(maxLat, minLng, zoom)
        val (x2, y2) = tileXY(minLat, maxLng, zoom)
        for (x in x1..x2) {
            for (y in y1..y2) {
                try {
                    provider.getMapTile(MapTileIndex.getTileIndex(zoom, x, y))
                } catch (e: Exception) {
                    Log.w("DoctorTracking", "Prefetch tile failed at $zoom/$x/$y", e)
                }
            }
        }
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
    patientId: String = "",
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
        val road = fetchRoadRoute()
        Log.d("DoctorTracking", "OSRM route: ${road.size} points")
        routePoints = resampleRoute(road, START_ETA_SECONDS)

        // Wait for the map to attach, then pre-download every tile covering
        // the route so the whole path is painted before tracking starts.
        var map = mapView.value
        while (map == null) {
            delay(100L)
            map = mapView.value
        }
        withContext(Dispatchers.IO) {
            prefetchRouteTiles(map.tileProvider, routePoints)
        }

        isRouteLoading = false
    }

    LaunchedEffect(routePoints, isRouteLoading) {
        if (routePoints.size < 2 || isRouteLoading) return@LaunchedEffect
        val total = routePoints.size - 1

        // Wait until the map overlays are attached by the factory block
        var marker = doctorMarker.value
        var trail = trailPolyline.value
        while (marker == null || trail == null) {
            delay(100L)
            marker = doctorMarker.value
            trail = trailPolyline.value
        }

        for (step in 1..total) {
            delay(1000L)
            currentStep = step
            etaSeconds = total - step + 1
            marker.setPosition(routePoints[step])
            trail.setPoints(ArrayList(routePoints.take(step + 1)))
            mapView.value?.controller?.animateTo(routePoints[step], ANIMATION_ZOOM, 1000L)
            mapView.value?.invalidate()
        }
        etaSeconds = 0
        isArrived = true
    }

    LaunchedEffect(isArrived) {
        if (isArrived && patientId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    RetrofitClient.apiService.markArrived(patientId)
                    Log.d("DoctorTracking", "markArrived called for patient $patientId")
                } catch (e: Exception) {
                    Log.e("DoctorTracking", "Failed to mark arrived: ${e.message}")
                }
            }
            VisitStorage.clearVisitingPatientId()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(OSM_TILE_SOURCE)
                        setMultiTouchControls(true)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setTilesScaledToDpi(true)
                        controller.setZoom(ANIMATION_ZOOM)
                        controller.setCenter(WAYPOINTS[0])
                        overlays.add(CopyrightOverlay(ctx))

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
                                else "$doctorName is on the way",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleBlack,
                            )
                            Text(
                                text = if (isArrived) "Please be ready for your consultation"
                                else if (isRouteLoading) "Calculating route..."
                                else buildEtaText(etaSeconds),
                                fontSize = 13.sp,
                                color = CoolGray,
                            )
                        }
                    }

                    if (!isRouteLoading && !isArrived) {
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

            // Full-screen loading gate: the map and its tiles render behind
            // this overlay while the route is being calculated; once everything
            // is ready it disappears and the path tracing starts.
            if (isRouteLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SoftSlate),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TechTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading The Doctor Tracking Portal...",
                            fontSize = 15.sp,
                            color = CoolGray,
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
