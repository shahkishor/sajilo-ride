package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SajiloViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SajiloDatabase.getDatabase(application)
    val repository = SajiloRepository(database.dao())

    // 1. App-wide states
    val userProfile: StateFlow<DbUserProfile?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allRides: StateFlow<List<DbRide>> = repository.allRides.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTickets: StateFlow<List<DbSupportTicket>> = repository.allTickets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val driverOnboardings: StateFlow<List<DbDriverOnboarding>> = repository.driverOnboardings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current screen navigation: "BOOK", "TRIP_PROGRESS", "HISTORY", "ADMIN", "ONBOARD_FORM", "TICKET_FORM"
    private val _currentScreen = MutableStateFlow("BOOK")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Interactive booking dynamic state
    var selectedPickupIndex = MutableStateFlow(0)
    var selectedDropIndex = MutableStateFlow(2)
    var selectedCategory = MutableStateFlow("BIKE") // "BIKE", "CAR", "SUV", "SHARED"
    var paymentMethod = MutableStateFlow("CASH") // "CASH", "ESEWA", "KHALTI", "IMEPAY"
    var isHillyTerrain = MutableStateFlow(false)
    var hasFuelSurge = MutableStateFlow(false)
    var isPreScheduled = MutableStateFlow(false)
    var scheduledTimeText = MutableStateFlow("07:00 AM")

    // Active trip tracker
    private val _activeRide = MutableStateFlow<DbRide?>(null)
    val activeRide: StateFlow<DbRide?> = _activeRide.asStateFlow()

    private val _tripProgressFraction = MutableStateFlow(0f) // 0.0 to 1.0 (corresponds to map movement)
    val tripProgressFraction: StateFlow<Float> = _tripProgressFraction.asStateFlow()

    private val _activeNotifications = MutableStateFlow<List<String>>(emptyList())
    val activeNotifications: StateFlow<List<String>> = _activeNotifications.asStateFlow()

    private val _activeChatMessages = MutableStateFlow<List<DbChatMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<DbChatMessage>> = _activeChatMessages.asStateFlow()

    // Network connectivity and signal status monitoring StateFlows
    private val _isNetworkOnline = MutableStateFlow(true)
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    private val _isPoorSignal = MutableStateFlow(false)
    val isPoorSignal: StateFlow<Boolean> = _isPoorSignal.asStateFlow()

    // Seeding data indicators
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var simulationJob: Job? = null
    private var chatJob: Job? = null

    // Locations in Kathmandu, Pokhara, and Biratnagar (Static master details with Google Maps lat/long coordinates)
    val locations = listOf(
        SajiloLocation("Thamel", "NPI-01", 180f, 150f, "Lively cultural & tourist hub", latitude = 27.7149, longitude = 85.3121, region = "Kathmandu"),
        SajiloLocation("Tribhuvan Airport (TIA)", "TIA-55", 380f, 260f, "Kathmandu gateway terminal", latitude = 27.6966, longitude = 85.3591, region = "Kathmandu"),
        SajiloLocation("Patan Durbar Square", "PDS-99", 220f, 360f, "Historic courtyard & narrow streets", latitude = 27.6727, longitude = 85.3252, region = "Kathmandu"),
        SajiloLocation("Kalimati Market", "KMT-12", 100f, 230f, "Busy crossroads & high traffic denseness", latitude = 27.6983, longitude = 85.2974, region = "Kathmandu"),
        SajiloLocation("Boudha Stupa", "BNS-44", 320f, 110f, "Holy monumental heritage circuit", latitude = 27.7215, longitude = 85.3620, region = "Kathmandu"),
        SajiloLocation("Bhaktapur Durbar Square", "BDS-88", 440f, 320f, "Suburban red-brick historic alleys", latitude = 27.6713, longitude = 85.4290, region = "Kathmandu"),
        SajiloLocation("Sarangkot Hill, Pokhara", "PKR-SK", 60f, 80f, "Pokhara range steep climb, gravel road", isHilly = true, latitude = 28.2439, longitude = 83.9486, region = "Pokhara"),
        SajiloLocation("Lakeside Pokhara", "PKR-LS", 140f, 180f, "Scenic tourist restaurant strip by Phewa Lake", latitude = 28.2096, longitude = 83.9584, region = "Pokhara"),
        SajiloLocation("Biratnagar Airport (BRT)", "BRT-01", 120f, 60f, "Koshi terminal runway gateway", latitude = 26.4812, longitude = 87.2667, region = "Biratnagar"),
        SajiloLocation("Jatashankar Chowk, Biratnagar", "JSC-10", 250f, 180f, "Busy central high street intersection", latitude = 26.4605, longitude = 87.2750, region = "Biratnagar"),
        SajiloLocation("Biratnagar Jute Mills", "BJM-99", 370f, 320f, "Historic pioneer Nepalese industrial site", latitude = 26.4385, longitude = 87.2775, region = "Biratnagar"),
        SajiloLocation("Koshi Highway Interchange", "KHI-55", 140f, 250f, "Regional highway connecting Koshi artery", latitude = 26.4710, longitude = 87.2730, region = "Biratnagar")
    )

    init {
        viewModelScope.launch {
            // Check & seed profile
            userProfile.collect { profile ->
                if (profile == null) {
                    val defaultProfile = DbUserProfile(
                        name = "Rojendra Shakya",
                        phoneNumber = "+977-9841234567",
                        email = "rojendra@sajiloride.com.np",
                        balanceEseva = 1500.0,
                        balanceKhalti = 850.0,
                        balanceImePay = 300.0,
                        raterCount = 12,
                        rating = 4.8,
                        selectedLanguage = "en",
                        isDataSavingsMode = false
                    )
                    repository.saveProfile(defaultProfile)
                }
            }
        }

        // Seed some historic sessions if database is empty
        viewModelScope.launch {
            allRides.collect { rides ->
                if (rides.isEmpty()) {
                    val seedRides = listOf(
                        DbRide(
                            pickup = "Thamel",
                            destination = "Tribhuvan Airport (TIA)",
                            fareNpr = 180.0,
                            distanceKm = 6.4,
                            durationMinutes = 22,
                            category = "BIKE",
                            paymentMethod = "ESEWA",
                            isHillyTerrain = false,
                            hasFuelSurge = false,
                            driverName = "Ram Bahadur",
                            driverPhone = "+977-9801112223",
                            driverRating = 4.9,
                            driverVehiclePlate = "Ba 1 Pa 4022",
                            status = "COMPLETED",
                            timestamp = System.currentTimeMillis() - 86400000 * 2
                        ),
                        DbRide(
                            pickup = "Boudha Stupa",
                            destination = "Patan Durbar Square",
                            fareNpr = 520.0,
                            distanceKm = 9.8,
                            durationMinutes = 38,
                            category = "CAR",
                            paymentMethod = "KHALTI",
                            isHillyTerrain = false,
                            hasFuelSurge = true,
                            driverName = "Sita Shrestha",
                            driverPhone = "+977-9811223344",
                            driverRating = 4.7,
                            driverVehiclePlate = "Ba 3 Cha 9011",
                            status = "COMPLETED",
                            timestamp = System.currentTimeMillis() - 86400000
                        )
                    )
                    for (ride in seedRides) {
                        repository.insertRide(ride)
                    }
                }
            }
        }

        // Seed default driver onboardings for admin portal demo status
        viewModelScope.launch {
            driverOnboardings.collect { onboardings ->
                if (onboardings.isEmpty()) {
                    repository.submitDriverOnboarding(
                        DbDriverOnboarding(
                            fullName = "Harish Khadka",
                            licenseNumber = "12-09-08241",
                            citizenshipId = "271044/899",
                            vehicleModel = "Bajaj Pulsar 150",
                            vehiclePlate = "Ba 4 Pa 8840",
                            documentStatus = "PENDING"
                        )
                    )
                    repository.submitDriverOnboarding(
                        DbDriverOnboarding(
                            fullName = "Maya Tamang",
                            licenseNumber = "04-99-10523",
                            citizenshipId = "440188/233",
                            vehicleModel = "Suzuki Alto",
                            vehiclePlate = "Ba 2 Cha 5562",
                            documentStatus = "APPROVED"
                        )
                    )
                }
            }
        }
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Query current network state
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isNetworkOnline.value = isOnline
            
            val bandwidth = capabilities?.linkDownstreamBandwidthKbps ?: 0
            _isPoorSignal.value = isOnline && bandwidth > 0 && bandwidth < 1500

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkOnline.value = true
                    _toastMessage.value = "Connection restored! Online mode active."
                }

                override fun onLost(network: Network) {
                    _isNetworkOnline.value = false
                    _toastMessage.value = "Connection lost! Offline mode active."
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val isInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isNetworkOnline.value = isInternet
                    
                    val linkDownstream = networkCapabilities.linkDownstreamBandwidthKbps
                    val meetsPoorThreshold = isInternet && linkDownstream > 0 && linkDownstream < 1500
                    if (meetsPoorThreshold != _isPoorSignal.value) {
                        _isPoorSignal.value = meetsPoorThreshold
                        if (meetsPoorThreshold) {
                            _toastMessage.value = "Signal became poor! Switched to offline map mode."
                        }
                    }
                }
            })
        } catch (e: Exception) {
            // Graceful fallback if permission or context is restricted
        }
    }

    fun simulatePoorSignalToggle() {
        _isPoorSignal.value = !_isPoorSignal.value
        if (_isPoorSignal.value) {
            _toastMessage.value = "Poor signal detected! Switched to offline map mode."
        } else {
            _toastMessage.value = "Good signal restored! Online map fully active."
        }
    }

    fun simulateConnectionToggle() {
        _isNetworkOnline.value = !_isNetworkOnline.value
        if (!_isNetworkOnline.value) {
            _toastMessage.value = "Connection lost! Entering offline mode."
        } else {
            _toastMessage.value = "Connection restored! Connected online."
        }
    }

    // Language selector helpers
    fun toggleLanguage() {
        val current = userProfile.value ?: return
        val nextLanguage = if (current.selectedLanguage == "en") "ne" else "en"
        viewModelScope.launch {
            repository.saveProfile(current.copy(selectedLanguage = nextLanguage))
            val logMsg = if (nextLanguage == "en") "Switched to English" else "नेपाली भाषामा फेरिएको छ"
            _toastMessage.value = logMsg
        }
    }

    fun toggleDataSavings() {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            repository.saveProfile(current.copy(isDataSavingsMode = !current.isDataSavingsMode))
            _toastMessage.value = if (!current.isDataSavingsMode) "Low Bandwidth Mode Enabled" else "Standard Map Graphics Active"
        }
    }

    fun addNotification(message: String) {
        val list = _activeNotifications.value.toMutableList()
        list.add(0, message)
        _activeNotifications.value = list
    }

    fun clearNotifications() {
        _activeNotifications.value = emptyList()
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Wallet Reload Simulation
    fun reloadWallet(service: String, amount: Double) {
        val cur = userProfile.value ?: return
        viewModelScope.launch {
            val updated = when (service) {
                "ESEWA" -> cur.copy(balanceEseva = cur.balanceEseva + amount)
                "KHALTI" -> cur.copy(balanceKhalti = cur.balanceKhalti + amount)
                "IMEPAY" -> cur.copy(balanceImePay = cur.balanceImePay + amount)
                else -> cur
            }
            repository.saveProfile(updated)
            _toastMessage.value = "NPR $amount added via $service wallet!"
        }
    }

    // Dynamic precise calculations
    fun calculateRideFare(
        dist: Double,
        cat: String,
        isHilly: Boolean,
        surge: Boolean
    ): Double {
        val baseRate = when (cat) {
            "BIKE" -> 20.0 + (dist * 15.0)  // Bike: Base 20 + 15 NPR/KM
            "CAR" -> 60.0 + (dist * 45.0)   // Taxi: Base 60 + 45 NPR/KM
            "SUV" -> 100.0 + (dist * 75.0)  // SUV: Base 100 + 75 NPR/KM
            "SHARED" -> 15.0 + (dist * 10.0) // Micro/Scooter-Share: Base 15 + 10 NPR/KM
            else -> dist * 20.0
        }

        var multiplier = 1.0
        if (isHilly) multiplier += 0.45 // +45% hills (heavy load/steep terrain surcharges)
        if (surge) multiplier += 0.20   // +20% fuel shortage volatility surcharges

        return Math.round(baseRate * multiplier * 10.0) / 10.0
    }

    // Instantly generate and trigger ride request
    fun requestInstantRide() {
        viewModelScope.launch {
            val rider = userProfile.value ?: return@launch
            val fromLoc = locations[selectedPickupIndex.value]
            val toLoc = locations[selectedDropIndex.value]

            if (fromLoc.name == toLoc.name) {
                _toastMessage.value = if (rider.selectedLanguage == "en") {
                    "Pickup and drop locations cannot be the same!"
                } else {
                    "पिकअप र ड्रप ठेगाना एउटै हुन सक्दैन!"
                }
                return@launch
            }

            // Estimate geo distance
            val dx = toLoc.x - fromLoc.x
            val dy = toLoc.y - fromLoc.y
            val rawDist = Math.sqrt((dx * dx + dy * dy).toDouble()) / 40.0 // Scaled KM
            val km = Math.round(rawDist * 10.0) / 10.0
            val durationMin = Math.max(5, (km * 2.5).toInt())

            val finalFare = calculateRideFare(km, selectedCategory.value, isHillyTerrain.value, hasFuelSurge.value)

            // Validate wallet balance if paying with online gateway
            if (paymentMethod.value == "ESEWA" && rider.balanceEseva < finalFare) {
                _toastMessage.value = if (rider.selectedLanguage == "en") "Insufficient eSewa balance! Reload wallet." else "ई-सेवा ब्यालेन्स अपुग छ! ब्यालेन्स थप्नुहोस्।"
                return@launch
            } else if (paymentMethod.value == "KHALTI" && rider.balanceKhalti < finalFare) {
                _toastMessage.value = if (rider.selectedLanguage == "en") "Insufficient Khalti balance! Reload wallet." else "खल्ती ब्यालेन्स अपुग छ! ब्यालेन्स थप्नुहोस्।"
                return@launch
            } else if (paymentMethod.value == "IMEPAY" && rider.balanceImePay < finalFare) {
                _toastMessage.value = if (rider.selectedLanguage == "en") "Insufficient IME Pay balance! Reload wallet." else "IME Pay ब्यालेन्स अपुग छ।"
                return@launch
            }

            val mockDrivers = listOf(
                Pair("Lokesh Adhikari", "9841908272"),
                Pair("Sujan Shrestha", "9812401889"),
                Pair("Raj Kumar Thapa", "9805562110"),
                Pair("Kabita Tamang", "9810884521")
            )
            val selectedDriver = mockDrivers.random()
            val vehicleSuffix = if (selectedCategory.value == "BIKE") "Pa" else "Cha"
            val plateNum = "Ba " + (2..4).random() + " " + vehicleSuffix + " " + (1000..9999).random()

            val newRide = DbRide(
                pickup = fromLoc.name,
                destination = toLoc.name,
                fareNpr = finalFare,
                distanceKm = km,
                durationMinutes = durationMin,
                category = selectedCategory.value,
                paymentMethod = paymentMethod.value,
                isHillyTerrain = isHillyTerrain.value,
                hasFuelSurge = hasFuelSurge.value,
                driverName = selectedDriver.first,
                driverPhone = selectedDriver.second,
                driverRating = Math.round((4.6 + Math.random() * 0.4) * 10.0) / 10.0,
                driverVehiclePlate = plateNum,
                status = "REQUESTED",
                isPreScheduled = isPreScheduled.value,
                scheduledTime = if (isPreScheduled.value) scheduledTimeText.value else ""
            )

            val newRideId = repository.insertRide(newRide)
            val persistedRide = newRide.copy(id = newRideId)
            _activeRide.value = persistedRide

            _currentScreen.value = "TRIP_PROGRESS"
            _tripProgressFraction.value = 0f
            _activeChatMessages.value = emptyList()

            // Trigger simulated matching lifecycle
            startTripSimulation(persistedRide)
        }
    }

    private fun startTripSimulation(ride: DbRide) {
        simulationJob?.cancel()
        chatJob?.cancel()

        simulationJob = viewModelScope.launch {
            val rider = userProfile.value
            val isNe = rider?.selectedLanguage == "ne"

            addNotification(
                if (isNe) "तपाईको राइड अनुरोध पठाइएको छ। चालक खोज्दै..."
                else "Requesting Ride... Spotting closest drivers in Nepal's network."
            )
            delay(2000)

            // Update status to ACCEPTED
            val acceptedRide = ride.copy(status = "ACCEPTED")
            _activeRide.value = acceptedRide
            repository.updateRideStatus(ride.id, "ACCEPTED")
            addNotification(
                if (isNe) "${acceptedRide.driverName} (${acceptedRide.category}) ले राइड स्वीकार गर्नुभयो!"
                else "Driver ${acceptedRide.driverName} matched on vehicle ${acceptedRide.driverVehiclePlate}!"
            )

            // Start chat matching trigger
            simulateIncomingDriverChat(ride.id)

            // Drive progress in loops
            // Stages:
            // 0% to 30% -> Driver coming to pick you up
            // 30% -> Driver Arrived
            // 30% to 100% -> In Trip
            // 100% -> Completed

            // Arriving progress
            for (p in 0..6) {
                _tripProgressFraction.value = (p * 0.05f)
                delay(1200)
            }

            val arrivedRide = acceptedRide.copy(status = "ACCEPTED") // "Arrived" state
            _activeRide.value = arrivedRide
            addNotification(
                if (isNe) "राइड चालक पिकअप विन्दुमा आइपुग्नुभयो! कृपया भेट्नुहोला।"
                else "Driver ${ride.driverName} arrived! Touch targets set, search for Bajaj/Alto plated ${ride.driverVehiclePlate}."
            )

            delay(2000)

            // IN TRIP
            val inTripRide = arrivedRide.copy(status = "IN_PROGRESS")
            _activeRide.value = inTripRide
            repository.updateRideStatus(ride.id, "IN_PROGRESS")
            addNotification(
                if (isNe) "सुखद यात्रा! गन्तव्य स्थान तर्फ प्रस्थान गरियो।"
                else "Trip started! Cruising towards ${ride.destination} safely."
            )

            for (p in 7..20) {
                _tripProgressFraction.value = (p * 0.05f)
                delay(1200)
            }

            // Pay deduction
            val finalProfile = userProfile.value
            if (finalProfile != null) {
                val dFare = ride.fareNpr
                val isCash = ride.paymentMethod == "CASH"
                if (!isCash) {
                    val updatedProfile = when (ride.paymentMethod) {
                        "ESEWA" -> finalProfile.copy(balanceEseva = finalProfile.balanceEseva - dFare)
                        "KHALTI" -> finalProfile.copy(balanceKhalti = finalProfile.balanceKhalti - dFare)
                        "IMEPAY" -> finalProfile.copy(balanceImePay = finalProfile.balanceImePay - dFare)
                        else -> finalProfile
                    }
                    repository.saveProfile(updatedProfile)
                }
            }

            // COMPLETED
            val compRide = inTripRide.copy(status = "COMPLETED")
            _activeRide.value = compRide
            repository.updateRideStatus(ride.id, "COMPLETED")
            addNotification(
                if (isNe) "यात्रा सफलतापूर्वक सम्पन्न भयो! धन्यवाद।"
                else "Arrived at ${ride.destination}! Payment of NPR ${ride.fareNpr} processed via ${ride.paymentMethod}."
            )

            _toastMessage.value = if (isNe) {
                "यात्रा पुरा भयो! NPR ${ride.fareNpr} भुक्तानी गरियो।"
            } else {
                "Ride Completed! Paid NPR ${ride.fareNpr} via ${ride.paymentMethod}."
            }
        }
    }

    private fun simulateIncomingDriverChat(rideId: Long) {
        chatJob = viewModelScope.launch {
            val isNe = userProfile.value?.selectedLanguage == "ne"
            delay(3000)
            val text1 = if (isNe) "नमस्ते, म आउँदैछु। तपाई कता हुनुहुन्छ?" else "Namaste! I am on the way. Where exact in the hub?"
            sendDirectChat(rideId, "DRIVER", text1)

            delay(8000)
            val text2 = if (isNe) "ट्राफिक जाम छ, ३ मिनेट जति लाग्छ है मलाई।" else "A bit of jam in Kalimati crossroads, will take 3 mins maximum."
            sendDirectChat(rideId, "DRIVER", text2)
        }
    }

    suspend fun sendRiderChat(message: String) {
        val ride = _activeRide.value ?: return
        sendDirectChat(ride.id, "RIDER", message)
    }

    private suspend fun sendDirectChat(rideId: Long, sender: String, text: String) {
        val msg = DbChatMessage(
            rideId = rideId,
            sender = sender,
            messageText = text
        )
        repository.sendChatMessage(msg)
        // Refresh local memory stream active list
        val currentChats = _activeChatMessages.value.toMutableList()
        currentChats.add(msg)
        _activeChatMessages.value = currentChats
    }

    // Cancel active ride
    fun cancelActiveRide() {
        val current = _activeRide.value ?: return
        viewModelScope.launch {
            simulationJob?.cancel()
            chatJob?.cancel()
            val cancelled = current.copy(status = "CANCELLED")
            _activeRide.value = cancelled
            repository.updateRideStatus(current.id, "CANCELLED")
            addNotification(
                if (userProfile.value?.selectedLanguage == "ne") "राइड रद्द गरिएको छ।"
                else "Ride was successfully cancelled by Rider. Safe travels."
            )
            _toastMessage.value = "Ride Cancelled"
            _currentScreen.value = "BOOK"
        }
    }

    // Back from Active trip details (when completed or cancelled)
    fun resetActiveTripToBook() {
        _activeRide.value = null
        _tripProgressFraction.value = 0f
        _currentScreen.value = "BOOK"
    }

    // Create a support Ticket
    fun submitSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            val ticket = DbSupportTicket(
                subject = subject,
                message = message,
                status = "OPEN"
            )
            repository.createTicket(ticket)
            _toastMessage.value = "Support Request Submitted. Feedback soon!"
            _currentScreen.value = "BOOK"
        }
    }

    // Submit driver onboarding
    fun submitOnboarding(fullName: String, license: String, citizenship: String, model: String, plate: String) {
        viewModelScope.launch {
            val driver = DbDriverOnboarding(
                fullName = fullName,
                licenseNumber = license,
                citizenshipId = citizenship,
                vehicleModel = model,
                vehiclePlate = plate,
                documentStatus = "PENDING"
            )
            repository.submitDriverOnboarding(driver)
            _toastMessage.value = "Documents submitted for physical review!"
            _currentScreen.value = "BOOK"
        }
    }

    // Admin commands
    fun updateOnboardingStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateDriverOnboardingStatus(id, status)
            _toastMessage.value = "Onboarding #$id updated to $status!"
        }
    }
}

// Data holder objects for simulation helper
data class SajiloLocation(
    val name: String,
    val zipCode: String,
    val x: Float,
    val y: Float,
    val description: String,
    val isHilly: Boolean = false,
    val latitude: Double = 27.7149,
    val longitude: Double = 85.3121,
    val region: String = "Kathmandu"
)
