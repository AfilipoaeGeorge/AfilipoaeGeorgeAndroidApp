package com.example.mindfocus.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.repository.SessionRepository
import com.example.mindfocus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not logged in"
                    )
                    return@launch
                }

                val user = userRepository.getById(userId)
                val username = user?.displayName

                val lastSession = sessionRepository.getLastCompletedSession(userId)
                
                val lastFocusScore = lastSession?.focusAvg?.toInt()
                val lastSessionDate = lastSession?.let { formatSessionDate(it.endedAtEpochMs ?: it.startedAtEpochMs) }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    username = username,
                    lastFocusScore = lastFocusScore,
                    lastSessionDate = lastSessionDate,
                    errorMessage = null
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading user data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                authPreferencesManager.setLoggedOut()
                onLogoutComplete()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error logging out: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout failed: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        loadUserData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun formatSessionDate(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(epochMs))
            }
        }
    }
}

