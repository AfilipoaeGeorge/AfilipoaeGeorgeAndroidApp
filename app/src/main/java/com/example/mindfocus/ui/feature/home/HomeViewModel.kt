package com.example.mindfocus.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.core.location.LocationManager
import com.example.mindfocus.data.repository.SessionRepository
import com.example.mindfocus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
        observeSessions()
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    username = username,
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

    private fun observeSessions() {
        viewModelScope.launch {
            try {
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    return@launch
                }

                sessionRepository.observeForUser(userId)
                    .map { sessions ->
                        sessions.filter { it.endedAtEpochMs != null }
                            .sortedByDescending { it.startedAtEpochMs }
                            .firstOrNull()
                    }
                    .collect { lastSession ->
                        android.util.Log.d("HomeViewModel", "Observed last session: ${lastSession?.id}, Location: ${lastSession?.latitude}, ${lastSession?.longitude}")
                        
                        val lastFocusScore = lastSession?.focusAvg?.toInt()
                        val lastSessionDate = lastSession?.let { formatSessionDate(it.endedAtEpochMs ?: it.startedAtEpochMs) }
                        
                        val lastSessionLocation = lastSession?.let { session ->
                            if (session.latitude != null && session.longitude != null) {
                                try {
                                    val locationManager = LocationManager(context)
                                    val formatted = locationManager.formatLocation(session.latitude, session.longitude)
                                    formatted ?: "${session.latitude}, ${session.longitude}"
                                } catch (e: Exception) {
                                    android.util.Log.e("HomeViewModel", "Error formatting location: ${e.message}", e)
                                    "${session.latitude}, ${session.longitude}"
                                }
                            } else {
                                null
                            }
                        } ?: null
                        
                        android.util.Log.d("HomeViewModel", "Updated last session location: ${lastSession?.latitude}, ${lastSession?.longitude}, Formatted: $lastSessionLocation")

                        _uiState.value = _uiState.value.copy(
                            lastFocusScore = lastFocusScore,
                            lastSessionDate = lastSessionDate,
                            lastSessionLocation = lastSessionLocation
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error observing sessions: ${e.message}", e)
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

