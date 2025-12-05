package com.example.mindfocus.ui.feature.login

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.auth.AccountLimit
import com.example.mindfocus.core.auth.BiometricAuthManager
import com.example.mindfocus.core.auth.BiometricResult
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.UserEntity
import com.example.mindfocus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LoginViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val biometricAuthManager = BiometricAuthManager(context)
    
    private val _uiState = MutableStateFlow(
        LoginUiState(
            isBiometricAvailable = biometricAuthManager.isBiometricAvailable()
        )
    )
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
            _uiState.value = currentState.copy(errorMessage = context.getString(com.example.mindfocus.R.string.login_error_username_empty))
            return
        }

        // For login mode, only username is required
        // For register mode, email is also required
        if (currentState.isRegisterMode) {
            if (email.isEmpty()) {
                _uiState.value = currentState.copy(errorMessage = context.getString(com.example.mindfocus.R.string.login_error_email_empty))
                return
            }

            if (!isValidEmail(email)) {
                _uiState.value = currentState.copy(errorMessage = context.getString(com.example.mindfocus.R.string.login_error_email_invalid))
                return
            }
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                if (currentState.isRegisterMode) {
                    val accountCount = userRepository.getCount()
                    if (accountCount >= AccountLimit.MAX_ACCOUNTS_PER_DEVICE) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = context.getString(com.example.mindfocus.R.string.login_error_account_limit_reached, AccountLimit.MAX_ACCOUNTS_PER_DEVICE)
                        )
                        return@launch
                    }
                    
                    // Register mode: create new user with email and username
                    // Check if username already exists
                    val existingUserByUsername = userRepository.getByUsername(username)
                    if (existingUserByUsername != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = context.getString(com.example.mindfocus.R.string.login_error_username_exists)
                        )
                        return@launch
                    }

                    // Check if email already exists
                    val existingUserByEmail = userRepository.getByEmail(email)
                    if (existingUserByEmail != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = context.getString(com.example.mindfocus.R.string.login_error_email_exists)
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
                            errorMessage = context.getString(com.example.mindfocus.R.string.login_error_user_creation_failed)
                        )
                    }
                } else {
                    // Login mode: find user by username only
                    val foundUser = userRepository.getByUsername(username)

                    if (foundUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = context.getString(com.example.mindfocus.R.string.login_error_username_not_found)
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
                        context.getString(com.example.mindfocus.R.string.login_error_registration_failed, e.message ?: "")
                    } else {
                        context.getString(com.example.mindfocus.R.string.login_error_login_failed, e.message ?: "")
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
    
    fun selectUserForBiometric(userId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedUserId = userId,
            showUserSelection = false
        )
    }
    
    fun dismissUserSelection() {
        _uiState.value = _uiState.value.copy(
            showUserSelection = false,
            availableUsers = emptyList(),
            biometricAlreadyVerified = false
        )
    }
    
    fun resetBiometricState() {
        _uiState.value = _uiState.value.copy(
            biometricAlreadyVerified = false,
            showUserSelection = false,
            availableUsers = emptyList(),
            isBiometricAuthenticating = false,
            isLoginSuccessful = false,
            selectedUserId = null,
            errorMessage = null
        )
    }
    
    fun loadUsersForSelection() {
        viewModelScope.launch {
            val allUsers = userRepository.observeAll().first()
            val preferredUserId = authPreferencesManager.getPreferredUserId()
            
            _uiState.value = _uiState.value.copy(
                availableUsers = allUsers,
                selectedUserId = preferredUserId ?: allUsers.firstOrNull()?.id
            )
        }
    }
    
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: (Long) -> Unit
    ) {
        if (!_uiState.value.isBiometricAvailable) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(com.example.mindfocus.R.string.biometric_auth_not_available)
            )
            return
        }
        
        viewModelScope.launch {
            val allUsers = userRepository.observeAll().first()
            
            if (allUsers.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(com.example.mindfocus.R.string.login_error_no_accounts)
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isBiometricAuthenticating = true,
                errorMessage = null,
                showUserSelection = false,
                biometricAlreadyVerified = false // Reset flag - require fresh authentication each time
            )
            
            val result = biometricAuthManager.authenticate(
                activity = activity,
                title = context.getString(com.example.mindfocus.R.string.biometric_auth_title),
                subtitle = context.getString(com.example.mindfocus.R.string.biometric_auth_unlock_app),
                negativeButtonText = context.getString(com.example.mindfocus.R.string.biometric_auth_cancel)
            )
            
            when (result) {
                is BiometricResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isBiometricAuthenticating = false,
                        biometricAlreadyVerified = true
                    )
                    
                    if (allUsers.size == 1) {
                        val user = allUsers.first()
                        authenticateWithSelectedUser(user.id, activity) { userId ->
                            onSuccess(userId)
                        }
                    } else {
                        val preferredUserId = authPreferencesManager.getPreferredUserId()
                        _uiState.value = _uiState.value.copy(
                            showUserSelection = true,
                            availableUsers = allUsers,
                            selectedUserId = preferredUserId ?: allUsers.first().id
                        )
                    }
                }
                is BiometricResult.Error -> {
                    val shouldShowError = !result.message.contains("cancel", ignoreCase = true) && 
                                         !result.message.contains("negative", ignoreCase = true)
                    _uiState.value = _uiState.value.copy(
                        isBiometricAuthenticating = false,
                        showUserSelection = false,
                        availableUsers = emptyList(),
                        biometricAlreadyVerified = false,
                        errorMessage = if (shouldShowError) {
                            context.getString(com.example.mindfocus.R.string.biometric_auth_error, result.message)
                        } else {
                            null
                        }
                    )
                }
                is BiometricResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isBiometricAuthenticating = false,
                        showUserSelection = false,
                        availableUsers = emptyList(),
                        biometricAlreadyVerified = false,
                        errorMessage = context.getString(com.example.mindfocus.R.string.biometric_auth_failed)
                    )
                }
            }
        }
    }
    
    fun authenticateWithSelectedUser(
        userId: Long,
        activity: FragmentActivity?,
        onSuccess: (Long) -> Unit
    ) {
        if (activity == null || !_uiState.value.isBiometricAvailable) {
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isBiometricAuthenticating = true,
            errorMessage = null,
            showUserSelection = false
        )
        
        viewModelScope.launch {
            try {
                val user = userRepository.getById(userId)
                val subtitle = if (user != null) {
                    context.getString(com.example.mindfocus.R.string.biometric_auth_authenticate_with, user.displayName)
                } else {
                    context.getString(com.example.mindfocus.R.string.biometric_auth_subtitle_login)
                }
                
                val result = biometricAuthManager.authenticate(
                    activity = activity,
                    title = context.getString(com.example.mindfocus.R.string.biometric_auth_title),
                    subtitle = subtitle,
                    negativeButtonText = context.getString(com.example.mindfocus.R.string.biometric_auth_cancel)
                )
                
                when (result) {
                    is BiometricResult.Success -> {
                        val authenticatedUser = userRepository.getById(userId)
                        if (authenticatedUser != null) {
                            authPreferencesManager.setLoggedIn(authenticatedUser.id)
                            authPreferencesManager.setPreferredUserId(authenticatedUser.id)
                            _uiState.value = _uiState.value.copy(
                                isBiometricAuthenticating = false,
                                isLoginSuccessful = true,
                                showUserSelection = false,
                                biometricAlreadyVerified = false
                            )
                            onSuccess(authenticatedUser.id)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isBiometricAuthenticating = false,
                                errorMessage = context.getString(com.example.mindfocus.R.string.login_error_user_not_found),
                                biometricAlreadyVerified = false
                            )
                        }
                    }
                is BiometricResult.Error -> {
                    val shouldShowError = !result.message.contains("cancel", ignoreCase = true) && 
                                         !result.message.contains("negative", ignoreCase = true)
                    _uiState.value = _uiState.value.copy(
                        isBiometricAuthenticating = false,
                        biometricAlreadyVerified = false,
                        showUserSelection = false,
                        availableUsers = emptyList(),
                        errorMessage = if (shouldShowError) {
                            context.getString(com.example.mindfocus.R.string.biometric_auth_error, result.message)
                        } else {
                            null
                        }
                    )
                }
                is BiometricResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isBiometricAuthenticating = false,
                        biometricAlreadyVerified = false,
                        showUserSelection = false,
                        availableUsers = emptyList(),
                        errorMessage = context.getString(com.example.mindfocus.R.string.biometric_auth_failed)
                    )
                }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Error in biometric authentication: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isBiometricAuthenticating = false,
                    errorMessage = context.getString(com.example.mindfocus.R.string.biometric_auth_error_generic, e.message ?: "")
                )
            }
        }
    }
}
