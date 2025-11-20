package com.example.mindfocus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mindfocus.data.local.dao.BaselineDao
import com.example.mindfocus.data.local.dao.MetricDao
import com.example.mindfocus.data.local.dao.SessionDao
import com.example.mindfocus.data.local.dao.UserDao
import com.example.mindfocus.data.local.entities.BaselineEntity
import com.example.mindfocus.data.local.entities.MetricEntity
import com.example.mindfocus.data.local.entities.SessionEntity
import com.example.mindfocus.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        BaselineEntity::class,
        SessionEntity::class,
        MetricEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MindFocusDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun baselineDao(): BaselineDao
    abstract fun sessionDao(): SessionDao
    abstract fun metricDao(): MetricDao

    companion object {
        @Volatile private var INSTANCE: MindFocusDatabase? = null

        fun getInstance(context: Context): MindFocusDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MindFocusDatabase::class.java,
                    "mindfocus.db"
                )
                    .fallbackToDestructiveMigration()
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                    .also { 
                        INSTANCE = it
                        // force database initialization by opening it
                        // tables are created immediately
                        it.openHelper.writableDatabase
                    }
            }
    }
}
