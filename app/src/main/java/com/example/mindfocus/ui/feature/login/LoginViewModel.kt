package com.example.mindfocus.ui.feature.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.UserEntity
import com.example.mindfocus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LoginViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            errorMessage = null
        )
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null
        )
    }

    fun toggleRegisterMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null
        )
    }

    fun login(onSuccess: (Long) -> Unit) {
        val currentState = _uiState.value
        val username = currentState.username.trim()
        val email = currentState.email.trim()

        // Validation
        if (username.isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "Username cannot be empty")
            return
        }

        // For login mode, only username is required
        // For register mode, email is also required
        if (currentState.isRegisterMode) {
            if (email.isEmpty()) {
                _uiState.value = currentState.copy(errorMessage = "Email cannot be empty")
                return
            }

            if (!isValidEmail(email)) {
                _uiState.value = currentState.copy(errorMessage = "Please enter a valid email address")
                return
            }
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                if (currentState.isRegisterMode) {
                    // Register mode: create new user with email and username
                    // Check if username already exists
                    val existingUserByUsername = userRepository.getByUsername(username)
                    if (existingUserByUsername != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Username already exists. Please choose a different username."
                        )
                        return@launch
                    }

                    // Check if email already exists
                    val existingUserByEmail = userRepository.getByEmail(email)
                    if (existingUserByEmail != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Email already registered. Please use login instead."
                        )
                        return@launch
                    }

                    // Create new user
                    val newUser = UserEntity(
                        email = email,
                        displayName = username
                    )
                    val userId = userRepository.upsert(newUser)
                    val user = userRepository.getByUsername(username)

                    user?.let {
                        // Save login state to DataStore
                        authPreferencesManager.setLoggedIn(it.id)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoginSuccessful = true
                        )
                        onSuccess(it.id)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to create user"
                        )
                    }
                } else {
                    // Login mode: find user by username only
                    val foundUser = userRepository.getByUsername(username)

                    if (foundUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Username not found. Please register or check your username."
                        )
                        return@launch
                    }

                    // User found, login successful
                    authPreferencesManager.setLoggedIn(foundUser.id)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccessful = true
                    )
                    onSuccess(foundUser.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Error in ${if (currentState.isRegisterMode) "registration" else "login"}: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (currentState.isRegisterMode) {
                        "Registration failed: ${e.message}"
                    } else {
                        "Login failed: ${e.message}"
                    }
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        )
        return emailPattern.matcher(email).matches()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}


