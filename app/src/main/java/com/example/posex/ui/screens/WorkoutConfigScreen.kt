package com.example.posex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.WorkoutConfig
import com.example.posex.exercise.WorkoutConfigValidator

@Composable
fun WorkoutConfigScreen(
    exerciseType: ExerciseType,
    onStartWorkout: (WorkoutConfig) -> Unit,
    onBack: () -> Unit
) {
    val defaults = remember(exerciseType) {
        when (exerciseType) {
            ExerciseType.SQUAT, ExerciseType.PUSHUP, ExerciseType.BICEPS_CURL -> Triple(10, 3, 60)
            ExerciseType.PLANK -> Triple(30, 2, 60)
        }
    }

    var repsOrHold by remember { mutableStateOf(defaults.first.toFloat()) }
    var sets by remember { mutableStateOf(defaults.second.toFloat()) }
    var restSeconds by remember { mutableStateOf(defaults.third.toFloat()) }

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
    val hasCriticalIssues = false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF112233)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Back", color = Color.White)
                }

                Text(
                    text = exerciseType.name,
                    color = Color(0xFF00E5FF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF112233), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (exerciseType == ExerciseType.PLANK) {
                    LabeledSlider(
                        label = "Hold Duration (seconds)",
                        value = repsOrHold,
                        valueRange = 1f..300f,
                        onValueChange = { repsOrHold = it },
                        displayValue = repsOrHold.toInt().toString()
                    )
                } else {
                    LabeledSlider(
                        label = "Reps per Set",
                        value = repsOrHold,
                        valueRange = 1f..50f,
                        onValueChange = { repsOrHold = it },
                        displayValue = repsOrHold.toInt().toString()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LabeledSlider(
                    label = "Sets",
                    value = sets,
                    valueRange = if (exerciseType == ExerciseType.PLANK) 1f..5f else 1f..10f,
                    onValueChange = { sets = it },
                    displayValue = sets.toInt().toString()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledSlider(
                    label = "Rest Between Sets (seconds)",
                    value = restSeconds,
                    valueRange = 10f..300f,
                    onValueChange = { restSeconds = it },
                    displayValue = restSeconds.toInt().toString()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val targetLabel = if (exerciseType == ExerciseType.PLANK) {
                    "Target per set: ${validation.config.holdSeconds}s hold"
                } else {
                    "Target per set: ${validation.config.repsPerSet} reps"
                }

                Text(
                    text = targetLabel,
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF112233), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    warnings.forEach { warning ->
                        Text(
                            text = warning,
                            color = Color(0xFFFFB300),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onStartWorkout(validation.config) },
                enabled = !hasCriticalIssues,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Start Workout",
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp)
            Text(text = displayValue, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
