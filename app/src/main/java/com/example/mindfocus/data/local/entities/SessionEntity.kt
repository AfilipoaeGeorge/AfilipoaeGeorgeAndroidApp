package com.example.mindfocus.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val breaksCount: Int = 0,
    val focusAvg: Double? = null,
    val earAvg: Double? = null,
    val marAvg: Double? = null,
    val headPitchAvgDegrees: Double? = null
)
