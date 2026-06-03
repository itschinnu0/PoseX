package com.example.posex.exercise

/**
 * Result bundle returned by every exercise analyzer each frame.
 *
 * [cues]               — list of FormCue emitted this frame.
 *                        Use CuePrioritizer.topCue(cues) to get the one to show.
 * [repCount]           — current rep count (0 for plank).
 * [metricValue]        — primary metric (knee angle, elbow angle, hip deviation).
 *                        Null if landmarks were not visible.
 * [holdDurationSeconds]— seconds of continuous good-form hold (plank only; 0 for others).
 * [isCalibrating]      — true during the first rep while thresholds are being learned.
 *                        WorkoutScreen uses this to show a calibration hint to the user.
 * [repRejected]        — true if the rep that just completed was blocked by the form gate.
 *                        WorkoutScreen uses this to show a "rep not counted" message.
 * [rejectionReason]    — human-readable reason for rejection. Empty if not rejected.
 * [exerciseCompleted]   — true when the exercise should end immediately
 *                        (e.g., plank terminated by knees on the floor).
 */
data class ExerciseAnalysisResult(
    val cues: List<FormCue>,
    val repCount: Int,
    val metricValue: Double?,
    val holdDurationSeconds: Int = 0,
    val isCalibrating: Boolean = false,
    val repRejected: Boolean = false,
    val rejectionReason: String = "",
    val exerciseCompleted: Boolean = false,
    val isTimerRunning: Boolean = false
)