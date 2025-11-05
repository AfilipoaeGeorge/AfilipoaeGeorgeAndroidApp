package com.example.mindfocus.data.repository

import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(private val db: MindFocusDatabase) {

    suspend fun upsert(user: UserEntity): Long = db.userDao().upsert(user)

    suspend fun getByEmail(email: String): UserEntity? = db.userDao().getByEmail(email)

    suspend fun getByUsername(username: String): UserEntity? = db.userDao().getByUsername(username)

    fun observeAll(): Flow<List<UserEntity>> = db.userDao().observeAll()
}
