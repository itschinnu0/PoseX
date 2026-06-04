package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.data.UserProfile
import com.example.posex.exercise.*
import com.example.posex.ui.theme.*

@Composable
fun WorkoutConfigScreen(
    exerciseType: ExerciseType,
    activeProfile: UserProfile?,
    onStartWorkout: (WorkoutConfig) -> Unit,
    onBack: () -> Unit
) {
    val recommendedConfig = remember(exerciseType, activeProfile) {
        val category = activeProfile?.healthStatus ?: com.example.posex.data.BmiCategory.NORMAL
        WorkoutRecommender.recommend(category, exerciseType)
    }

    var repsOrHold by remember { 
        mutableFloatStateOf(
            if (exerciseType == ExerciseType.PLANK) recommendedConfig.holdSeconds.toFloat() 
            else recommendedConfig.repsPerSet.toFloat()
        ) 
    }
    var sets by remember { mutableFloatStateOf(recommendedConfig.sets.toFloat()) }
    var restSeconds by remember { mutableFloatStateOf(recommendedConfig.restSeconds.toFloat()) }

    val config = when (exerciseType) {
        ExerciseType.PLANK -> WorkoutConfig(
            exerciseType = exerciseType,
            holdSeconds = repsOrHold.toInt(),
            sets = sets.toInt(),
            restSeconds = restSeconds.toInt()
        )
        else -> WorkoutConfig(
            exerciseType = exerciseType,
            repsPerSet = repsOrHold.toInt(),
            sets = sets.toInt(),
            restSeconds = restSeconds.toInt()
        )
    }

    val validation = WorkoutConfigValidator.validate(config)
    val warnings = validation.warnings

    Scaffold(
        containerColor = PoseXBackground,
        topBar = {
            Surface(color = PoseXBackground) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text("BACK", color = PoseXOnSurface)
                    }
                    Text(
                        exerciseType.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = PoseXAccent,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.weight(1f).padding(end = 48.dp)
                    )
                }
            }
        }
    ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp)
                ) {
                    Text("WORKOUT SETTINGS", style = MaterialTheme.typography.labelMedium, color = PoseXAccent)
                    Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = PoseXSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    LabeledSlider(
                        label = if (exerciseType == ExerciseType.PLANK) "HOLD DURATION" else "REPS PER SET",
                        value = repsOrHold,
                        valueRange = 1f..120f,
                        onValueChange = { repsOrHold = it },
                        displayValue = repsOrHold.toInt().toString()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LabeledSlider(
                        label = "SETS",
                        value = sets,
                        valueRange = 1f..10f,
                        onValueChange = { sets = it },
                        displayValue = sets.toInt().toString()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LabeledSlider(
                        label = "REST PERIOD (S)",
                        value = restSeconds,
                        valueRange = 10f..180f,
                        onValueChange = { restSeconds = it },
                        displayValue = restSeconds.toInt().toString()
                    )
                }
            }

            if (warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = PoseXError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        warnings.forEach { warning ->
                            Text(warning, color = PoseXError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onStartWorkout(validation.config) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PoseXAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "START WORKOUT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = PoseXBackground
                        )
                    )
                }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = PoseXOnSurface)
            Text(displayValue, style = MaterialTheme.typography.titleMedium, color = PoseXAccent, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PoseXAccent,
                activeTrackColor = PoseXAccent,
                inactiveTrackColor = PoseXOnSurface.copy(alpha = 0.2f)
            )
        )
    }
}
