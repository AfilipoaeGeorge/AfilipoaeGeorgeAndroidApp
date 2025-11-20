package com.example.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mindfocus.data.local.entities.BaselineEntity

@Dao
interface BaselineDao {
    @Upsert
    suspend fun upsert(baseline: BaselineEntity): Long

    @Query("SELECT * FROM baseline WHERE userId = :userId LIMIT 1")
    suspend fun getForUser(userId: Long): BaselineEntity?

    @Query("DELETE FROM baseline")
    suspend fun deleteAll()
}
