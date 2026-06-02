package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
        sessionRecord.criticalCues == 0 && sessionRecord.warningCues == 0 -> "Perfect"
        sessionRecord.criticalCues == 0 -> "Good"
        sessionRecord.criticalCues <= 3 -> "Fair"
        else -> "Needs Work"
    }
    val formScoreColor = when (formScore) {
        "Perfect", "Good" -> Color(0xFF00E676)
        "Fair" -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }

    val beatPersonalBest = if (isPlank) {
        sessionRecord.holdSeconds > (personalBest?.holdSeconds ?: 0)
    } else {
        sessionRecord.repCount > (personalBest?.repCount ?: 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = exerciseName,
                color = Color(0xFF00E5FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Workout Complete",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF112233), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPlank) {
                    StatRow(label = "Hold time", value = "${sessionRecord.holdSeconds}s")
                } else {
                    StatRow(label = "Reps completed", value = sessionRecord.repCount.toString())
                }

                StatRow(label = "Duration", value = durationText)

                StatRow(
                    label = "Form score",
                    value = formScore,
                    valueColor = formScoreColor
                )

                StatRow(label = "Critical corrections", value = sessionRecord.criticalCues.toString())
                StatRow(label = "Form warnings", value = sessionRecord.warningCues.toString())
            }

            if (beatPersonalBest) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏆 New Personal Best!",
                        color = Color(0xFF0A0F1E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Done",
                    color = Color(0xFF0A0F1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFB0BEC5), fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt().coerceAtLeast(0)
    if (totalSeconds < 60) {
        return "${totalSeconds}s"
    }
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}
