package com.example.mindfocus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

object AuthPreferencesKeys {
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val USER_ID = longPreferencesKey("user_id")
    val PREFERRED_USER_ID = longPreferencesKey("preferred_user_id")
}

class AuthPreferencesManager(private val context: Context) {
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AuthPreferencesKeys.IS_LOGGED_IN] ?: false
    }

    val userId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[AuthPreferencesKeys.USER_ID]
    }

    suspend fun setLoggedIn(userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[AuthPreferencesKeys.IS_LOGGED_IN] = true
            preferences[AuthPreferencesKeys.USER_ID] = userId
        }
    }

    suspend fun setLoggedOut() {
        context.dataStore.edit { preferences ->
            preferences[AuthPreferencesKeys.IS_LOGGED_IN] = false
            preferences.remove(AuthPreferencesKeys.USER_ID)
        }
    }

    suspend fun getCurrentUserId(): Long? {
        return context.dataStore.data.first()[AuthPreferencesKeys.USER_ID]
    }
    
    suspend fun getPreferredUserId(): Long? {
        return context.dataStore.data.first()[AuthPreferencesKeys.PREFERRED_USER_ID]
    }
    
    suspend fun setPreferredUserId(userId: Long?) {
        context.dataStore.edit { preferences ->
            if (userId != null) {
                preferences[AuthPreferencesKeys.PREFERRED_USER_ID] = userId
            } else {
                preferences.remove(AuthPreferencesKeys.PREFERRED_USER_ID)
            }
        }
    }
}

