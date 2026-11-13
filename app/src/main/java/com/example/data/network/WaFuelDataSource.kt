package com.example.data.network

import android.util.Log
import com.example.data.model.FuelStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class WaFuelDataSource(
    private val okHttpClient: OkHttpClient
) : StateFuelDataSource {
    private val TAG = "WaFuelDataSource"

    override val stateCode: String = "WA"

    override suspend fun fetchPrices(
        productId: Int?,
        day: String?,
        suburb: String?,
        brand: String?,
        region: String?
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
                    Log.e(TAG, "Unsuccessful response from WA FuelWatch: ${response.code} ${response.message}")
                    return@withContext emptyList()
                }

                val xmlBody = response.body?.string() ?: ""
                if (xmlBody.isBlank()) {
                    Log.e(TAG, "Empty body returned from WA FuelWatch")
                    return@withContext emptyList()
                }

                val stations = FuelWatchParser.parse(xmlBody, day ?: "today")
                // Enhance stations with State Code
                return@withContext stations.map { it.copy(state = stateCode) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WA fetchFuelPrices: ${e.message}", e)
            emptyList()
        }
    }
}
