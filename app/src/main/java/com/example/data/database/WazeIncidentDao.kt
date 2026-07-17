package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WazeIncidentDao {
    @Query("SELECT * FROM waze_incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<WazeIncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: WazeIncidentEntity)

    @Query("DELETE FROM waze_incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Int)

    @Query("DELETE FROM waze_incidents WHERE timestamp < :thresholdTime")
    suspend fun clearOldIncidents(thresholdTime: Long)
}
