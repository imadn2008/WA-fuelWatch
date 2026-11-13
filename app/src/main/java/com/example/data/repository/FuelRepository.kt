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

    private val dataSources = listOf(
        com.example.data.network.WaFuelDataSource(okHttpClient),
        com.example.data.network.NswFuelDataSource(okHttpClient),
        com.example.data.network.QldFuelDataSource(okHttpClient),
        com.example.data.network.VicFuelDataSource(okHttpClient),
        com.example.data.network.SaFuelDataSource(okHttpClient),
        com.example.data.network.TasFuelDataSource(okHttpClient),
        com.example.data.network.NtFuelDataSource(okHttpClient)
    )

    suspend fun fetchFuelPrices(
        productId: Int? = null,
        day: String? = null,
        suburb: String? = null,
        brand: String? = null,
        region: String? = null,
        stateCode: String? = null
    ): List<FuelStation> = withContext(Dispatchers.IO) {
        val targetSources = if (stateCode != null) {
            dataSources.filter { it.stateCode.equals(stateCode, ignoreCase = true) }
        } else {
            // For safety, just use WA if no state is specified for now
            dataSources.filter { it.stateCode == "WA" }
        }
        
        val allStations = mutableListOf<FuelStation>()
        
        // Fetch from all targeted state providers concurrently (if we wanted to)
        // Here we just iterate sequentially for simplicity
        for (source in targetSources) {
            try {
                allStations.addAll(source.fetchPrices(productId, day, suburb, brand, region))
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from state ${source.stateCode}: ${e.message}", e)
            }
        }
        
        return@withContext allStations
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
