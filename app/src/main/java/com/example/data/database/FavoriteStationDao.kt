package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStationDao {
    @Query("SELECT * FROM favorite_stations ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE stationId = :stationId")
    suspend fun deleteFavoriteById(stationId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE stationId = :stationId)")
    suspend fun isFavorite(stationId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE stationId = :stationId)")
    fun observeIsFavorite(stationId: String): Flow<Boolean>
}
