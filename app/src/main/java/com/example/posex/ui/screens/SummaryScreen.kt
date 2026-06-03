package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.PersonalBest
import com.example.posex.data.SessionRecord
import com.example.posex.ui.theme.*
import java.util.*

@Composable
fun SummaryScreen(
    sessionRecord: SessionRecord,
    personalBest: PersonalBest?,
    onDone: () -> Unit
) {
    val exerciseName = sessionRecord.exerciseType.uppercase(Locale.US)
    val isPlank = exerciseName == "PLANK"
    val durationText = formatDuration(sessionRecord.durationMs)

    val formScore = when {
        sessionRecord.criticalCues == 0 && sessionRecord.warningCues == 0 -> "PERFECT"
        sessionRecord.criticalCues == 0 -> "EXCELLENT"
        sessionRecord.criticalCues <= 3 -> "STABLE"
        else -> "IMPROVE"
    }
    
    val formScoreColor = when (formScore) {
        "PERFECT", "EXCELLENT" -> PoseXSuccess
        "STABLE" -> Color(0xFFFFB300)
        else -> PoseXError
    }

    val beatPersonalBest = if (isPlank) {
        sessionRecord.holdSeconds > (personalBest?.holdSeconds ?: 0)
    } else {
        sessionRecord.repCount > (personalBest?.repCount ?: 0)
    }

    Scaffold(
        containerColor = PoseXBackground,
        bottomBar = {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PoseXAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "DONE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = PoseXBackground
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "WORKOUT COMPLETE",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = PoseXAccent,
                    letterSpacing = 4.sp
                )
            )
            
            Text(
                exerciseName,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score Panel
            Surface(
                color = PoseXSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AVERAGE FORM QUALITY", style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
                    Text(
                        formScore,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = formScoreColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    label = if (isPlank) "HOLD TIME" else "REPS",
                    value = if (isPlank) "${sessionRecord.holdSeconds}s" else sessionRecord.repCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "DURATION",
                    value = durationText,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback Breakdown
            Surface(
                color = PoseXSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("WORKOUT DETAILS", style = MaterialTheme.typography.labelLarge, color = PoseXAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    LogItem("CRITICAL CORRECTIONS", sessionRecord.criticalCues.toString(), PoseXError)
                    LogItem("FORM WARNINGS", sessionRecord.warningCues.toString(), Color(0xFFFFB300))
                }
            }

            if (beatPersonalBest) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "🏆 NEW PERSONAL BEST!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = PoseXAccent,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
            Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogItem(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = PoseXOnSurface)
        Text(value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt().coerceAtLeast(0)
    if (totalSeconds < 60) return "${totalSeconds}S"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}M ${seconds}S"
}
