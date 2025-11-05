package com.example.mindfocus.data.repository

import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val db: MindFocusDatabase) {

    suspend fun start(session: SessionEntity): Long = db.sessionDao().upsert(session)

    suspend fun close(
        id: Long,
        endMs: Long,
        breaks: Int,
        focusAvg: Double?,
        earAvg: Double?,
        marAvg: Double?,
        headPitchAvgDegrees: Double?,
        noiseDbAvg: Double?
    ) = db.sessionDao().closeSession(id, endMs, breaks, focusAvg, earAvg, marAvg, headPitchAvgDegrees, noiseDbAvg)

    fun observeForUser(userId: Long): Flow<List<SessionEntity>> =
        db.sessionDao().observeForUser(userId)

    suspend fun getById(id: Long): SessionEntity? = db.sessionDao().getById(id)
    
    suspend fun getLastCompletedSession(userId: Long): SessionEntity? = 
        db.sessionDao().getLastCompletedSession(userId)

    suspend fun delete(id: Long) = db.sessionDao().delete(id)
}