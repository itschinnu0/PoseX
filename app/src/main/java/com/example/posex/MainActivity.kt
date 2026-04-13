package com.example.posex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.posex.exercise.ExerciseType
import com.example.posex.ui.screens.HomeScreen
import com.example.posex.ui.screens.WorkoutScreen
import com.example.posex.ui.theme.PoseXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoseXTheme {
                PoseXApp()
            }
        }
    }
}

@Composable
fun PoseXApp() {
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }

    if (selectedExercise == null) {
        HomeScreen(
            onExerciseSelected = { exercise ->
                selectedExercise = exercise
            }
        )
    } else {
        WorkoutScreen(
            exerciseType = selectedExercise!!,
            onExit = {
                selectedExercise = null
            }
        )
    }
}