package com.example.mindfocus.ui.feature.login

data class LoginUiState(
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false,
    val isRegisterMode: Boolean = false
)


