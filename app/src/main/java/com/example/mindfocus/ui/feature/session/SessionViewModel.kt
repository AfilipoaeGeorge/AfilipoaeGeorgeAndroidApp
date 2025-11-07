package com.example.mindfocus.ui.feature.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.core.face.FaceMetricsCalculator
import com.example.mindfocus.data.local.entities.BaselineEntity
import com.example.mindfocus.data.local.entities.MetricEntity
import com.example.mindfocus.data.local.entities.SessionEntity
import com.example.mindfocus.data.repository.BaselineRepository
import com.example.mindfocus.data.repository.MetricRepository
import com.example.mindfocus.data.repository.SessionRepository
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val sessionRepository: SessionRepository,
    private val metricRepository: MetricRepository,
    private val baselineRepository: BaselineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var saveMetricsJob: Job? = null
    private var lastVibrationTime = 0L

    private val metricsBuffer = ArrayDeque<Triple<Double, Double, Double>>()
    private val bufferSize = 60
    
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0L
    private var baseline: BaselineEntity? = null
    
    //metrics accumulation for periodic saving
    private val accumulatedMetrics = mutableListOf<MetricEntity>()
    //data class for metric values
    private data class MetricValues(val focus: Double, val ear: Double, val mar: Double, val head: Double)
    private val metricsPerBucket = mutableMapOf<Int, MutableList<MetricValues>>() // bucketSec -> list of metric values
    private var lastSaveTime: Long = 0L
    //save every 30 seconds to reduce database size
    //2-hour session: 7200 seconds / 30 = 240 records
    //1-hour session: 3600 seconds / 30 = 120 records
    private val saveIntervalSeconds = 30L
    private var currentBucketSec = 0
    private var breaksCount = 0
    
    //accumulated values for session averages
    private val sessionFocusScores = mutableListOf<Double>()
    private val sessionEarValues = mutableListOf<Double>()
    private val sessionMarValues = mutableListOf<Double>()
    private val sessionHeadPoseValues = mutableListOf<Double>()

    init {
        loadBaselineAndStartSession()
    }

    private fun loadBaselineAndStartSession() {
        viewModelScope.launch {
            try {
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId != null) {
                    baseline = baselineRepository.getForUser(userId)
                    
                    //start session in database
                    sessionStartTime = System.currentTimeMillis()
                    val session = SessionEntity(
                        userId = userId,
                        startedAtEpochMs = sessionStartTime,
                        endedAtEpochMs = null,
                        breaksCount = 0
                    )
                    currentSessionId = sessionRepository.start(session)
                    lastSaveTime = sessionStartTime
                }
                
                startSession()
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error loading baseline: ${e.message}", e)
                startSession() //start anyway, will use default values
            }
        }
    }

    fun startSession() {
        startTimer()
        startPeriodicMetricSaving()
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
                    currentBucketSec = t
                }
            }
        }
    }
    
    private fun startPeriodicMetricSaving() {
        saveMetricsJob?.cancel()
        saveMetricsJob = viewModelScope.launch {
            while (currentSessionId != null && _uiState.value.isRunning) {
                delay(saveIntervalSeconds * 1000)
                if (!_uiState.value.isPaused && accumulatedMetrics.isNotEmpty()) {
                    saveAccumulatedMetrics()
                }
            }
        }
    }
    
    private suspend fun saveAccumulatedMetrics() {
        if (currentSessionId == null) return
        
        try {
            // convert metricsPerBucket to MetricEntity list, averaging values within each bucket
            val metricsToSave = mutableListOf<MetricEntity>()
            
            metricsPerBucket.forEach { (bucketSec, values) ->
                if (values.isNotEmpty() && currentSessionId != null) {
                    val avgFocus = values.map { it.focus }.average()
                    val avgEar = values.map { it.ear }.average()
                    val avgMar = values.map { it.mar }.average()
                    val avgHead = values.map { it.head }.average()
                    
                    metricsToSave.add(
                        MetricEntity(
                            sessionId = currentSessionId!!,
                            bucketSec = bucketSec,
                            focusScore = avgFocus,
                            ear = avgEar,
                            mar = avgMar,
                            headPitchDeg = avgHead
                        )
                    )
                }
            }
            
            if (metricsToSave.isNotEmpty()) {
                metricRepository.insertBatch(metricsToSave)
                accumulatedMetrics.addAll(metricsToSave)
                android.util.Log.d("SessionViewModel", "Saved ${metricsToSave.size} metrics to database")
            }
            
            metricsPerBucket.clear()
        } catch (e: Exception) {
            android.util.Log.e("SessionViewModel", "Error saving metrics: ${e.message}", e)
        }
    }

    fun updateFaceMetrics(result: FaceLandmarkerResult?) {
        val currentState = _uiState.value
        if (currentState.isPaused) {
            return
        }

        if (result == null || result.faceLandmarks().isEmpty()) {
            val currentScore = currentState.focusScore
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

        //use baseline values if available, otherwise use defaults
        val earRef = baseline?.earMean ?: 0.25
        val marRef = baseline?.marMean ?: 0.01
        val headRef = baseline?.headPitchMeanDeg ?: 0.0
        
        val w1 = 0.5
        val w2 = 0.2
        val w3 = 0.3

        //normalize based on baseline: if user has smaller eyes naturally, don't penalize them
        //use a relative approach: compare current value to baseline
        val earNorm = if (earRef > 0) {
            //if current EAR is close to or above baseline, normalize positively
            val relativeEar = avgEar / earRef
            relativeEar.coerceIn(0.0, 1.0)
        } else {
            (avgEar / 0.25).coerceIn(0.0, 1.0)
        }
        
        val marNorm = if (avgMar <= marRef) {
            1.0
        } else {
            //MAR should be low, so if it's higher than baseline, penalize more gradually
            val maxMar = 0.5
            val adjustedMarRef = marRef + (maxMar - marRef) * 0.3 // Allow some tolerance
            if (avgMar <= adjustedMarRef) {
                1.0 - ((avgMar - marRef) / (adjustedMarRef - marRef)) * 0.5
            } else {
                ((maxMar - avgMar) / (maxMar - adjustedMarRef)).coerceIn(0.0, 0.5)
            }
        }
        
        //head pose: compare deviation from baseline
        val headDeviation = kotlin.math.abs(avgHead - headRef)
        val headNorm = (1 - (headDeviation / 45.0)).coerceIn(0.0, 1.0)

        val focusScore = (w1 * earNorm + w2 * marNorm + w3 * headNorm).coerceIn(0.0, 1.0) * 100
        val displayMar = avgMar

        //accumulate values for session averages
        if (!_uiState.value.isPaused) {
            sessionFocusScores.add(focusScore)
            sessionEarValues.add(avgEar)
            sessionMarValues.add(avgMar)
            sessionHeadPoseValues.add(avgHead)
        }

        _uiState.value = _uiState.value.copy(
            ear = avgEar,
            mar = displayMar,
            headPose = avgHead,
            focusScore = focusScore,
            faceDetected = true
        )

        checkFocusAlert(focusScore)
        
        // accumulate metric for periodic saving (only when not paused)
        if (!_uiState.value.isPaused && currentSessionId != null) {
            accumulateMetric(focusScore, avgEar, avgMar, avgHead)
        }
    }
    
    private fun accumulateMetric(focusScore: Double, ear: Double, mar: Double, headPose: Double) {
        val now = System.currentTimeMillis()
        val elapsedSeconds = ((now - sessionStartTime) / 1000).toInt()
        
        // create metric for current time bucket
        // each bucket represents a time period (e.g., every 15 seconds)
        val bucketSec = (elapsedSeconds / saveIntervalSeconds.toInt()) * saveIntervalSeconds.toInt()
        
        // add to bucket for averaging later
        if (!metricsPerBucket.containsKey(bucketSec)) {
            metricsPerBucket[bucketSec] = mutableListOf()
        }
        metricsPerBucket[bucketSec]?.add(MetricValues(focusScore, ear, mar, headPose))
        
        // auto-save if enough time has passed
        if (now - lastSaveTime >= saveIntervalSeconds * 1000) {
            viewModelScope.launch {
                saveAccumulatedMetrics()
                lastSaveTime = now
            }
        }
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
        breaksCount++
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
        viewModelScope.launch {
            try {
                // save any remaining accumulated metrics
                if (metricsPerBucket.isNotEmpty()) {
                    saveAccumulatedMetrics()
                }
                
                // close session with averages
                if (currentSessionId != null) {
                    val endTime = System.currentTimeMillis()
                    val focusAvg = if (sessionFocusScores.isNotEmpty()) {
                        sessionFocusScores.average()
                    } else null
                    val earAvg = if (sessionEarValues.isNotEmpty()) {
                        sessionEarValues.average()
                    } else null
                    val marAvg = if (sessionMarValues.isNotEmpty()) {
                        sessionMarValues.average()
                    } else null
                    val headPitchAvg = if (sessionHeadPoseValues.isNotEmpty()) {
                        sessionHeadPoseValues.average()
                    } else null
                    
                    sessionRepository.close(
                        id = currentSessionId!!,
                        endMs = endTime,
                        breaks = breaksCount,
                        focusAvg = focusAvg,
                        earAvg = earAvg,
                        marAvg = marAvg,
                        headPitchAvgDegrees = headPitchAvg
                    )
                    
                    // metrics storage strategy:
                    // - metrics are saved every 30 seconds during session
                    // - 2-hour session: ~240 records (7200s / 30s)
                    // - 1-hour session: ~120 records (3600s / 30s)
                    // - when displaying graphs, use MetricRepository.getForSessionForGraph() 
                    //   which downsamples to max 150 points for efficient rendering
                    // - SessionEntity contains summary data (averages) for history list
                    // - if storage becomes an issue, you can clean up old metrics (e.g., older than 30 days)
                    //   by calling: metricRepository.deleteForSession(sessionId) for old sessions
                    
                    android.util.Log.d("SessionViewModel", "Session closed: ID=$currentSessionId, FocusAvg=$focusAvg")
                }
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error closing session: ${e.message}", e)
            } finally {
                // clean up
                timerJob?.cancel()
                saveMetricsJob?.cancel()
                metricsBuffer.clear()
                accumulatedMetrics.clear()
                metricsPerBucket.clear()
                sessionFocusScores.clear()
                sessionEarValues.clear()
                sessionMarValues.clear()
                sessionHeadPoseValues.clear()
                currentSessionId = null
                _uiState.value = SessionUiState()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (currentSessionId != null) {
            stopSession()
        }
        timerJob?.cancel()
        saveMetricsJob?.cancel()
    }
}
