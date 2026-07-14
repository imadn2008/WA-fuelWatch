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
import okhttp3.OkHttpClient
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class DesignStyle {
    MATERIAL_3, GLASSMORPHIC, FUTURISTIC, MATERIAL_EXPERIENCE, IOS_26_GLASS
}

enum class ThemeColor {
    BLUE, GREEN, ORANGE, PURPLE, RED
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
    private val repository = FuelRepository(db.favoriteStationDao(), OkHttpClient())

    // UI Input States
    private val _selectedProduct = MutableStateFlow(FuelProduct.UNLEADED)
    val selectedProduct: StateFlow<FuelProduct> = _selectedProduct.asStateFlow()

    private val _selectedDay = MutableStateFlow("today") // "today", "tomorrow"
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _selectedBrand = MutableStateFlow("") // Empty for all
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    private val _selectedSuburb = MutableStateFlow("PERTH") // Default to Perth
    val selectedSuburb: StateFlow<String> = _selectedSuburb.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.PRICE)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    // Location State
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // API Loaded Stations
    private val _stations = MutableStateFlow<List<FuelStation>>(emptyList())
    val stations: StateFlow<List<FuelStation>> = _stations.asStateFlow()

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
        refreshFuelPrices()
    }

    fun setDay(day: String) {
        _selectedDay.value = day
        refreshFuelPrices()
    }

    fun setBrand(brand: String) {
        _selectedBrand.value = brand
    }

    fun setSuburb(suburb: String) {
        _selectedSuburb.value = suburb
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(option: SortOption) {
        _sortBy.value = option
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

    fun refreshFuelPrices() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch from feed
                val result = repository.fetchFuelPrices(
                    productId = _selectedProduct.value.id,
                    day = _selectedDay.value,
                    // Suburb/brand parameters can optionally be passed to API,
                    // but loading all and filtering in-memory is way better
                    // because it allows changing suburb/brand filters instantly without a roundtrip loading spinner!
                )
                _stations.value = result
                Log.d(TAG, "Successfully loaded ${result.size} fuel prices.")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading fuel prices: ${e.message}", e)
                _error.value = "Failed to load real-time prices. Please check your internet connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

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
