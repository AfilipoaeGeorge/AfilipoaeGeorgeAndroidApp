package com.example.mindfocus.ui.feature.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val lastFocusScore: Int? = null,
    val lastSessionDate: String? = null,
    val lastSessionLocation: String? = null,
    val username: String? = null,
    val errorMessage: String? = null
)

