package com.example.posex.data

/**
 * Immutable record of a single completed workout session.
 *
 * [id]           — UUID string; unique key per session.
 * [exerciseType] — "SQUAT", "PUSHUP", or "PLANK". Stored as String so the
 *                  model survives ExerciseType enum renames without migration.
 * [date]         — epoch milliseconds at session start (System.currentTimeMillis()).
 * [repCount]     — final rep count. 0 for PLANK.
 * [holdSeconds]  — final hold duration in seconds. 0 for SQUAT/PUSHUP.
 * [durationMs]   — wall-clock ms from ACTIVE state start to session end.
 *                  Does not include countdown time. Pause time IS included
 *                  because WorkoutSession does not subtract it — keep this
 *                  in mind if you add a rest timer later.
 * [criticalCues] — number of CRITICAL-severity cues fired during the session.
 * [warningCues]  — number of WARNING-severity cues fired during the session.
 */
data class SessionRecord(
    val id: String,
    val exerciseType: String,
    val date: Long,
    val repCount: Int,
    val holdSeconds: Int,
    val durationMs: Long,
    val criticalCues: Int,
    val warningCues: Int
)