package com.example.mindfocus.data.repository

import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.BaselineEntity

class BaselineRepository(private val db: MindFocusDatabase) {

    suspend fun upsert(b: BaselineEntity): Long = db.baselineDao().upsert(b)

    suspend fun getForUser(userId: Long): BaselineEntity? =
        db.baselineDao().getForUser(userId)
}
