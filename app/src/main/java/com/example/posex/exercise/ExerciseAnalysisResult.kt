package com.example.posex.exercise

data class ExerciseAnalysisResult(
    val feedback: List<String>,
    val repCount: Int,
    val metricValue: Double?,
    val holdDurationSeconds: Int = 0
)