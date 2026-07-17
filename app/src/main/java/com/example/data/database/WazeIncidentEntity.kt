package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waze_incidents")
data class WazeIncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // 'police', 'car', 'hazard', 'roadworks'
    val label: String,
    val desc: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
