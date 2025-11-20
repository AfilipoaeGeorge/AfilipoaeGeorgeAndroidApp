package com.example.mindfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mindfocus.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE displayName = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
