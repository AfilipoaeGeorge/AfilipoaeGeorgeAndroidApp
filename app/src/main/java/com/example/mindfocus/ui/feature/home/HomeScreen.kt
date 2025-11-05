package com.example.mindfocus.ui.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onCalibrationClick: () -> Unit = {},
    onStartSessionClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    
    val lastFocusScore = 75
    val lastSessionDate = "2 hours ago"
    
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
                    text = stringResource(R.string.home_app_logo),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
                
                IconButton(
                    onClick = {
                        scope.launch {
                            authPreferencesManager.setLoggedOut()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExitToApp,
                        contentDescription = "Logout",
                        tint = colorResource(R.color.amber)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LastFocusScoreCard(
                focusScore = lastFocusScore,
                lastSessionDate = lastSessionDate,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ActionButton(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.calibration_button),
                description = stringResource(R.string.calibration_description),
                onClick = onCalibrationClick,
                gradientColors = listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.amber)
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
                    colorResource(R.color.amber),
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
                    colorResource(R.color.amber)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LastFocusScoreCard(
    focusScore: Int,
    lastSessionDate: String,
    modifier: Modifier = Modifier
) {
    val color = when {
        focusScore >= 80 -> colorResource(R.color.amber)
        focusScore >= 50 -> colorResource(R.color.skyblue)
        else -> colorResource(R.color.coralred)
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
                    text = "$focusScore",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = stringResource(R.string.focus_score_max),
                    fontSize = 24.sp,
                    color = colorResource(R.color.lightsteelblue)
                )
            }
            
            Text(
                text = lastSessionDate,
                fontSize = 12.sp,
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
            )
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
                    Brush.horizontalGradient(gradientColors)
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
