package com.example.mindfocus.ui.feature.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.face.FaceMetricsCalculator
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.BaselineEntity
import com.example.mindfocus.data.repository.BaselineRepository
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.example.mindfocus.core.datastore.AuthPreferencesManager

class CalibrationViewModel(
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    private val metricsBuffer = ArrayDeque<Triple<Double, Double, Double>>()
    private val bufferSize = 60
    
    // metrics collection for baseline
    private val earSamples = mutableListOf<Double>()
    private val marSamples = mutableListOf<Double>()
    private val headPoseSamples = mutableListOf<Double>()
    
    // blink detection
    private var blinkCount = 0
    private var lastEar: Double = 0.0
    private val earThreshold = 0.18 // threshold for blink detection
    private var isBlinking = false
    
    // calibration duration
    private val calibrationDurationSeconds = 60
    
    // repository
    private val baselineRepository = BaselineRepository(
        MindFocusDatabase.getInstance(context)
    )
    private val authPreferencesManager = AuthPreferencesManager(context)
    
    fun startCalibration() {
        if (_uiState.value.isRunning) return
        
        // reset all metrics
        metricsBuffer.clear()
        earSamples.clear()
        marSamples.clear()
        headPoseSamples.clear()
        blinkCount = 0
        lastEar = 0.0
        isBlinking = false
        
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = false,
            timerSeconds = 0,
            isCompleted = false,
            errorMessage = null,
            faceDetected = false
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
        saveBaseline()
        metricsBuffer.clear()
        _uiState.value = CalibrationUiState()
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

        // only collect metrics if calibration is running and not paused
        if (_uiState.value.isRunning && !_uiState.value.isPaused) {
            // Add to metrics buffer for averaging
            if (metricsBuffer.size >= bufferSize) metricsBuffer.removeFirst()
            metricsBuffer.addLast(Triple(ear, mar, head))
            
            // collect samples for baseline calculation
            earSamples.add(ear)
            marSamples.add(mar)
            headPoseSamples.add(head)
            
            // detect blinks
            detectBlink(ear)
        }

        // calculate averages from buffer
        val avgEar = if (metricsBuffer.isNotEmpty()) {
            metricsBuffer.map { it.first }.average()
        } else {
            ear
        }
        val avgMar = if (metricsBuffer.isNotEmpty()) {
            metricsBuffer.map { it.second }.average()
        } else {
            mar
        }
        val avgHead = if (metricsBuffer.isNotEmpty()) {
            metricsBuffer.map { it.third }.average()
        } else {
            head
        }

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

        lastEar = ear
    }
    
    private fun detectBlink(currentEar: Double) {
        // blink detection: EAR drops below threshold and then rises above it
        if (currentEar < earThreshold && !isBlinking) {
            //start of a blink
            isBlinking = true
        } else if (currentEar >= earThreshold && isBlinking) {
            // end of a blink
            blinkCount++
            isBlinking = false
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                val newTimerSeconds = _uiState.value.timerSeconds + 1
                _uiState.value = _uiState.value.copy(
                    timerSeconds = newTimerSeconds
                )
                
                if (newTimerSeconds >= calibrationDurationSeconds) {
                    // timer completed, save baseline
                    saveBaseline()
                    _uiState.value = _uiState.value.copy(
                        isRunning = false,
                        isCompleted = true
                    )
                }
            }
        }
    }
    
    private fun saveBaseline() {
        viewModelScope.launch {
            try {
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "User not logged in"
                    )
                    return@launch
                }
                
                // calculate averages
                if (earSamples.isEmpty() || marSamples.isEmpty() || headPoseSamples.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Insufficient data collected"
                    )
                    return@launch
                }
                
                val earMean = earSamples.average()
                val marMean = marSamples.average()
                val headPitchMeanDeg = headPoseSamples.average()
                
                //calculate blinks per minute
                val elapsedSeconds = _uiState.value.timerSeconds
                val actualMinutes = if (elapsedSeconds > 0) {
                    elapsedSeconds / 60.0
                } else {
                    1.0 //dfault to 1 minute if calibration completed
                }
                val blinkPerMin = if (actualMinutes > 0) {
                    blinkCount / actualMinutes
                } else {
                    blinkCount.toDouble()
                }
                
                //noise is not implemented yet, use default value
                val noiseDbMean = 0.0
                
                val baseline = BaselineEntity(
                    userId = userId,
                    earMean = earMean,
                    marMean = marMean,
                    headPitchMeanDeg = headPitchMeanDeg,
                    blinkPerMin = blinkPerMin,
                    noiseDbMean = noiseDbMean
                )
                
                baselineRepository.upsert(baseline)
                
                _uiState.value = _uiState.value.copy(
                    isCompleted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to save baseline: ${e.message}"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

