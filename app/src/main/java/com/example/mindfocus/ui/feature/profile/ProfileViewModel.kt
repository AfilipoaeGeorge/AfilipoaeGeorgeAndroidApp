package com.example.mindfocus.ui.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.entities.UserEntity
import com.example.mindfocus.data.repository.SessionRepository
import com.example.mindfocus.data.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.profile_error_user_not_logged_in)
                    )
                    return@launch
                }

                val user = userRepository.getById(userId)
                
                //load user statistics
                val sessions = sessionRepository.observeForUser(userId).first()
                val completedSessions = sessions.filter { it.endedAtEpochMs != null }
                val totalSessions = completedSessions.size
                val averageFocusScore = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { it.focusAvg }.average()
                } else null
                
                val totalSessionTime = completedSessions.sumOf { session ->
                    val duration = (session.endedAtEpochMs ?: 0) - session.startedAtEpochMs
                    duration / (1000 * 60) //convert to minutes
                }
                
                val accountCreatedDate = user?.createdAtEpochMs?.let {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(it))
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    username = user?.displayName,
                    email = user?.email,
                    profilePictureUri = user?.profilePictureUri,
                    accountCreatedDate = accountCreatedDate,
                    totalSessions = totalSessions,
                    averageFocusScore = averageFocusScore,
                    totalSessionTime = totalSessionTime,
                    errorMessage = null
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading user profile: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.profile_error_failed_to_load, e.message ?: "Unknown error")
                )
            }
        }
    }

    fun startEditingUsername() {
        _uiState.value = _uiState.value.copy(
            isEditingUsername = true,
            newUsername = _uiState.value.username ?: "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEditingUsername() {
        _uiState.value = _uiState.value.copy(
            isEditingUsername = false,
            newUsername = "",
            errorMessage = null
        )
    }

    fun updateNewUsername(newUsername: String) {
        _uiState.value = _uiState.value.copy(
            newUsername = newUsername,
            errorMessage = null
        )
    }

    fun saveUsername() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val trimmedUsername = currentState.newUsername.trim()
                
                if (trimmedUsername.isEmpty()) {
                    _uiState.value = currentState.copy(
                        errorMessage = context.getString(R.string.profile_error_username_empty)
                    )
                    return@launch
                }

                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = currentState.copy(
                        errorMessage = context.getString(R.string.profile_error_user_not_logged_in)
                    )
                    return@launch
                }

                //check if username already exists (excluding current user)
                val existingUser = userRepository.getByUsername(trimmedUsername)
                if (existingUser != null && existingUser.id != userId) {
                    _uiState.value = currentState.copy(
                        errorMessage = context.getString(R.string.profile_error_username_exists)
                    )
                    return@launch
                }

                //get current user
                val currentUser = userRepository.getById(userId)
                if (currentUser == null) {
                    _uiState.value = currentState.copy(
                        errorMessage = context.getString(R.string.profile_error_user_not_found)
                    )
                    return@launch
                }

                //update user with new username
                val updatedUser = currentUser.copy(displayName = trimmedUsername)
                userRepository.upsert(updatedUser)

                _uiState.value = _uiState.value.copy(
                    isEditingUsername = false,
                    username = trimmedUsername,
                    newUsername = "",
                    errorMessage = null,
                    successMessage = context.getString(R.string.profile_success_username_updated)
                )

                //clear success message after 3 seconds
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(successMessage = null)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error saving username: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.profile_error_failed_to_save_username, e.message ?: "Unknown error")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun saveProfilePicture(uriString: String) {
        viewModelScope.launch {
            try {
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = context.getString(R.string.profile_error_user_not_logged_in)
                    )
                    return@launch
                }

                val currentUser = userRepository.getById(userId)
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = context.getString(R.string.profile_error_user_not_found)
                    )
                    return@launch
                }

                val updatedUser = currentUser.copy(profilePictureUri = uriString)
                userRepository.upsert(updatedUser)

                _uiState.value = _uiState.value.copy(
                    profilePictureUri = uriString,
                    errorMessage = null,
                    successMessage = context.getString(R.string.profile_success_picture_updated)
                )

                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(successMessage = null)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error saving profile picture: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.profile_error_failed_to_save_picture, e.message ?: "Unknown error")
                )
            }
        }
    }
}

