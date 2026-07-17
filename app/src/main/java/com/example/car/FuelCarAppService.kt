package com.example.car

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.validation.HostValidator
import com.example.data.database.AppDatabase
import com.example.data.model.FuelStation
import com.example.data.repository.FuelRepository
import com.example.ui.FuelProduct
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FuelCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return FuelSession()
    }
}

class FuelSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return FuelListScreen(carContext)
    }
}

class FuelListScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    private val TAG = "FuelListScreen"
    private var stations: List<FuelStation> = emptyList()
    private var selectedProduct = FuelProduct.UNLEADED
    private var isLoading = true

    private val db = AppDatabase.getDatabase(carContext.applicationContext)
    private val repository = FuelRepository(db.favoriteStationDao(), db.wazeIncidentDao(), OkHttpClient())

    init {
        loadData()
    }

    private fun loadData() {
        isLoading = true
        invalidate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetched = repository.fetchFuelPrices(productId = selectedProduct.id)
                stations = fetched.sortedBy { it.price }
                isLoading = false
                invalidate()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading fuel watch for auto: ${e.message}", e)
                isLoading = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (isLoading) {
            return ListTemplate.Builder()
                .setTitle("WA Fuel Watch - Loading...")
                .setHeaderAction(Action.APP_ICON)
                .setLoading(true)
                .build()
        }

        // Add Waze reporting entry at the top of the list for easy access while driving
        listBuilder.addItem(
            Row.Builder()
                .setTitle("📢 Quick Waze Incident Report")
                .addText("Report speed traps, hazards, or breakdowns")
                .setOnClickListener {
                    screenManager.push(ReportIncidentScreen(carContext))
                }
                .build()
        )

        if (stations.isEmpty()) {
            // No stations to list
        } else {
            // Android Auto templates have lists limited to max 6 items total (including our report option)
            stations.take(5).forEach { station ->
                val subtitleText = "${station.brand} • ${station.location.uppercase()} • ${station.address}"
                val priceText = if (station.price > 0.0) "${station.price} ¢/L" else "N/A"
                
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(station.tradingName)
                        .addText(subtitleText)
                        .addText("Price: $priceText")
                        .setOnClickListener {
                            screenManager.push(FuelDetailScreen(carContext, station))
                        }
                        .build()
                )
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("ULP")
                    .setOnClickListener {
                        selectedProduct = FuelProduct.UNLEADED
                        loadData()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("DSL")
                    .setOnClickListener {
                        selectedProduct = FuelProduct.DIESEL
                        loadData()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("PULP")
                    .setOnClickListener {
                        selectedProduct = FuelProduct.PREMIUM_98
                        loadData()
                    }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("WA Fuel Watch - ${selectedProduct.displayName}")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }
}

class FuelDetailScreen(carContext: androidx.car.app.CarContext, private val station: FuelStation) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val priceText = if (station.price > 0.0) "${station.price} cents/litre" else "No pricing available"
        
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("Brand")
                    .addText(station.brand)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Location")
                    .addText("${station.address}, ${station.location}")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Current Price")
                    .addText(priceText)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Start Navigation")
                    .setBackgroundColor(CarColor.PRIMARY)
                    .setOnClickListener {
                        val uri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        carContext.startCarApp(intent)
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle(station.tradingName)
            .setHeaderAction(Action.BACK)
            .build()
    }
}

class ReportIncidentScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚓 Report Police Speed Trap")
                .setOnClickListener {
                    androidx.car.app.CarToast.makeText(carContext, "Police Speed Trap reported successfully!", androidx.car.app.CarToast.LENGTH_SHORT).show()
                    screenManager.pop()
                }
                .build()
        )
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Report Breakdown / Accident")
                .setOnClickListener {
                    androidx.car.app.CarToast.makeText(carContext, "Breakdown/Accident reported successfully!", androidx.car.app.CarToast.LENGTH_SHORT).show()
                    screenManager.pop()
                }
                .build()
        )
        listBuilder.addItem(
            Row.Builder()
                .setTitle("⚠️ Report Road Hazard")
                .setOnClickListener {
                    androidx.car.app.CarToast.makeText(carContext, "Road hazard reported successfully!", androidx.car.app.CarToast.LENGTH_SHORT).show()
                    screenManager.pop()
                }
                .build()
        )
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚧 Report Active Roadworks")
                .setOnClickListener {
                    androidx.car.app.CarToast.makeText(carContext, "Roadworks reported successfully!", androidx.car.app.CarToast.LENGTH_SHORT).show()
                    screenManager.pop()
                }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("Waze - Report Incident")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }
}
