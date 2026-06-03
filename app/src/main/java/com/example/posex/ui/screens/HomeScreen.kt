package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.exercise.ExerciseType
import com.example.posex.ui.theme.PoseXAccent
import com.example.posex.ui.theme.PoseXBackground
import com.example.posex.ui.theme.PoseXSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onExerciseSelected: (ExerciseType) -> Unit
) {
    val exercises = listOf(
        Triple(ExerciseType.SQUAT, "Squats", "Knee angle & torso alignment"),
        Triple(ExerciseType.PUSHUP, "Push-Ups", "Elbow depth & plank line"),
        Triple(ExerciseType.PLANK, "Plank", "Hold duration & core stability"),
        Triple(ExerciseType.BICEPS_CURL, "Bicep Curls", "Elbow path & isolation")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "POSEX",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = PoseXAccent
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PoseXBackground
                )
            )
        },
        containerColor = PoseXBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
        ) {
            item {
                Text(
                    "SELECT YOUR WORKOUT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(exercises) { (type, name, description) ->
                ExerciseProtocolCard(
                    name = name,
                    description = description,
                    onClick = { onExerciseSelected(type) }
                )
            }
        }
    }
}

@Composable
fun ExerciseProtocolCard(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = PoseXSurface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = PoseXAccent,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
