package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FuelStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

abstract class BaseMockFuelDataSource(
    private val okHttpClient: OkHttpClient,
    override val stateCode: String,
    private val apiKey: String
) : StateFuelDataSource {
    private val TAG = "MockDataSource_$stateCode"

    override suspend fun fetchPrices(
        productId: Int?,
        day: String?,
        suburb: String?,
        brand: String?,
        region: String?
    ): List<FuelStation> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching prices for $stateCode with Key: \${apiKey.take(4)}...")
        // In a real implementation, you would call the respective State API
        // For example: api.nsw.gov.au for NSW, fuelpricesqld.com.au for QLD

        // Return an empty list or mock data
        if (apiKey == "MOCK_KEY") {
            Log.w(TAG, "Using MOCK_KEY for $stateCode. Real implementation required.")
        }
        emptyList()
    }
}

class NswFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "NSW", BuildConfig.NSW_FUEL_API_KEY)
class QldFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "QLD", BuildConfig.QLD_FUEL_API_KEY)
class VicFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "VIC", BuildConfig.VIC_FUEL_API_KEY)
class SaFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "SA", BuildConfig.SA_FUEL_API_KEY)
class TasFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "TAS", BuildConfig.TAS_FUEL_API_KEY)
class NtFuelDataSource(client: OkHttpClient) : BaseMockFuelDataSource(client, "NT", BuildConfig.NT_FUEL_API_KEY)
