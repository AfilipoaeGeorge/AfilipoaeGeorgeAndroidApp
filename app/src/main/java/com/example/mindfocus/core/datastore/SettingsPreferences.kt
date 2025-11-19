package com.example.mindfocus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

object SettingsPreferencesKeys {
    val CAMERA_MONITORING_ENABLED = booleanPreferencesKey("camera_monitoring_enabled")
    val LOW_FOCUS_ALERTS_ENABLED = booleanPreferencesKey("low_focus_alerts_enabled")
    val EYES_CLOSED_ALERTS_ENABLED = booleanPreferencesKey("eyes_closed_alerts_enabled")
    val BLINK_ALERTS_ENABLED = booleanPreferencesKey("blink_alerts_enabled")
    val HEAD_POSE_ALERTS_ENABLED = booleanPreferencesKey("head_pose_alerts_enabled")
    val YAWN_ALERTS_ENABLED = booleanPreferencesKey("yawn_alerts_enabled")
    val FACE_LOST_ALERTS_ENABLED = booleanPreferencesKey("face_lost_alerts_enabled")
    val GPS_ENABLED = booleanPreferencesKey("gps_enabled")
}

data class UserSettings(
    val cameraMonitoringEnabled: Boolean = false,
    val lowFocusAlertsEnabled: Boolean = true,
    val eyesClosedAlertsEnabled: Boolean = true,
    val blinkAlertsEnabled: Boolean = true,
    val headPoseAlertsEnabled: Boolean = true,
    val yawnAlertsEnabled: Boolean = true,
    val faceLostAlertsEnabled: Boolean = true,
    val gpsEnabled: Boolean = true
)

class SettingsPreferencesManager(private val context: Context) {

    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                cameraMonitoringEnabled = preferences[SettingsPreferencesKeys.CAMERA_MONITORING_ENABLED]
                    ?: false,
                lowFocusAlertsEnabled = preferences[SettingsPreferencesKeys.LOW_FOCUS_ALERTS_ENABLED]
                    ?: true,
                eyesClosedAlertsEnabled = preferences[SettingsPreferencesKeys.EYES_CLOSED_ALERTS_ENABLED]
                    ?: true,
                blinkAlertsEnabled = preferences[SettingsPreferencesKeys.BLINK_ALERTS_ENABLED]
                    ?: true,
                headPoseAlertsEnabled = preferences[SettingsPreferencesKeys.HEAD_POSE_ALERTS_ENABLED]
                    ?: true,
                yawnAlertsEnabled = preferences[SettingsPreferencesKeys.YAWN_ALERTS_ENABLED]
                    ?: true,
                faceLostAlertsEnabled = preferences[SettingsPreferencesKeys.FACE_LOST_ALERTS_ENABLED]
                    ?: true,
                gpsEnabled = preferences[SettingsPreferencesKeys.GPS_ENABLED]
                    ?: true
            )
        }

    suspend fun setCameraMonitoringEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.CAMERA_MONITORING_ENABLED] = enabled
        }
    }

    suspend fun setLowFocusAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.LOW_FOCUS_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setEyesClosedAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.EYES_CLOSED_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setBlinkAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.BLINK_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setHeadPoseAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.HEAD_POSE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setYawnAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.YAWN_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setFaceLostAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.FACE_LOST_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setGpsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsPreferencesKeys.GPS_ENABLED] = enabled
        }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}