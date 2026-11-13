package com.example.data.network

import com.example.data.model.FuelStation

interface StateFuelDataSource {
    val stateCode: String
    suspend fun fetchPrices(
        productId: Int?,
        day: String?,
        suburb: String?,
        brand: String?,
        region: String?
    ): List<FuelStation>
}
