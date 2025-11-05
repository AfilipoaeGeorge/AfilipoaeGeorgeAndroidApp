package com.example.mindfocus.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Int = 0, // in seconds
    val focusScore: Int = 0,
    val ear: Double = 0.0,
    val mar: Double = 0.0,
    val headPose: String = "0°"
)

class SessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    fun startSession() {
        if (_uiState.value.isRunning) return
        
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = false,
            elapsedTime = 0
        )
        
        startTimer()
    }
    
    fun pause() {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return
        
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isPaused = true)
    }
    
    fun resume() {
        if (!_uiState.value.isRunning || !_uiState.value.isPaused) return
        
        _uiState.value = _uiState.value.copy(isPaused = false)
        startTimer()
    }
    
    fun stop() {
        timerJob?.cancel()
        _uiState.value = SessionUiState()
    }
    
    fun updateMetrics(ear: Double, mar: Double, headPose: Double, focusScore: Int) {
        if (!_uiState.value.isPaused && _uiState.value.isRunning) {
            _uiState.value = _uiState.value.copy(
                ear = ear,
                mar = mar,
                headPose = String.format("%.1f°", headPose),
                focusScore = focusScore
            )
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    elapsedTime = _uiState.value.elapsedTime + 1
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

