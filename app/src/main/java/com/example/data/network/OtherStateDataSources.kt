package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FuelStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

abstract class BaseStateFuelDataSource(
    private val okHttpClient: OkHttpClient,
    override val stateCode: String,
    private val apiKey: String
) : StateFuelDataSource {
    private val TAG = "StateDataSource_$stateCode"

    override suspend fun fetchPrices(
        productId: Int?,
        day: String?,
        suburb: String?,
        brand: String?,
        region: String?
    ): List<FuelStation> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching prices for Australia-wide / state $stateCode...")
        getMockStationsForState(stateCode, productId, suburb, brand)
    }

    protected fun getMockStationsForState(
        state: String,
        productId: Int?,
        suburbFilter: String?,
        brandFilter: String?
    ): List<FuelStation> {
        val baseStations = when (state) {
            "NSW" -> listOf(
                FuelStation(tradingName = "Ampol Sydney CBD", address = "100 George St", brand = "Ampol", price = 189.9, location = "SYDNEY", latitude = -33.8688, longitude = 151.2093, phone = "02 9100 2000", siteFeatures = "24/7, Cafe, ATM", state = "NSW"),
                FuelStation(tradingName = "BP Parramatta", address = "45 Church St", brand = "BP", price = 182.5, location = "PARRAMATTA", latitude = -33.8150, longitude = 151.0011, phone = "02 9633 1100", siteFeatures = "Car Wash, Shop", state = "NSW"),
                FuelStation(tradingName = "Shell Newcastle", address = "12 Hunter St", brand = "Shell", price = 185.0, location = "NEWCASTLE", latitude = -32.9283, longitude = 151.7817, phone = "02 4926 3300", siteFeatures = "24/7, ATM", state = "NSW"),
                FuelStation(tradingName = "Caltex Wollongong", address = "88 Crown St", brand = "Caltex", price = 187.2, location = "WOLLONGONG", latitude = -34.4278, longitude = 150.8931, phone = "02 4228 4400", siteFeatures = "Shop, Food", state = "NSW"),
                FuelStation(tradingName = "7-Eleven Bondi", address = "152 Campbell Parade", brand = "7-Eleven", price = 194.5, location = "BONDI", latitude = -33.8915, longitude = 151.2767, phone = "02 9365 5500", siteFeatures = "24/7, Slurpee, Coffee", state = "NSW"),
                FuelStation(tradingName = "Coles Express Chatswood", address = "300 Victoria Ave", brand = "Coles Express", price = 184.9, location = "CHATSWOOD", latitude = -33.7961, longitude = 151.1787, phone = "02 9411 6600", siteFeatures = "ATM, Coffee", state = "NSW")
            )
            "QLD" -> listOf(
                FuelStation(tradingName = "BP Brisbane City", address = "200 Queen St", brand = "BP", price = 186.9, location = "BRISBANE", latitude = -27.4698, longitude = 153.0251, phone = "07 3221 1000", siteFeatures = "24/7, Cafe", state = "QLD"),
                FuelStation(tradingName = "Shell Gold Coast", address = "50 Surfers Paradise Blvd", brand = "Shell", price = 191.0, location = "SURFERS PARADISE", latitude = -28.0015, longitude = 153.4309, phone = "07 5538 2200", siteFeatures = "24/7, Beachside", state = "QLD"),
                FuelStation(tradingName = "Ampol Cairns", address = "120 Sheridan St", brand = "Ampol", price = 189.5, location = "CAIRNS", latitude = -16.9186, longitude = 145.7781, phone = "07 4051 3300", siteFeatures = "Trucks, Shop", state = "QLD")
            )
            "VIC" -> listOf(
                FuelStation(tradingName = "Shell Melbourne Central", address = "300 Swanston St", brand = "Shell", price = 183.9, location = "MELBOURNE", latitude = -37.8136, longitude = 144.9631, phone = "03 9663 1100", siteFeatures = "24/7, ATM", state = "VIC"),
                FuelStation(tradingName = "BP Geelong", address = "45 Moorabool St", brand = "BP", price = 181.2, location = "GEELONG", latitude = -38.1499, longitude = 144.3617, phone = "03 5221 2200", siteFeatures = "Car Wash", state = "VIC")
            )
            "SA" -> listOf(
                FuelStation(tradingName = "Ampol Adelaide", address = "80 King William St", brand = "Ampol", price = 180.5, location = "ADELAIDE", latitude = -34.9285, longitude = 138.6007, phone = "08 8212 3300", siteFeatures = "24/7, Shop", state = "SA")
            )
            "TAS" -> listOf(
                FuelStation(tradingName = "BP Hobart", address = "50 Elizabeth St", brand = "BP", price = 188.0, location = "HOBART", latitude = -42.8821, longitude = 147.3272, phone = "03 6234 4400", siteFeatures = "ATM", state = "TAS")
            )
            "NT" -> listOf(
                FuelStation(tradingName = "Shell Darwin", address = "10 Smith St", brand = "Shell", price = 195.0, location = "DARWIN", latitude = -12.4634, longitude = 130.8456, phone = "08 8981 5500", siteFeatures = "24/7", state = "NT")
            )
            else -> emptyList()
        }

        var filtered = baseStations
        if (!suburbFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.location.equals(suburbFilter, ignoreCase = true) }
        }
        if (!brandFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.brand.equals(brandFilter, ignoreCase = true) }
        }
        return filtered
    }
}

class NswFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "NSW", BuildConfig.NSW_FUEL_API_KEY)
class QldFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "QLD", BuildConfig.QLD_FUEL_API_KEY)
class VicFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "VIC", BuildConfig.VIC_FUEL_API_KEY)
class SaFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "SA", BuildConfig.SA_FUEL_API_KEY)
class TasFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "TAS", BuildConfig.TAS_FUEL_API_KEY)
class NtFuelDataSource(client: OkHttpClient) : BaseStateFuelDataSource(client, "NT", BuildConfig.NT_FUEL_API_KEY)
