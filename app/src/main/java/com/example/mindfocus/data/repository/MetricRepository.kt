package com.example.mindfocus.data.repository

import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.MetricEntity

class MetricRepository(private val db: MindFocusDatabase) {

    suspend fun insertBatch(list: List<MetricEntity>) =
        db.metricDao().insertAll(list)

    suspend fun getForSession(sessionId: Long): List<MetricEntity> =
        db.metricDao().getForSession(sessionId)

    suspend fun deleteForSession(sessionId: Long) =
        db.metricDao().deleteForSession(sessionId)
}
