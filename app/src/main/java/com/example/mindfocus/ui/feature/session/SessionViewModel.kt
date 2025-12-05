package com.example.mindfocus.ui.feature.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.core.datastore.SettingsPreferencesManager
import com.example.mindfocus.core.datastore.UserSettings
import com.example.mindfocus.core.face.FaceMetricsCalculator
import com.example.mindfocus.core.location.LocationManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import com.example.mindfocus.R

class SessionViewModel(
    private val context: Context,
    private val authPreferencesManager: AuthPreferencesManager,
    private val sessionRepository: SessionRepository,
    private val metricRepository: MetricRepository,
    private val baselineRepository: BaselineRepository,
    private val settingsPreferencesManager: SettingsPreferencesManager
) : ViewModel() {

    private val locationManager = LocationManager(context)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var saveMetricsJob: Job? = null
    private var alertAutoDismissJob: Job? = null
    private var lastVibrationTime = 0L

    private val metricsBuffer = ArrayDeque<Triple<Double, Double, Double>>()
    private val bufferSize = 60
    
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0L
    private var baseline: BaselineEntity? = null
    
    private val accumulatedMetrics = mutableListOf<MetricEntity>()
    private data class MetricValues(val focus: Double, val ear: Double, val mar: Double, val head: Double)
    private val metricsPerBucket = mutableMapOf<Int, MutableList<MetricValues>>()
    private var lastSaveTime: Long = 0L
    private val saveIntervalSeconds = 30L
    private var currentBucketSec = 0
    private var breaksCount = 0
    
    //accumulated values for session averages
    private val sessionFocusScores = mutableListOf<Double>()
    private val sessionEarValues = mutableListOf<Double>()
    private val sessionMarValues = mutableListOf<Double>()
    private val sessionHeadPoseValues = mutableListOf<Double>()

    private var userSettings: UserSettings = UserSettings()

    private val activeAlerts = mutableMapOf<SessionAlertType, SessionAlert>()
    private var earClosedStartTime: Long? = null
    private var blinkInProgress = false
    private val blinkTimestamps = ArrayDeque<Long>()
    private var lastBlinkTimestamp: Long? = null
    private var headDeviationStartTime: Long? = null
    private var lowFocusStartTime: Long? = null
    private var faceLostStartTime: Long? = null
    private var yawnInProgress = false
    private val yawnTimestamps = ArrayDeque<Long>()
    private var isUserPaused = false
    init {
        loadBaselineAndStartSession()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsPreferencesManager.settings.collect { settings ->
                val previousSettings = userSettings
                userSettings = settings
                _uiState.update {
                    it.copy(
                        cameraMonitoringEnabled = settings.cameraMonitoringEnabled
                    )
                }
                handleAlertSettingChanges(previousSettings, settings)
            }
        }
    }

    private fun loadBaselineAndStartSession() {
        viewModelScope.launch {
            try {
                val userId = authPreferencesManager.getCurrentUserId()
                if (userId != null) {
                    baseline = baselineRepository.getForUser(userId)
                    
                    val currentSettings = settingsPreferencesManager.settings.first()
                    val gpsEnabled = currentSettings.gpsEnabled
                    
                    android.util.Log.d("SessionViewModel", "Current GPS setting: $gpsEnabled")
                    
                    val locationData = if (gpsEnabled) {
                        val hasPermission = locationManager.hasLocationPermission()
                        val isLocationEnabled = locationManager.isLocationEnabled()
                        android.util.Log.d("SessionViewModel", "GPS enabled. Has permission: $hasPermission, Location services enabled: $isLocationEnabled")
                        
                        if (hasPermission) {
                            if (!isLocationEnabled) {
                                android.util.Log.w("SessionViewModel", "GPS enabled and permission granted, but location services are disabled on device")
                            }
                            
                            try {
                                android.util.Log.d("SessionViewModel", "Attempting to get current location...")
                                val currentLocation = locationManager.getCurrentLocation()
                                android.util.Log.d("SessionViewModel", "Current location: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
                                
                                if (currentLocation != null) {
                                    currentLocation
                                } else {
                                    android.util.Log.d("SessionViewModel", "Current location is null, trying last known location...")
                                    val lastKnownLocation = locationManager.getLastKnownLocation()
                                    android.util.Log.d("SessionViewModel", "Last known location: ${lastKnownLocation?.latitude}, ${lastKnownLocation?.longitude}")
                                    lastKnownLocation
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SessionViewModel", "Error getting location: ${e.message}", e)
                                null
                            }
                        } else {
                            android.util.Log.w("SessionViewModel", "GPS enabled but no location permission granted. Location will not be saved.")
                            null
                        }
                    } else {
                        android.util.Log.d("SessionViewModel", "GPS disabled. Location will not be saved.")
                        null
                    }
                    
                    android.util.Log.d("SessionViewModel", "Final location data: ${locationData?.latitude}, ${locationData?.longitude}")
                    
                    //start session in database
                    sessionStartTime = System.currentTimeMillis()
                    val session = SessionEntity(
                        userId = userId,
                        startedAtEpochMs = sessionStartTime,
                        endedAtEpochMs = null,
                        breaksCount = 0,
                        latitude = locationData?.latitude,
                        longitude = locationData?.longitude
                    )
                    currentSessionId = sessionRepository.start(session)
                    android.util.Log.d("SessionViewModel", "Session created with ID: $currentSessionId, Location: ${session.latitude}, ${session.longitude}")
                    
                    currentSessionId?.let { sessionId ->
                        val savedSession = sessionRepository.getById(sessionId)
                        android.util.Log.d("SessionViewModel", "Verified saved session - ID: ${savedSession?.id}, Location: ${savedSession?.latitude}, ${savedSession?.longitude}")
                    }
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
        isUserPaused = false
        setAutoPaused(false)
        activeAlerts.clear()
        publishAlerts()
        clearContinuousTracking()
        lastBlinkTimestamp = System.currentTimeMillis()
        startTimer()
        startPeriodicMetricSaving()
        startAlertAutoDismiss()
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
    
    private fun startAlertAutoDismiss() {
        alertAutoDismissJob?.cancel()
        alertAutoDismissJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(500) // check every 500ms
                val now = System.currentTimeMillis()
                val alertsToRemove = activeAlerts.values.filter { alert ->
                    alert.type != SessionAlertType.FACE_LOST && 
                    (now - alert.timestamp) >= 5000L // 5 seconds
                }
                alertsToRemove.forEach { alert ->
                    removeAlert(alert.type)
                }
            }
        }
    }
    
    private suspend fun saveAccumulatedMetrics() {
        if (currentSessionId == null) return
        
        try {
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
        val isManualPause = currentState.isPaused && !currentState.isAutoPaused

        if (!userSettings.cameraMonitoringEnabled) {
            resetTrackingForMonitoringDisabled()
            return
        }

        val now = System.currentTimeMillis()

        if (result == null || result.faceLandmarks().isEmpty()) {
            if (isManualPause) {
                updateAlertStates(
                    ear = null,
                    mar = null,
                    head = null,
                    focusScore = currentState.focusScore,
                    faceDetected = false,
                    now = now,
                    allowUserAlerts = false
                )
                return
            }

            val currentScore = currentState.focusScore
            val decayed = (currentScore - 2).coerceAtLeast(0.0)

            _uiState.value = _uiState.value.copy(
                faceDetected = false,
                focusScore = decayed
            )

            updateAlertStates(
                ear = null,
                mar = null,
                head = null,
                focusScore = decayed,
                faceDetected = false,
                now = now
            )
            return
        }

        if (isManualPause) {
            updateAlertStates(
                ear = null,
                mar = null,
                head = null,
                focusScore = currentState.focusScore,
                faceDetected = true,
                now = now,
                allowUserAlerts = false
            )
            return
        }

        if (metricsBuffer.size >= bufferSize) metricsBuffer.removeFirst()

        val (ear, mar, head) = FaceMetricsCalculator.calculateFromResult(result)
        metricsBuffer.addLast(Triple(ear, mar, head))

        val avgEar = metricsBuffer.map { it.first }.average()
        val avgMar = metricsBuffer.map { it.second }.average()
        val avgHead = metricsBuffer.map { it.third }.average()

        val earRef = (baseline?.earMean ?: 0.25).takeIf { it > 0 } ?: 0.25
        val marRef = baseline?.marMean ?: 0.01
        val headRef = baseline?.headPitchMeanDeg ?: 0.0

        val w1 = 0.5
        val w2 = 0.2
        val w3 = 0.3

        val earNorm = (avgEar / earRef).coerceIn(0.0, 1.0)

        val marNorm = if (avgMar <= marRef) {
            1.0
        } else {
            val maxMar = 0.5
            val adjustedMarRef = marRef + (maxMar - marRef) * 0.3
            if (avgMar <= adjustedMarRef) {
                1.0 - ((avgMar - marRef) / (adjustedMarRef - marRef)) * 0.5
            } else {
                ((maxMar - avgMar) / (maxMar - adjustedMarRef)).coerceIn(0.0, 0.5)
            }
        }

        val headDeviation = kotlin.math.abs(avgHead - headRef)
        val headNorm = (1 - (headDeviation / 45.0)).coerceIn(0.0, 1.0)

        val focusScore = (w1 * earNorm + w2 * marNorm + w3 * headNorm).coerceIn(0.0, 1.0) * 100
        val displayMar = avgMar

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

        updateAlertStates(
            ear = avgEar,
            mar = avgMar,
            head = avgHead,
            focusScore = focusScore,
            faceDetected = true,
            now = now
        )

        if (!_uiState.value.isPaused && currentSessionId != null) {
            accumulateMetric(focusScore, avgEar, avgMar, avgHead)
        }
    }
    
    private fun accumulateMetric(focusScore: Double, ear: Double, mar: Double, headPose: Double) {
        val now = System.currentTimeMillis()
        val elapsedSeconds = ((now - sessionStartTime) / 1000).toInt()
        
        val bucketSec = (elapsedSeconds / saveIntervalSeconds.toInt()) * saveIntervalSeconds.toInt()
        
        if (!metricsPerBucket.containsKey(bucketSec)) {
            metricsPerBucket[bucketSec] = mutableListOf()
        }
        metricsPerBucket[bucketSec]?.add(MetricValues(focusScore, ear, mar, headPose))
        
        if (now - lastSaveTime >= saveIntervalSeconds * 1000) {
            viewModelScope.launch {
                saveAccumulatedMetrics()
                lastSaveTime = now
            }
        }
    }

    private fun updateAlertStates(
        ear: Double?,
        mar: Double?,
        head: Double?,
        focusScore: Double,
        faceDetected: Boolean,
        now: Long,
        allowUserAlerts: Boolean = true
    ) {
        val faceDetectedNow = faceDetected
        if (!faceDetectedNow) {
            if (faceLostStartTime == null) {
                faceLostStartTime = now
            }

            clearContinuousTracking(exceptFace = true)

            if (now - (faceLostStartTime ?: now) >= FACE_LOST_THRESHOLD_MS) {
                if (isAlertEnabled(SessionAlertType.FACE_LOST)) {
                    addAlert(
                        SessionAlertType.FACE_LOST,
                        context.getString(R.string.session_alert_face_lost)
                    )
                    if (!isUserPaused) {
                        setAutoPaused(true)
                    }
                } else {
                    faceLostStartTime = null
                }
            }
            return
        } else {
            faceLostStartTime = null
            removeAlert(SessionAlertType.FACE_LOST)
            if (_uiState.value.isAutoPaused && !isUserPaused) {
                setAutoPaused(false)
            }
            if (lastBlinkTimestamp == null) {
                lastBlinkTimestamp = now
            }
        }

        val earBaseline = (baseline?.earMean ?: 0.25).takeIf { it > 0 } ?: 0.25
        val marBaseline = baseline?.marMean ?: 0.01
        val headBaseline = baseline?.headPitchMeanDeg ?: 0.0

        if (!allowUserAlerts) {
            clearUserManagedAlerts()
            return
        }

        ear?.let { currentEar ->
            if (userSettings.eyesClosedAlertsEnabled) {
                val eyeClosedThreshold = earBaseline * 0.7
                if (currentEar <= eyeClosedThreshold) {
                    if (earClosedStartTime == null) {
                        earClosedStartTime = now
                    }
                    if (now - (earClosedStartTime ?: now) >= EYES_CLOSED_THRESHOLD_MS) {
                        addAlert(
                            SessionAlertType.EYES_CLOSED,
                            context.getString(R.string.session_alert_eyes_closed)
                        )
                    }
                } else {
                    earClosedStartTime = null
                    removeAlert(SessionAlertType.EYES_CLOSED)
                }
            } else {
                earClosedStartTime = null
                removeAlert(SessionAlertType.EYES_CLOSED)
            }

            if (userSettings.blinkAlertsEnabled) {
                val blinkThreshold = earBaseline * 0.8
                if (currentEar < blinkThreshold) {
                    if (!blinkInProgress) {
                        blinkInProgress = true
                        blinkTimestamps.addLast(now)
                        lastBlinkTimestamp = now
                        removeAlert(SessionAlertType.LOW_BLINK_RATE)
                    }
                } else {
                    blinkInProgress = false
                }
            } else {
                blinkInProgress = false
                blinkTimestamps.clear()
                removeAlert(SessionAlertType.LOW_BLINK_RATE)
            }
        } ?: run {
            earClosedStartTime = null
            blinkInProgress = false
            blinkTimestamps.clear()
        }

        while (blinkTimestamps.isNotEmpty() && now - blinkTimestamps.first() > BLINK_WINDOW_MS) {
            blinkTimestamps.removeFirst()
        }

        if (userSettings.blinkAlertsEnabled && allowUserAlerts) {
            val noBlinkForWindow = lastBlinkTimestamp != null && now - (lastBlinkTimestamp ?: now) >= BLINK_WINDOW_MS
            val lowBlinkInWindow = blinkTimestamps.isNotEmpty() &&
                now - blinkTimestamps.first() >= BLINK_WINDOW_MS &&
                blinkTimestamps.size < MIN_BLINKS_PER_WINDOW

            if (noBlinkForWindow || lowBlinkInWindow) {
                addAlert(
                    SessionAlertType.LOW_BLINK_RATE,
                    context.getString(R.string.session_alert_low_blink_rate)
                )
            } else {
                removeAlert(SessionAlertType.LOW_BLINK_RATE)
            }
        } else {
            removeAlert(SessionAlertType.LOW_BLINK_RATE)
        }

        head?.let { currentHead ->
            if (userSettings.headPoseAlertsEnabled) {
                val deviation = kotlin.math.abs(currentHead - headBaseline)
                if (deviation >= HEAD_DEVIATION_DEGREES) {
                    if (headDeviationStartTime == null) {
                        headDeviationStartTime = now
                    }
                    if (now - (headDeviationStartTime ?: now) >= HEAD_DEVIATION_THRESHOLD_MS) {
                        addAlert(
                            SessionAlertType.HEAD_POSE_DEVIATION,
                            context.getString(R.string.session_alert_head_pose)
                        )
                    }
                } else {
                    headDeviationStartTime = null
                    removeAlert(SessionAlertType.HEAD_POSE_DEVIATION)
                }
            } else {
                headDeviationStartTime = null
                removeAlert(SessionAlertType.HEAD_POSE_DEVIATION)
            }
        } ?: run {
            headDeviationStartTime = null
        }

        mar?.let { currentMar ->
            if (userSettings.yawnAlertsEnabled) {
                val yawnThreshold = maxOf(marBaseline * 3, marBaseline + 0.2, 0.6)
                if (currentMar >= yawnThreshold) {
                    if (!yawnInProgress) {
                        yawnInProgress = true
                        yawnTimestamps.addLast(now)
                    }
                } else if (currentMar <= yawnThreshold * 0.8) {
                    yawnInProgress = false
                }
            } else {
                yawnInProgress = false
                yawnTimestamps.clear()
                removeAlert(SessionAlertType.REPEATED_YAWN)
            }
        } ?: run {
            yawnInProgress = false
            yawnTimestamps.clear()
        }

        while (yawnTimestamps.isNotEmpty() && now - yawnTimestamps.first() > YAWN_WINDOW_MS) {
            yawnTimestamps.removeFirst()
        }

        if (userSettings.yawnAlertsEnabled && allowUserAlerts) {
            if (yawnTimestamps.size >= MIN_YAWNS_IN_WINDOW) {
                addAlert(
                    SessionAlertType.REPEATED_YAWN,
                    context.getString(R.string.session_alert_repeated_yawn)
                )
            } else {
                removeAlert(SessionAlertType.REPEATED_YAWN)
            }
        } else {
            removeAlert(SessionAlertType.REPEATED_YAWN)
        }

        if (userSettings.lowFocusAlertsEnabled && allowUserAlerts) {
            if (focusScore < LOW_FOCUS_THRESHOLD) {
                if (lowFocusStartTime == null) {
                    lowFocusStartTime = now
                }
                if (now - (lowFocusStartTime ?: now) >= LOW_FOCUS_DURATION_MS) {
                    addAlert(
                        SessionAlertType.PERSISTENT_LOW_FOCUS,
                        context.getString(R.string.session_alert_persistent_low_focus)
                    )
                }
            } else {
                lowFocusStartTime = null
                removeAlert(SessionAlertType.PERSISTENT_LOW_FOCUS)
            }
        } else {
            lowFocusStartTime = null
            removeAlert(SessionAlertType.PERSISTENT_LOW_FOCUS)
        }
    }

    private fun addAlert(
        type: SessionAlertType,
        message: String
    ) {
        if (!isAlertEnabled(type)) {
            return
        }

        val wasPresent = activeAlerts.containsKey(type)
        if (!wasPresent) {
            activeAlerts[type] = SessionAlert(type, message, System.currentTimeMillis())
            publishAlerts()
            if (!isUserPaused) {
                triggerVibration()
            }
        }
    }

    private fun removeAlert(type: SessionAlertType) {
        if (activeAlerts.remove(type) != null) {
            publishAlerts()
        }
    }

    private fun publishAlerts() {
        val ordered = activeAlerts.values.sortedBy { it.type.ordinal }
        _uiState.update { it.copy(alerts = ordered) }
    }

    private fun triggerVibration() {
        val now = System.currentTimeMillis()
        if (now - lastVibrationTime < MIN_VIBRATION_INTERVAL_MS) {
            return
        }
        lastVibrationTime = now
        _uiState.update { it.copy(shouldVibrate = true) }
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(shouldVibrate = false) }
        }
    }

    private fun setAutoPaused(enabled: Boolean) {
        val current = _uiState.value
        if (enabled) {
            if (!current.isAutoPaused) {
                _uiState.value = current.copy(isPaused = true, isAutoPaused = true)
            }
        } else {
            if (current.isAutoPaused) {
                _uiState.value = current.copy(
                    isPaused = isUserPaused,
                    isAutoPaused = false
                )
            }
        }
    }

    private fun clearContinuousTracking(exceptFace: Boolean = false) {
        if (!exceptFace) {
            faceLostStartTime = null
            removeAlert(SessionAlertType.FACE_LOST)
        }
        earClosedStartTime = null
        blinkInProgress = false
        blinkTimestamps.clear()
        lastBlinkTimestamp = null
        headDeviationStartTime = null
        lowFocusStartTime = null
        yawnInProgress = false
        yawnTimestamps.clear()
        removeAlert(SessionAlertType.EYES_CLOSED)
        removeAlert(SessionAlertType.LOW_BLINK_RATE)
        removeAlert(SessionAlertType.HEAD_POSE_DEVIATION)
        removeAlert(SessionAlertType.REPEATED_YAWN)
        removeAlert(SessionAlertType.PERSISTENT_LOW_FOCUS)
    }

    private fun clearUserManagedAlerts() {
        val removed = activeAlerts.keys.filter {
            it != SessionAlertType.FACE_LOST
        }
        if (removed.isNotEmpty()) {
            removed.forEach { activeAlerts.remove(it) }
            publishAlerts()
        }
        earClosedStartTime = null
        headDeviationStartTime = null
        lowFocusStartTime = null
        blinkInProgress = false
        yawnInProgress = false
        blinkTimestamps.clear()
        yawnTimestamps.clear()
        lastBlinkTimestamp = null
        _uiState.update { it.copy(shouldVibrate = false) }
    }

    private fun resetTrackingForMonitoringDisabled() {
        clearContinuousTracking()
        activeAlerts.clear()
        publishAlerts()
        lastBlinkTimestamp = null
        setAutoPaused(false)
        _uiState.value = _uiState.value.copy(
            focusScore = 0.0,
            faceDetected = false
        )
    }

    private fun handleAlertSettingChanges(previous: UserSettings, current: UserSettings) {
        if (!current.eyesClosedAlertsEnabled && previous.eyesClosedAlertsEnabled) {
            earClosedStartTime = null
            removeAlert(SessionAlertType.EYES_CLOSED)
        }

        if (!current.blinkAlertsEnabled && previous.blinkAlertsEnabled) {
            blinkInProgress = false
            blinkTimestamps.clear()
            lastBlinkTimestamp = null
            removeAlert(SessionAlertType.LOW_BLINK_RATE)
        } else if (current.blinkAlertsEnabled && !previous.blinkAlertsEnabled) {
            lastBlinkTimestamp = System.currentTimeMillis()
        }

        if (!current.headPoseAlertsEnabled && previous.headPoseAlertsEnabled) {
            headDeviationStartTime = null
            removeAlert(SessionAlertType.HEAD_POSE_DEVIATION)
        }

        if (!current.yawnAlertsEnabled && previous.yawnAlertsEnabled) {
            yawnInProgress = false
            yawnTimestamps.clear()
            removeAlert(SessionAlertType.REPEATED_YAWN)
        }

        if (!current.lowFocusAlertsEnabled && previous.lowFocusAlertsEnabled) {
            lowFocusStartTime = null
            removeAlert(SessionAlertType.PERSISTENT_LOW_FOCUS)
        }

        if (!current.faceLostAlertsEnabled && previous.faceLostAlertsEnabled) {
            faceLostStartTime = null
            removeAlert(SessionAlertType.FACE_LOST)
            setAutoPaused(false)
        }
    }

    private fun isAlertEnabled(type: SessionAlertType): Boolean {
        return when (type) {
            SessionAlertType.EYES_CLOSED -> userSettings.eyesClosedAlertsEnabled
            SessionAlertType.LOW_BLINK_RATE -> userSettings.blinkAlertsEnabled
            SessionAlertType.HEAD_POSE_DEVIATION -> userSettings.headPoseAlertsEnabled
            SessionAlertType.REPEATED_YAWN -> userSettings.yawnAlertsEnabled
            SessionAlertType.PERSISTENT_LOW_FOCUS -> userSettings.lowFocusAlertsEnabled
            SessionAlertType.FACE_LOST -> userSettings.faceLostAlertsEnabled
        }
    }

    companion object {
        private const val FACE_LOST_THRESHOLD_MS = 2_000L
        private const val EYES_CLOSED_THRESHOLD_MS = 3_000L
        private const val BLINK_WINDOW_MS = 60_000L
        private const val MIN_BLINKS_PER_WINDOW = 5
        private const val HEAD_DEVIATION_DEGREES = 25.0
        private const val HEAD_DEVIATION_THRESHOLD_MS = 10_000L
        private const val YAWN_WINDOW_MS = 5 * 60_000L
        private const val MIN_YAWNS_IN_WINDOW = 3
        private const val LOW_FOCUS_THRESHOLD = 60.0
        private const val LOW_FOCUS_DURATION_MS = 5 * 60_000L
        private const val MIN_VIBRATION_INTERVAL_MS = 5_000L
    }

    fun pauseSession() {
        val wasPaused = _uiState.value.isPaused
        isUserPaused = true
        setAutoPaused(false)
        _uiState.value = _uiState.value.copy(isPaused = true, isAutoPaused = false)
        if (!wasPaused) {
            breaksCount++
        }
    }

    fun resumeSession() {
        isUserPaused = false
        setAutoPaused(false)
        removeAlert(SessionAlertType.FACE_LOST)
        _uiState.value = _uiState.value.copy(isPaused = false, isAutoPaused = false)
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
                if (metricsPerBucket.isNotEmpty()) {
                    saveAccumulatedMetrics()
                }
                
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
                    
                    val existingSession = sessionRepository.getById(currentSessionId!!)
                    android.util.Log.d("SessionViewModel", "Closing session - existing session location: ${existingSession?.latitude}, ${existingSession?.longitude}")
                    
                    sessionRepository.close(
                        id = currentSessionId!!,
                        endMs = endTime,
                        breaks = breaksCount,
                        focusAvg = focusAvg,
                        earAvg = earAvg,
                        marAvg = marAvg,
                        headPitchAvgDegrees = headPitchAvg,
                        latitude = existingSession?.latitude,
                        longitude = existingSession?.longitude
                    )
                    android.util.Log.d("SessionViewModel", "Session closed: ID=$currentSessionId, FocusAvg=$focusAvg, Location: ${existingSession?.latitude}, ${existingSession?.longitude}")
                    
                    val closedSession = sessionRepository.getById(currentSessionId!!)
                    android.util.Log.d("SessionViewModel", "Verified closed session - ID: ${closedSession?.id}, Location: ${closedSession?.latitude}, ${closedSession?.longitude}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error closing session: ${e.message}", e)
            } finally {
                isUserPaused = false
                clearContinuousTracking()
                activeAlerts.clear()
                publishAlerts()
                timerJob?.cancel()
                saveMetricsJob?.cancel()
                alertAutoDismissJob?.cancel()
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
        alertAutoDismissJob?.cancel()
    }
}
