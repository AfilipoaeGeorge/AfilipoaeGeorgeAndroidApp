package com.example.mindfocus.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.MetricRepository
import com.example.mindfocus.data.repository.SessionRepository
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.SimpleDateFormat
import java.util.*

data class SessionHistoryItem(
    val id: Long,
    val sessionNumber: Int,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val focusScore: Int,
    val ear: Double,
    val mar: Double,
    val headPose: Double,
    val scoreEvolution: List<Int>
)

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val sessionRepository = remember { SessionRepository(database) }
    val metricRepository = remember { MetricRepository(database) }
    
    val viewModel: HistoryViewModel = viewModel {
        HistoryViewModel(
            context = context,
            authPreferencesManager = authPreferencesManager,
            sessionRepository = sessionRepository,
            metricRepository = metricRepository
        )
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val sessionList = uiState.sessions
    val errorMessage = uiState.errorMessage
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
                        text = stringResource(R.string.history_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.amber)
                    )
                }
                
                Text(
                    text = stringResource(R.string.sessions_count, sessionList.size),
                    fontSize = 14.sp,
                    color = colorResource(R.color.lightsteelblue)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.amber)
                    )
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.coralred)
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = colorResource(R.color.lightsteelblue),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.amber)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else if (sessionList.isEmpty()) {
                EmptyHistoryState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sessionList) { session ->
                        SessionHistoryCard(
                            session = session,
                            onDeleteClick = { viewModel.requestDeleteSession(session.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (uiState.sessionToDelete != null) {
            DeleteSessionConfirmationDialog(
                onDismiss = { viewModel.cancelDeleteSession() },
                onConfirm = { viewModel.confirmDeleteSession() }
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: SessionHistoryItem,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val startDate = Date(session.startTime)
    val endDate = Date(session.endTime)
    
    val durationMinutes = session.duration / 60
    val durationSeconds = session.duration % 60
    
    val scoreColor = when {
        session.focusScore >= 80 -> colorResource(R.color.amber)
        session.focusScore >= 50 -> colorResource(R.color.skyblue)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.session_number, session.sessionNumber),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.amber)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(startDate),
                        fontSize = 12.sp,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${session.focusScore}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = stringResource(R.string.focus_score_max),
                            fontSize = 16.sp,
                            color = colorResource(R.color.lightsteelblue)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete session",
                            tint = colorResource(R.color.coralred)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.start_time_label),
                        fontSize = 11.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
                    )
                    Text(
                        text = timeFormat.format(startDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.end_time_label),
                        fontSize = 11.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
                    )
                    Text(
                        text = timeFormat.format(endDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.duration_label),
                        fontSize = 11.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (durationMinutes > 0) {
                            "${durationMinutes}m ${durationSeconds}s"
                        } else {
                            "${durationSeconds}s"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
            }
            
            Divider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(label = stringResource(R.string.ear_label), value = String.format("%.2f", session.ear))
                MetricItem(label = stringResource(R.string.mar_label), value = String.format("%.2f", session.mar))
                MetricItem(label = stringResource(R.string.head_pose_label), value = String.format("%.1f°", session.headPose))
            }
            
            Divider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            Column {
                Text(
                    text = stringResource(R.string.focus_score_evolution),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.lightsteelblue),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (session.scoreEvolution.isNotEmpty()) {
                    ScoreEvolutionChart(
                        scores = session.scoreEvolution,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_score_data),
                        fontSize = 12.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 30.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.amber)
        )
    }
}

@Composable
private fun ScoreEvolutionChart(
    scores: List<Int>,
    modifier: Modifier = Modifier
) {
    val gridColor = colorResource(R.color.lightsteelblue).copy(alpha = 0.2f)
    val lineColor = colorResource(R.color.amber)
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.darkslategray).copy(alpha = 0.5f)
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (scores.isEmpty()) return@Canvas
            
            val width = size.width
            val height = size.height
            val maxScore = scores.maxOrNull()?.coerceAtLeast(100) ?: 100
            val minScore = scores.minOrNull()?.coerceAtLeast(0) ?: 0
            val scoreRange = (maxScore - minScore).coerceAtLeast(1)
            
            val stepX = width / (scores.size - 1).coerceAtLeast(1)
            
            for (i in 0..4) {
                val y = height * i / 4
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            val path = Path()
            scores.forEachIndexed { index, score ->
                val x = index * stepX
                val normalizedScore = (score - minScore).toFloat() / scoreRange
                val y = height * (1f - normalizedScore)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f)
            )
            
            scores.forEachIndexed { index, score ->
                val x = index * stepX
                val normalizedScore = (score - minScore).toFloat() / scoreRange
                val y = height * (1f - normalizedScore)
                
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun DeleteSessionConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_delete_confirm),
                    color = colorResource(R.color.coralred)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_delete_cancel),
                    color = colorResource(R.color.lightsteelblue)
                )
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = colorResource(R.color.coralred)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_session_title),
                color = colorResource(R.color.amber)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_session_message),
                color = colorResource(R.color.lightsteelblue)
            )
        },
        containerColor = colorResource(R.color.midnightblue)
    )
}

@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.empty_history_emoji),
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_history_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.amber)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_history_message),
            fontSize = 14.sp,
            color = colorResource(R.color.lightsteelblue),
            textAlign = TextAlign.Center
        )
    }
}