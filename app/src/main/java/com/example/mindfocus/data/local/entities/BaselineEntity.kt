package com.example.mindfocus.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "baseline",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"], unique = true)]
)
data class BaselineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val earMean: Double,
    val marMean: Double,
    val headPitchMeanDeg: Double,
    val blinkPerMin: Double,
    val noiseDbMean: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
