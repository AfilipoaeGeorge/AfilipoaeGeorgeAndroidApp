package com.example.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mindfocus.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?

    @Query("UPDATE sessions SET endedAtEpochMs = :endMs, breaksCount = :breaks, focusAvg = :focusAvg, earAvg = :earAvg, marAvg = :marAvg, headPitchAvgDegrees = :headPitchAvgDegrees WHERE id = :id")
    suspend fun closeSession(
        id: Long,
        endMs: Long,
        breaks: Int,
        focusAvg: Double?,
        earAvg: Double?,
        marAvg: Double?,
        headPitchAvgDegrees: Double?
    )

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startedAtEpochMs DESC")
    fun observeForUser(userId: Long): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE userId = :userId AND endedAtEpochMs IS NOT NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getLastCompletedSession(userId: Long): SessionEntity?

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
