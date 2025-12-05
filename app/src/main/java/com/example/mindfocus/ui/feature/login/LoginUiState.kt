package com.example.mindfocus.ui.feature.login

import com.example.mindfocus.data.local.entities.UserEntity

data class LoginUiState(
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false,
    val isRegisterMode: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricAuthenticating: Boolean = false,
    val showUserSelection: Boolean = false,
    val availableUsers: List<UserEntity> = emptyList(),
    val selectedUserId: Long? = null,
    val biometricAlreadyVerified: Boolean = false
)


