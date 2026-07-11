package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey val stationId: String,
    val tradingName: String,
    val address: String,
    val location: String, // Suburb
    val brand: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
