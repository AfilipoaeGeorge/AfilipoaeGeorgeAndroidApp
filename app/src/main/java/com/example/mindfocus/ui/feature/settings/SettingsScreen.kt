package com.example.mindfocus.ui.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.SettingsPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.SettingsRepository

data class SettingsSection(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val isSwitch: Boolean = false,
    val isEnabled: Boolean = false,
    val onClick: () -> Unit = {},
    val onCheckedChange: (Boolean) -> Unit = {}
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onTipsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsPreferencesManager = remember { SettingsPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val settingsRepository = remember { SettingsRepository(settingsPreferencesManager, database) }

    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            context = context.applicationContext,
            settingsRepository = settingsRepository
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.onCameraPermissionDenied()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("SettingsScreen", "Location permission result: $permissions")
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        android.util.Log.d("SettingsScreen", "Fine location granted: $fineLocationGranted, Coarse location granted: $coarseLocationGranted")

        if (!fineLocationGranted && !coarseLocationGranted) {
            android.util.Log.w("SettingsScreen", "Location permissions denied")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_location_permission_denied)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colorResource(R.color.darkcharcoal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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
                            text = stringResource(R.string.settings_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.amber)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorResource(R.color.amber))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val settings = uiState.settings

                        item {
                            SettingsSectionCard(
                                section = SettingsSection(
                                    title = stringResource(R.string.settings_camera),
                                    icon = Icons.Outlined.CameraAlt,
                                    items = listOf(
                                        SettingsItem(
                                            title = stringResource(R.string.enable_camera_monitoring),
                                            isSwitch = true,
                                            isEnabled = settings.cameraMonitoringEnabled,
                                            onCheckedChange = { checked ->
                                                viewModel.onCameraMonitoringChanged(checked)
                                                if (checked) {
                                                    val granted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.CAMERA
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (!granted) {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                }
                                            }
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            SettingsSectionCard(
                                section = SettingsSection(
                                    title = stringResource(R.string.settings_location),
                                    icon = Icons.Outlined.LocationOn,
                                    items = listOf(
                                        SettingsItem(
                                            title = stringResource(R.string.enable_gps),
                                            description = stringResource(R.string.enable_gps_description),
                                            isSwitch = true,
                                            isEnabled = settings.gpsEnabled,
                                            onCheckedChange = { enabled ->
                                                android.util.Log.d("SettingsScreen", "GPS switch changed to: $enabled")
                                                viewModel.onGpsChanged(enabled)
                                                
                                                if (enabled) {
                                                    val fineLocationGranted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.ACCESS_FINE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    val coarseLocationGranted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                    android.util.Log.d("SettingsScreen", "Current permissions - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")

                                                    if (!fineLocationGranted && !coarseLocationGranted) {
                                                        android.util.Log.d("SettingsScreen", "Requesting location permissions")
                                                        locationPermissionLauncher.launch(
                                                            arrayOf(
                                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            SettingsSectionCard(
                                section = SettingsSection(
                                    title = stringResource(R.string.settings_notifications),
                                    icon = Icons.Outlined.Notifications,
                                    items = listOf(
                                        SettingsItem(
                                            title = stringResource(R.string.eyes_closed_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.eyesClosedAlertsEnabled,
                                            onCheckedChange = viewModel::onEyesClosedAlertsChanged
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.blink_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.blinkAlertsEnabled,
                                            onCheckedChange = viewModel::onBlinkAlertsChanged
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.head_pose_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.headPoseAlertsEnabled,
                                            onCheckedChange = viewModel::onHeadPoseAlertsChanged
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.yawn_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.yawnAlertsEnabled,
                                            onCheckedChange = viewModel::onYawnAlertsChanged
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.face_lost_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.faceLostAlertsEnabled,
                                            onCheckedChange = viewModel::onFaceLostAlertsChanged
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.low_focus_alerts),
                                            isSwitch = true,
                                            isEnabled = settings.lowFocusAlertsEnabled,
                                            onCheckedChange = viewModel::onLowFocusAlertsChanged
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            SettingsSectionCard(
                                section = SettingsSection(
                                    title = stringResource(R.string.settings_privacy),
                                    icon = Icons.Outlined.Lock,
                                    items = listOf(
                                        SettingsItem(
                                            title = stringResource(R.string.delete_all_data),
                                            icon = Icons.Outlined.Delete,
                                            onClick = viewModel::onDeleteAllDataClicked
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            SettingsSectionCard(
                                section = SettingsSection(
                                    title = stringResource(R.string.settings_about),
                                    icon = Icons.Outlined.Info,
                                    items = listOf(
                                        SettingsItem(
                                            title = stringResource(R.string.tips_button),
                                            description = stringResource(R.string.tips_button_description),
                                            icon = Icons.Outlined.Lightbulb,
                                            onClick = onTipsClick
                                        ),
                                        SettingsItem(
                                            title = stringResource(R.string.app_version),
                                            description = stringResource(
                                                R.string.app_version_label,
                                                uiState.appVersion
                                            ),
                                            icon = Icons.Outlined.Apps
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                uiState.errorMessage?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = colorResource(R.color.coralred),
                        fontSize = 12.sp
                    )
                }
            }

            if (uiState.showDeleteConfirmation) {
                DeleteConfirmationDialog(
                    isLoading = uiState.isDeletingData,
                    onDismiss = viewModel::onDismissDeleteDialog,
                    onConfirm = viewModel::onConfirmDelete
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = section.title,
                    tint = colorResource(R.color.amber),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = section.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
            }
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            section.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                SettingsItemRow(
                    item = item,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable {
                if (item.isSwitch) {
                    item.onCheckedChange(!item.isEnabled)
                } else {
                    item.onClick()
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = item.title,
                    tint = colorResource(R.color.lightsteelblue),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.lightsteelblue)
                )
                item.description?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        if (item.isSwitch) {
            Switch(
                checked = item.isEnabled,
                onCheckedChange = { checked -> item.onCheckedChange(checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(R.color.amber),
                    checkedTrackColor = colorResource(R.color.amber).copy(alpha = 0.5f),
                    uncheckedThumbColor = colorResource(R.color.lightsteelblue),
                    uncheckedTrackColor = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colorResource(R.color.amber),
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm),
                        color = colorResource(R.color.coralred)
                    )
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.settings_delete_cancel),
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = colorResource(R.color.coralred)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_delete_confirmation_title),
                color = colorResource(R.color.amber)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_delete_confirmation_message),
                color = colorResource(R.color.lightsteelblue)
            )
        },
        containerColor = colorResource(R.color.midnightblue)
    )
}
