package com.example.mindfocus.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.face.FaceMetricsCalculator
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastVibrationTime = 0L

    private val metricsBuffer = ArrayDeque<Triple<Double, Double, Double>>()
    private val bufferSize = 60

    init {
        startSession()
    }

    fun startSession() {
        startTimer()
        _uiState.value = _uiState.value.copy(isPaused = false, isRunning = true)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var t = _uiState.value.timerSeconds
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    t += 1
                    _uiState.value = _uiState.value.copy(timerSeconds = t)
                }
            }
        }
    }

    fun updateFaceMetrics(result: FaceLandmarkerResult?) {
        if (result == null || result.faceLandmarks().isEmpty()) {
            val currentScore = _uiState.value.focusScore
            val decayed = (currentScore - 2).coerceAtLeast(0.0)

            _uiState.value = _uiState.value.copy(
                faceDetected = false,
                focusScore = decayed
            )
            return
        }

        val (ear, mar, head) = FaceMetricsCalculator.calculateFromResult(result)

        if (metricsBuffer.size >= bufferSize) metricsBuffer.removeFirst()
        metricsBuffer.addLast(Triple(ear, mar, head))

        val avgEar = metricsBuffer.map { it.first }.average()
        val avgMar = metricsBuffer.map { it.second }.average()
        val avgHead = metricsBuffer.map { it.third }.average()

        val earRef = 0.25
        val marRef = 0.01
        val w1 = 0.5
        val w2 = 0.2
        val w3 = 0.3

        val earNorm = (avgEar / earRef).coerceIn(0.0, 1.0)
        val marNorm = if (avgMar <= marRef) {
            1.0
        } else {
            ((0.5 - avgMar) / (0.5 - marRef)).coerceIn(0.0, 1.0)
        }
        val headNorm = (1 - (kotlin.math.abs(avgHead) / 45.0)).coerceIn(0.0, 1.0)

        val focusScore = (w1 * earNorm + w2 * marNorm + w3 * headNorm).coerceIn(0.0, 1.0) * 100
        val displayMar = avgMar

        _uiState.value = _uiState.value.copy(
            ear = avgEar,
            mar = displayMar,
            headPose = avgHead,
            focusScore = focusScore,
            faceDetected = true
        )

        checkFocusAlert(focusScore)
    }

    private fun checkFocusAlert(focusScore: Double) {
        val now = System.currentTimeMillis()
        if ((focusScore < 60) && (now - lastVibrationTime > 5000)) {
            _uiState.value = _uiState.value.copy(shouldVibrate = true)
            lastVibrationTime = now
            viewModelScope.launch {
                delay(500)
                _uiState.value = _uiState.value.copy(shouldVibrate = false)
            }
        } else {
            if (_uiState.value.shouldVibrate) {
                _uiState.value = _uiState.value.copy(shouldVibrate = false)
            }
        }
    }

    fun pauseSession() {
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    fun resumeSession() {
        _uiState.value = _uiState.value.copy(isPaused = false)
    }

    fun freezeUi() {
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = true)
    }

    fun unfreezeUi() {
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
    }

    fun stopSession() {
        timerJob?.cancel()
        metricsBuffer.clear()
        _uiState.value = SessionUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
