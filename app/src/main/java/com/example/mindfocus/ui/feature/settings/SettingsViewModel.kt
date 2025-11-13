package com.example.mindfocus.ui.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.UserSettings
import com.example.mindfocus.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = settings,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onCameraMonitoringChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setCameraMonitoringEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_camera_monitoring_on else R.string.settings_camera_monitoring_off
            )
        )
    }

    fun onCameraPermissionDenied() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.ShowSnackbar(context.getString(R.string.settings_camera_permission_denied)))
        }
    }

    fun onLowFocusAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setLowFocusAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_low_focus_alerts_enabled else R.string.settings_low_focus_alerts_disabled
            )
        )
    }

    fun onEyesClosedAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setEyesClosedAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_eyes_closed_alerts_enabled else R.string.settings_eyes_closed_alerts_disabled
            )
        )
    }

    fun onBlinkAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setBlinkAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_blink_alerts_enabled else R.string.settings_blink_alerts_disabled
            )
        )
    }

    fun onHeadPoseAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setHeadPoseAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_head_pose_alerts_enabled else R.string.settings_head_pose_alerts_disabled
            )
        )
    }

    fun onYawnAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setYawnAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_yawn_alerts_enabled else R.string.settings_yawn_alerts_disabled
            )
        )
    }

    fun onFaceLostAlertsChanged(enabled: Boolean) {
        updateSetting(
            action = { settingsRepository.setFaceLostAlertsEnabled(enabled) },
            successMessage = context.getString(
                if (enabled) R.string.settings_face_lost_alerts_enabled else R.string.settings_face_lost_alerts_disabled
            )
        )
    }

    fun onPrivacyPolicyClicked() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.OpenUrl(context.getString(R.string.privacy_policy_url)))
        }
    }

    fun onHelpAndSupportClicked() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.OpenUrl(context.getString(R.string.support_center_url)))
        }
    }

    fun onDeleteAllDataClicked() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingData = true) }
            try {
                settingsRepository.clearAllData()
                _uiState.update {
                    it.copy(
                        isDeletingData = false,
                        showDeleteConfirmation = false,
                        errorMessage = null,
                        settings = UserSettings()
                    )
                }
                _events.emit(SettingsEvent.ShowSnackbar(context.getString(R.string.settings_delete_data_success)))
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isDeletingData = false,
                        errorMessage = context.getString(
                            R.string.settings_delete_data_error,
                            e.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun updateSetting(
        action: suspend () -> Unit,
        successMessage: String? = null
    ) {
        viewModelScope.launch {
            try {
                action()
                successMessage?.let {
                    _events.emit(SettingsEvent.ShowSnackbar(it))
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error updating setting: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        errorMessage = context.getString(
                            R.string.settings_generic_error,
                            e.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

}


