package com.example.ui

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.FuelStation
import com.example.data.repository.FuelRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class DesignStyle {
    MATERIAL_3, GLASSMORPHIC, FUTURISTIC, MATERIAL_EXPERIENCE, IOS_26_GLASS, NEO_BRUTALISM, CARBON_DARK
}

enum class ThemeColor {
    BLUE, GREEN, ORANGE, PURPLE, RED, TEAL, AMBER, ROSE, SLATE, CYAN, INDIGO, GOLD, EMERALD
}

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "FuelViewModel"

    private val prefs = application.getSharedPreferences("wafuelwatch_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            AppTheme.valueOf(prefs.getString("theme_mode", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    )
    val themeMode: StateFlow<AppTheme> = _themeMode.asStateFlow()

    fun setThemeMode(theme: AppTheme) {
        _themeMode.value = theme
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    private val _themeColor = MutableStateFlow(
        try {
            ThemeColor.valueOf(prefs.getString("theme_color", ThemeColor.BLUE.name) ?: ThemeColor.BLUE.name)
        } catch (e: Exception) {
            ThemeColor.BLUE
        }
    )
    val themeColor: StateFlow<ThemeColor> = _themeColor.asStateFlow()

    fun setThemeColor(color: ThemeColor) {
        _themeColor.value = color
        prefs.edit().putString("theme_color", color.name).apply()
    }

    private val _designStyle = MutableStateFlow(
        try {
            DesignStyle.valueOf(prefs.getString("design_style", DesignStyle.MATERIAL_3.name) ?: DesignStyle.MATERIAL_3.name)
        } catch (e: Exception) {
            DesignStyle.MATERIAL_3
        }
    )
    val designStyle: StateFlow<DesignStyle> = _designStyle.asStateFlow()

    fun setDesignStyle(style: DesignStyle) {
        _designStyle.value = style
        prefs.edit().putString("design_style", style.name).apply()
    }

    private val db = AppDatabase.getDatabase(application)
    private val repository = FuelRepository(db.favoriteStationDao(), db.wazeIncidentDao(), OkHttpClient())

    val wazeIncidents: StateFlow<List<com.example.data.database.WazeIncidentEntity>> = repository.wazeIncidents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addWazeIncident(type: String, label: String, desc: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val incident = com.example.data.database.WazeIncidentEntity(
                type = type,
                label = label,
                desc = desc,
                latitude = latitude,
                longitude = longitude
            )
            repository.addWazeIncident(incident)
        }
    }

    fun removeWazeIncident(id: Int) {
        viewModelScope.launch {
            repository.removeWazeIncident(id)
        }
    }

    fun clearOldIncidents() {
        viewModelScope.launch {
            val fourHoursAgo = System.currentTimeMillis() - (4 * 60 * 60 * 1000)
            repository.clearOldWazeIncidents(fourHoursAgo)
        }
    }

    // UI Input States
    private val _selectedProduct = MutableStateFlow(
        try {
            FuelProduct.valueOf(prefs.getString("selected_product", FuelProduct.UNLEADED.name) ?: FuelProduct.UNLEADED.name)
        } catch (e: Exception) {
            FuelProduct.UNLEADED
        }
    )
    val selectedProduct: StateFlow<FuelProduct> = _selectedProduct.asStateFlow()

    private val _selectedDay = MutableStateFlow("today") // "today", "tomorrow"
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _selectedBrand = MutableStateFlow(prefs.getString("selected_brand", "") ?: "")
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    private val _selectedSuburb = MutableStateFlow(prefs.getString("selected_suburb", "PERTH") ?: "PERTH")
    val selectedSuburb: StateFlow<String> = _selectedSuburb.asStateFlow()

    private val _tripStartSuburb = MutableStateFlow(prefs.getString("trip_start_suburb", "PERTH") ?: "PERTH")
    val tripStartSuburb: StateFlow<String> = _tripStartSuburb.asStateFlow()

    private val _tripEndSuburb = MutableStateFlow(prefs.getString("trip_end_suburb", "MANDURAH") ?: "MANDURAH")
    val tripEndSuburb: StateFlow<String> = _tripEndSuburb.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(
        try {
            SortOption.valueOf(prefs.getString("sort_by", SortOption.PRICE.name) ?: SortOption.PRICE.name)
        } catch (e: Exception) {
            SortOption.PRICE
        }
    )
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    // Persistent Tab State (0 = Trends, 1 = My Trip, 2 = Near Me, 3 = Favorites, 4 = Settings)
    private val _selectedTab = MutableStateFlow(prefs.getInt("selected_tab", 2))
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
        prefs.edit().putInt("selected_tab", tab).apply()
    }

    // Price Alert / Notification Settings
    private val _priceAlertsEnabled = MutableStateFlow(prefs.getBoolean("price_alerts_enabled", false))
    val priceAlertsEnabled: StateFlow<Boolean> = _priceAlertsEnabled.asStateFlow()

    fun setPriceAlertsEnabled(enabled: Boolean) {
        _priceAlertsEnabled.value = enabled
        prefs.edit().putBoolean("price_alerts_enabled", enabled).apply()
    }

    // Individual User Metadata and persistent preferences
    private val _userId = MutableStateFlow(
        prefs.getString("user_id", null) ?: run {
            val newId = "WA-FW-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString("user_id", newId).apply()
            newId
        }
    )
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Valued Motorist") ?: "Valued Motorist")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit().putString("user_name", name).apply()
    }

    private val _userCarType = MutableStateFlow(prefs.getString("user_car_type", "Standard SUV/Sedan") ?: "Standard SUV/Sedan")
    val userCarType: StateFlow<String> = _userCarType.asStateFlow()

    fun setUserCarType(carType: String) {
        _userCarType.value = carType
        prefs.edit().putString("user_car_type", carType).apply()
    }

    private val _userInstalledAt = MutableStateFlow(
        prefs.getString("user_installed_at", null) ?: run {
            val currentFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            prefs.edit().putString("user_installed_at", currentFormat).apply()
            currentFormat
        }
    )
    val userInstalledAt: StateFlow<String> = _userInstalledAt.asStateFlow()

    private val _priceAlertThreshold = MutableStateFlow(prefs.getFloat("price_alert_threshold", 175.0f))
    val priceAlertThreshold: StateFlow<Float> = _priceAlertThreshold.asStateFlow()

    fun setPriceAlertThreshold(threshold: Float) {
        _priceAlertThreshold.value = threshold
        prefs.edit().putFloat("price_alert_threshold", threshold).apply()
    }

    private val _priceAlertSuburb = MutableStateFlow(prefs.getString("price_alert_suburb", "PERTH") ?: "PERTH")
    val priceAlertSuburb: StateFlow<String> = _priceAlertSuburb.asStateFlow()

    fun setPriceAlertSuburb(suburb: String) {
        _priceAlertSuburb.value = suburb
        prefs.edit().putString("price_alert_suburb", suburb).apply()
    }

    private val _priceAlertProduct = MutableStateFlow(
        try {
            FuelProduct.valueOf(prefs.getString("price_alert_product", FuelProduct.UNLEADED.name) ?: FuelProduct.UNLEADED.name)
        } catch (e: Exception) {
            FuelProduct.UNLEADED
        }
    )
    val priceAlertProduct: StateFlow<FuelProduct> = _priceAlertProduct.asStateFlow()

    fun setPriceAlertProduct(product: FuelProduct) {
        _priceAlertProduct.value = product
        prefs.edit().putString("price_alert_product", product.name).apply()
    }

    // Location State
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // API Loaded Stations
    private val _stations = MutableStateFlow<List<FuelStation>>(emptyList())
    val stations: StateFlow<List<FuelStation>> = _stations.asStateFlow()

    private val _yesterdayStations = MutableStateFlow<List<FuelStation>>(emptyList())
    val yesterdayStations: StateFlow<List<FuelStation>> = _yesterdayStations.asStateFlow()

    private val _todayStations = MutableStateFlow<List<FuelStation>>(emptyList())
    val todayStations: StateFlow<List<FuelStation>> = _todayStations.asStateFlow()

    private val _tomorrowStations = MutableStateFlow<List<FuelStation>>(emptyList())
    val tomorrowStations: StateFlow<List<FuelStation>> = _tomorrowStations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Favorites from Database
    val favoriteStations: StateFlow<List<FuelStation>> = repository.favoriteStations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cached favorite IDs for faster lookups
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // Location client
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // Tomorrow status check
    val isTomorrowPricesPublished: Boolean
        get() {
            // WA tomorrow prices are published at 2:30 PM (Perth Time / AWST)
            val tz = TimeZone.getTimeZone("Australia/Perth")
            val calendar = Calendar.getInstance(tz)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            return hour > 14 || (hour == 14 && minute >= 30)
        }

    init {
        // Fetch initially
        refreshFuelPrices()
        clearOldIncidents()
        
        // Listen to favorite updates
        viewModelScope.launch {
            repository.favoriteStations.collect { favs ->
                _favoriteIds.value = favs.map { it.id }.toSet()
            }
        }

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
            updateUserLocation()
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not yet granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location: ${e.message}")
        }
    }

    fun setProduct(product: FuelProduct) {
        _selectedProduct.value = product
        prefs.edit().putString("selected_product", product.name).apply()
        refreshFuelPrices()
    }

    fun setDay(day: String) {
        _selectedDay.value = day
        refreshFuelPrices()
    }

    fun setBrand(brand: String) {
        _selectedBrand.value = brand
        prefs.edit().putString("selected_brand", brand).apply()
    }

    fun setSuburb(suburb: String) {
        _selectedSuburb.value = suburb
        prefs.edit().putString("selected_suburb", suburb).apply()
    }

    fun setTripStartSuburb(suburb: String) {
        _tripStartSuburb.value = suburb
        prefs.edit().putString("trip_start_suburb", suburb).apply()
    }

    fun setTripEndSuburb(suburb: String) {
        _tripEndSuburb.value = suburb
        prefs.edit().putString("trip_end_suburb", suburb).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(option: SortOption) {
        _sortBy.value = option
        prefs.edit().putString("sort_by", option.name).apply()
    }

    fun updateUserLocation() {
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    _userLocation.value = location
                    Log.d(TAG, "User location updated: ${location.latitude}, ${location.longitude}")
                }
            }?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting location: ${e.message}")
        }
    }

    private fun getDateString(offsetDays: Int): String {
        val tz = TimeZone.getTimeZone("Australia/Perth")
        val calendar = Calendar.getInstance(tz)
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private fun getDayOfWeek(offsetDays: Int): Int {
        val tz = TimeZone.getTimeZone("Australia/Perth")
        val calendar = Calendar.getInstance(tz)
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    private fun getCycleMultiplier(dayOfWeek: Int): Double {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> 0.96
            Calendar.MONDAY -> 0.94
            Calendar.TUESDAY -> 0.93
            Calendar.WEDNESDAY -> 1.08
            Calendar.THURSDAY -> 1.04
            Calendar.FRIDAY -> 1.01
            Calendar.SATURDAY -> 0.98
            else -> 1.0
        }
    }

    private fun applyFilters(
        list: List<FuelStation>,
        brand: String,
        suburb: String,
        query: String
    ): List<FuelStation> {
        var filtered = list
        if (brand.isNotBlank()) {
            filtered = filtered.filter { it.brand.equals(brand, ignoreCase = true) }
        }
        if (suburb.isNotBlank()) {
            val trimmedSuburb = suburb.trim()
            if (trimmedSuburb.all { it.isDigit() }) {
                filtered = filtered.filter { it.address.contains(trimmedSuburb) }
            } else {
                filtered = filtered.filter { it.location.equals(trimmedSuburb, ignoreCase = true) }
            }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.tradingName.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
            }
        }
        return filtered
    }

    private fun saveAveragesToCache(
        productId: Int,
        yesterdayList: List<FuelStation>,
        todayList: List<FuelStation>,
        tomorrowList: List<FuelStation>
    ) {
        val editor = prefs.edit()
        
        if (yesterdayList.isNotEmpty()) {
            val avg = yesterdayList.map { it.price }.filter { it > 0.0 }.average()
            if (!avg.isNaN()) {
                editor.putFloat("avg_price_${productId}_${getDateString(-1)}", avg.toFloat())
            }
        }
        
        if (todayList.isNotEmpty()) {
            val avg = todayList.map { it.price }.filter { it > 0.0 }.average()
            if (!avg.isNaN()) {
                editor.putFloat("avg_price_${productId}_${getDateString(0)}", avg.toFloat())
            }
        }
        
        if (tomorrowList.isNotEmpty()) {
            val avg = tomorrowList.map { it.price }.filter { it > 0.0 }.average()
            if (!avg.isNaN()) {
                editor.putFloat("avg_price_${productId}_${getDateString(1)}", avg.toFloat())
            }
        }
        
        editor.apply()
    }

    fun refreshFuelPrices() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val productId = _selectedProduct.value.id
                
                // Fetch yesterday, today, and tomorrow (if published) in parallel
                val yesterdayDeferred = async(Dispatchers.IO) {
                    try {
                        repository.fetchFuelPrices(productId = productId, day = "yesterday")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch yesterday prices", e)
                        emptyList()
                    }
                }
                
                val todayDeferred = async(Dispatchers.IO) {
                    try {
                        repository.fetchFuelPrices(productId = productId, day = "today")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch today prices", e)
                        emptyList()
                    }
                }
                
                val tomorrowDeferred = async(Dispatchers.IO) {
                    if (isTomorrowPricesPublished) {
                        try {
                            repository.fetchFuelPrices(productId = productId, day = "tomorrow")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fetch tomorrow prices", e)
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
                
                val yesterdayResult = yesterdayDeferred.await()
                val todayResult = todayDeferred.await()
                val tomorrowResult = tomorrowDeferred.await()
                
                _yesterdayStations.value = yesterdayResult
                _todayStations.value = todayResult
                _tomorrowStations.value = tomorrowResult
                
                // Active stations is based on selectedDay
                val result = when (_selectedDay.value) {
                    "yesterday" -> yesterdayResult
                    "tomorrow" -> if (tomorrowResult.isNotEmpty()) tomorrowResult else todayResult
                    else -> todayResult
                }
                _stations.value = result
                
                // Save averages to historical cache in SharedPreferences
                saveAveragesToCache(productId, yesterdayResult, todayResult, tomorrowResult)
                
                // Trigger Price Alerts Check
                checkAndTriggerPriceAlerts(todayResult)
                
                Log.d(TAG, "Loaded: yesterday=${yesterdayResult.size}, today=${todayResult.size}, tomorrow=${tomorrowResult.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading fuel prices: ${e.message}", e)
                _error.value = "Failed to load real-time prices. Please check your internet connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkAndTriggerPriceAlerts(stations: List<FuelStation>) {
        if (!priceAlertsEnabled.value) return
        
        val alertProduct = priceAlertProduct.value
        val alertSuburb = priceAlertSuburb.value.trim()
        val alertThreshold = priceAlertThreshold.value

        // Filter stations matching alert criteria
        val matchingStations = stations.filter { station ->
            station.location.equals(alertSuburb, ignoreCase = true) &&
            station.price > 0.0 &&
            station.price <= alertThreshold
        }

        if (matchingStations.isNotEmpty()) {
            val cheapest = matchingStations.minByOrNull { it.price }
            cheapest?.let { station ->
                sendPriceAlertNotification(station, alertProduct)
            }
        }
    }

    private fun sendPriceAlertNotification(station: FuelStation, product: FuelProduct) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Build Notification Intent
        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Price Alert: ${product.displayName} dropped!"
        val message = "${station.tradingName} in ${station.location} is selling for ${station.price}¢/L (below your ${priceAlertThreshold.value}¢/L threshold)!"

        val notification = androidx.core.app.NotificationCompat.Builder(context, "FUEL_ALERT_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard dialog info icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(42, notification)
    }

    // Expose the calculated 7-day average prices for the UI trends view
    val trendPrices: StateFlow<List<Double>> = combine(
        _todayStations,
        _yesterdayStations,
        _selectedProduct,
        _selectedBrand,
        _selectedSuburb,
        _searchQuery
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val todayRaw = args[0] as List<FuelStation>
        @Suppress("UNCHECKED_CAST")
        val yesterdayRaw = args[1] as List<FuelStation>
        val product = args[2] as FuelProduct
        val brand = args[3] as String
        val suburb = args[4] as String
        val query = args[5] as String

        val todayFiltered = applyFilters(todayRaw, brand, suburb, query)
        val yesterdayFiltered = applyFilters(yesterdayRaw, brand, suburb, query)
        
        val todayFilteredAvg = todayFiltered.map { it.price }.filter { it > 0.0 }.average().let { if (it.isNaN()) 0.0 else it }
        val yesterdayFilteredAvg = yesterdayFiltered.map { it.price }.filter { it > 0.0 }.average().let { if (it.isNaN()) 0.0 else it }
        
        val overallTodayAvg = todayRaw.map { it.price }.filter { it > 0.0 }.average().let { if (it.isNaN()) 0.0 else it }
        
        val list = mutableListOf<Double>()
        for (i in -6..0) {
            val dateStr = getDateString(i)
            val dayOfWeek = getDayOfWeek(i)
            
            val price = when (i) {
                0 -> {
                    if (todayFilteredAvg > 0.0) todayFilteredAvg else overallTodayAvg
                }
                -1 -> {
                    if (yesterdayFilteredAvg > 0.0) yesterdayFilteredAvg else {
                        val yesterdayOverall = yesterdayRaw.map { it.price }.filter { it > 0.0 }.average().let { if (it.isNaN()) 0.0 else it }
                        if (yesterdayOverall > 0.0) {
                            if (todayFilteredAvg > 0.0 && overallTodayAvg > 0.0) {
                                todayFilteredAvg * (yesterdayOverall / overallTodayAvg)
                            } else {
                                yesterdayOverall
                            }
                        } else {
                            val multToday = getCycleMultiplier(getDayOfWeek(0))
                            val multYesterday = getCycleMultiplier(dayOfWeek)
                            val estOverall = if (overallTodayAvg > 0.0) {
                                overallTodayAvg * (multYesterday / multToday)
                            } else {
                                185.0 * (multYesterday / multToday)
                            }
                            if (todayFilteredAvg > 0.0 && overallTodayAvg > 0.0) {
                                todayFilteredAvg * (estOverall / overallTodayAvg)
                            } else {
                                estOverall
                            }
                        }
                    }
                }
                else -> {
                    val cachedOverall = prefs.getFloat("avg_price_${product.id}_$dateStr", 0f).toDouble()
                    val overallAvgD = if (cachedOverall > 0.0) {
                        cachedOverall
                    } else {
                        val multToday = getCycleMultiplier(getDayOfWeek(0))
                        val multD = getCycleMultiplier(dayOfWeek)
                        if (overallTodayAvg > 0.0) {
                            overallTodayAvg * (multD / multToday)
                        } else {
                            185.0 * (multD / multToday)
                        }
                    }
                    
                    if (todayFilteredAvg > 0.0 && overallTodayAvg > 0.0) {
                        todayFilteredAvg * (overallAvgD / overallTodayAvg)
                    } else {
                        overallAvgD
                    }
                }
            }
            
            val roundedPrice = if (price > 0.0) {
                String.format(Locale.US, "%.1f", price).toDoubleOrNull() ?: price
            } else {
                185.0
            }
            list.add(roundedPrice)
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = List(7) { 185.0 }
    )

    // Computed / Filtered State for UI
    val filteredStations: StateFlow<List<FuelStation>> = combine(
        _stations,
        _selectedBrand,
        _selectedSuburb,
        _searchQuery,
        _sortBy,
        _userLocation
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val list = args[0] as List<FuelStation>
        val brand = args[1] as String
        val suburb = args[2] as String
        val query = args[3] as String
        val sorting = args[4] as SortOption
        val location = args[5] as Location?

        var filtered = list

        // Filter by Brand
        if (brand.isNotBlank()) {
            filtered = filtered.filter { it.brand.equals(brand, ignoreCase = true) }
        }

        // Filter by Suburb or Postcode
        if (suburb.isNotBlank()) {
            val trimmedSuburb = suburb.trim()
            if (trimmedSuburb.all { it.isDigit() }) {
                filtered = filtered.filter { it.address.contains(trimmedSuburb) }
            } else {
                filtered = filtered.filter { it.location.equals(trimmedSuburb, ignoreCase = true) }
            }
        }

        // Search Query (trading name, address, or suburb)
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.tradingName.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
            }
        }

        // Sorting
        when (sorting) {
            SortOption.PRICE -> {
                filtered = filtered.sortedBy { it.price }
            }
            SortOption.NAME -> {
                filtered = filtered.sortedBy { it.tradingName }
            }
            SortOption.DISTANCE -> {
                if (location != null) {
                    filtered = filtered.sortedBy { station ->
                        calculateDistance(location.latitude, location.longitude, station.latitude, station.longitude)
                    }
                } else {
                    // Fallback to Perth CBD
                    filtered = filtered.sortedBy { station ->
                        calculateDistance(-31.9505, 115.8605, station.latitude, station.longitude)
                    }
                }
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Pricing Statistics
    val pricingStats: StateFlow<PricingStats> = filteredStations.map { list ->
        if (list.isEmpty()) return@map PricingStats(0.0, 0.0, 0.0)
        val validPrices = list.map { it.price }.filter { it > 0.0 }
        if (validPrices.isEmpty()) return@map PricingStats(0.0, 0.0, 0.0)
        
        val min = validPrices.minOrNull() ?: 0.0
        val max = validPrices.maxOrNull() ?: 0.0
        val avg = validPrices.average()
        PricingStats(min, max, avg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PricingStats(0.0, 0.0, 0.0)
    )

    // Manage Favorites
    fun toggleFavorite(station: FuelStation) {
        viewModelScope.launch {
            if (isFavorite(station.id)) {
                repository.removeFavorite(station.id)
            } else {
                repository.addFavorite(station)
            }
        }
    }

    private suspend fun isFavorite(id: String): Boolean {
        return repository.isFavorite(id)
    }

    // Popular WA brands & suburbs helper lists for the filter dropdowns
    val availableBrands: List<String> = listOf(
        "Ampol", "BP", "Caltex", "Coles Express", "EG Ampol", "Liberty", "Puma", "Shell", "United", "Vibe", "Better Choice", "Kleenheat"
    )

    val popularSuburbs: List<String> = listOf(
        "PERTH", "MANDURAH", "FREMANTLE", "JOONDALUP", "MIDLAND", "ROCKINGHAM", "ARMADALE", 
        "BUNBURY", "ALBANY", "GERALDTON", "KALGOORLIE", "CANNINGTON", "SUBIACO", "OSBORNE PARK", 
        "SCARBOROUGH", "BALCATTA", "MORLEY", "VICTORIA PARK", "BELMONT", "GOSNELLS", "WANNEROO"
    ).sorted()

    val allSuburbs: StateFlow<List<String>> = _stations.map { list ->
        val suburbsFromStations = list.map { it.location.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
        if (suburbsFromStations.isEmpty()) {
            popularSuburbs
        } else {
            suburbsFromStations.sorted()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = popularSuburbs
    )

    // Helper to calculate distance in KM
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

enum class FuelProduct(val id: Int, val displayName: String) {
    UNLEADED(1, "Unleaded 91"),
    PREMIUM_95(2, "Premium 95"),
    PREMIUM_98(6, "Premium 98"),
    DIESEL(4, "Diesel"),
    LPG(5, "LPG"),
    E10(10, "E10"),
    E85(11, "E85")
}

enum class SortOption(val displayName: String) {
    PRICE("Lowest Price"),
    DISTANCE("Distance"),
    NAME("Name (A-Z)")
}

data class PricingStats(
    val lowest: Double,
    val highest: Double,
    val average: Double
)
