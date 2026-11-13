package com.example.workers

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.repository.FuelRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient

class FuelAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("FuelAlertWorker", "Starting background fuel alert check")

        val prefs = appContext.getSharedPreferences("wafuelwatch_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("price_alerts_enabled", false)) {
            return Result.success()
        }

        // 1. Get current location & heading
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
        val location: Location? = try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }

        if (location == null) return Result.retry()

        val bearing = location.bearing
        // If bearing is 0.0, user might be stationary, but we can still check nearby

        val db = AppDatabase.getDatabase(appContext)
        val repository = FuelRepository(db.favoriteStationDao(), db.wazeIncidentDao(), OkHttpClient())

        try {
            // Fetch prices for the preferred state/product
            val productId = prefs.getString("price_alert_product", "UNLEADED")?.let {
                try { com.example.ui.FuelProduct.valueOf(it).id } catch (e: Exception) { 1 }
            } ?: 1

            // Note: Since this is a worker, we might not have a UI to select state, default to WA or read from prefs
            val stateCode = "WA" // Could read from prefs

            val stations = repository.fetchFuelPrices(productId = productId, stateCode = stateCode)

            val threshold = prefs.getFloat("price_alert_threshold", 175.0f).toDouble()

            // 2. Forward-Cone Algorithm
            // Filter stations that are ahead of the user and below the threshold
            val forwardStations = stations.filter { station ->
                val distanceKm = calculateDistance(location.latitude, location.longitude, station.latitude, station.longitude)

                if (distanceKm > 0.5 && distanceKm < 15.0 && station.price > 0.0 && station.price <= threshold) {
                    if (location.hasBearing()) {
                        val bearingToStation = calculateBearing(location.latitude, location.longitude, station.latitude, station.longitude)
                        val bearingDiff = Math.abs(bearingToStation - bearing)
                        // Within +/- 45 degrees
                        bearingDiff < 45.0 || bearingDiff > 315.0
                    } else {
                        // If no bearing, just check radius
                        true
                    }
                } else {
                    false
                }
            }

            if (forwardStations.isNotEmpty()) {
                val cheapest = forwardStations.minByOrNull { it.price }
                cheapest?.let {
                    sendPriceAlertNotification(it, productId, location)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("FuelAlertWorker", "Error in background check", e)
            return Result.retry()
        }
    }

    private fun sendPriceAlertNotification(station: com.example.data.model.FuelStation, productId: Int, userLocation: Location) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceKm = calculateDistance(userLocation.latitude, userLocation.longitude, station.latitude, station.longitude)

        val title = "Cheaper Fuel Ahead!"
        val message = "${station.tradingName} is ${String.format("%.1f", distanceKm)}km ahead on your route, selling for ${station.price}¢/L."

        val notification = NotificationCompat.Builder(appContext, "FUEL_ALERT_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_directions, "Navigate", pendingIntent)
            .build()

        notificationManager.notify(43, notification)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = Math.sin(dLon) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
        var brng = Math.toDegrees(Math.atan2(y, x))
        brng = (brng + 360) % 360
        return brng
    }
}
