package com.example.mindfocus.ui.feature.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.entities.SessionEntity
import com.example.mindfocus.data.repository.MetricRepository
import com.example.mindfocus.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val sessionRepository: SessionRepository,
    private val metricRepository: MetricRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
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

                sessionRepository.observeForUser(userId)
                    .map { sessions ->
                        sessions.filter { it.endedAtEpochMs != null }
                            .sortedByDescending { it.startedAtEpochMs }
                    }
                    .flatMapLatest { completedSessions ->
                        //convert SessionEntity to SessionHistoryItem in a flow
                        flow {
                            val historyItems = mutableListOf<SessionHistoryItem>()
                            completedSessions.forEachIndexed { index, session ->
                                val historyItem = convertToHistoryItem(session, index + 1)
                                historyItems.add(historyItem)
                            }
                            emit(historyItems)
                        }
                    }
                    .catch { e ->
                        android.util.Log.e("HistoryViewModel", "Error loading history: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error loading history: ${e.message}"
                        )
                    }
                    .collect { historyItems ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            sessions = historyItems,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error loading history: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading history: ${e.message}"
                )
            }
        }
    }

    private suspend fun convertToHistoryItem(
        session: SessionEntity,
        sessionNumber: Int
    ): SessionHistoryItem {
        val endTime = session.endedAtEpochMs ?: session.startedAtEpochMs
        val duration = (endTime - session.startedAtEpochMs) / 1000 // duration in seconds
        
        val metrics = metricRepository.getForSessionForGraph(session.id, maxPoints = 150)
        
        val scoreEvolution = metrics.map { it.focusScore.toInt() }
        
        return SessionHistoryItem(
            id = session.id,
            sessionNumber = sessionNumber,
            startTime = session.startedAtEpochMs,
            endTime = endTime,
            duration = duration,
            focusScore = session.focusAvg?.toInt() ?: 0,
            ear = session.earAvg ?: 0.0,
            mar = session.marAvg ?: 0.0,
            headPose = session.headPitchAvgDegrees ?: 0.0,
            scoreEvolution = scoreEvolution,
            latitude = session.latitude,
            longitude = session.longitude
        )
    }

    fun refresh() {
        loadHistory()
    }

    fun requestDeleteSession(sessionId: Long) {
        _uiState.value = _uiState.value.copy(sessionToDelete = sessionId)
    }

    fun cancelDeleteSession() {
        _uiState.value = _uiState.value.copy(sessionToDelete = null)
    }

    fun confirmDeleteSession() {
        val sessionId = _uiState.value.sessionToDelete
        if (sessionId != null) {
            viewModelScope.launch {
                try {
                    sessionRepository.delete(sessionId)
                    _uiState.value = _uiState.value.copy(sessionToDelete = null)
                    // History will automatically refresh due to the Flow observation
                } catch (e: Exception) {
                    android.util.Log.e("HistoryViewModel", "Error deleting session: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error deleting session: ${e.message}",
                        sessionToDelete = null
                    )
                }
            }
        }
    }
}

