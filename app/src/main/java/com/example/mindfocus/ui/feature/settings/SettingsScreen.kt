package com.example.mindfocus.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R

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
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var cameraMonitoringEnabled by remember { mutableStateOf(true) }
    var autoDndEnabled by remember { mutableStateOf(true) }
    var lowFocusAlertsEnabled by remember { mutableStateOf(true) }
    var dailyRemindersEnabled by remember { mutableStateOf(false) }

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
                            imageVector = Icons.Default.ArrowBack,
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
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    SettingsSectionCard(
                        section = SettingsSection(
                            title = stringResource(R.string.settings_camera),
                            icon = Icons.Outlined.CameraAlt,
                            items = listOf(
                                SettingsItem(
                                    title = stringResource(R.string.enable_camera_monitoring),
                                    isSwitch = true,
                                    isEnabled = cameraMonitoringEnabled,
                                    onClick = { cameraMonitoringEnabled = !cameraMonitoringEnabled }
                                )
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    SettingsSectionCard(
                        section = SettingsSection(
                            title = stringResource(R.string.settings_dnd),
                            icon = Icons.Outlined.NotificationsOff,
                            items = listOf(
                                SettingsItem(
                                    title = stringResource(R.string.auto_enable_dnd),
                                    isSwitch = true,
                                    isEnabled = autoDndEnabled,
                                    onClick = { autoDndEnabled = !autoDndEnabled }
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
                                    title = stringResource(R.string.low_focus_alerts),
                                    isSwitch = true,
                                    isEnabled = lowFocusAlertsEnabled,
                                    onClick = { lowFocusAlertsEnabled = !lowFocusAlertsEnabled }
                                ),
                                SettingsItem(
                                    title = stringResource(R.string.daily_reminders),
                                    isSwitch = true,
                                    isEnabled = dailyRemindersEnabled,
                                    onClick = { dailyRemindersEnabled = !dailyRemindersEnabled }
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
                                    title = stringResource(R.string.privacy_policy),
                                    icon = Icons.Outlined.Info,
                                    onClick = { /* Navigate to privacy policy */ }
                                ),
                                SettingsItem(
                                    title = stringResource(R.string.delete_all_data),
                                    icon = Icons.Outlined.Delete,
                                    onClick = { /* Show delete confirmation */ }
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
                                    title = stringResource(R.string.app_version),
                                    description = stringResource(R.string.app_description),
                                    icon = Icons.Outlined.Apps
                                ),
                                SettingsItem(
                                    title = stringResource(R.string.help_and_support),
                                    icon = Icons.Outlined.Help,
                                    onClick = { /* Navigate to help */ }
                                )
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
            
            Divider(
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
            .clickable(enabled = !item.isSwitch) { item.onClick() }
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
                onCheckedChange = { item.onClick() },
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

