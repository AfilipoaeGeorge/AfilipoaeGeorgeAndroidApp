package com.example.mindfocus.data.repository

import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.MetricEntity

class MetricRepository(private val db: MindFocusDatabase) {

    suspend fun insertBatch(list: List<MetricEntity>) =
        db.metricDao().insertAll(list)

    suspend fun getForSession(sessionId: Long): List<MetricEntity> =
        db.metricDao().getForSession(sessionId)

    //for a 2-hour session with 240 records, this will reduce to ~120 points (enough for smooth graph).
    suspend fun getForSessionForGraph(sessionId: Long, maxPoints: Int = 150): List<MetricEntity> {
        val allMetrics = db.metricDao().getForSession(sessionId)
        
        //if we have fewer points than max, return all
        if (allMetrics.size <= maxPoints) {
            return allMetrics
        }
        
        // downsample: take evenly distributed points, this preserves the overall shape of the graph
        val step = allMetrics.size.toDouble() / maxPoints
        val downsampled = mutableListOf<MetricEntity>()
        
        for (i in 0 until maxPoints) {
            val index = (i * step).toInt().coerceAtMost(allMetrics.size - 1)
            downsampled.add(allMetrics[index])
        }
        
        // always include first and last point for complete timeline
        if (downsampled.first() != allMetrics.first()) {
            downsampled[0] = allMetrics.first()
        }
        if (downsampled.last() != allMetrics.last()) {
            downsampled[downsampled.size - 1] = allMetrics.last()
        }
        
        return downsampled
    }

    suspend fun deleteForSession(sessionId: Long) =
        db.metricDao().deleteForSession(sessionId)
}
