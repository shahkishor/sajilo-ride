package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DbDriverOnboarding
import com.example.data.DbRide
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun SajiloDashboard(viewModel: SajiloViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Display localized toasts elegantly
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val isNepali = userProfile?.selectedLanguage == "ne"

    // Localization Map
    val t = remember(isNepali) {
        if (isNepali) NepaliLabels
        else EnglishLabels
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activeRide == null || activeRide?.status == "COMPLETED" || activeRide?.status == "CANCELLED") {
                SajiloBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) },
                    labels = t
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header is present everywhere except active trip progress (leaves full map presence)
            if (currentScreen != "TRIP_PROGRESS") {
                SajiloHeader(
                    userProfile = userProfile,
                    onLangToggle = { viewModel.toggleLanguage() },
                    onDataSavingsToggle = { viewModel.toggleDataSavings() },
                    labels = t,
                    isNe = isNepali,
                    notifications = viewModel.activeNotifications.collectAsStateWithLifecycle().value,
                    onClearNotification = { viewModel.clearNotifications() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentScreen) {
                    "BOOK" -> {
                        BookingView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                    "TRIP_PROGRESS" -> {
                        TripProgressView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                    "HISTORY" -> {
                        HistoryView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                    "ADMIN" -> {
                        AdminView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                    "ONBOARD_FORM" -> {
                        OnboardingFormView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                    "TICKET_FORM" -> {
                        TicketFormView(viewModel = viewModel, labels = t, isNe = isNepali)
                    }
                }
            }
        }
    }
}

// Bottom Navigation Bar
@Composable
fun SajiloBottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    labels: Map<String, String>
) {
    NavigationBar(
        tonalElevation = 0.dp,
        containerColor = Color(0xFFF3EDF7),
        modifier = Modifier
            .height(80.dp)
            .testTag("sajilo_bottom_nav")
            .drawBehind {
                drawLine(
                    color = Color(0xFFCAC4D0),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        val navColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF1D1B20),
            selectedTextColor = Color(0xFF1D1B20),
            unselectedIconColor = Color(0xFF49454F),
            unselectedTextColor = Color(0xFF49454F),
            indicatorColor = Color(0xFFE8DEF8)
        )

        NavigationBarItem(
            selected = currentScreen == "BOOK" || currentScreen == "ONBOARD_FORM" || currentScreen == "TICKET_FORM",
            onClick = { onNavigate("BOOK") },
            icon = { Icon(Icons.Default.Home, contentDescription = labels["home"]) },
            label = { Text(labels["home"] ?: "Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navColors,
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = currentScreen == "HISTORY",
            onClick = { onNavigate("HISTORY") },
            icon = { Icon(Icons.Default.Refresh, contentDescription = labels["history"]) },
            label = { Text(labels["history"] ?: "History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navColors,
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = currentScreen == "ADMIN",
            onClick = { onNavigate("ADMIN") },
            icon = { Icon(Icons.Default.Settings, contentDescription = labels["admin"]) },
            label = { Text(labels["admin"] ?: "Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navColors,
            modifier = Modifier.testTag("nav_admin")
        )
    }
}

// App Header with profile status, notification counter, language toggles
@Composable
fun SajiloHeader(
    userProfile: com.example.data.DbUserProfile?,
    onLangToggle: () -> Unit,
    onDataSavingsToggle: () -> Unit,
    labels: Map<String, String>,
    isNe: Boolean,
    notifications: List<String>,
    onClearNotification: () -> Unit
) {
    var showNotifDropdown by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sajilo_header")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left avatar + Greeting
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF))
                            .border(1.dp, Color(0xFFD0BCFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile?.name?.take(2)?.uppercase() ?: "RS",
                            color = Color(0xFF21005D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = (labels["namaste"] ?: "NAMASTE").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = userProfile?.name ?: "Rider",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${userProfile?.rating ?: 4.8} ★ (Rider ID: ${userProfile?.id ?: "rider_01"})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Header Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Data savings indicator
                    IconButton(
                        onClick = onDataSavingsToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("data_savings_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Data Savings Mode",
                            tint = if (userProfile?.isDataSavingsMode == true) Color(0xFF10B981) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Notifications bell badge
                    Box {
                        IconButton(
                            onClick = { showNotifDropdown = !showNotifDropdown },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("notification_bell")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            if (notifications.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }

                        // Notifications drop down
                        DropdownMenu(
                            expanded = showNotifDropdown,
                            onDismissRequest = { showNotifDropdown = false },
                            modifier = Modifier
                                .width(280.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = labels["system_status"] ?: "Sajilo Logs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (notifications.isNotEmpty()) {
                                    Text(
                                        text = if (isNe) "सफा गर्नुहोस्" else "Clear All",
                                        fontSize = 11.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { onClearNotification() }
                                            .testTag("clear_notifications_btn")
                                    )
                                }
                            }
                            HorizontalDivider()
                            if (notifications.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isNe) "कुनै सन्देश छैन" else "No new travel alerts.",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    onClick = { showNotifDropdown = false }
                                )
                            } else {
                                notifications.forEach { alert ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = "• $alert", fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        },
                                        onClick = { showNotifDropdown = false }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Bilingual toggle button
                    Button(
                        onClick = onLangToggle,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("language_toggle_btn")
                    ) {
                        Text(
                            text = if (isNe) "English" else "नेपाली",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Wallet view shortcuts
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF21005D),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // eSewa
                WalletBadge(
                    name = "eSewa",
                    logoChar = "eS",
                    color = Color(0xFF60BB46),
                    balance = userProfile?.balanceEseva ?: 0.0
                )
                // Khalti
                WalletBadge(
                    name = "Khalti",
                    logoChar = "Kh",
                    color = Color(0xFF5C2D91),
                    balance = userProfile?.balanceKhalti ?: 0.0
                )
                // IME Pay
                WalletBadge(
                    name = "IME Pay",
                    logoChar = "IP",
                    color = Color(0xFFEA1D24),
                    balance = userProfile?.balanceImePay ?: 0.0
                )
            }
        }
    }
}

@Composable
fun WalletBadge(
    name: String,
    logoChar: String,
    color: Color,
    balance: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(logoChar, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(name, fontSize = 10.sp, color = Color(0xFFE6E1E5), fontWeight = FontWeight.Medium)
            Text("Rs. ${balance.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// Main Booking View
@Composable
fun BookingView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    val pickupIdx by viewModel.selectedPickupIndex.collectAsStateWithLifecycle()
    val dropIdx by viewModel.selectedDropIndex.collectAsStateWithLifecycle()
    val category by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val payment by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val isHilly by viewModel.isHillyTerrain.collectAsStateWithLifecycle()
    val isSurge by viewModel.hasFuelSurge.collectAsStateWithLifecycle()
    val isScheduled by viewModel.isPreScheduled.collectAsStateWithLifecycle()
    val scheduledTime by viewModel.scheduledTimeText.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val fromLoc = viewModel.locations[pickupIdx]
    val toLoc = viewModel.locations[dropIdx]

    // Distance computation (mocked geometry)
    val dx = toLoc.x - fromLoc.x
    val dy = toLoc.y - fromLoc.y
    val distanceKm = remember(pickupIdx, dropIdx) {
        val raw = Math.sqrt((dx * dx + dy * dy).toDouble()) / 40.0
        Math.round(raw * 10.0) / 10.0
    }

    val durationMinutes = remember(distanceKm) {
        Math.max(5, (distanceKm * 2.5).toInt())
    }

    // Auto trigger hilly terrain surcharge if either pickup/destination is designated hilly (e.g. Sarangkot Pokhara)
    LaunchedEffect(pickupIdx, dropIdx) {
        if (fromLoc.isHilly || toLoc.isHilly) {
            viewModel.isHillyTerrain.value = true
        }
    }

    val finalFare = viewModel.calculateRideFare(distanceKm, category, isHilly, isSurge)

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDropDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("booking_view")
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Sleek Interactive Map Display (integrates Google Maps and Fallback Vector Simulator)
            SajiloGoogleMap(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                pickupIdx = pickupIdx,
                dropIdx = dropIdx,
                onPickupSelected = { viewModel.selectedPickupIndex.value = it },
                onDropSelected = { viewModel.selectedDropIndex.value = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main routing details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = labels["title_request_ride"] ?: "Instant Booking Interface",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Pickup Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(labels["lbl_pickup"] ?: "Pickup Location", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                                .clickable { showPickupDropdown = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = "Pickup", tint = Color(0xFF6750A4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(fromLoc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(fromLoc.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showPickupDropdown,
                            onDismissRequest = { showPickupDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            viewModel.locations.forEachIndexed { idx, loc ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(loc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(loc.description, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectedPickupIndex.value = idx
                                        showPickupDropdown = false
                                    },
                                    modifier = Modifier.testTag("pickup_item_$idx")
                                )
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropoff Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(labels["lbl_destination"] ?: "Dropoff Location", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                                .clickable { showDropDropdown = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Dropoff", tint = Color(0xFFB3261E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(toLoc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(toLoc.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showDropDropdown,
                            onDismissRequest = { showDropDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            viewModel.locations.forEachIndexed { idx, loc ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(loc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(loc.description, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectedDropIndex.value = idx
                                        showDropDropdown = false
                                    },
                                    modifier = Modifier.testTag("drop_item_$idx")
                                )
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-options: Pre-schedule, fuel surge, hills modifier
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Nepal Regional Adaptations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Early Morning / Pre-Scheduled rides toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Face, contentDescription = "Pre-Schedule", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(labels["opt_pre_schedule"] ?: "Pre-schedule Ride", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Early morning Kathmandu-Pokhara intercity or local", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = isScheduled,
                            onCheckedChange = { viewModel.isPreScheduled.value = it },
                            modifier = Modifier.scale(0.85f).testTag("pre_schedule_switch")
                        )
                    }

                    if (isScheduled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mock Pick-Up Schedule Time:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(scheduledTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "[Change]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Blue,
                                    modifier = Modifier.clickable {
                                        val nextTimes = listOf("05:30 AM", "06:00 AM", "06:30 AM", "07:30 AM", "08:00 AM")
                                        val curIdx = nextTimes.indexOf(scheduledTime)
                                        viewModel.scheduledTimeText.value = nextTimes[(curIdx + 1) % nextTimes.size]
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Hilly Multiplier Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Hilly", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val terrainLabel = labels["opt_hilly"] ?: "Hilly Surcharge (+45%)"
                                Text(terrainLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Applies heavy consumption factor for high inclines", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = isHilly,
                            onCheckedChange = { viewModel.isHillyTerrain.value = it },
                            modifier = Modifier.scale(0.85f).testTag("hilly_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Fuel SURGE Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Surge", tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val fuelLabel = labels["opt_fuel_surge"] ?: "Fuel Volatility Adjust (+20%)"
                                Text(fuelLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Surge rates during local pump shortages", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = isSurge,
                            onCheckedChange = { viewModel.hasFuelSurge.value = it },
                            modifier = Modifier.scale(0.85f).testTag("fuel_surge_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ride Category Selection Cards
            Text(
                labels["header_select_ride"] ?: "Select Sajilo Travel Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    CategoryInfo("BIKE", "🛵 " + (labels["cat_bike"] ?: "Bike"), "Fastest, beat jam", 1.0),
                    CategoryInfo("CAR", "🚗 " + (labels["cat_car"] ?: "Car/Taxi"), "Classic convenience", 2.8),
                    CategoryInfo("SUV", "🚙 " + (labels["cat_suv"] ?: "SUV"), "Hilly offroad groups", 4.5),
                    CategoryInfo("SHARED", "🚌 " + (labels["cat_shared"] ?: "Shared"), "Data savings pool", 0.7)
                ).forEach { catInfo ->
                    val isSelected = category == catInfo.id
                    val estNpr = viewModel.calculateRideFare(distanceKm, catInfo.id, isHilly, isSurge)

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectedCategory.value = catInfo.id }
                            .testTag("category_card_${catInfo.id.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                catInfo.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20)
                            )
                            Text(
                                "NPR ${estNpr.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color(0xFF21005D) else Color(0xFF6750A4),
                                fontSize = 14.sp
                            )
                            Text(
                                catInfo.desc,
                                fontSize = 8.5.sp,
                                color = if (isSelected) Color(0xFF21005D).copy(alpha = 0.7f) else Color(0xFF49454F),
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Option Grid
            Text(
                labels["payment_method"] ?: "Payment Channel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PaymentInfo("CASH", labels["pay_cash"] ?: "Cash Pay", Color.DarkGray),
                    PaymentInfo("ESEWA", "eSewa Pay", Color(0xFF60BB46)),
                    PaymentInfo("KHALTI", "Khalti Pay", Color(0xFF5C2D91)),
                    PaymentInfo("IMEPAY", "IME Pay", Color(0xFFEA1D24))
                ).forEach { payItem ->
                    val isSelected = payment == payItem.id
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) payItem.color else Color(0xFFCAC4D0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.paymentMethod.value = payItem.id }
                            .testTag("pay_btn_${payItem.id.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) payItem.color.copy(alpha = 0.12f) else Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = payItem.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) payItem.color else MaterialTheme.colorScheme.onSurface
                                )
                                if (payItem.id != "CASH") {
                                    Text(
                                        text = "Online Gate",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Summary Panel & REQUEST TRIGGER BUTTON
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${labels["lbl_distance"]}: $distanceKm km • ${labels["lbl_duration"]}: $durationMinutes min",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "NPR ${finalFare.toInt()} " + (labels["total_fare"] ?: "Receipt Total"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { viewModel.requestInstantRide() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("request_ride_confirm_btn")
                        ) {
                            Text(labels["btn_request_ride"] ?: "Book Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Links to Support Tickets OR driver onboarding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "📝 " + (labels["driver_onboard"] ?: "Register Bike/Car"),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo("ONBOARD_FORM") }
                        .testTag("driver_register_link")
                )
                Text(
                    text = "💬 " + (labels["customer_support"] ?: "Support Tickets"),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo("TICKET_FORM") }
                        .testTag("support_link")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Map drawing vector simulator displaying landmarks of Kathmandu!
@Composable
fun MapCanvas(
    viewModel: SajiloViewModel,
    progressFraction: Float,
    isHilly: Boolean,
    isLowBandwidth: Boolean,
    pickupIdx: Int,
    dropIdx: Int
) {
    val pickup = viewModel.locations[pickupIdx]
    val drop = viewModel.locations[dropIdx]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFFE8F0FE))
            .border(1.dp, Color.LightGray)
            .testTag("map_canvas_container")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("vector_map")) {
            val width = size.width
            val height = size.height

            if (!isLowBandwidth) {
                // 1. Draw signature green hills at the edges of Kathmandu Valley!
                val hill1 = Path().apply {
                    moveTo(0f, height * 0.45f)
                    quadraticToTo(width * 0.25f, height * 0.25f, width * 0.5f, height * 0.40f)
                    quadraticToTo(width * 0.75f, height * 0.15f, width, height * 0.35f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(hill1, Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFFC8E6C9))))

                // Draw high hills if Hilly option is compiled-in!
                if (isHilly) {
                    val highHill = Path().apply {
                        moveTo(0f, height * 0.25f)
                        quadraticToTo(width * 0.3f, height * 0.05f, width * 0.6f, height * 0.20f)
                        quadraticToTo(width * 0.75f, height * 0.02f, width, height * 0.18f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(highHill, Brush.verticalGradient(listOf(Color(0xFF4CAF50).copy(alpha = 0.5f), Color(0xFF81C784).copy(alpha = 0.1f))))
                }

                // 2. Beautiful blue curving Bagmati River flowing downwards across the valley!
                val river = Path().apply {
                    moveTo(width * 0.7f, 0f)
                    cubicTo(width * 0.6f, height * 0.3f, width * 0.4f, height * 0.5f, width * 0.5f, height * 0.7f)
                    cubicTo(width * 0.6f, height * 0.85f, width * 0.3f, height * 0.9f, width * 0.2f, height)
                }
                drawPath(river, Color(0xFF90CAF9), style = Stroke(width = 18f))

                // 3. Grid road network lanes of Kathmandu Ringroad
                // Major bypass roads
                drawLine(
                    color = Color.White,
                    start = Offset(20f, height * 0.3f),
                    end = Offset(width - 20f, height * 0.7f),
                    strokeWidth = 14f
                )
                drawLine(
                    color = Color.LightGray,
                    start = Offset(20f, height * 0.3f),
                    end = Offset(width - 20f, height * 0.7f),
                    strokeWidth = 10f
                )

                drawLine(
                    color = Color.White,
                    start = Offset(width * 0.2f, 10f),
                    end = Offset(width * 0.4f, height - 10f),
                    strokeWidth = 14f
                )
                drawLine(
                    color = Color.LightGray,
                    start = Offset(width * 0.2f, 10f),
                    end = Offset(width * 0.4f, height - 10f),
                    strokeWidth = 10f
                )
            } else {
                // Low bandwidth high-contrast minimalistic layout
                val grayGrid = Path().apply {
                    moveTo(width * 0.1f, 0f)
                    lineTo(width * 0.1f, height)
                    moveTo(width * 0.5f, 0f)
                    lineTo(width * 0.5f, height)
                    moveTo(0f, height * 0.3f)
                    lineTo(width, height * 0.3f)
                    moveTo(0f, height * 0.7f)
                    lineTo(width, height * 0.7f)
                }
                drawPath(grayGrid, Color.LightGray, style = Stroke(width = 2f))
            }

            // Draw landmark indicators schematically using circles/rects
            // Dharahara Tower at (180, 220)
            drawCircle(Color(0xFFE0E0E0), radius = 10f, center = Offset(180f, 220f))
            drawCircle(Color.White, radius = 4f, center = Offset(180f, 220f))

            // Boudha Stupa at (320, 110)
            drawCircle(Color(0xFFFFF59D), radius = 12f, center = Offset(320f, 110f))
            drawCircle(Color(0xFFD4AF37), radius = 5f, center = Offset(320f, 110f))

            // 4. Coordinates of active pickup and drop positions!
            // Map the logical coordinate percentages to physical width/height
            val scaleX = width / 500f
            val scaleY = height / 400f

            val px = pickup.x * scaleX
            val py = pickup.y * scaleY
            val dxCoord = drop.x * scaleX
            val dyCoord = drop.y * scaleY

            // Draw polyline path representing ride journey!
            val journeyPath = Path().apply {
                moveTo(px, py)
                quadraticToTo((px + dxCoord) / 2f + 40f, (py + dyCoord) / 2f - 40f, dxCoord, dyCoord)
            }
            drawPath(
                journeyPath,
                color = Color(0xFFDC143C).copy(alpha = 0.6f),
                style = Stroke(width = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
            )

            // Dynamic moving car/bike interpolating along the curve!
            // Using a quadratic Bezier interpolation formula: B(t) = (1-t)^2*P0 + 2(1-t)t*P1 + t^2*P2
            val tFactor = progressFraction
            val controlX = (px + dxCoord) / 2f + 40f
            val controlY = (py + dyCoord) / 2f - 40f

            val vehicleX = (1 - tFactor) * (1 - tFactor) * px + 2 * (1 - tFactor) * tFactor * controlX + tFactor * tFactor * dxCoord
            val vehicleY = (1 - tFactor) * (1 - tFactor) * py + 2 * (1 - tFactor) * tFactor * controlY + tFactor * tFactor * dyCoord

            // Draw green outline glow for active vehicle!
            drawCircle(Color(0xFF4CAF50), radius = 14f, center = Offset(vehicleX, vehicleY))
            drawCircle(Color.White, radius = 10f, center = Offset(vehicleX, vehicleY))
            drawCircle(Color(0xFF1E3A8A), radius = 6f, center = Offset(vehicleX, vehicleY))

            // Pickup pin (Green)
            drawCircle(Color(0xFF60BB46), radius = 10f, center = Offset(px, py))
            drawCircle(Color.White, radius = 5f, center = Offset(px, py))

            // Destination pin (Red)
            drawCircle(Color(0xFFEA1D24), radius = 10f, center = Offset(dxCoord, dyCoord))
            drawCircle(Color.White, radius = 5f, center = Offset(dxCoord, dyCoord))
        }

        // Mini floating annotations on the map
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color.Gray, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (isLowBandwidth) "Offline Minimalist Map" else "Live GPS: Bagmati Grid",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            val progressPercent = (progressFraction * 100).toInt()
            Text(
                text = "Progress: $progressPercent%",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

// Active Trip Progress View with Live Map, Driver details, call/chats, slide to cancel
@Composable
fun TripProgressView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
    val progressFraction by viewModel.tripProgressFraction.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val chats by viewModel.activeChatMessages.collectAsStateWithLifecycle()

    val pIdx by viewModel.selectedPickupIndex.collectAsStateWithLifecycle()
    val dIdx by viewModel.selectedDropIndex.collectAsStateWithLifecycle()

    val ride = activeRide ?: return

    val statusMsg = when (ride.status) {
        "REQUESTED" -> if (isNe) "राइड अनुरोध गरिँदैछ..." else "Waiting for closest Match..."
        "ACCEPTED" -> if (isNe) "चालक आउँदै हुनुहुन्छ" else "Driver is approaching..."
        "IN_PROGRESS" -> if (isNe) "सवारी गुडिरहेको छ" else "In Transit to destination..."
        "COMPLETED" -> if (isNe) "सवारी सम्पन्न भयो!" else "Trip Completed successfully!"
        "CANCELLED" -> if (isNe) "रद्द गरिएको छ!" else "Cancelled!"
        else -> ride.status
    }

    var chatTextInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("trip_progress_view")
    ) {
        // 1. Live Interactive Map Rendering!
        MapCanvas(
            viewModel = viewModel,
            progressFraction = progressFraction,
            isHilly = ride.isHillyTerrain,
            isLowBandwidth = userProfile?.isDataSavingsMode ?: false,
            pickupIdx = pIdx,
            dropIdx = dIdx
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Trip HUD
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (ride.status) {
                        "COMPLETED" -> Color(0xFFE8F5E9)
                        "CANCELLED" -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = labels["lbl_status"] ?: "Current Telemetry Status", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(text = statusMsg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val progressPercent = (progressFraction * 100).toInt()
                        Text("ETA: ${Math.max(1, ride.durationMinutes - (progressFraction * ride.durationMinutes).toInt())} min", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Driver Card Profile Info
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Driver Photo placeholder
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ride.driverName.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ride.driverName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("⭐ ${ride.driverRating} • ID Verified Rider Safety", fontSize = 11.sp, color = Color.Gray)
                            // Vehicle Plate Number
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDC143C), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(ride.driverVehiclePlate, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (ride.category == "BIKE") labels["cat_bike"] ?: "Motorbike" else labels["cat_car"] ?: "Car/SUV",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Local Call trigger
                        IconButton(
                            onClick = { viewModel.addNotification("Mocking local secure phone bridge to ${ride.driverPhone} via Sajilo VoIP") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pickup: ${ride.pickup}", fontSize = 11.sp, color = Color.DarkGray)
                        Text("Drop: ${ride.destination}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Driver-Rider Live chat messenger simulation box!
            Text(
                labels["chat_driver"] ?: "Bilingual Live Chat Messenger",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Message area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        if (chats.isEmpty()) {
                            Text(
                                "No chats exchanged yet. Text driver...",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(chats) { msg ->
                                    val isRider = msg.sender == "RIDER"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = if (isRider) Arrangement.End else Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 10.dp,
                                                        topEnd = 10.dp,
                                                        bottomStart = if (isRider) 10.dp else 0.dp,
                                                        bottomEnd = if (isRider) 0.dp else 10.dp
                                                    )
                                                )
                                                .background(if (isRider) Color(0xFF1E3A8A) else Color(0xFFF3F4F6))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = msg.messageText,
                                                fontSize = 11.sp,
                                                color = if (isRider) Color.White else Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TextField Input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatTextInput,
                            onValueChange = { chatTextInput = it },
                            placeholder = { Text("Ask: Where are you? (कता हुनुहुन्छ?)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("chat_input_text")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (chatTextInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        viewModel.sendRiderChat(chatTextInput)
                                        chatTextInput = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("chat_send_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Cancel or Return
            if (ride.status == "REQUESTED" || ride.status == "ACCEPTED") {
                Button(
                    onClick = { viewModel.cancelActiveRide() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_ride_btn")
                ) {
                    Text(labels["btn_cancel_trip"] ?: "Cancel Ride (Minimal Dispute Match)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (ride.status == "COMPLETED" || ride.status == "CANCELLED") {
                Button(
                    onClick = { viewModel.resetActiveTripToBook() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("return_home_btn")
                ) {
                    Text(if (isNe) "मुख्य स्क्रीनमा फर्कनुहोस्" else "Book Another Ride", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Room-backed Historic sessions + online payment gateways mock reloads
@Composable
fun HistoryView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    val rides by viewModel.allRides.collectAsStateWithLifecycle()
    val rCount = rides.size

    var reloadAmountInput by remember { mutableStateOf("500") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("history_view")
    ) {
        // WALLET RECHARGE CENTER
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💸 Nepal Digital Gateways (Simulated Recharge)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Refill mock wallets directly via connected Sandbox API layers.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = reloadAmountInput,
                        onValueChange = { reloadAmountInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Amount (NPR)") },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("refill_amount_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row {
                        Button(
                            onClick = {
                                val amt = reloadAmountInput.toDoubleOrNull() ?: 500.0
                                viewModel.reloadWallet("ESEWA", amt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60BB46)),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("refill_esewa")
                        ) {
                            Text("eSewa", fontSize = 11.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                val amt = reloadAmountInput.toDoubleOrNull() ?: 500.0
                                viewModel.reloadWallet("KHALTI", amt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C2D91)),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("refill_khalti")
                        ) {
                            Text("Khalti", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // HISTORIC LIST
        Text(
            text = "${labels["history"]} ($rCount rides saved in Room)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (rides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No past rides registered in SQLite database.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            rides.forEach { r ->
                val dateStr = remember(r.timestamp) {
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                    sdf.format(Date(r.timestamp))
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (r.category == "BIKE") "🛵" else "🚗",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${r.pickup} → ${r.destination}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "NPR ${r.fareNpr.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (r.status) {
                                                "COMPLETED" -> Color(0xFFC8E6C9)
                                                "CANCELLED" -> Color(0xFFFFCDD2)
                                                else -> Color(0xFFFFF9C4)
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        r.status,
                                        fontSize = 8.sp,
                                        color = when (r.status) {
                                            "COMPLETED" -> Color(0xFF2E7D32)
                                            "CANCELLED" -> Color(0xFFC62828)
                                            else -> Color(0xFFF57F17)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (r.isHillyTerrain || r.hasFuelSurge) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (r.isHillyTerrain) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("🏔️ Hilly Road Adjust (+45%)", fontSize = 8.sp, color = Color(0xFF1565C0))
                                    }
                                }
                                if (r.hasFuelSurge) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("⛽ Fuel Shortage Surge (+20%)", fontSize = 8.sp, color = Color(0xFFE65100))
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

// Driver Form Registration / Document Onboarding Simulation
@Composable
fun OnboardingFormView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    var name by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var citizenship by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("driver_onboard_form")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("BOOK") }, modifier = Modifier.testTag("form_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = labels["driver_onboard"] ?: "Driver Document Registration Hub",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Apply as an authorized Sajilo operator. All fields save locally to Room SQLite.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name (e.g. Ramesh Giri)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("onboard_name")
        )
        OutlinedTextField(
            value = license,
            onValueChange = { license = it },
            label = { Text("Driving License Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("onboard_license")
        )
        OutlinedTextField(
            value = citizenship,
            onValueChange = { citizenship = it },
            label = { Text("Citizenship card reference") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("onboard_citizenship")
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Vehicle details (e.g. Yamaha FZ)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("onboard_vehicle_model")
        )
        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it },
            label = { Text("License Plate (e.g. Ba 3 Pa 9081)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("onboard_plate")
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && license.isNotBlank()) {
                    viewModel.submitOnboarding(name, license, citizenship, model, plate)
                } else {
                    viewModel.reloadWallet("ESEWA", 0.0) // trigger error or default
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboard_submit_btn")
        ) {
            Text("Submit Documents for Verity Evaluation", fontWeight = FontWeight.Bold)
        }
    }
}

// Customer Support Tickets List or creation form
@Composable
fun TicketFormView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    var subject by remember { mutableStateOf("") }
    var msgBody by remember { mutableStateOf("") }

    val tickets by viewModel.allTickets.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("ticket_form_view")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("BOOK") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "💬 Customer Dispute & Ticket Support",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Open New Investigation Ticket", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Issue Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("ticket_subject")
                )
                OutlinedTextField(
                    value = msgBody,
                    onValueChange = { msgBody = it },
                    label = { Text("Incident details or payment dispute logs") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(vertical = 4.dp)
                        .testTag("ticket_msg"),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (subject.isNotBlank() && msgBody.isNotBlank()) {
                            viewModel.submitSupportTicket(subject, msgBody)
                            subject = ""
                            msgBody = ""
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("ticket_submit_btn")
                ) {
                    Text("Submit Support Request")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("Historic Submissions Open/Closed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (tickets.isEmpty()) {
            Text("No ongoing disputes registered with our operations wing.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        } else {
            tickets.forEach { tk ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tk.subject, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(tk.message, fontSize = 11.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E8), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tk.status, color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Operational Dashboard / Admin Portal representation
@Composable
fun AdminView(
    viewModel: SajiloViewModel,
    labels: Map<String, String>,
    isNe: Boolean
) {
    val pendingDrivers by viewModel.driverOnboardings.collectAsStateWithLifecycle()
    val tickets by viewModel.allTickets.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_portal_view")
    ) {
        Text(
            text = "📊 Admin Command & Regional Dashboard",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Simulated administrative view representing background-checks of Nepalese ride licensing.",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Metrics Summary Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Total Drivers Registered", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${pendingDrivers.size} authorized", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Active Support Investigations", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${tickets.size} open", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Background check physical review checklist
        Text("Pending Driver Onboarding Reviews", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))

        if (pendingDrivers.isEmpty()) {
            Text("No applications currently queued.", fontSize = 11.sp, color = Color.Gray)
        } else {
            pendingDrivers.forEach { d ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(d.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Lic #: ${d.licenseNumber} • Citizenship ref: ${d.citizenshipId}", fontSize = 10.sp, color = Color.Gray)
                                Text("Vehicle Model: ${d.vehicleModel} (${d.vehiclePlate})", fontSize = 10.sp, color = Color.Gray)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (d.documentStatus) {
                                            "APPROVED" -> Color(0xFFC8E6C9)
                                            "REJECTED" -> Color(0xFFFFCDD2)
                                            else -> Color(0xFFFFF9C4)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(d.documentStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }

                        if (d.documentStatus == "PENDING") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { viewModel.updateOnboardingStatus(d.id, "REJECTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("admin_reject_${d.id}")
                                ) {
                                    Text("Reject License", color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.updateOnboardingStatus(d.id, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60BB46)),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("admin_approve_${d.id}")
                                ) {
                                    Text("Approve / Verify", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data structures for styling
data class CategoryInfo(val id: String, val label: String, val desc: String, val rateFactor: Double)
data class PaymentInfo(val id: String, val name: String, val color: Color)

// Extension function for Canvas Bezier drawing helper
fun Path.quadraticToTo(controlX: Float, controlY: Float, endX: Float, endY: Float) {
    quadraticTo(controlX, controlY, endX, endY)
}

// Extension to scale layouts evenly
fun Modifier.scale(f: Float): Modifier = this.then(Modifier)

val NepaliLabels = mapOf(
    "namaste" to "नमस्ते",
    "home" to "मुख्य",
    "history" to "इतिहास",
    "admin" to "प्रशासक",
    "title_request_ride" to "सवारी बुक गर्नुहोस् (तत्काल बुकिंग)",
    "lbl_pickup" to "सवारी सुरु हुने स्थान (पिकअप)",
    "lbl_destination" to "सवारी पुग्ने स्थान (गन्तव्य)",
    "opt_pre_schedule" to "अग्रिम राइड बुक गर्नुहोस्",
    "opt_hilly" to "पहाडी बाटो शुल्क (+४५%)",
    "opt_fuel_surge" to "इन्धन अभाव विशेष शुल्क (+२०%)",
    "header_select_ride" to "सवारीको श्रेणी छान्नुहोस्",
    "cat_bike" to "बाइक / स्कूटी",
    "cat_car" to "कार / ट्याक्सी",
    "cat_suv" to "एसयूवी (ठूलो कार)",
    "cat_shared" to "साझा सवारी (कम्प्याक्ट)",
    "payment_method" to "भुक्तानीको माध्यम",
    "pay_cash" to "नगद भुक्तानी (क्यास)",
    "lbl_distance" to "दुरी",
    "lbl_duration" to "समय",
    "total_fare" to "कुल भाडा",
    "btn_request_ride" to "राइड सुरू गर्नुहोस्",
    "driver_onboard" to "चालक (ड्राइभर) दर्ता",
    "customer_support" to "ग्राहक सेवा / टिकट",
    "lbl_status" to "राइडको हालको स्थिति",
    "chat_driver" to "चालकसँग कुराकानी (च्याट)",
    "btn_cancel_trip" to "सवारी रद्द गर्नुहोस् (विवाद रहित)",
    "system_status" to "सजिलो सवारी सूचना"
)

val EnglishLabels = mapOf(
    "namaste" to "Namaste",
    "home" to "Request Ride",
    "history" to "History",
    "admin" to "Admin Operations",
    "title_request_ride" to "Instant Ride Booking",
    "lbl_pickup" to "Pickup Location",
    "lbl_destination" to "Dropoff Location",
    "opt_pre_schedule" to "Post-Schedule Trip",
    "opt_hilly" to "Hilly Terrain Surcharge (+45%)",
    "opt_fuel_surge" to "Fuel Price Volatility Surge (+20%)",
    "header_select_ride" to "Select Ride Category",
    "cat_bike" to "Two-Wheeler Bike",
    "cat_car" to "Sajilo Car/Taxi",
    "cat_suv" to "Hilly Range SUV",
    "cat_shared" to "Shared Micro",
    "payment_method" to "Payment Gateway Selector",
    "pay_cash" to "Cash Payment (NPR)",
    "lbl_distance" to "Distance",
    "lbl_duration" to "Duration",
    "total_fare" to "Fare Total (NPR)",
    "btn_request_ride" to "Send Ride Request",
    "driver_onboard" to "Driver Verification Portal",
    "customer_support" to "Disputes Support Tickets",
    "lbl_status" to "Current Telemetry Status",
    "chat_driver" to "Contact Matching Driver (VoIP)",
    "btn_cancel_trip" to "Cancel Active Session Safely",
    "system_status" to "Sajilo System Bulletins"
)
