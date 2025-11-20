package com.example.mindfocus.ui.feature.settings

import com.example.mindfocus.BuildConfig
import com.example.mindfocus.core.datastore.UserSettings

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val appVersion: String = BuildConfig.VERSION_NAME,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeletingData: Boolean = false
)

sealed interface SettingsEvent {
    data class ShowSnackbar(val message: String) : SettingsEvent
}


