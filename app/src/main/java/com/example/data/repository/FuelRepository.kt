package com.example.data.repository

import android.util.Log
import com.example.data.database.FavoriteStationDao
import com.example.data.database.FavoriteStationEntity
import com.example.data.database.WazeIncidentDao
import com.example.data.database.WazeIncidentEntity
import com.example.data.model.FuelStation
import com.example.data.network.FuelWatchParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class FuelRepository(
    private val favoriteStationDao: FavoriteStationDao,
    private val wazeIncidentDao: WazeIncidentDao,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val TAG = "FuelRepository"

    val wazeIncidents: Flow<List<WazeIncidentEntity>> = wazeIncidentDao.getAllIncidents()

    suspend fun addWazeIncident(incident: WazeIncidentEntity) = withContext(Dispatchers.IO) {
        wazeIncidentDao.insertIncident(incident)
        Log.d(TAG, "Added Waze incident in DB: ${incident.label} (${incident.type}) at ${incident.latitude}, ${incident.longitude}")
    }

    suspend fun removeWazeIncident(id: Int) = withContext(Dispatchers.IO) {
        wazeIncidentDao.deleteIncidentById(id)
        Log.d(TAG, "Removed Waze incident from DB ID: $id")
    }

    suspend fun clearOldWazeIncidents(thresholdTime: Long) = withContext(Dispatchers.IO) {
        wazeIncidentDao.clearOldIncidents(thresholdTime)
        Log.d(TAG, "Cleared Waze incidents older than: $thresholdTime")
    }

    val favoriteStations: Flow<List<FuelStation>> = favoriteStationDao.getAllFavorites()
        .map { entities ->
            entities.map { entity ->
                FuelStation(
                    title = entity.tradingName,
                    brand = entity.brand,
                    tradingName = entity.tradingName,
                    location = entity.location,
                    address = entity.address,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    phone = entity.phone,
                    price = 0.0 // Cached list item
                )
            }
        }

    suspend fun fetchFuelPrices(
        productId: Int? = null,
        day: String? = null,
        suburb: String? = null,
        brand: String? = null,
        region: String? = null
    ): List<FuelStation> = withContext(Dispatchers.IO) {
        val baseUrl = "https://www.fuelwatch.wa.gov.au/fuelwatch/fuelwatchRSS"
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        productId?.let { urlBuilder.addQueryParameter("Product", it.toString()) }
        day?.let { urlBuilder.addQueryParameter("Day", it) }
        suburb?.let { if (it.isNotBlank()) urlBuilder.addQueryParameter("Suburb", it) }
        brand?.let { if (it.isNotBlank()) urlBuilder.addQueryParameter("Brand", it) }
        region?.let { if (it.isNotBlank()) urlBuilder.addQueryParameter("Region", it) }

        val url = urlBuilder.build().toString()
        Log.d(TAG, "Fetching fuel prices from URL: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from FuelWatch: ${response.code} ${response.message}")
                    throw IOException("Unexpected code $response")
                }

                val xmlBody = response.body?.string() ?: ""
                if (xmlBody.isBlank()) {
                    Log.e(TAG, "Empty body returned from FuelWatch")
                    return@withContext emptyList()
                }

                return@withContext FuelWatchParser.parse(xmlBody, day ?: "today")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchFuelPrices: ${e.message}", e)
            throw e
        }
    }

    suspend fun addFavorite(station: FuelStation) = withContext(Dispatchers.IO) {
        val entity = FavoriteStationEntity(
            stationId = station.id,
            tradingName = station.tradingName,
            address = station.address,
            location = station.location,
            brand = station.brand,
            latitude = station.latitude,
            longitude = station.longitude,
            phone = station.phone
        )
        favoriteStationDao.insertFavorite(entity)
        Log.d(TAG, "Added favorite station: ${station.tradingName} (${station.id})")
    }

    suspend fun removeFavorite(stationId: String) = withContext(Dispatchers.IO) {
        favoriteStationDao.deleteFavoriteById(stationId)
        Log.d(TAG, "Removed favorite station ID: $stationId")
    }

    suspend fun isFavorite(stationId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext favoriteStationDao.isFavorite(stationId)
    }

    fun observeIsFavorite(stationId: String): Flow<Boolean> {
        return favoriteStationDao.observeIsFavorite(stationId)
    }
}
