package com.example.mindfocus.ui.feature.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val username: String? = null,
    val email: String? = null,
    val profilePictureUri: String? = null,
    val accountCreatedDate: String? = null,
    val totalSessions: Int = 0,
    val averageFocusScore: Double? = null,
    val totalSessionTime: Long = 0, // in seconds (for accurate calculation including seconds)
    val isEditingUsername: Boolean = false,
    val newUsername: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

