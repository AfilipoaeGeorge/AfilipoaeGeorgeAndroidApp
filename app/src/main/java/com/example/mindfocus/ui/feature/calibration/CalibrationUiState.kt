package com.example.mindfocus.ui.feature.calibration

data class CalibrationUiState(
    val timerSeconds: Int = 0,
    val focusScore: Double = 0.0,
    val shouldVibrate: Boolean = false,
    val ear: Double = 0.0,
    val mar: Double = 0.0,
    val headPose: Double = 0.0,
    val isPaused: Boolean = false,
    val isRunning: Boolean = false,
    val faceDetected: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

