package com.example.posex.exercise

/**
 * User-configured workout parameters.
 *
 * [repsPerSet]      — target reps per set (squats/pushups). 0 for plank.
 * [sets]            — total number of sets.
 * [restSeconds]     — rest duration between sets in seconds.
 * [holdSeconds]     — target hold duration per set (plank only). 0 for others.
 */
data class WorkoutConfig(
    val exerciseType: ExerciseType,
    val repsPerSet: Int = 0,
    val sets: Int = 1,
    val restSeconds: Int = 60,
    val holdSeconds: Int = 30
)