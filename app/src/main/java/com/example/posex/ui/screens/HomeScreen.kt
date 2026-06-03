package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.exercise.ExerciseType

@Composable
fun HomeScreen(onExerciseSelected: (ExerciseType) -> Unit) {

    val exercises = listOf(
        Triple(ExerciseType.SQUAT, "Squats", "Tracks knee angle and torso upright position"),
        Triple(ExerciseType.PUSHUP, "Push-Ups", "Tracks elbow angle and body alignment"),
        Triple(ExerciseType.PLANK, "Plank", "Tracks body straightness and elbow position"),
        Triple(ExerciseType.BICEPS_CURL, "Bicep Curls", "Tracks elbow angle and stationary arm position")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0F1E), Color(0xFF0D1B2A))
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PoseX",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select your exercise",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            exercises.forEach { (type, name, description) ->
                ExerciseCard(
                    name = name,
                    description = description,
                    onClick = { onExerciseSelected(type) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ExerciseCard(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF112233)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}
