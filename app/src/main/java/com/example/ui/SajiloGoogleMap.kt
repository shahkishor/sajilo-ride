package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.example.ui.theme.MyApplicationTheme

// Standard LatLng data holder so that we don't strictly require com.google.android.gms if class loading is isolated
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Composable
fun SajiloGoogleMap(
    viewModel: SajiloViewModel,
    modifier: Modifier = Modifier,
    pickupIdx: Int,
    dropIdx: Int,
    onPickupSelected: (Int) -> Unit,
    onDropSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val locations = viewModel.locations
    val currentPickup = locations[pickupIdx]
    val currentDrop = locations[dropIdx]

    // Local state for GPS coordinate display and search
    var mapsMode by remember { mutableStateOf("SIMULATOR") } // "SIMULATOR" or "GOOGLE_MAPS"
    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Real or Simulated Current User GPS Coordinate State
    val defaultKathmanduGPS = LatLng(27.7149, 85.3121) // Thamel coordinates
    var userCurrentGPS by remember { mutableStateOf(defaultKathmanduGPS) }
    var isSimulatingTracking by remember { mutableStateOf(true) }

    // Launcher for Location Permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        if (granted) {
            viewModel.isHillyTerrain.value = false // reset state
        }
    }

    // Try to derive the user's current physical position if permissions are granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        userCurrentGPS = LatLng(loc.latitude, loc.longitude)
                        isSimulatingTracking = false
                    }
                }
            } catch (e: SecurityException) {
                // Ignore security changes
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    // Setup active coordinates based on dropdown indexing
    val pickupLatLng = LatLng(currentPickup.latitude, currentPickup.longitude)
    val dropLatLng = LatLng(currentDrop.latitude, currentDrop.longitude)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sajilo_ride_booking_map"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Map header bar with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map Location",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Interactive GPS Navigator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1D1B20)
                    )
                }

                // Interactive Mode Switch tabs (Material 3 Segment Styling)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 19.dp, bottomStart = 19.dp))
                                .background(if (mapsMode == "SIMULATOR") Color(0xFFE8DEF8) else Color.Transparent)
                                .clickable { mapsMode = "SIMULATOR" }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Vector Sim",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mapsMode == "SIMULATOR") Color(0xFF21005D) else Color(0xFF49454F)
                            )
                        }
                        VerticalDivider(color = Color(0xFFCAC4D0))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 19.dp, bottomEnd = 19.dp))
                                .background(if (mapsMode == "GOOGLE_MAPS") Color(0xFFE8DEF8) else Color.Transparent)
                                .clickable { mapsMode = "GOOGLE_MAPS" }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Google Maps",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mapsMode == "GOOGLE_MAPS") Color(0xFF21005D) else Color(0xFF49454F)
                            )
                        }
                    }
                }
            }

            // City Quick Selectors Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeCity = currentPickup.region
                Text(
                    text = "Active City Map:",
                    fontSize = 10.5.sp,
                    color = Color(0xFF49454F),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
                )
                listOf("Kathmandu", "Pokhara", "Biratnagar").forEach { city ->
                    val isSelected = activeCity == city
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                        ),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)),
                        modifier = Modifier
                            .clickable {
                                when (city) {
                                    "Kathmandu" -> {
                                        onPickupSelected(0) // Thamel
                                        onDropSelected(2)   // Patan Durbar Square
                                    }
                                    "Pokhara" -> {
                                        onPickupSelected(6) // Sarangkot
                                        onDropSelected(7)   // Lakeside
                                    }
                                    "Biratnagar" -> {
                                        onPickupSelected(8) // Airport
                                        onDropSelected(9)   // Chowk
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = city,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Connection & Signal Simulation controls
            val isOnline by viewModel.isNetworkOnline.collectAsState()
            val isPoor by viewModel.isPoorSignal.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (!isOnline) Color(0xFFFFEBEE)
                        else if (isPoor) Color(0xFFFFF3E0)
                        else Color(0xFFE8F5E9)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Info State
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!isOnline) Icons.Default.Warning 
                                      else if (isPoor) Icons.Default.Warning 
                                      else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (!isOnline) Color(0xFFD32F2F) 
                               else if (isPoor) Color(0xFFF57C00) 
                               else Color(0xFF388E3C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (!isOnline) "Offline Mode (No Connection)"
                               else if (isPoor) "Offline Map Active (Poor Signal)"
                               else "Online (High-speed Connected)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isOnline) Color(0xFFD32F2F)
                                else if (isPoor) Color(0xFFF57C00)
                                else Color(0xFF388E3C)
                    )
                }

                // Interactive simulation actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Toggle simulated internet drop
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        modifier = Modifier.clickable { viewModel.simulateConnectionToggle() }
                    ) {
                        Text(
                            "Sim Offline",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    // Toggle simulated poor signal
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        modifier = Modifier.clickable { viewModel.simulatePoorSignalToggle() }
                    ) {
                        Text(
                            "Sim Poor Signal",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Inner Map Layout containing search interface and viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFEADDFF))
            ) {
                
                // Show real Google Maps or Simulated Landmarks
                if (mapsMode == "GOOGLE_MAPS") {
                    GoogleMapRenderer(
                        pickupLatLng = pickupLatLng,
                        dropLatLng = dropLatLng,
                        userCurrentGPS = userCurrentGPS,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Beautiful custom Kathmandu landmarks vector simulation
                    Column(modifier = Modifier.fillMaxSize()) {
                        MapCanvasContainer(
                            viewModel = viewModel,
                            pickupIdx = pickupIdx,
                            dropIdx = dropIdx
                        )
                    }
                }

                // OVERLAY: Search Destination Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    searchFocused = true
                                },
                                placeholder = { Text("Search location in Nepal...", fontSize = 12.sp, color = Color(0xFF49454F)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("map_search_field")
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF49454F),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            searchQuery = ""
                                            searchFocused = false
                                        }
                                )
                            }
                        }
                    }

                    // Floating Search results dropdown
                    AnimatedVisibility(
                        visible = searchFocused && searchQuery.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.8f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .heightIn(max = 140.dp)
                        ) {
                            val filtered = locations.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                Text(
                                    "No landmarks match in Pokhara/Kathmandu",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyColumn {
                                    itemsIndexed(filtered) { _, loc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val actualIdx = locations.indexOf(loc)
                                                    if (actualIdx != -1) {
                                                        // Auto set drop location as destination
                                                        onDropSelected(actualIdx)
                                                        searchQuery = loc.name
                                                        searchFocused = false
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFFB3261E),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(loc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(loc.description, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }

                // OVERLAY: Floating Actions (Permission Prompt, Center GPS, Sim tracking status)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (!locationPermissionGranted) {
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("request_gps_permission_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share My GPS", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Floating GPS lock status
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable {
                                // Simulate user centering map camera to current position
                                userCurrentGPS = LatLng(currentPickup.latitude, currentPickup.longitude)
                                isSimulatingTracking = true
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (locationPermissionGranted) Color(0xFF4CAF50) else Color(0xFFFFB300))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (locationPermissionGranted) "GPS Satellites Locked" else "MOCK GPS (Thamel Pin)",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Lower Status strip describing Coordinates, Elevations, and Land routes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pickup Lat/Long",
                        fontSize = 8.5.sp,
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.4f° N • %.4f° E", pickupLatLng.latitude, pickupLatLng.longitude),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(14.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Destination Lat/Long",
                        fontSize = 8.5.sp,
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.4f° N • %.4f° E", dropLatLng.latitude, dropLatLng.longitude),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }
            }
        }
    }
}

// MapView Interop Wrapper with AndroidView
@Composable
fun GoogleMapRenderer(
    pickupLatLng: LatLng,
    dropLatLng: LatLng,
    userCurrentGPS: LatLng,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Sync MapView lifecycle with Compose lifecycle
    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            } catch (e: Exception) {
                // Ignore lifecycle problems gracefully
            }
        }
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // Embed using AndroidView with fail-safe bounds mapping
    AndroidView(
        factory = { mapView },
        modifier = modifier.testTag("google_map_sdk_view")
    ) { view ->
        view.getMapAsync { googleMap ->
            try {
                googleMap.clear()
                
                // Enable default UI controls
                googleMap.uiSettings.isZoomControlsEnabled = true
                googleMap.uiSettings.isCompassEnabled = true

                // Add Current User Position marker (Mock or Real)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(userCurrentGPS)
                        .title("My Position")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                // Add Pickup station marker
                googleMap.addMarker(
                    MarkerOptions()
                        .position(pickupLatLng)
                        .title("Pickup Station")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                // Add Dropoff destination marker
                googleMap.addMarker(
                    MarkerOptions()
                        .position(dropLatLng)
                        .title("Ride Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // Draw connecting Polyline representing route
                val path = PolylineOptions()
                    .add(pickupLatLng, dropLatLng)
                    .color(android.graphics.Color.parseColor("#6750A4"))
                    .width(8f)
                googleMap.addPolyline(path)

                // Fit both marker bounds elegantly inside the Camera View
                val builder = LatLngBounds.Builder()
                builder.include(pickupLatLng)
                builder.include(dropLatLng)
                builder.include(userCurrentGPS)
                val bounds = builder.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                // Instantly fallback to standard zoom focus if bounds fail to calculate on initialization
                try {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 12f))
                } catch (ex: Exception) {
                    // Fail-safe
                }
            }
        }
    }
}

// Clean fallback local schematic vector map utilizing standard graphics canvas!
@Composable
fun MapCanvasContainer(
    viewModel: SajiloViewModel,
    pickupIdx: Int,
    dropIdx: Int
) {
    val pickup = viewModel.locations[pickupIdx]
    val drop = viewModel.locations[dropIdx]
    val currentRegion = pickup.region

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_canvas_simulator_fallback")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (currentRegion) {
                "Pokhara" -> {
                    // Massive snowy mountain peaks (Annapurna range)
                    val peakRange = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height * 0.35f)
                        lineTo(width * 0.15f, height * 0.12f)
                        lineTo(width * 0.3f, height * 0.25f)
                        lineTo(width * 0.5f, height * 0.08f)
                        lineTo(width * 0.65f, height * 0.24f)
                        lineTo(width * 0.8f, height * 0.14f)
                        lineTo(width, height * 0.32f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        peakRange,
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFFE0F7FA), Color(0xFF006064).copy(alpha = 0.1f))
                        )
                    )
                    
                    // Pokhara Phewa Lake schema
                    val lake = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.15f, height * 0.65f)
                        quadraticToTo(width * 0.5f, height * 0.5f, width * 0.85f, height * 0.72f)
                        quadraticToTo(width * 0.5f, height * 0.9f, width * 0.15f, height * 0.65f)
                    }
                    drawPath(lake, Color(0xFF00E5FF).copy(alpha = 0.35f))
                    
                    // Simple local mountain highway
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(10f, height * 0.5f),
                        end = androidx.compose.ui.geometry.Offset(width - 10f, height * 0.8f),
                        strokeWidth = 8f
                    )
                    drawLine(
                        color = Color(0xFF0097A7),
                        start = androidx.compose.ui.geometry.Offset(10f, height * 0.5f),
                        end = androidx.compose.ui.geometry.Offset(width - 10f, height * 0.8f),
                        strokeWidth = 4f
                    )
                }
                "Biratnagar" -> {
                    // Terai flat plains background color
                    drawRect(
                        color = Color(0xFFE8F5E9),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(width, height)
                    )
                    
                    // Koshi River pathway
                    val koshiRiver = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.45f, 0f)
                        lineTo(width * 0.48f, height * 0.3f)
                        lineTo(width * 0.2f, height * 0.65f)
                        lineTo(width * 0.25f, height)
                    }
                    drawPath(koshiRiver, Color(0xFF0288D1).copy(alpha = 0.4f), style = Stroke(width = 24f))
                    
                    // Terai field agricultural grid mapping
                    for (i in 0..5) {
                        val spacing = height * 0.15f
                        drawLine(
                            color = Color(0xAAD1E7DD),
                            start = androidx.compose.ui.geometry.Offset(0f, height * 0.2f + i * spacing),
                            end = androidx.compose.ui.geometry.Offset(width, height * 0.15f + i * spacing),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xAAD1E7DD),
                            start = androidx.compose.ui.geometry.Offset(width * 0.1f + i * width * 0.18f, 0f),
                            end = androidx.compose.ui.geometry.Offset(width * 0.05f + i * width * 0.18f, height),
                            strokeWidth = 3f
                        )
                    }
                    
                    // Koshi Highway route
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(width * 0.7f, 10f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.72f, height - 10f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = androidx.compose.ui.geometry.Offset(width * 0.7f, 10f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.72f, height - 10f),
                        strokeWidth = 5f
                    )
                }
                else -> {
                    // Signature Kathmandu green backdrop valley hills
                    val hill1 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height * 0.45f)
                        quadraticToTo(width * 0.25f, height * 0.25f, width * 0.5f, height * 0.40f)
                        quadraticToTo(width * 0.75f, height * 0.15f, width, height * 0.35f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        hill1,
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFFC8E6C9).copy(alpha = 0.5f), Color(0xFFE8F5E9).copy(alpha = 0.1f))
                        )
                    )

                    // Deep blue curving Bagmati River schema
                    val river = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.75f, 0f)
                        cubicTo(width * 0.65f, height * 0.3f, width * 0.45f, height * 0.5f, width * 0.55f, height * 0.7f)
                        cubicTo(width * 0.65f, height * 0.85f, width * 0.35f, height * 0.9f, width * 0.25f, height)
                    }
                    drawPath(river, Color(0xFFBBDEFB), style = Stroke(width = 12f))

                    // Ring roads layout structure grid
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(20f, height * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(width - 20f, height * 0.75f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color(0xFFD1C4E9),
                        start = androidx.compose.ui.geometry.Offset(20f, height * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(width - 20f, height * 0.75f),
                        strokeWidth = 5f
                    )

                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(width * 0.3f, 10f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.35f, height - 10f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color(0xFFD1C4E9),
                        start = androidx.compose.ui.geometry.Offset(width * 0.3f, 10f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.35f, height - 10f),
                        strokeWidth = 5f
                    )
                }
            }

            // Coordinate scale translators mapping logically
            val scaleX = width / 500f
            val scaleY = height / 400f

            val px = pickup.x * scaleX
            val py = pickup.y * scaleY
            val dxCoord = drop.x * scaleX
            val dyCoord = drop.y * scaleY

            // Active path route trajectory representation line
            drawLine(
                color = Color(0xFF6750A4),
                start = androidx.compose.ui.geometry.Offset(px, py),
                end = androidx.compose.ui.geometry.Offset(dxCoord, dyCoord),
                strokeWidth = 6f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )

            // Render Pickup Pin (Pulsing green indicator bounds)
            drawCircle(Color(0xFF4CAF50).copy(alpha = 0.3f), radius = 24f, center = androidx.compose.ui.geometry.Offset(px, py))
            drawCircle(Color(0xFF4CAF50), radius = 8f, center = androidx.compose.ui.geometry.Offset(px, py))
            drawCircle(Color.White, radius = 3.5f, center = androidx.compose.ui.geometry.Offset(px, py))

            // Render Dropoff Pin (Pulsing red indicator bounds)
            drawCircle(Color(0xFFB3261E).copy(alpha = 0.3f), radius = 24f, center = androidx.compose.ui.geometry.Offset(dxCoord, dyCoord))
            drawCircle(Color(0xFFB3261E), radius = 8f, center = androidx.compose.ui.geometry.Offset(dxCoord, dyCoord))
            drawCircle(Color.White, radius = 3.5f, center = androidx.compose.ui.geometry.Offset(dxCoord, dyCoord))
        }

        // Overlay text descriptions for Landmarks directly on fallback canvas
        val locationsList = viewModel.locations
        locationsList.forEach { loc ->
            val transX = loc.x * (400f / 500f) // Normalized estimate positions
            val transY = loc.y * (180f / 400f)

            // Only show labels belonging to the SAME active region!
            if (loc.region == currentRegion && loc.name != pickup.name && loc.name != drop.name) {
                Box(
                    modifier = Modifier
                        .offset(x = transX.dp, y = transY.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = loc.name,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }
    }
}
