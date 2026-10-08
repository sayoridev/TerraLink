package com.terralink.iotfarm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TelemetryEntity)

    @Query("SELECT * FROM telemetry_history ORDER BY timestamp DESC")
    fun getAllTelemetryHistory(): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<TelemetryEntity>>

    @Query("DELETE FROM telemetry_history WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
