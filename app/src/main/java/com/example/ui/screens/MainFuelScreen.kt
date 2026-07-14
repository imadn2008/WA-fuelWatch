package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.location.Location
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.FuelStation
import com.example.ui.FuelProduct
import com.example.ui.FuelViewModel
import com.example.ui.SortOption
import com.example.ui.PricingStats
import com.example.ui.AppTheme
import com.example.ui.DesignStyle
import com.example.ui.ThemeColor
import com.example.ui.theme.getThemeColors
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFuelScreen(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(2) } // 0 = Trends, 1 = My Trip, 2 = Near Me (Default Map), 3 = Favorites, 4 = Settings
    var activeStationForDetails by remember { mutableStateOf<FuelStation?>(null) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var showFuelWatchInfoDialog by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(true) }
    var isProductMenuExpanded by remember { mutableStateOf(false) }

    val stations by viewModel.filteredStations.collectAsState()
    val favorites by viewModel.favoriteStations.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val stats by viewModel.pricingStats.collectAsState()
    val trendPrices by viewModel.trendPrices.collectAsState()
    val trendLabels = remember {
        val labels = mutableListOf<String>()
        val tz = TimeZone.getTimeZone("Australia/Perth")
        for (i in -6..-1) {
            val cal = Calendar.getInstance(tz)
            cal.add(Calendar.DAY_OF_YEAR, i)
            val label = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> ""
            }
            labels.add(label)
        }
        labels.add("Today")
        labels
    }

    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedSuburb by viewModel.selectedSuburb.collectAsState()
    val tripStartSuburb by viewModel.tripStartSuburb.collectAsState()
    val tripEndSuburb by viewModel.tripEndSuburb.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    val allLoadedStations by viewModel.stations.collectAsState()
    val mapStations = remember(allLoadedStations, selectedBrand, searchQuery) {
        var list = allLoadedStations
        if (selectedBrand.isNotBlank()) {
            list = list.filter { it.brand.equals(selectedBrand, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.tradingName.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }
    
    val designStyle by viewModel.designStyle.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Dynamic app-bar container colors based on design style
    val appBarContainerColor = when (designStyle) {
        DesignStyle.GLASSMORPHIC -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        DesignStyle.FUTURISTIC -> Color(0xFF06070D)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    val appBarBorderModifier = when (designStyle) {
        DesignStyle.GLASSMORPHIC -> Modifier.border(1.dp, Color.White.copy(alpha = 0.15f))
        DesignStyle.FUTURISTIC -> Modifier.border(2.dp, Color(0xFF00F0FF))
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectedTab == 2) {
                        // Near Me: list/map toggle on compact screen
                        BoxWithConstraints {
                            if (maxWidth < 600.dp) {
                                IconButton(onClick = { isMapView = !isMapView }) {
                                    Icon(
                                        imageVector = if (isMapView) Icons.Default.FormatListBulleted else Icons.Default.Map,
                                        contentDescription = "Toggle View",
                                        tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalGasStation,
                                        contentDescription = null,
                                        tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Filled.LocalGasStation,
                                contentDescription = null,
                                tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val screenTitleText = when (selectedTab) {
                            0 -> "Price Trends"
                            1 -> "Trip Planner"
                            2 -> "Near Me"
                            3 -> "My Watchlist"
                            else -> "App Settings"
                        }
                        Text(
                            text = screenTitleText,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                            fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default
                        )

                        if (selectedTab in 0..3) {
                            Box(modifier = Modifier.wrapContentSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { isProductMenuExpanded = true }
                                        .background(
                                            color = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF).copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = selectedProduct.displayName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                                        fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Choose Fuel Type",
                                        tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = isProductMenuExpanded,
                                    onDismissRequest = { isProductMenuExpanded = false }
                                ) {
                                    FuelProduct.values().forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text(prod.displayName) },
                                            onClick = {
                                                viewModel.setProduct(prod)
                                                isProductMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFuelWatchInfoDialog = true },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "WA Fuel Watch Info",
                            tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshFuelPrices() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarContainerColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = appBarContainerColor,
                tonalElevation = 0.dp,
                modifier = Modifier.then(appBarBorderModifier)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Trends") },
                    label = { Text("Trends", fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default) },
                    modifier = Modifier.testTag("tab_trends")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AltRoute, contentDescription = "My Trip") },
                    label = { Text("My Trip", fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default) },
                    modifier = Modifier.testTag("tab_trip")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Place, contentDescription = "Near Me") },
                    label = { Text("Near Me", fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default) },
                    modifier = Modifier.testTag("tab_near_me")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorites"
                        )
                    },
                    label = { Text("Favourites", fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default) },
                    modifier = Modifier.testTag("tab_favorites")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontFamily = if (designStyle == DesignStyle.FUTURISTIC) FontFamily.Monospace else FontFamily.Default) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isFoldUnfolded = maxWidth >= 600.dp

            // Background Canvas based on DesignStyle
            StyledBackground(designStyle = designStyle)

            Column(modifier = Modifier.fillMaxSize()) {
                // Today/Tomorrow Quick Filter Header shown for Near Me / Map tab
                if (selectedTab == 2) {
                    StatsAndQuickFiltersHeader(
                        stats = stats,
                        selectedDay = selectedDay,
                        isPublished = viewModel.isTomorrowPricesPublished,
                        onDayChange = { viewModel.setDay(it) }
                    )
                }

                // Main Content View Switch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (error != null && stations.isEmpty() && selectedTab != 4 && selectedTab != 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = error ?: "An error occurred",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.refreshFuelPrices() },
                                    modifier = Modifier.testTag("retry_button")
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        when (selectedTab) {
                            0 -> TrendsView(
                                stations = stations,
                                stats = stats,
                                selectedProduct = selectedProduct,
                                trendPrices = trendPrices,
                                trendLabels = trendLabels,
                                designStyle = designStyle,
                                isFoldUnfolded = isFoldUnfolded
                            )
                            1 -> MyTripView(
                                stations = allLoadedStations,
                                suburbs = viewModel.allSuburbs.collectAsState().value,
                                designStyle = designStyle,
                                onStationClick = { activeStationForDetails = it },
                                isFoldUnfolded = isFoldUnfolded,
                                favoriteIds = favoriteIds,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                userLocation = viewModel.userLocation.collectAsState().value,
                                onGpsClick = { viewModel.updateUserLocation() },
                                startSuburb = tripStartSuburb,
                                endSuburb = tripEndSuburb,
                                onStartSuburbChange = { viewModel.setTripStartSuburb(it) },
                                onEndSuburbChange = { viewModel.setTripEndSuburb(it) }
                            )
                            2 -> NearMeView(
                                stations = stations,
                                allStations = mapStations,
                                favoriteIds = favoriteIds,
                                isMapView = isMapView,
                                isFoldUnfolded = isFoldUnfolded,
                                designStyle = designStyle,
                                onStationClick = { activeStationForDetails = it },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                selectedBrand = selectedBrand,
                                onBrandChange = { viewModel.setBrand(it) },
                                selectedSuburb = selectedSuburb,
                                onSuburbChange = { viewModel.setSuburb(it) },
                                sortBy = sortBy,
                                onSortChange = { viewModel.setSortBy(it) },
                                brands = viewModel.availableBrands,
                                suburbs = viewModel.allSuburbs.collectAsState().value,
                                stats = stats,
                                userLocation = viewModel.userLocation.collectAsState().value,
                                onGpsClick = { viewModel.updateUserLocation() }
                            )
                            3 -> FavoritesListView(
                                favoriteStations = favorites,
                                currentLivePrices = stations,
                                onStationClick = { activeStationForDetails = it },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                isFoldUnfolded = isFoldUnfolded,
                                designStyle = designStyle
                            )
                            4 -> SettingsAndInfoView(
                                currentTheme = themeMode,
                                onThemeSelect = { viewModel.setThemeMode(it) },
                                currentDesignStyle = designStyle,
                                onDesignStyleSelect = { viewModel.setDesignStyle(it) },
                                isFoldUnfolded = isFoldUnfolded,
                                currentThemeColor = themeColor,
                                onThemeColorSelect = { viewModel.setThemeColor(it) }
                            )
                        }
                    }
                }
            }

            // Bottom sheet or full screen overlay for Station Details
            activeStationForDetails?.let { station ->
                // Check current live price in case it's a favorited item
                val livePrice = stations.find { it.id == station.id }?.price ?: station.price
                StationDetailsSheet(
                    station = station.copy(price = livePrice),
                    isFavorite = favoriteIds.contains(station.id),
                    onClose = { activeStationForDetails = null },
                    onFavoriteToggle = { viewModel.toggleFavorite(station) }
                )
            }

            if (showThemeSettings) {
                ThemeSettingsSheet(
                    currentTheme = themeMode,
                    onThemeSelect = { viewModel.setThemeMode(it) },
                    currentThemeColor = themeColor,
                    onThemeColorSelect = { viewModel.setThemeColor(it) },
                    onClose = { showThemeSettings = false }
                )
            }

            if (showFuelWatchInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showFuelWatchInfoDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "WA FuelWatch Rule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "WA Law requires retail fuel outlets to lock and announce tomorrow's prices at exactly 2:30 PM (AWST) daily. Our 'Tomorrow' toggle allows you to see upcoming rates so you can decide whether it is best to refuel today or wait until tomorrow to save.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showFuelWatchInfoDialog = false }) {
                            Text("Got it")
                        }
                    }
                )
            }
        }
    }
}

// 1. Stats and Quick Filters Header
@Composable
fun StatsAndQuickFiltersHeader(
    stats: PricingStats,
    selectedDay: String,
    isPublished: Boolean,
    onDayChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day selector + metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today / Tomorrow toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ElevatedFilterChip(
                        selected = selectedDay == "today",
                        onClick = { onDayChange("today") },
                        label = { Text("Today") },
                        modifier = Modifier.testTag("day_today")
                    )
                    
                    Box {
                        ElevatedFilterChip(
                            selected = selectedDay == "tomorrow",
                            onClick = { onDayChange("tomorrow") },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.testTag("day_tomorrow")
                        )
                        // Dot indicating tomorrow's prices are now loaded
                        if (isPublished) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }

                // Price Metrics Summary
                if (stats.lowest > 0.0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Lowest", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${stats.lowest}¢",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 14.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Average", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format("%.1f", stats.average)}¢",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. Search & Filter Bar
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchBarAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedBrand: String,
    onBrandChange: (String) -> Unit,
    selectedSuburb: String,
    onSuburbChange: (String) -> Unit,
    sortBy: SortOption,
    onSortChange: (SortOption) -> Unit,
    brands: List<String>,
    suburbs: List<String>
) {
    var isBrandMenuExpanded by remember { mutableStateOf(false) }
    var isSuburbDialogOpen by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search suburb, station, address...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("search_bar")
        )

        // Filter chips dropdown toggles in FlowRow for responsiveness
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Brand filter dropdown
            Box {
                AssistChip(
                    onClick = { isBrandMenuExpanded = true },
                    label = { Text(if (selectedBrand.isBlank()) "All Brands" else selectedBrand) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.testTag("filter_brand")
                )
                DropdownMenu(
                    expanded = isBrandMenuExpanded,
                    onDismissRequest = { isBrandMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Brands") },
                        onClick = {
                            onBrandChange("")
                            isBrandMenuExpanded = false
                        }
                    )
                    brands.forEach { brand ->
                        DropdownMenuItem(
                            text = { Text(brand) },
                            onClick = {
                                onBrandChange(brand)
                                isBrandMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Suburb filter button triggering searchable dialog
            Box {
                AssistChip(
                    onClick = { isSuburbDialogOpen = true },
                    label = { Text(if (selectedSuburb.isBlank()) "All Suburbs" else selectedSuburb) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.testTag("filter_suburb")
                )
            }

            // Sorting dropdown
            Box {
                AssistChip(
                    onClick = { isSortMenuExpanded = true },
                    label = { Text(sortBy.displayName) },
                    leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.testTag("filter_sort")
                )
                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false }
                ) {
                    SortOption.values().forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.displayName) },
                            onClick = {
                                onSortChange(opt)
                                isSortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (isSuburbDialogOpen) {
        AlertDialog(
            onDismissRequest = { isSuburbDialogOpen = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isSuburbDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Search Suburb or Postcode", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var localQuery by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = { localQuery = it },
                        placeholder = { Text("Type suburb name or postcode...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (localQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { localQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )
                    
                    val filteredSuggestions = remember(localQuery, suburbs) {
                        if (localQuery.isBlank()) {
                            suburbs
                        } else {
                            val q = localQuery.trim()
                            if (q.all { it.isDigit() }) {
                                listOf(q)
                            } else {
                                suburbs.filter { it.contains(q, ignoreCase = true) }
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedSuburb.isNotEmpty()) {
                            item {
                                Surface(
                                    onClick = {
                                        onSuburbChange("")
                                        isSuburbDialogOpen = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Clear filter: $selectedSuburb", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        
                        items(filteredSuggestions) { item ->
                            val isNumeric = item.all { it.isDigit() }
                            val displayText = if (isNumeric) "Postcode: $item" else item.uppercase()
                            
                            DropdownMenuItem(
                                text = { Text(displayText, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onSuburbChange(item)
                                    isSuburbDialogOpen = false
                                }
                            )
                        }
                        
                        val cleanQuery = localQuery.trim().uppercase()
                        if (cleanQuery.isNotEmpty() && !filteredSuggestions.contains(cleanQuery) && !cleanQuery.all { it.isDigit() }) {
                            item {
                                DropdownMenuItem(
                                    text = { Text("Search suburb: \"$cleanQuery\"", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onSuburbChange(cleanQuery)
                                        isSuburbDialogOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

// 3. Station Card Item
@Composable
fun FuelStationCard(
    station: FuelStation,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    designStyle: DesignStyle = DesignStyle.MATERIAL_3
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClick() }
            .applyDesignCard(designStyle, isDark)
            .testTag("station_card_${station.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brand Badge Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getBrandColor(station.brand)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = station.brand.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // Station details
                Column {
                    Text(
                        text = station.tradingName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = station.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Price & Favorite Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Price text (e.g. 195.9)
                if (station.price > 0.0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${station.price}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "cents/L",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "N/A",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Star / Watchlist favorite toggle
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("favorite_toggle_${station.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Watchlist",
                        tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 4. List View Container
@Composable
fun FuelListView(
    stations: List<FuelStation>,
    favoriteIds: Set<String>,
    onStationClick: (FuelStation) -> Unit,
    onFavoriteToggle: (FuelStation) -> Unit,
    designStyle: DesignStyle = DesignStyle.MATERIAL_3
) {
    if (stations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No fuel stations match your filter criteria.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(stations, key = { it.id }) { station ->
                FuelStationCard(
                    station = station,
                    isFavorite = favoriteIds.contains(station.id),
                    onClick = { onStationClick(station) },
                    onFavoriteToggle = { onFavoriteToggle(station) },
                    designStyle = designStyle
                )
            }
        }
    }
}

// 5. Watchlist View Container
@Composable
fun FavoritesListView(
    favoriteStations: List<FuelStation>,
    currentLivePrices: List<FuelStation>,
    onStationClick: (FuelStation) -> Unit,
    onFavoriteToggle: (FuelStation) -> Unit,
    isFoldUnfolded: Boolean = false,
    designStyle: DesignStyle = DesignStyle.MATERIAL_3
) {
    if (favoriteStations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Your Watchlist is empty.\nTap the heart icon on any station to pin it here for rapid access!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        if (isFoldUnfolded) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Pane: Watchlist elements
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(favoriteStations) { station ->
                        // Map the favorite item to its current live price if available in the parsed list!
                        val liveStation = currentLivePrices.find { it.id == station.id }
                        val displayPrice = liveStation?.price ?: 0.0
                        FuelStationCard(
                            station = station.copy(price = displayPrice),
                            isFavorite = true,
                            onClick = { onStationClick(liveStation ?: station) },
                            onFavoriteToggle = { onFavoriteToggle(station) },
                            designStyle = designStyle
                        )
                    }
                }

                // Right Pane: Live Map showing ONLY your favorite stations side-by-side
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    FuelMapView(
                        stations = favoriteStations.map { fav ->
                            val live = currentLivePrices.find { it.id == fav.id }
                            fav.copy(price = live?.price ?: fav.price)
                        },
                        designStyle = designStyle,
                        onStationClick = onStationClick
                    )
                }
            }
        } else {
            // Compact Screen layout: Standard list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favoriteStations) { station ->
                    // Map the favorite item to its current live price if available in the parsed list!
                    val liveStation = currentLivePrices.find { it.id == station.id }
                    val displayPrice = liveStation?.price ?: 0.0
                    FuelStationCard(
                        station = station.copy(price = displayPrice),
                        isFavorite = true,
                        onClick = { onStationClick(liveStation ?: station) },
                        onFavoriteToggle = { onFavoriteToggle(station) },
                        designStyle = designStyle
                    )
                }
            }
        }
    }
}

// 6. Interactive Leaflet Map View inside WebView with Google Maps Tile Integration
@Composable
fun FuelMapView(
    stations: List<FuelStation>,
    designStyle: DesignStyle,
    onStationClick: (FuelStation) -> Unit,
    userLocation: Location? = null,
    onGpsClick: (() -> Unit)? = null,
    selectedSuburb: String = "",
    routeStations: List<FuelStation> = emptyList()
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var forceCenterTrigger by remember { mutableStateOf(0) }

    // Request launcher for GPS location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if ((fineGranted || coarseGranted) && onGpsClick != null) {
            onGpsClick()
        }
    }

    LaunchedEffect(userLocation, forceCenterTrigger) {
        userLocation?.let { loc ->
            val forceCenter = forceCenterTrigger > 0
            val isInWA = loc.latitude in -36.0..-13.0 && loc.longitude in 113.0..129.0
            webViewRef?.evaluateJavascript("setUserLocation(${loc.latitude}, ${loc.longitude}, ${forceCenter || isInWA});", null)
        }
    }

    LaunchedEffect(selectedSuburb) {
        if (selectedSuburb.isNotBlank()) {
            webViewRef?.evaluateJavascript("centerOnSuburb('$selectedSuburb');", null)
        }
    }

    // Serialize route coordinates into lightweight JSON array for injecting into JS Leaflet map
    val routePointsJson = remember(routeStations) {
        val list = routeStations.map { s ->
            "[ ${s.latitude}, ${s.longitude} ]"
        }
        "[ ${list.joinToString(", ")} ]"
    }

    LaunchedEffect(routePointsJson) {
        if (routeStations.isNotEmpty()) {
            webViewRef?.evaluateJavascript("drawRoute($routePointsJson);", null)
        } else {
            webViewRef?.evaluateJavascript("drawRoute([]);", null)
        }
    }

    // Serialize station properties into lightweight JSON array for injecting into JS Leaflet map
    val stationsJson = remember(stations) {
        val list = stations.map { s ->
            """
            {
                "id": "${s.id}",
                "name": "${s.tradingName.replace("'", "\\'").replace("\"", "\\\"")}",
                "address": "${s.address.replace("'", "\\'").replace("\"", "\\\"")}",
                "brand": "${s.brand}",
                "price": ${s.price},
                "lat": ${s.latitude},
                "lng": ${s.longitude},
                "phone": "${s.phone}"
            }
            """.trimIndent()
        }
        "[ ${list.joinToString(", ")} ]"
    }

    // Google Maps Roadmap tile server (works perfectly without API keys!)
    val tileLayerUrl = "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"

    // Custom pins styling depending on theme and design style!
    val pinStyles = when (designStyle) {
        DesignStyle.GLASSMORPHIC -> {
            val bgCol = if (isDark) "rgba(30, 30, 35, 0.75)" else "rgba(255, 255, 255, 0.85)"
            val textCol = if (isDark) "#e2e2e6" else "#1a1c1e"
            val borderCol = if (isDark) "rgba(255, 255, 255, 0.25)" else "rgba(0, 0, 0, 0.15)"
            """
            .price-pin {
                border-radius: 12px;
                padding: 4px 10px;
                font-size: 11px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                font-weight: 700;
                backdrop-filter: blur(8px);
                -webkit-backdrop-filter: blur(8px);
                background-color: $bgCol;
                color: $textCol;
                border: 1px solid $borderCol;
                box-shadow: 0 4px 12px rgba(0,0,0,0.12);
                text-align: center;
                white-space: nowrap;
                display: inline-block;
            }
            .price-pin.cheap {
                background-color: rgba(0, 97, 164, 0.85);
                color: white;
                border: 1.5px solid rgba(209, 228, 255, 0.6);
                box-shadow: 0 4px 12px rgba(0, 97, 164, 0.3);
            }
            """
        }
        DesignStyle.FUTURISTIC -> {
            """
            .price-pin {
                border-radius: 4px;
                padding: 3px 8px;
                font-size: 11px;
                font-family: 'JetBrains Mono', 'Courier New', monospace;
                font-weight: 900;
                background-color: #0b0c10;
                color: #c5a1ff;
                border: 1.5px solid #a155ff;
                box-shadow: 0 0 8px #a155ff;
                text-align: center;
                white-space: nowrap;
                display: inline-block;
            }
            .price-pin.cheap {
                color: #00f0ff;
                border: 1.5px solid #00f0ff;
                box-shadow: 0 0 10px #00f0ff;
            }
            .price-pin.medium {
                color: #ff007f;
                border: 1.5px solid #ff007f;
                box-shadow: 0 0 10px #ff007f;
            }
            .price-pin.expensive {
                color: #ffffff;
                border: 1.5px solid #ffffff;
                box-shadow: 0 0 5px #ffffff;
            }
            """
        }
        DesignStyle.MATERIAL_EXPERIENCE -> {
            """
            .price-pin {
                border-radius: 12px;
                padding: 5px 11px;
                font-size: 12px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                font-weight: 800;
                background-color: ${if (isDark) "#1E293B" else "#FFFFFF"};
                color: ${if (isDark) "#F8FAFC" else "#0F172A"};
                border: 2px solid ${if (isDark) "#38BDF8" else "#0284C7"};
                box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
                text-align: center;
                white-space: nowrap;
                display: inline-block;
            }
            .price-pin.cheap {
                background-color: #10B981;
                color: white;
                border: 2px solid #34D399;
                box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
            }
            .price-pin.medium {
                background-color: ${if (isDark) "#1E293B" else "#FFFFFF"};
                color: ${if (isDark) "#F8FAFC" else "#0F172A"};
                border: 2.5px solid ${if (isDark) "#6366F1" else "#4F46E5"};
            }
            .price-pin.expensive {
                background-color: ${if (isDark) "#334155" else "#F1F5F9"};
                color: ${if (isDark) "#94A3B8" else "#64748B"};
                border: 2px solid ${if (isDark) "#475569" else "#CBD5E1"};
            }
            """
        }
        DesignStyle.IOS_26_GLASS -> {
            """
            .price-pin {
                border-radius: 16px;
                padding: 4px 10px;
                font-size: 11px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                font-weight: 700;
                backdrop-filter: blur(12px);
                -webkit-backdrop-filter: blur(12px);
                background-color: ${if (isDark) "rgba(17, 24, 39, 0.7)" else "rgba(255, 255, 255, 0.85)"};
                color: ${if (isDark) "#F3F4F6" else "#111827"};
                border: 1.5px solid ${if (isDark) "rgba(255, 255, 255, 0.35)" else "rgba(0, 0, 0, 0.25)"};
                box-shadow: 0 8px 16px rgba(0,0,0,0.15);
                text-align: center;
                white-space: nowrap;
                display: inline-block;
            }
            .price-pin.cheap {
                background-color: rgba(16, 185, 129, 0.8);
                color: white;
                border: 1.5px solid rgba(255, 255, 255, 0.5);
                box-shadow: 0 6px 14px rgba(16, 185, 129, 0.35);
            }
            .price-pin.medium {
                background-color: ${if (isDark) "rgba(59, 130, 246, 0.8)" else "rgba(59, 130, 246, 0.85)"};
                color: white;
                border: 1.5px solid rgba(255, 255, 255, 0.5);
                box-shadow: 0 6px 14px rgba(59, 130, 246, 0.35);
            }
            .price-pin.expensive {
                background-color: ${if (isDark) "rgba(107, 114, 128, 0.7)" else "rgba(243, 244, 246, 0.8)"};
                color: ${if (isDark) "#D1D5DB" else "#4B5563"};
                border: 1.5px solid ${if (isDark) "rgba(255, 255, 255, 0.2)" else "rgba(0, 0, 0, 0.15)"};
            }
            """
        }
        else -> {
            // Material 3 Standard
            val bgCol = if (isDark) "#2b2d31" else "#ffffff"
            val textCol = if (isDark) "#e2e2e6" else "#1a1c1e"
            val borderCol = if (isDark) "#44474e" else "#e1e2ec"
            """
            .price-pin {
                border-radius: 8px;
                padding: 4px 8px;
                font-size: 11px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                font-weight: 800;
                box-shadow: 0 4px 8px rgba(0,0,0,0.15);
                text-align: center;
                white-space: nowrap;
                display: inline-block;
                background-color: $bgCol;
                color: $textCol;
                border: 2px solid $borderCol;
            }
            .price-pin.cheap {
                background-color: #0061a4;
                color: white;
                border: 2px solid #d1e4ff;
                box-shadow: 0 4px 8px rgba(0, 97, 164, 0.35);
            }
            .price-pin.medium {
                background-color: $bgCol;
                color: $textCol;
                border: 2px solid $borderCol;
            }
            .price-pin.expensive {
                background-color: ${if (isDark) "#1a1c1e" else "#f2f0f4"};
                color: ${if (isDark) "#8e9099" else "#44474e"};
                border: 2px solid $borderCol;
            }
            """
        }
    }

    // Embed Leaflet HTML template inside string
    val mapHtml = remember(isDark, designStyle) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: ${if (isDark) "#1a1c1e" else "#fdfbff"};
                }
                /* Invert light tiles in dark theme to generate Google Maps Dark Mode! */
                ${if (isDark) """
                .leaflet-tile {
                    filter: invert(90%) hue-rotate(180deg) brightness(105%) contrast(95%);
                }
                """ else ""}
                $pinStyles
                
                /* Custom styles for GPS user location blue dot */
                .user-location-pulse {
                    display: block;
                    width: 20px;
                    height: 20px;
                }
                .pulse-dot {
                    width: 12px;
                    height: 12px;
                    background-color: #0078FF;
                    border: 2.5px solid white;
                    border-radius: 50%;
                    box-shadow: 0 0 8px rgba(0, 120, 255, 0.6);
                    animation: pulse 1.6s infinite;
                }
                @keyframes pulse {
                    0% {
                        box-shadow: 0 0 0 0px rgba(0, 120, 255, 0.7);
                    }
                    70% {
                        box-shadow: 0 0 0 10px rgba(0, 120, 255, 0);
                    }
                    100% {
                        box-shadow: 0 0 0 0px rgba(0, 120, 255, 0);
                    }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                // Standard Map initialized centering on Perth
                window.mapInstance = null;
                window.markersGroup = null;
                window.allStations = [];
                window.stationsData = [];
                window.userLocationMarker = null;
                window.currentCenter = [-31.9505, 115.8605]; // Default to Perth CBD
                window.hasSetInitialView = false;

                function getDistance(lat1, lon1, lat2, lon2) {
                    var R = 6371; // km
                    var dLat = (lat2 - lat1) * Math.PI / 180;
                    var dLon = (lon2 - lon1) * Math.PI / 180;
                    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
                    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                    return R * c;
                }

                function initMap() {
                    try {
                        if (typeof L === 'undefined') {
                            console.error("Leaflet (L) is not loaded yet.");
                            setTimeout(initMap, 200);
                            return;
                        }
                        if (window.mapInstance) {
                            return; // already initialized
                        }
                        
                        window.mapInstance = L.map('map', { zoomControl: false }).setView(window.currentCenter, 13);
                        
                        L.tileLayer('$tileLayerUrl', {
                            attribution: '&copy; Google Maps standard tiles',
                            maxZoom: 19
                        }).addTo(window.mapInstance);

                        window.markersGroup = L.layerGroup().addTo(window.mapInstance);

                        // Listen to moveend event to load markers dynamically in real-time as the map moves
                        window.mapInstance.on('moveend', function() {
                            updateVisibleMarkers();
                        });

                        // If markers were loaded prior to map initialization, draw them immediately!
                        if (window.stationsData && window.stationsData.length > 0) {
                            loadMarkers(window.stationsData);
                        }

                        // Force invalidateSize to prevent black or gray screens due to hidden rendering measurements
                        setTimeout(function() {
                            if (window.mapInstance) {
                                window.mapInstance.invalidateSize();
                            }
                        }, 300);
                    } catch (e) {
                        console.error("Failed to initialize Leaflet Map: " + e.message);
                    }
                }

                function setUserLocation(lat, lng, shouldCenter) {
                    if (!window.mapInstance || typeof L === 'undefined') return;
                    
                    window.currentCenter = [lat, lng];
                    
                    if (window.userLocationMarker) {
                        window.mapInstance.removeLayer(window.userLocationMarker);
                    }
                    
                    var pulseIcon = L.divIcon({
                        className: 'user-location-pulse',
                        html: '<div class="pulse-dot"></div>',
                        iconSize: [20, 20],
                        iconAnchor: [10, 10]
                    });
                    
                    window.userLocationMarker = L.marker([lat, lng], { icon: pulseIcon }).addTo(window.mapInstance);
                    
                    if (shouldCenter || !window.hasSetInitialView) {
                        window.mapInstance.setView([lat, lng], 13);
                        window.hasSetInitialView = true;
                    }
                }

                function updateVisibleMarkers() {
                    if (!window.mapInstance || !window.markersGroup || !window.allStations) return;
                    
                    var bounds = window.mapInstance.getBounds();
                    var visibleStations = window.allStations.filter(function(s) {
                        if (!s.lat || !s.lng) return false;
                        return bounds.contains(L.latLng(s.lat, s.lng));
                    });
                    
                    drawMarkersList(visibleStations);
                }

                function drawMarkersList(stations) {
                    if (!window.mapInstance || !window.markersGroup || typeof L === 'undefined') return;
                    
                    var map = window.mapInstance;
                    var markersGroup = window.markersGroup;
                    markersGroup.clearLayers();

                    if (!stations || stations.length === 0) return;

                    // Calculate price quantiles to color pins dynamically (cheap, medium, expensive)
                    var prices = stations.map(function(s) { return s.price; }).filter(function(p) { return p > 0; });
                    prices.sort(function(a, b) { return a - b; });

                    var cheapBoundary = prices[Math.floor(prices.length * 0.25)] || 0;
                    var expensiveBoundary = prices[Math.floor(prices.length * 0.75)] || 0;

                    stations.forEach(function(s) {
                        if (!s.lat || !s.lng || s.lat === 0 || s.lng === 0) return;

                        var pinType = "medium";
                        if (s.price > 0) {
                            if (s.price <= cheapBoundary) {
                                pinType = "cheap";
                            } else if (s.price >= expensiveBoundary) {
                                pinType = "expensive";
                            }
                        }

                        var customIcon = L.divIcon({
                            className: 'custom-leaflet-icon',
                            html: "<div class='price-pin " + pinType + "'>" + (s.price > 0 ? s.price : s.brand) + "</div>",
                            iconSize: [44, 26],
                            iconAnchor: [22, 13]
                        });

                        var marker = L.marker([s.lat, s.lng], { icon: customIcon }).addTo(markersGroup);
                        
                        marker.on('click', function() {
                            AndroidInterface.onStationClick(JSON.stringify(s));
                        });
                    });
                }

                function centerOnSuburb(suburbName) {
                    if (!window.mapInstance || !window.allStations || !suburbName) return;
                    var subStations = window.allStations.filter(function(s) {
                        return s.location && s.location.toLowerCase() === suburbName.toLowerCase();
                    });
                    if (subStations.length > 0) {
                        var sumLat = 0, sumLng = 0;
                        subStations.forEach(function(s) {
                            sumLat += s.lat;
                            sumLng += s.lng;
                        });
                        var avgLat = sumLat / subStations.length;
                        var avgLng = sumLng / subStations.length;
                        window.mapInstance.setView([avgLat, avgLng], 13);
                        window.hasSetInitialView = true;
                    }
                }

                window.routePolyline = null;
                function drawRoute(latLngs) {
                    if (!window.mapInstance || typeof L === 'undefined') return;
                    if (window.routePolyline) {
                        window.mapInstance.removeLayer(window.routePolyline);
                        window.routePolyline = null;
                    }
                    if (!latLngs || latLngs.length < 2) return;
                    
                    var routeColor = '#0061A4';
                    var routeWeight = 5;
                    var styleName = '$designStyle';
                    if (styleName === 'FUTURISTIC') {
                        routeColor = '#00F0FF';
                        routeWeight = 6;
                    } else if (styleName === 'NEO_BRUTALISM') {
                        routeColor = '#000000';
                        routeWeight = 8;
                    } else if (styleName === 'MATERIAL_EXPERIENCE') {
                        routeColor = '#6366F1';
                        routeWeight = 6;
                    } else if (styleName === 'IOS_26_GLASS') {
                        routeColor = '#EC4899';
                        routeWeight = 5;
                    } else if (styleName === 'GLASSMORPHIC') {
                        routeColor = '#818CF8';
                        routeWeight = 5;
                    }
                    
                    window.routePolyline = L.polyline(latLngs, {
                        color: routeColor,
                        weight: routeWeight,
                        opacity: 0.85,
                        lineJoin: 'round'
                    }).addTo(window.mapInstance);
                    
                    try {
                        var bounds = L.latLngBounds(latLngs);
                        window.mapInstance.fitBounds(bounds, { padding: [50, 50] });
                        window.hasSetInitialView = true;
                    } catch (e) {
                        console.error(e);
                    }
                }

                function loadMarkers(stations) {
                    window.allStations = stations;
                    window.stationsData = stations;
                    
                    if (!window.mapInstance || typeof L === 'undefined') return;

                    // Always make sure Leaflet recalculates map size so it is fully visible and not black
                    window.mapInstance.invalidateSize();

                    if (!window.hasSetInitialView) {
                        var defaultCenter = [-31.9505, 115.8605]; // Perth CBD
                        window.mapInstance.setView(defaultCenter, 13);
                        window.hasSetInitialView = true;
                    }

                    updateVisibleMarkers();
                }

                // Run init on window load
                window.onload = function() {
                    initMap();
                };
                
                // Extra safety: execute initialization immediately if document is already loaded
                if (document.readyState === 'complete' || document.readyState === 'interactive') {
                    initMap();
                } else {
                    document.addEventListener('DOMContentLoaded', initMap);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("FuelMapJS", "${consoleMessage?.messageLevel()}: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            evaluateJavascript("loadMarkers($stationsJson);", null)
                            if (routeStations.isNotEmpty()) {
                                evaluateJavascript("drawRoute($routePointsJson);", null)
                            } else if (selectedSuburb.isNotBlank()) {
                                evaluateJavascript("centerOnSuburb('$selectedSuburb');", null)
                            }
                            userLocation?.let { loc ->
                                val isInWA = loc.latitude in -36.0..-13.0 && loc.longitude in 113.0..129.0
                                evaluateJavascript("setUserLocation(${loc.latitude}, ${loc.longitude}, $isInWA);", null)
                            }
                        }
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onStationClick(stationJson: String) {
                            try {
                                val json = JSONObject(stationJson)
                                val clickedId = json.getString("id")
                                val matchedStation = stations.find { it.id == clickedId }
                                if (matchedStation != null) {
                                    post { onStationClick(matchedStation) }
                                }
                            } catch (e: Exception) {
                                Log.e("FuelMapView", "Error handling JS marker click callback: ${e.message}")
                            }
                        }
                    }, "AndroidInterface")

                    tag = isDark
                    loadDataWithBaseURL("file:///android_asset/", mapHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webViewInstance ->
                webViewRef = webViewInstance
                val lastIsDark = webViewInstance.tag as? Boolean
                if (lastIsDark != isDark) {
                    webViewInstance.tag = isDark
                    webViewInstance.loadDataWithBaseURL("file:///android_asset/", mapHtml, "text/html", "UTF-8", null)
                } else {
                    webViewInstance.evaluateJavascript("loadMarkers($stationsJson);", null)
                    if (selectedSuburb.isNotBlank()) {
                        webViewInstance.evaluateJavascript("centerOnSuburb('$selectedSuburb');", null)
                    }
                    userLocation?.let { loc ->
                        val isInWA = loc.latitude in -36.0..-13.0 && loc.longitude in 113.0..129.0
                        webViewInstance.evaluateJavascript("setUserLocation(${loc.latitude}, ${loc.longitude}, $isInWA);", null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("web_map")
        )

        // GPS Button overlay shown when onGpsClick callback is provided
        if (onGpsClick != null) {
            FloatingActionButton(
                onClick = {
                    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasFine || hasCoarse) {
                        forceCenterTrigger++
                        onGpsClick()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .testTag("gps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 7. Info/Help View
@Composable
fun WAInfoView() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Western Australia FuelWatch Rule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The 2:30 PM Tomorrow Prices Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Under WA law, fuel retailers must lock in their retail fuel prices for the entire next day, starting at 6:00 AM.\n\nEvery day at 2:30 PM (AWST), FuelWatch publishes the prices for tomorrow. Use our Tomorrow toggle to compare and plan whether to fuel up today or wait!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Understanding Pricing Colors on Map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cheap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(
                    text = "Refers to the lowest 25% of station prices for the selected fuel product. Fill up here to save!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE65100)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Medium", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(
                    text = "Refers to mid-range pricing. Average of prices on the market today.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFC62828)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("High", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(
                    text = "Refers to the highest 25% of station prices. Avoid unless urgent!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// 8. Detailed Overlay / Bottom Sheet for Station details
@Composable
fun StationDetailsSheet(
    station: FuelStation,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // Prevent click-through closing
                .testTag("details_bottom_sheet"),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drag handle visual indicator
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.tradingName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = station.brand,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Price display
                    if (station.price > 0.0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${station.price}",
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "cents/litre",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Station metadata address/phone
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Address", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${station.address}, ${station.location}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (station.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Contact Phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(station.phone, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (station.siteFeatures.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Site Amenities", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(station.siteFeatures, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Interaction Buttons (Favorite, Navigate, Close)
                val context = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("details_favorite_button"),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isFavorite) "Saved" else "Save", fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            try {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                try {
                                    // Fallback to generic geo intent if Google Maps app package is not explicitly handled
                                    val fallbackUri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${Uri.encode(station.tradingName + ", " + station.address)}")
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                                    context.startActivity(fallbackIntent)
                                } catch (err: Exception) {
                                    Log.e("Navigation", "Error fallback routing: ${err.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("details_navigate_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Navigate", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }

                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("details_close_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Close", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Helper to resolve brand colors to add visual flair to the cards
fun getBrandColor(brandName: String): Color {
    return when (brandName.lowercase()) {
        "bp" -> Color(0xFF008542) // Green/yellow
        "shell" -> Color(0xFFFCD116) // Shell Yellow
        "ampol" -> Color(0xFF003882) // Ampol Blue
        "coles express" -> Color(0xFFD11215) // Coles Red
        "7-eleven" -> Color(0xFF008060) // 7-Eleven Teal
        "united" -> Color(0xFF00509E) // United Blue
        "puma" -> Color(0xFFE21836) // Puma Red
        "liberty" -> Color(0xFF004B87) // Liberty Blue
        "vibe" -> Color(0xFF7A2A85) // Vibe Purple
        else -> Color(0xFF6200EE) // Fallback default purple
    }
}

// 9. Glassmorphic Theme Selection Bottom Sheet (iOS Style)
@Composable
fun ThemeSettingsSheet(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    currentThemeColor: ThemeColor,
    onThemeColorSelect: (ThemeColor) -> Unit,
    onClose: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .testTag("theme_settings_sheet"),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Display Style",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    val themes = listOf(
                        Triple(AppTheme.SYSTEM, "System Default", Icons.Outlined.BrightnessAuto),
                        Triple(AppTheme.LIGHT, "Professional Light", Icons.Outlined.LightMode),
                        Triple(AppTheme.DARK, "Premium Dark", Icons.Outlined.DarkMode)
                    )

                    themes.forEachIndexed { index, (theme, label, icon) ->
                        val isSelected = currentTheme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onThemeSelect(theme) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (index < themes.lastIndex) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Theme Accent Color",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeColor.values().forEach { col ->
                        val isSelected = currentThemeColor == col
                        val mappedColor = getThemeColors(col, isDark).first
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(mappedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onThemeColorSelect(col) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 10. Design Style Layout & Card Helpers
@Composable
fun StyledBackground(designStyle: DesignStyle) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    when (designStyle) {
        DesignStyle.GLASSMORPHIC -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                // Draw elegant, pastel frosted glowing circles
                drawCircle(
                    color = if (isDark) Color(0x354C1A5C) else Color(0x55EAD9FF),
                    radius = width * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(width * 0.15f, height * 0.15f)
                )
                drawCircle(
                    color = if (isDark) Color(0x35123954) else Color(0x55C6E6FF),
                    radius = width * 0.65f,
                    center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.75f)
                )
                drawCircle(
                    color = if (isDark) Color(0x35541838) else Color(0x55FFD2E4),
                    radius = width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.45f)
                )
            }
        }
        DesignStyle.FUTURISTIC -> {
            val bgColor = if (isDark) Color(0xFF04050A) else Color(0xFFF1F3F6)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                // Subtle glowing cyber grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSpacing = 50.dp.toPx()
                    val gridColor = if (isDark) Color(0x1100F0FF) else Color(0x0600F0FF)
                    
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        x += gridSpacing
                    }
                    
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += gridSpacing
                    }
                }
            }
        }
        DesignStyle.MATERIAL_EXPERIENCE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            } else {
                                listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                            }
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    drawCircle(
                        color = if (isDark) Color(0x2210B981) else Color(0x3334D399),
                        radius = width * 0.5f,
                        center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.2f)
                    )
                    drawCircle(
                        color = if (isDark) Color(0x226366F1) else Color(0x33818CF8),
                        radius = width * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.8f)
                    )
                }
            }
        }
        DesignStyle.IOS_26_GLASS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF07090E) else Color(0xFFF4F6FB))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    drawCircle(
                        color = if (isDark) Color(0x44EC4899) else Color(0x55FBCFE8),
                        radius = width * 0.5f,
                        center = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.3f)
                    )
                    drawCircle(
                        color = if (isDark) Color(0x4406B6D4) else Color(0x55AEDFF7),
                        radius = width * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.7f)
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
fun Modifier.applyDesignCard(
    designStyle: DesignStyle,
    isDark: Boolean,
    neonColor: Color = Color(0xFF00F0FF)
): Modifier {
    return when (designStyle) {
        DesignStyle.GLASSMORPHIC -> {
            val bgColor = if (isDark) Color(0x1AFFFFFF) else Color(0x9EFFFFFF)
            val borderCol = if (isDark) Color(0x24FFFFFF) else Color(0x3D000000)
            this.clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .border(1.dp, borderCol, RoundedCornerShape(24.dp))
        }
        DesignStyle.FUTURISTIC -> {
            val bgColor = if (isDark) Color(0xFF090A11) else Color(0xFFFAFAFD)
            val neonBorder = if (isDark) neonColor.copy(alpha = 0.85f) else neonColor.copy(alpha = 0.6f)
            this.clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(2.dp, neonBorder, RoundedCornerShape(12.dp))
        }
        DesignStyle.MATERIAL_EXPERIENCE -> {
            val bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
            val borderBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
            this.clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(2.dp, borderBrush, RoundedCornerShape(20.dp))
        }
        DesignStyle.IOS_26_GLASS -> {
            val bgColor = if (isDark) Color(0x26111827) else Color(0xCCFFFFFF)
            val borderCol = if (isDark) Color(0x40FFFFFF) else Color(0x60000000)
            this.clip(RoundedCornerShape(28.dp))
                .background(bgColor)
                .border(1.5.dp, borderCol, RoundedCornerShape(28.dp))
        }
        else -> {
            // Material 3 Standard Card
            val bgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            val borderCol = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            this.clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
        }
    }
}

// 11. Redesigned Settings & WA Info View (4th Tab)
@Composable
fun SettingsAndInfoView(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    currentDesignStyle: DesignStyle,
    onDesignStyleSelect: (DesignStyle) -> Unit,
    isFoldUnfolded: Boolean = false,
    currentThemeColor: ThemeColor,
    onThemeColorSelect: (ThemeColor) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    if (isFoldUnfolded) {
        // Unfolded Foldable (Dual-Pane) Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Settings selectors (Design Style & Display Theme)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "App Settings & Design",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Customize themes and visual design styles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 1: UI Design Style
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "UI Design Style",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Switch between four unique handcrafted visual designs. True craftsmanship in every layout!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val stylesList = listOf(
                                Triple(DesignStyle.MATERIAL_3, "Modern Material 3", "Clean standard layout, smooth outlines and default shapes."),
                                Triple(DesignStyle.GLASSMORPHIC, "iOS Glassmorphism", "Elegant frosted glass transparency, double outlines and soft pastel backgrounds."),
                                Triple(DesignStyle.MATERIAL_EXPERIENCE, "Material Experience", "Vibrant, hyper-polished Material 3 experience with rich gradients, rounded elements, and dynamic contrast."),
                                Triple(DesignStyle.IOS_26_GLASS, "iOS 26 Glass", "Premium next-gen glassmorphism with ultra-blurred backdrops, frosted translucent panels, and vibrant accent boundaries.")
                            )

                            stylesList.forEach { (style, label, desc) ->
                                val isSelected = currentDesignStyle == style
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                                        .clickable { onDesignStyleSelect(style) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onDesignStyleSelect(style) },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Display Theme Selection
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrightnessMedium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Display Theme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf(
                                    Triple(AppTheme.SYSTEM, "System", Icons.Default.BrightnessAuto),
                                    Triple(AppTheme.LIGHT, "Light", Icons.Default.LightMode),
                                    Triple(AppTheme.DARK, "Dark", Icons.Default.DarkMode)
                                )

                                themes.forEach { (theme, label, icon) ->
                                    val isSelected = currentTheme == theme
                                    Button(
                                        onClick = { onThemeSelect(theme) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            // Section 2.5: Theme Accent Color Selection
            item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Theme Accent Color",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Personalize your experience by selecting one of our high-contrast, beautiful premium color schemes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemeColor.values().forEach { col ->
                                    val isSelected = currentThemeColor == col
                                    val mappedColor = getThemeColors(col, isDark).first
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(mappedColor.copy(alpha = if (isSelected) 1f else 0.4f))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { onThemeColorSelect(col) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (isDark) Color.Black else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(mappedColor, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Text(
                                text = "Selected Palette: ${currentThemeColor.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Right Pane: WA FuelWatch Rule, Map Legend & App Info
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 3: WA FuelWatch Information & Legend
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Map Color Pin Legend",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Cheap
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 65.dp, height = 28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0061A4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cheap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    text = "Represents the lowest 25% of fuel prices in your search. Highly recommended!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Medium
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 65.dp, height = 28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF2B2D31) else Color(0xFFFFFFFF))
                                        .border(1.dp, if (isDark) Color(0xFF44474E) else Color(0xFFE1E2EC), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Medium", color = if (isDark) Color(0xFFE2E2E6) else Color(0xFF1A1C1E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    text = "Average mid-range market rates.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Expensive
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 65.dp, height = 28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF1A1C1E) else Color(0xFFF2F0F4))
                                        .border(1.dp, if (isDark) Color(0xFF44474E) else Color(0xFFC4C6D0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Expensive", color = if (isDark) Color(0xFF8E9099) else Color(0xFF44474E), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Text(
                                    text = "Highest 25% price range. Avoid unless absolutely urgent!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Section 4: App Version & Legal
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "WA FuelWatch v2.1.0",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Made for Western Australia Motorists\nData sourced in real-time from FuelWatch WA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        // Compact Screen Layout (Single Column Scrollable)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Screen Header
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "App Settings & Design",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Customize themes, visual styles and read FuelWatch info",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: UI Design Style
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "UI Design Style",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Switch between four unique handcrafted visual designs. True craftsmanship in every layout!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val stylesList = listOf(
                            Triple(DesignStyle.MATERIAL_3, "Modern Material 3", "Clean standard layout, smooth outlines and default shapes."),
                            Triple(DesignStyle.GLASSMORPHIC, "iOS Glassmorphism", "Elegant frosted glass transparency, double outlines and soft pastel backgrounds."),
                            Triple(DesignStyle.MATERIAL_EXPERIENCE, "Material Experience", "Vibrant, hyper-polished Material 3 experience with rich gradients, rounded elements, and dynamic contrast."),
                            Triple(DesignStyle.IOS_26_GLASS, "iOS 26 Glass", "Premium next-gen glassmorphism with ultra-blurred backdrops, frosted translucent panels, and vibrant accent boundaries.")
                        )

                        stylesList.forEach { (style, label, desc) ->
                            val isSelected = currentDesignStyle == style
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                                    .clickable { onDesignStyleSelect(style) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onDesignStyleSelect(style) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Display Theme Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Display Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                Triple(AppTheme.SYSTEM, "System", Icons.Default.BrightnessAuto),
                                Triple(AppTheme.LIGHT, "Light", Icons.Default.LightMode),
                                Triple(AppTheme.DARK, "Dark", Icons.Default.DarkMode)
                            )

                            themes.forEach { (theme, label, icon) ->
                                val isSelected = currentTheme == theme
                                Button(
                                    onClick = { onThemeSelect(theme) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2.5: Theme Accent Color Selection
            item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Theme Accent Color",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Personalize your experience by selecting one of our high-contrast, beautiful premium color schemes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemeColor.values().forEach { col ->
                                    val isSelected = currentThemeColor == col
                                    val mappedColor = getThemeColors(col, isDark).first
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(mappedColor.copy(alpha = if (isSelected) 1f else 0.4f))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { onThemeColorSelect(col) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (isDark) Color.Black else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(mappedColor, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Text(
                                text = "Selected Palette: ${currentThemeColor.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

            // Section 3: WA FuelWatch Information & Legend
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Map Color Pin Legend",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Cheap
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 65.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0061A4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cheap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(
                                text = "Represents the lowest 25% of fuel prices in your search. Highly recommended!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Medium
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 65.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF2B2D31) else Color(0xFFFFFFFF))
                                    .border(1.dp, if (isDark) Color(0xFF44474E) else Color(0xFFE1E2EC), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Medium", color = if (isDark) Color(0xFFE2E2E6) else Color(0xFF1A1C1E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(
                                text = "Average mid-range market rates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Expensive
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 65.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF1A1C1E) else Color(0xFFF2F0F4))
                                    .border(1.dp, if (isDark) Color(0xFF44474E) else Color(0xFFC4C6D0), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Expensive", color = if (isDark) Color(0xFF8E9099) else Color(0xFF44474E), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Text(
                                text = "Highest 25% price range. Avoid unless absolutely urgent!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 4: App Version and legal
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WA FuelWatch v2.1.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Made for Western Australia Motorists\nData sourced in real-time from FuelWatch WA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 12. Interactive Trends Analytics View (1st Tab)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsView(
    stations: List<FuelStation>,
    stats: PricingStats,
    selectedProduct: FuelProduct,
    trendPrices: List<Double>,
    trendLabels: List<String>,
    designStyle: DesignStyle,
    isFoldUnfolded: Boolean
) {
    val avgPrice = if (stats.average > 0) stats.average else 185.0
    val days = trendLabels
    val prices = trendPrices
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
    val minPrice = prices.minOrNull() ?: 150.0
    val maxPrice = prices.maxOrNull() ?: 210.0
    val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 10.0
 
    // High quality dynamic advice text based on current price cycles
    val isCheapestNow = avgPrice < (minPrice + priceRange * 0.35)
    val adviceTitle = if (isCheapestNow) "Advice: FILL UP NOW" else "Advice: WAIT IF POSSIBLE"
    val adviceText = if (isCheapestNow) {
        "Prices are currently near the lowest point of the cycle. Refueling today is highly recommended to save the most."
    } else {
        "Prices are on the rise or higher than the weekly average. If you can wait a few days, prices are expected to decrease."
    }
    val adviceColor = if (isCheapestNow) Color(0xFF2E7D32) else Color(0xFFEF6C00)
    val adviceIcon = if (isCheapestNow) Icons.Default.CheckCircle else Icons.Default.Warning
 
    // Shared content composables to prevent duplicate definitions
    val adviceCard = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = adviceColor.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, adviceColor.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = adviceIcon,
                    contentDescription = null,
                    tint = adviceColor,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = adviceTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = adviceColor
                    )
                    Text(
                        text = adviceText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val priceTrendGraph = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "7-Day Price Trend (${selectedProduct.displayName})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val paddingX = 45f
                    val paddingY = 30f
                    
                    val chartWidth = widthPx - 2 * paddingX
                    val chartHeight = heightPx - 2 * paddingY

                    val strokeColor = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFF00F0FF) else Color(0xFF0061A4)
                    val glowColor = if (designStyle == DesignStyle.FUTURISTIC) Color(0xFFFF007F) else Color(0x330061A4)

                    // Plot Points & Line
                    val points = remember(prices, widthPx, heightPx) {
                        val pts = mutableListOf<androidx.compose.ui.geometry.Offset>()
                        if (prices.size > 1) {
                            val stepX = chartWidth / (prices.size - 1)
                            for (i in prices.indices) {
                                val x = paddingX + i * stepX
                                val y = paddingY + chartHeight * (1.0 - (prices[i] - minPrice) / priceRange).toFloat()
                                pts.add(androidx.compose.ui.geometry.Offset(x, y))
                            }
                        }
                        pts
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(prices, points) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val anyPressed = event.changes.any { it.pressed }
                                        if (anyPressed && prices.size > 1) {
                                            val position = event.changes.first().position
                                            val stepX = chartWidth / (prices.size - 1)
                                            val relativeX = position.x - paddingX
                                            val index = (relativeX / stepX + 0.5f).toInt().coerceIn(prices.indices)
                                            selectedPointIndex = index
                                        } else {
                                            selectedPointIndex = null
                                        }
                                    }
                                }
                            }
                    ) {
                        // Draw Grid Lines (Y-Axis)
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = paddingY + chartHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = strokeColor.copy(alpha = 0.08f),
                                start = androidx.compose.ui.geometry.Offset(paddingX, y),
                                end = androidx.compose.ui.geometry.Offset(widthPx - paddingX, y),
                                strokeWidth = 2f
                            )
                        }

                        if (points.isNotEmpty()) {
                            // Draw Connection path
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(points[0].x, points[0].y)
                            val stepX = chartWidth / (prices.size - 1)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                path.cubicTo(
                                    prev.x + stepX / 2f, prev.y,
                                    curr.x - stepX / 2f, curr.y,
                                    curr.x, curr.y
                                )
                            }
                            
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )

                            // Draw gradient under the curve
                            val fillPath = androidx.compose.ui.graphics.Path()
                            fillPath.addPath(path)
                            fillPath.lineTo(points.last().x, paddingY + chartHeight)
                            fillPath.lineTo(points.first().x, paddingY + chartHeight)
                            fillPath.close()
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(strokeColor.copy(alpha = 0.25f), Color.Transparent),
                                    startY = points.minOf { it.y },
                                    endY = paddingY + chartHeight
                                )
                            )

                            // Draw Normal Points (smaller, subtle)
                            points.forEachIndexed { idx, pt ->
                                drawCircle(
                                    color = strokeColor,
                                    radius = 6f,
                                    center = pt
                                )
                                drawCircle(
                                    color = strokeColor.copy(alpha = 0.15f),
                                    radius = 12f,
                                    center = pt
                                )
                            }

                            // Draw Selected / Hover Pointer Highlight
                            selectedPointIndex?.let { index ->
                                if (index in points.indices) {
                                    val pt = points[index]
                                    
                                    // Vertical reference line
                                    drawLine(
                                        color = strokeColor.copy(alpha = 0.4f),
                                        start = androidx.compose.ui.geometry.Offset(pt.x, paddingY),
                                        end = androidx.compose.ui.geometry.Offset(pt.x, paddingY + chartHeight),
                                        strokeWidth = 3f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )

                                    // Outer pulsing halo
                                    drawCircle(
                                        color = glowColor.copy(alpha = 0.35f),
                                        radius = 22f,
                                        center = pt
                                    )
                                    
                                    // Highlight ring
                                    drawCircle(
                                        color = strokeColor,
                                        radius = 10f,
                                        center = pt
                                    )

                                    // Center dot
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5f,
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    // --- Y-Axis labels (Left Edge) ---
                    val density = LocalDensity.current
                    val yLabels = listOf(
                        maxPrice to 20f,
                        (maxPrice + minPrice) / 2 to (heightPx / 2f - 10f),
                        minPrice to (heightPx - 40f)
                    )

                    yLabels.forEach { (valPrice, offsetPx) ->
                        val yDp = with(density) { offsetPx.toDp() }
                        Text(
                            text = "${String.format(Locale.US, "%.1f", valPrice)}¢",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier
                                .offset(x = 2.dp, y = yDp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // --- Floating Tooltip Card (Recharts/D3 Style) ---
                    selectedPointIndex?.let { index ->
                        if (index in prices.indices && points.isNotEmpty()) {
                            val pt = points[index]
                            val price = prices[index]
                            val day = days[index]

                            val tooltipWidthDp = 100.dp
                            val tooltipHeightDp = 58.dp

                            val ptXDp = with(density) { pt.x.toDp() }
                            val ptYDp = with(density) { pt.y.toDp() }

                            // Center horizontally above point, but constrain within boundaries
                            val tooltipLeft = (ptXDp - tooltipWidthDp / 2).coerceIn(4.dp, this.maxWidth - tooltipWidthDp - 4.dp)
                            val tooltipTop = (ptYDp - tooltipHeightDp - 12.dp).coerceIn(4.dp, this.maxHeight - tooltipHeightDp - 4.dp)

                            Card(
                                modifier = Modifier
                                    .size(width = tooltipWidthDp, height = tooltipHeightDp)
                                    .offset(x = tooltipLeft, y = tooltipTop),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.inverseSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", price)}¢",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Labels below chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { i, dayName ->
                        val isSelected = selectedPointIndex == i
                        Text(
                            text = dayName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val weeklyMetricsCard = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Weekly Pricing Metrics",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricItem(
                        label = "Lowest",
                        value = "${minPrice}¢",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    MetricItem(
                        label = "Average",
                        value = "${String.format("%.1f", avgPrice)}¢",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricItem(
                        label = "Highest",
                        value = "${maxPrice}¢",
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    val savingsCalculatorCard = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Refuel Savings Calculator",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                var tankSize by remember { mutableStateOf("60") }
                OutlinedTextField(
                    value = tankSize,
                    onValueChange = { tankSize = it.filter { c -> c.isDigit() } },
                    label = { Text("Fuel Tank Capacity (Litre)") },
                    trailingIcon = { Text("Litres", modifier = Modifier.padding(end = 12.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val sizeVal = tankSize.toDoubleOrNull() ?: 50.0
                val potentialSavings = (maxPrice - minPrice) * sizeVal / 100.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Potential Savings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("By filling up at cheapest vs. highest", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                    Text(
                        text = "$${String.format("%.2f", potentialSavings)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (isFoldUnfolded) {
        // Dual-Pane Layout (Row with two Columns)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                adviceCard()
                priceTrendGraph()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                weeklyMetricsCard()
                savingsCalculatorCard()
            }
        }
    } else {
        // Compact Scrollable Layout
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            adviceCard()
            priceTrendGraph()
            weeklyMetricsCard()
            savingsCalculatorCard()
        }
    }
}

// 13. Interactive Intelligent Route Planner (2nd Tab)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyTripView(
    stations: List<FuelStation>,
    suburbs: List<String>,
    designStyle: DesignStyle,
    onStationClick: (FuelStation) -> Unit,
    isFoldUnfolded: Boolean,
    favoriteIds: Set<String>,
    onFavoriteToggle: (FuelStation) -> Unit,
    userLocation: Location?,
    onGpsClick: () -> Unit,
    startSuburb: String,
    endSuburb: String,
    onStartSuburbChange: (String) -> Unit,
    onEndSuburbChange: (String) -> Unit
) {
    var isStartSearchOpen by remember { mutableStateOf(false) }
    var isEndSearchOpen by remember { mutableStateOf(false) }

    // Derive intermediate suburbs on the trip leg to build a realistic timeline route
    val intermediateSuburbs = remember(startSuburb, endSuburb) {
        val list = mutableListOf(startSuburb)
        if (startSuburb == "PERTH" && endSuburb == "MANDURAH") {
            list.addAll(listOf("ROCKINGHAM", "GOSNELLS", "ARMADALE"))
        } else if (startSuburb == "PERTH" && endSuburb == "JOONDALUP") {
            list.addAll(listOf("OSBORNE PARK", "BALCATTA", "WANNEROO"))
        } else {
            val randomIntermediates = suburbs.shuffled().take(2).filter { it != startSuburb && it != endSuburb }
            list.addAll(randomIntermediates)
        }
        list.add(endSuburb)
        list.distinct()
    }

    // Filter stations residing on or near the suburbs along the route path
    val stationsOnRoute = remember(stations, intermediateSuburbs) {
        stations.filter { station ->
            intermediateSuburbs.any { suburb -> station.location.equals(suburb, ignoreCase = true) }
        }.sortedBy { it.price }
    }

    val cheapestStation = stationsOnRoute.firstOrNull()

    // Composable blocks
    val tripParametersCard = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Trip Parameters",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Suburb selection
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = startSuburb,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start Suburb") },
                            trailingIcon = { IconButton(onClick = { isStartSearchOpen = true }) { Icon(Icons.Default.Search, null) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isStartSearchOpen = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isStartSearchOpen = true }
                        )
                    }

                    // End Suburb selection
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = endSuburb,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Suburb") },
                            trailingIcon = { IconButton(onClick = { isEndSearchOpen = true }) { Icon(Icons.Default.Search, null) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEndSearchOpen = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isEndSearchOpen = true }
                        )
                    }
                }
            }
        }
    }

    val cheapestStopBlock = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cheapest Re-fuel Station Along Route",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                if (cheapestStation != null) {
                    FuelStationCard(
                        station = cheapestStation,
                        isFavorite = favoriteIds.contains(cheapestStation.id),
                        onClick = { onStationClick(cheapestStation) },
                        onFavoriteToggle = { onFavoriteToggle(cheapestStation) }
                    )

                    Text(
                        text = "Route Leg Visualizer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        val w = size.width
                        val h = size.height / 2f

                        // Draw horizontal timeline connection track line
                        drawLine(
                            color = Color.LightGray,
                            start = androidx.compose.ui.geometry.Offset(0f, h),
                            end = androidx.compose.ui.geometry.Offset(w, h),
                            strokeWidth = 6f
                        )

                        // Start Node
                        drawCircle(color = Color(0xFF0061A4), radius = 12f, center = androidx.compose.ui.geometry.Offset(0f, h))
                        
                        // Intermediate refuel point node
                        drawCircle(color = Color(0xFF2E7D32), radius = 16f, center = androidx.compose.ui.geometry.Offset(w * 0.45f, h))
                        drawCircle(color = Color.White, radius = 6f, center = androidx.compose.ui.geometry.Offset(w * 0.45f, h))

                        // End Node
                        drawCircle(color = Color(0xFF0061A4), radius = 12f, center = androidx.compose.ui.geometry.Offset(w, h))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(startSuburb, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Cheapest Stop\n(${cheapestStation.tradingName})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center
                        )
                        Text(endSuburb, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No fuel stations found matching route suburbs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (isFoldUnfolded) {
        // Dual-Pane Layout (Row with two Columns)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tripParametersCard()
                cheapestStopBlock()
            }
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Real-time Route Map Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FuelMapView(
                            stations = stationsOnRoute,
                            designStyle = designStyle,
                            onStationClick = onStationClick,
                            userLocation = userLocation,
                            onGpsClick = onGpsClick,
                            routeStations = stationsOnRoute
                        )
                    }
                }

                // All Route Stations List Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = "All Route Stations (${stationsOnRoute.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(stationsOnRoute, key = { "route_${it.id}" }) { station ->
                                FuelStationCard(
                                    station = station,
                                    isFavorite = favoriteIds.contains(station.id),
                                    onClick = { onStationClick(station) },
                                    onFavoriteToggle = { onFavoriteToggle(station) }
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Compact Scrollable Layout with embedded map!
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { tripParametersCard() }
            item { cheapestStopBlock() }
            
            // Interactive Route Map item
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FuelMapView(
                            stations = stationsOnRoute,
                            designStyle = designStyle,
                            onStationClick = onStationClick,
                            userLocation = userLocation,
                            onGpsClick = onGpsClick,
                            routeStations = stationsOnRoute
                        )
                    }
                }
            }

            if (stationsOnRoute.isNotEmpty()) {
                item {
                    Text(
                        text = "All Route Stations (${stationsOnRoute.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(stationsOnRoute, key = { "compact_route_${it.id}" }) { station ->
                    FuelStationCard(
                        station = station,
                        isFavorite = favoriteIds.contains(station.id),
                        onClick = { onStationClick(station) },
                        onFavoriteToggle = { onFavoriteToggle(station) }
                    )
                }
            }
        }
    }

    if (isStartSearchOpen) {
        SuburbSearchTripDialog(
            title = "Select Start Suburb",
            suburbs = suburbs,
            onSelect = {
                onStartSuburbChange(it)
                isStartSearchOpen = false
            },
            onDismiss = { isStartSearchOpen = false }
        )
    }

    if (isEndSearchOpen) {
        SuburbSearchTripDialog(
            title = "Select End Suburb",
            suburbs = suburbs,
            onSelect = {
                onEndSuburbChange(it)
                isEndSearchOpen = false
            },
            onDismiss = { isEndSearchOpen = false }
        )
    }
}

@Composable
fun SuburbSearchTripDialog(
    title: String,
    suburbs: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, suburbs) {
        suburbs.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search suburb...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { sub ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(sub) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = sub,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No suburbs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 14. Responsive Toggleable NearMe Main Search Panel (3rd Tab)
@Composable
fun NearMeView(
    stations: List<FuelStation>,
    allStations: List<FuelStation>,
    favoriteIds: Set<String>,
    isMapView: Boolean,
    isFoldUnfolded: Boolean,
    designStyle: DesignStyle,
    onStationClick: (FuelStation) -> Unit,
    onFavoriteToggle: (FuelStation) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedBrand: String,
    onBrandChange: (String) -> Unit,
    selectedSuburb: String,
    onSuburbChange: (String) -> Unit,
    sortBy: SortOption,
    onSortChange: (SortOption) -> Unit,
    brands: List<String>,
    suburbs: List<String>,
    stats: PricingStats,
    userLocation: Location? = null,
    onGpsClick: (() -> Unit)? = null
) {
    if (isFoldUnfolded) {
        // Unfolded Foldable (Dual-Pane) Layout
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Pane: Search results & filters
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                SearchBarAndFilters(
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    selectedBrand = selectedBrand,
                    onBrandChange = onBrandChange,
                    selectedSuburb = selectedSuburb,
                    onSuburbChange = onSuburbChange,
                    sortBy = sortBy,
                    onSortChange = onSortChange,
                    brands = brands,
                    suburbs = suburbs
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    FuelListView(
                        stations = stations,
                        favoriteIds = favoriteIds,
                        onStationClick = onStationClick,
                        onFavoriteToggle = onFavoriteToggle,
                        designStyle = designStyle
                    )
                }
            }

            // Right Pane: Google Maps Integration inside Webview
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            ) {
                FuelMapView(
                    stations = allStations,
                    designStyle = designStyle,
                    onStationClick = onStationClick,
                    userLocation = userLocation,
                    onGpsClick = onGpsClick,
                    selectedSuburb = selectedSuburb
                )
            }
        }
    } else {
        // Compact Screen Layout (Toggle Map vs List via Floating Action Button or Top bar action)
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isMapView) {
                SearchBarAndFilters(
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    selectedBrand = selectedBrand,
                    onBrandChange = onBrandChange,
                    selectedSuburb = selectedSuburb,
                    onSuburbChange = onSuburbChange,
                    sortBy = sortBy,
                    onSortChange = onSortChange,
                    brands = brands,
                    suburbs = suburbs
                )
            }
            
            Box(modifier = Modifier.weight(1f)) {
                if (isMapView) {
                    FuelMapView(
                        stations = allStations,
                        designStyle = designStyle,
                        onStationClick = onStationClick,
                        userLocation = userLocation,
                        onGpsClick = onGpsClick,
                        selectedSuburb = selectedSuburb
                    )
                } else {
                    FuelListView(
                        stations = stations,
                        favoriteIds = favoriteIds,
                        onStationClick = onStationClick,
                        onFavoriteToggle = onFavoriteToggle,
                        designStyle = designStyle
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        }
    }
}

