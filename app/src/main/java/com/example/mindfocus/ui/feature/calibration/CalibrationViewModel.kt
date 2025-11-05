package com.example.mindfocus.ui.feature.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalibrationUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingTime: Int = 60, // in seconds
    val focusScore: Int = 0,
    val ear: Double = 0.0,
    val mar: Double = 0.0,
    val headPose: String = "0°"
)

class CalibrationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    fun startCalibration() {
        if (_uiState.value.isRunning) return
        
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = false,
            remainingTime = 60
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
        _uiState.value = CalibrationUiState()
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
            while (_uiState.value.remainingTime > 0 && _uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    remainingTime = _uiState.value.remainingTime - 1
                )
                
                if (_uiState.value.remainingTime <= 0) {
                    stop()
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

