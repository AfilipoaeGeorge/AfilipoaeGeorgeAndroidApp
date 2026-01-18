package com.example.mindfocus.ui.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.SessionRepository
import com.example.mindfocus.data.repository.UserRepository
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onCalibrationClick: () -> Unit = {},
    onStartSessionClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val userRepository = remember { UserRepository(database) }
    val sessionRepository = remember { SessionRepository(database) }
    
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(context, authPreferencesManager, userRepository, sessionRepository)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
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
                Text(
                    text = stringResource(R.string.home_app_logo, stringResource(R.string.home_icon_brain)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colorResource(R.color.amber)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            viewModel.logout(onLogoutClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                            contentDescription = "Logout",
                            tint = colorResource(R.color.amber)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.amber)
                    )
                }
            } else {
                LastFocusScoreCard(
                    focusScore = uiState.lastFocusScore,
                    lastSessionDate = uiState.lastSessionDate,
                    lastSessionLocation = uiState.lastSessionLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            uiState.errorMessage?.let { errorMessage ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = colorResource(R.color.coralred),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ActionButton(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.calibration_button),
                description = stringResource(R.string.calibration_description),
                onClick = onCalibrationClick,
                gradientColors = listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.skyblue)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ActionButton(
                icon = Icons.Outlined.PlayArrow,
                title = stringResource(R.string.start_session_button),
                description = stringResource(R.string.start_session_description),
                onClick = onStartSessionClick,
                gradientColors = listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.skyblue)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ActionButton(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.history_button),
                description = stringResource(R.string.history_description),
                onClick = onHistoryClick,
                gradientColors = listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.skyblue)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ActionButton(
                icon = Icons.Outlined.Person,
                title = stringResource(R.string.profile_button),
                description = stringResource(R.string.profile_description),
                onClick = onProfileClick,
                gradientColors = listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.skyblue)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LastFocusScoreCard(
    focusScore: Int?,
    lastSessionDate: String?,
    lastSessionLocation: String?,
    modifier: Modifier = Modifier
) {
    val displayScore = focusScore ?: 0
    val displayDate = lastSessionDate ?: "No sessions yet"
    
    val color = when {
        displayScore >= 80 -> colorResource(R.color.amber)
        displayScore >= 50 -> colorResource(R.color.skyblue)
        displayScore > 0 -> colorResource(R.color.skyblue)
        else -> colorResource(R.color.lightsteelblue)
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.last_focus_score),
                fontSize = 14.sp,
                color = colorResource(R.color.lightsteelblue),
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (focusScore != null) "$displayScore" else "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (focusScore != null) {
                    Text(
                        text = stringResource(R.string.focus_score_max),
                        fontSize = 24.sp,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
            }
            
            Text(
                text = displayDate,
                fontSize = 12.sp,
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
            )
            
            if (lastSessionLocation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = colorResource(R.color.amber),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lastSessionLocation,
                        fontSize = 11.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location not available",
                        tint = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    gradientColors.first()
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
