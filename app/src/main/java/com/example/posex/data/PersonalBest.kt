package com.example.posex.data

/**
 * Personal best record per exercise type.
 *
 * [exerciseType] — matches SessionRecord.exerciseType ("SQUAT", "PUSHUP", "PLANK").
 * [repCount]     — highest rep count ever recorded for this exercise.
 *                  Meaningful for SQUAT and PUSHUP only.
 * [holdSeconds]  — longest continuous good-form hold ever recorded.
 *                  Meaningful for PLANK only.
 *
 * Both fields are kept on every record so the model is uniform.
 * The irrelevant field will always be 0 for a given exercise type.
 */
data class PersonalBest(
    val exerciseType: String,
    val repCount: Int,
    val holdSeconds: Int
)