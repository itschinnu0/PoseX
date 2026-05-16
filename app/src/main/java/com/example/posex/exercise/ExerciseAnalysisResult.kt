package com.example.posex.exercise

/**
 * Result bundle returned by every exercise analyzer.
 *
 * [cues]               — ordered list of FormCue emitted this frame.
 *                        Use CuePrioritizer.topCue(cues) to get the one to show.
 * [repCount]           — current rep count (0 for plank).
 * [metricValue]        — primary metric for this exercise (knee angle, elbow angle,
 *                        hip deviation). Null if landmarks were not visible.
 * [holdDurationSeconds]— seconds of continuous good-form hold (plank only; 0 for others).
 */
data class ExerciseAnalysisResult(
    val cues: List<FormCue>,
    val repCount: Int,
    val metricValue: Double?,
    val holdDurationSeconds: Int = 0
)