package com.example.mindfocus.data.repository

import com.example.mindfocus.core.datastore.SettingsPreferencesManager
import com.example.mindfocus.core.datastore.UserSettings
import com.example.mindfocus.data.local.MindFocusDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(
    private val preferencesManager: SettingsPreferencesManager,
    private val database: MindFocusDatabase
) {

    val settingsFlow: Flow<UserSettings> = preferencesManager.settings

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun setCameraMonitoringEnabled(enabled: Boolean) {
        preferencesManager.setCameraMonitoringEnabled(enabled)
    }

    suspend fun setLowFocusAlertsEnabled(enabled: Boolean) {
        preferencesManager.setLowFocusAlertsEnabled(enabled)
    }

    suspend fun setEyesClosedAlertsEnabled(enabled: Boolean) {
        preferencesManager.setEyesClosedAlertsEnabled(enabled)
    }

    suspend fun setBlinkAlertsEnabled(enabled: Boolean) {
        preferencesManager.setBlinkAlertsEnabled(enabled)
    }

    suspend fun setHeadPoseAlertsEnabled(enabled: Boolean) {
        preferencesManager.setHeadPoseAlertsEnabled(enabled)
    }

    suspend fun setYawnAlertsEnabled(enabled: Boolean) {
        preferencesManager.setYawnAlertsEnabled(enabled)
    }

    suspend fun setFaceLostAlertsEnabled(enabled: Boolean) {
        preferencesManager.setFaceLostAlertsEnabled(enabled)
    }

    suspend fun resetSettings() {
        preferencesManager.reset()
    }

    suspend fun clearAllData() {
        database.clearAllTables()
        preferencesManager.reset()
    }
}



