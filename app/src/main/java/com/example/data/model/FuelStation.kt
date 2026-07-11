package com.example.data.model

import java.io.Serializable

data class FuelStation(
    val title: String = "",
    val description: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val tradingName: String = "",
    val location: String = "", // Suburb
    val address: String = "",
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val siteFeatures: String = "",
    val day: String = "today" // "today" or "tomorrow"
) : Serializable {
    val id: String
        get() = "${tradingName.replace(" ", "_")}_${location.replace(" ", "_")}"
}
