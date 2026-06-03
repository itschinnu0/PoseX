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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.PersonalBest
import com.example.posex.data.SessionRecord
import com.example.posex.data.StorageService
import com.example.posex.exercise.ExerciseType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable

@Composable
fun StatsScreen(
    storageService: StorageService,
    activeProfileId: String?,
    onSettingsTapped: () -> Unit
) {
    val sessions = remember(activeProfileId) {
        storageService.getAllSessions().filter { it.profileId == (activeProfileId ?: "") }
            .sortedByDescending { it.date }
            .take(20)
    }
    val personalBests = remember(activeProfileId) {
        mapOf(
            ExerciseType.SQUAT to storageService.getPersonalBest("SQUAT", activeProfileId),
            ExerciseType.PUSHUP to storageService.getPersonalBest("PUSHUP", activeProfileId),
            ExerciseType.PLANK to storageService.getPersonalBest("PLANK", activeProfileId),
            ExerciseType.BICEPS_CURL to storageService.getPersonalBest("BICEPS_CURL", activeProfileId)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stats",
                        color = Color(0xFF00E5FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettingsTapped) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Personal Bests",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PersonalBestCard(
                            title = "SQUAT",
                            value = personalBestValue(ExerciseType.SQUAT, personalBests[ExerciseType.SQUAT]),
                            modifier = Modifier.weight(1f)
                        )
                        PersonalBestCard(
                            title = "PUSHUP",
                            value = personalBestValue(ExerciseType.PUSHUP, personalBests[ExerciseType.PUSHUP]),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PersonalBestCard(
                            title = "PLANK",
                            value = personalBestValue(ExerciseType.PLANK, personalBests[ExerciseType.PLANK]),
                            modifier = Modifier.weight(1f)
                        )
                        PersonalBestCard(
                            title = "CURL",
                            value = personalBestValue(ExerciseType.BICEPS_CURL, personalBests[ExerciseType.BICEPS_CURL]),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recent Sessions",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No workouts yet. Start your first workout!",
                            color = Color(0xFFB0BEC5),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(sessions) { session ->
                    SessionRow(session)
                }
            }
        }
    }
}

@Composable
private fun PersonalBestCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(80.dp)
            .background(Color(0xFF112233), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color(0xFFB0BEC5),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = if (value == "—") Color(0xFFB0BEC5) else Color(0xFF00E5FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionRow(session: SessionRecord) {
    val exerciseName = session.exerciseType.uppercase(Locale.US)
    val dateText = formatDate(session.date)
    val scoreLabel = formScoreLabel(session)
    val scoreColor = formScoreColor(scoreLabel)

    val metricText = if (exerciseName == "PLANK") {
        "${session.holdSeconds}s"
    } else {
        "${session.repCount} reps"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF112233), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exerciseName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = dateText,
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = metricText,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = scoreLabel,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

private fun personalBestValue(type: ExerciseType, best: PersonalBest?): String {
    return when (type) {
        ExerciseType.PLANK -> best?.holdSeconds?.takeIf { it > 0 }?.let { "${it}s" }
        ExerciseType.SQUAT, ExerciseType.PUSHUP, ExerciseType.BICEPS_CURL -> best?.repCount?.takeIf { it > 0 }?.toString()
    } ?: "—"
}

private fun formScoreLabel(session: SessionRecord): String {
    return when {
        session.criticalCues == 0 && session.warningCues == 0 -> "Perfect"
        session.criticalCues == 0 -> "Good"
        session.criticalCues <= 3 -> "Fair"
        else -> "Needs Work"
    }
}

private fun formScoreColor(label: String): Color {
    return when (label) {
        "Perfect", "Good" -> Color(0xFF00E676)
        "Fair" -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(epochMillis))
}
