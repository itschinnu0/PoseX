package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.PersonalBest
import com.example.posex.data.SessionRecord
import com.example.posex.data.StorageService
import com.example.posex.exercise.ExerciseType
import com.example.posex.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = PoseXBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TRAINING HISTORY",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsTapped) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PoseXAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PoseXBackground, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            item {
                Text("PERSONAL BESTS", style = MaterialTheme.typography.labelMedium, color = PoseXAccent)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PBestCard("SQUAT", personalBestValue(ExerciseType.SQUAT, personalBests[ExerciseType.SQUAT]), Modifier.weight(1f))
                        PBestCard("PUSHUP", personalBestValue(ExerciseType.PUSHUP, personalBests[ExerciseType.PUSHUP]), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PBestCard("PLANK", personalBestValue(ExerciseType.PLANK, personalBests[ExerciseType.PLANK]), Modifier.weight(1f))
                        PBestCard("CURL", personalBestValue(ExerciseType.BICEPS_CURL, personalBests[ExerciseType.BICEPS_CURL]), Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("RECENT SESSIONS", style = MaterialTheme.typography.labelMedium, color = PoseXAccent)
            }

            if (sessions.isEmpty()) {
                item {
                    Text(
                        "NO WORKOUTS YET. TIME TO CRUSH IT!",
                        color = PoseXOnSurface,
                        modifier = Modifier.padding(top = 40.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(sessions) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
fun PBestCard(title: String, value: String, modifier: Modifier) {
    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = if (value == "—") PoseXOnSurface else PoseXAccent
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SessionCard(session: SessionRecord) {
    val score = formScoreLabel(session)
    val scoreColor = when (score) {
        "PERFECT", "EXCELLENT" -> PoseXSuccess
        "STABLE" -> Color(0xFFFFB300)
        else -> PoseXError
    }

    Surface(
        color = PoseXSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.exerciseType.uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(formatDate(session.date), style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (session.exerciseType == "PLANK") "${session.holdSeconds}S" else "${session.repCount} REPS",
                    style = MaterialTheme.typography.titleMedium,
                    color = PoseXAccent,
                    fontWeight = FontWeight.Black
                )
                Text(score, color = scoreColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun personalBestValue(type: ExerciseType, best: PersonalBest?): String {
    return when (type) {
        ExerciseType.PLANK -> best?.holdSeconds?.takeIf { it > 0 }?.let { "${it}S" }
        else -> best?.repCount?.takeIf { it > 0 }?.toString()
    } ?: "—"
}

private fun formScoreLabel(session: SessionRecord): String {
    return when {
        session.criticalCues == 0 && session.warningCues == 0 -> "PERFECT"
        session.criticalCues == 0 -> "EXCELLENT"
        session.criticalCues <= 3 -> "STABLE"
        else -> "IMPROVE"
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(epochMillis))
}
