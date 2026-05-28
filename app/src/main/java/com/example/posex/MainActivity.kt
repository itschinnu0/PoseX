package com.example.posex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.WorkoutConfig
import com.example.posex.ui.screens.HomeScreen
import com.example.posex.ui.screens.WorkoutConfigScreen
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
    var workoutConfig by remember { mutableStateOf<WorkoutConfig?>(null) }

    when {
        selectedExercise == null -> {
            HomeScreen(
                onExerciseSelected = { exercise ->
                    selectedExercise = exercise
                }
            )
        }
        workoutConfig == null -> {
            WorkoutConfigScreen(
                exerciseType = selectedExercise!!,
                onStartWorkout = { config ->
                    workoutConfig = config
                },
                onBack = {
                    selectedExercise = null
                    workoutConfig = null
                }
            )
        }
        else -> {
            WorkoutScreen(
                exerciseType = selectedExercise!!,
                config = workoutConfig!!,
                onExit = {
                    selectedExercise = null
                    workoutConfig = null
                }
            )
        }
    }
}