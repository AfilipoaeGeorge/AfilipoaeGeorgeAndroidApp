package com.example.mindfocus.ui.feature.session

data class SessionUiState(
    val timerSeconds: Int = 0,
    val focusScore: Double = 0.0,
    val shouldVibrate: Boolean = false,
    val ear: Double = 0.0,
    val mar: Double = 0.0,
    val headPose: Double = 0.0,
    val isPaused: Boolean = false,
    val isRunning: Boolean = false,
    val faceDetected: Boolean = false
)