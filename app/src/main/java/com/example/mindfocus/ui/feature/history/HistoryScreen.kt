package com.example.mindfocus.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R
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
    sessions: List<SessionHistoryItem>? = null,
    modifier: Modifier = Modifier
) {
    val sessionList = sessions ?: remember { sampleSessions() }
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
                    text = stringResource(R.string.history_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
                
                Text(
                    text = stringResource(R.string.sessions_count, sessionList.size),
                    fontSize = 14.sp,
                    color = colorResource(R.color.lightsteelblue)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (sessionList.isEmpty()) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: SessionHistoryItem,
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

private fun sampleSessions(): List<SessionHistoryItem> {
    val now = System.currentTimeMillis()
    return listOf(
        SessionHistoryItem(
            id = 1,
            sessionNumber = 1,
            startTime = now - 86400000,
            endTime = now - 86340000,
            duration = 600,
            focusScore = 85,
            ear = 0.32,
            mar = 0.38,
            headPose = 2.5,
            scoreEvolution = listOf(70, 75, 80, 85, 88, 85, 87, 90, 85, 82)
        ),
        SessionHistoryItem(
            id = 2,
            sessionNumber = 2,
            startTime = now - 172800000,
            endTime = now - 172740000,
            duration = 600,
            focusScore = 72,
            ear = 0.28,
            mar = 0.45,
            headPose = 5.0,
            scoreEvolution = listOf(60, 65, 70, 72, 75, 70, 68, 72, 75, 72)
        ),
        SessionHistoryItem(
            id = 3,
            sessionNumber = 3,
            startTime = now - 259200000,
            endTime = now - 259140000,
            duration = 600,
            focusScore = 68,
            ear = 0.25,
            mar = 0.50,
            headPose = 7.5,
            scoreEvolution = listOf(55, 60, 65, 68, 70, 65, 68, 70, 65, 68)
        )
    )
}

