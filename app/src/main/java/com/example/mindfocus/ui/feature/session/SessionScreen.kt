package com.example.mindfocus.ui.feature.session

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindfocus.R
import com.example.mindfocus.core.camera.FaceLandmarkerHelper
import com.example.mindfocus.core.camera.getCameraProvider
import com.example.mindfocus.core.vibration.VibrationHelper
import com.example.mindfocus.core.datastore.SettingsPreferencesManager
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.BaselineRepository
import com.example.mindfocus.data.repository.MetricRepository
import com.example.mindfocus.data.repository.SessionRepository
import com.example.mindfocus.ui.feature.session.components.CameraPermission
import com.example.mindfocus.ui.feature.session.components.OverlayCamera
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.max

@Composable
fun SessionScreen(
    onStopClick: () -> Unit = {},
    onPauseResumeClick: () -> Unit = {},
    onFlipCameraClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val sessionRepository = remember { SessionRepository(database) }
    val metricRepository = remember { MetricRepository(database) }
    val baselineRepository = remember { BaselineRepository(database) }
    val settingsPreferencesManager = remember { SettingsPreferencesManager(context) }
    
    val viewModel: SessionViewModel = viewModel {
        SessionViewModel(
            context = context,
            authPreferencesManager = authPreferencesManager,
            sessionRepository = sessionRepository,
            metricRepository = metricRepository,
            baselineRepository = baselineRepository,
            settingsPreferencesManager = settingsPreferencesManager
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    
    val faceImage = remember { mutableStateOf<MPImage?>(null) }
    val faceResult = remember { mutableStateOf<FaceLandmarkerResult?>(null) }
    
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    val faceHelper = remember {
        FaceLandmarkerHelper(
            context,
            faceLandmarkErrorListener = { error ->
                mainHandler.post {
                }
            },
            faceLandmarkResultListener = { result, image ->
                mainHandler.post {
                    try {
                        faceImage.value = image
                        faceResult.value = result
                        viewModel.updateFaceMetrics(result)
                        
                        val currentState = viewModel.uiState.value
                        if (currentState.shouldVibrate) {
                            VibrationHelper.vibrate(context)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SessionScreen", "Error updating UI: ${e.message}", e)
                    }
                }
            }
        )
    }
    
    val cameraExecutor = remember { Executors.newSingleThreadScheduledExecutor() }
    
    val lensFacing = remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    
    LaunchedEffect(Unit) {
        if (!uiState.isRunning) {
            viewModel.startSession()
        }
    }
    
    LaunchedEffect(uiState.shouldVibrate) {
        if (uiState.shouldVibrate) {
            try {
                VibrationHelper.vibrate(context)
            } catch (e: Exception) {
                android.util.Log.e("SessionScreen", "Error in vibration: ${e.message}", e)
            }
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                lifecycleOwner.lifecycleScope.launch {
                    cameraExecutor.shutdown()
                    val provider = context.getCameraProvider()
                    provider.unbindAll()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                cameraExecutor.shutdown()
                faceHelper.clearFaceLandmarker()
            }
    }
    
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    val preview = Preview.Builder()
        .build()
    
    val imageAnalysis = ImageAnalysis.Builder()
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    
    LaunchedEffect(lensFacing.value, hasCameraPermission, uiState.cameraMonitoringEnabled) {
        if (!hasCameraPermission) {
            viewModel.freezeUi()
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.unfreezeUi()
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing.value)
                .build()
            
            val provider = context.getCameraProvider()
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            if (!uiState.isPaused && uiState.cameraMonitoringEnabled) {
                setImageAnalyzer(imageAnalysis, cameraExecutor, faceHelper, lensFacing.value)
            } else {
                imageAnalysis.setAnalyzer(cameraExecutor) { image -> image.close() }
            }
        }
    }
    
    LaunchedEffect(uiState.isPaused, hasCameraPermission, uiState.cameraMonitoringEnabled) {
        if (hasCameraPermission) {
            if (!uiState.isPaused && uiState.cameraMonitoringEnabled) {
                setImageAnalyzer(imageAnalysis, cameraExecutor, faceHelper, lensFacing.value)
            } else {
                imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                    image.close()
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorResource(R.color.darkcharcoal),
                        colorResource(R.color.darkslategray)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorResource(R.color.amber)
                        )
                    }
                    Text(
                        text = stringResource(R.string.session_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.amber)
                    )
                }
                
                IconButton(
                    onClick = {
                        lensFacing.value = if (lensFacing.value == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                        onFlipCameraClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = stringResource(R.string.flip_camera),
                        tint = colorResource(R.color.amber)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (!uiState.cameraMonitoringEnabled) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(colorResource(R.color.darkcharcoal).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.settings_camera_disabled_message),
                                color = colorResource(R.color.lightsteelblue),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (!hasCameraPermission) {
                        CameraPermission(
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.matchParentSize()
                        )
                    } else if (!uiState.isPaused && faceImage.value != null && faceResult.value != null) {
                        val imageWidth = faceImage.value?.width ?: 0
                        val imageHeight = faceImage.value?.height ?: 0
                        
                        var overlaySize by remember { mutableStateOf(android.util.Size(0, 0)) }
                        
                        val scaleFactor = if (imageWidth > 0 && imageHeight > 0 && overlaySize.width > 0 && overlaySize.height > 0) {
                            max(overlaySize.width * 1f / imageWidth, overlaySize.height * 1f / imageHeight)
                        } else 1f
                        
                        OverlayCamera(
                            faceResult = faceResult.value,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            scaleFactor = scaleFactor,
                            modifier = Modifier
                                .matchParentSize()
                                .onSizeChanged { size ->
                                    overlaySize = android.util.Size(size.width, size.height)
                                }
                        )
                    }
                }
            }
            
            FocusScoreCard(
                score = uiState.focusScore.toInt(),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (uiState.alerts.isNotEmpty()) {
                SessionAlertsCard(
                    alerts = uiState.alerts,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = stringResource(R.string.ear_label),
                    value = String.format("%.2f", uiState.ear),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = stringResource(R.string.mar_label),
                    value = String.format("%.2f", uiState.mar),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = stringResource(R.string.head_pose_label),
                    value = String.format("%.1f°", uiState.headPose),
                    modifier = Modifier.weight(1f)
                )
            }
            
            SessionTimerCard(
                elapsedSeconds = uiState.timerSeconds,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.stopSession()
                        onStopClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.coralred)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = stringResource(R.string.stop_session),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = {
                        if (uiState.isPaused) {
                            viewModel.resumeSession()
                        } else {
                            viewModel.pauseSession()
                        }
                        onPauseResumeClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.skyblue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (uiState.isPaused) stringResource(R.string.resume_button) else stringResource(R.string.pause_button),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isPaused) stringResource(R.string.resume_button) else stringResource(R.string.pause_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun setImageAnalyzer(
    imageAnalysis: ImageAnalysis,
    cameraExecutor: java.util.concurrent.ExecutorService,
    faceLandmarkerHelper: FaceLandmarkerHelper,
    lensFacing: Int
) {
    imageAnalysis.setAnalyzer(cameraExecutor) { image ->
        try {
            faceLandmarkerHelper.detect(image, lensFacing == CameraSelector.LENS_FACING_FRONT)
        } catch (e: Exception) {
            android.util.Log.e("ImageAnalysis", "Error processing image: ${e.message}", e)
        } finally {
            try {
                image.close()
            } catch (e: Exception) {
                android.util.Log.e("ImageAnalysis", "Error closing image: ${e.message}", e)
            }
        }
    }
}

@Composable
private fun FocusScoreCard(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> colorResource(R.color.amber)
        score >= 50 -> colorResource(R.color.skyblue)
        else -> colorResource(R.color.coralred)
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.focus_score_label),
                    fontSize = 14.sp,
                    color = colorResource(R.color.lightsteelblue),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$score",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = stringResource(R.string.focus_score_max),
                        fontSize = 18.sp,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
            }
            
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.size(60.dp),
                color = color,
                strokeWidth = 6.dp,
                trackColor = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = colorResource(R.color.lightsteelblue),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
        }
    }
}

@Composable
private fun SessionTimerCard(
    elapsedSeconds: Int,
    modifier: Modifier = Modifier
) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.session_time),
                fontSize = 16.sp,
                color = colorResource(R.color.lightsteelblue),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (hours > 0) {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
        }
    }
}

@Composable
private fun SessionAlertsCard(
    alerts: List<SessionAlert>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = stringResource(R.string.session_alerts_title),
                    tint = colorResource(R.color.coralred)
                )
                Text(
                    text = stringResource(R.string.session_alerts_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
            }

            alerts.forEach { alert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.session_icon_bullet),
                        fontSize = 18.sp,
                        color = colorResource(R.color.coralred),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alert.message,
                        fontSize = 14.sp,
                        color = colorResource(R.color.lightsteelblue),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = true)
                    )
                }
            }
        }
    }
}
