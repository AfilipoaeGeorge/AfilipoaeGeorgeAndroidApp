package com.example.mindfocus.ui.feature.history

data class HistoryUiState(
    val isLoading: Boolean = false,
    val sessions: List<SessionHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val sessionToDelete: Long? = null
)

