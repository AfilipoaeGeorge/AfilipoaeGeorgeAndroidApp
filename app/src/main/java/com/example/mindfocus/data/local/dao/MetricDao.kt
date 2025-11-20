package com.example.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mindfocus.data.local.entities.MetricEntity

@Dao
interface MetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<MetricEntity>)

    @Query("SELECT * FROM metrics WHERE sessionId = :sessionId ORDER BY bucketSec ASC")
    suspend fun getForSession(sessionId: Long): List<MetricEntity>

    @Query("DELETE FROM metrics WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("DELETE FROM metrics")
    suspend fun deleteAll()
}
