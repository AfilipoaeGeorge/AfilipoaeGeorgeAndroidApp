package com.example.mindfocus.ui.feature.tips

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R

@Composable
fun TipsScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                .padding(24.dp)
        ) {
            //Header
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
                            contentDescription = stringResource(R.string.back_button),
                            tint = colorResource(R.color.amber)
                        )
                    }
                    Text(
                        text = stringResource(R.string.tips_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.amber)
                    )
                }
            }
            
            //Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TipsIntroCard()
                
                TipCard(
                    stepNumber = 1,
                    icon = stringResource(R.string.tips_icon_step1),
                    title = stringResource(R.string.tips_step1_title),
                    description = stringResource(R.string.tips_step1_description),
                    details = stringResource(R.string.tips_step1_details)
                )
                
                TipCard(
                    stepNumber = 2,
                    icon = stringResource(R.string.tips_icon_step2),
                    title = stringResource(R.string.tips_step2_title),
                    description = stringResource(R.string.tips_step2_description),
                    details = stringResource(R.string.tips_step2_details)
                )
                
                TipCard(
                    stepNumber = 3,
                    icon = stringResource(R.string.tips_icon_step3),
                    title = stringResource(R.string.tips_step3_title),
                    description = stringResource(R.string.tips_step3_description),
                    details = stringResource(R.string.tips_step3_details)
                )
                
                TipCard(
                    stepNumber = 4,
                    icon = stringResource(R.string.tips_icon_step4),
                    title = stringResource(R.string.tips_step4_title),
                    description = stringResource(R.string.tips_step4_description),
                    details = stringResource(R.string.tips_step4_details)
                )
                
                TipsAlertsCard()
                
                TipsDeleteDataCard()
                
                TipsBestPracticesCard()
            }
        }
    }
}

@Composable
private fun TipsIntroCard() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "intro_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
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
                text = stringResource(R.string.tips_icon_welcome),
                fontSize = 48.sp
            )
            Text(
                text = stringResource(R.string.tips_welcome_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
            Text(
                text = stringResource(R.string.tips_welcome_description),
                fontSize = 14.sp,
                color = colorResource(R.color.lightsteelblue),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TipCard(
    stepNumber: Int,
    icon: String,
    title: String,
    description: String,
    details: String
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = stepNumber * 100, easing = FastOutSlowInEasing),
        label = "tip_fade"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(600, delayMillis = stepNumber * 100, easing = FastOutSlowInEasing),
        label = "tip_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale),
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tips_step, stepNumber),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.amber).copy(alpha = 0.7f)
                        )
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.amber)
                        )
                    }
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = colorResource(R.color.lightsteelblue),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Text(
                text = details,
                fontSize = 13.sp,
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun TipsAlertsCard() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "alerts_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
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
                Text(
                    text = stringResource(R.string.tips_icon_alerts),
                    fontSize = 28.sp
                )
                Text(
                    text = stringResource(R.string.tips_alerts_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
            }
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_eyes_closed),
                    text = stringResource(R.string.tips_alert_eyes_closed)
                )
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_blink_rate),
                    text = stringResource(R.string.tips_alert_blink_rate)
                )
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_head_pose),
                    text = stringResource(R.string.tips_alert_head_pose)
                )
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_yawn),
                    text = stringResource(R.string.tips_alert_yawn)
                )
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_low_focus),
                    text = stringResource(R.string.tips_alert_low_focus)
                )
                AlertTipItem(
                    icon = stringResource(R.string.tips_icon_face_lost),
                    text = stringResource(R.string.tips_alert_face_lost)
                )
            }
        }
    }
}

@Composable
private fun AlertTipItem(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = colorResource(R.color.lightsteelblue).copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TipsDeleteDataCard() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 650, easing = FastOutSlowInEasing),
        label = "delete_data_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
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
                Text(
                    text = stringResource(R.string.tips_icon_delete_data),
                    fontSize = 28.sp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tips_delete_data_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.amber)
                    )
                    Text(
                        text = stringResource(R.string.tips_delete_data_description),
                        fontSize = 14.sp,
                        color = colorResource(R.color.lightsteelblue),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Text(
                text = stringResource(R.string.tips_delete_data_details),
                fontSize = 13.sp,
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun TipsBestPracticesCard() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing),
        label = "practices_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
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
                Text(
                    text = stringResource(R.string.tips_icon_best_practices),
                    fontSize = 28.sp
                )
                Text(
                    text = stringResource(R.string.tips_best_practices_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
            }
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BestPracticeItem(
                    text = stringResource(R.string.tips_practice_lighting)
                )
                BestPracticeItem(
                    text = stringResource(R.string.tips_practice_distance)
                )
                BestPracticeItem(
                    text = stringResource(R.string.tips_practice_breaks)
                )
                BestPracticeItem(
                    text = stringResource(R.string.tips_practice_consistency)
                )
            }
        }
    }
}

@Composable
private fun BestPracticeItem(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.tips_icon_bullet),
            fontSize = 18.sp,
            color = colorResource(R.color.amber),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = colorResource(R.color.lightsteelblue).copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            lineHeight = 18.sp
        )
    }
}

