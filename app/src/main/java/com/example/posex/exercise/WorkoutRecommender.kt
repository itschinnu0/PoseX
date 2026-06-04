package com.example.posex.exercise

import com.example.posex.data.BmiCategory

object WorkoutRecommender {

    /**
     * Recommends a WorkoutConfig based on the user's BMI category and the chosen exercise.
     * Follows sports science principles for Hypertrophy, Strength, and Mobility.
     */
    fun recommend(category: BmiCategory, exerciseType: ExerciseType): WorkoutConfig {
        return when (category) {
            BmiCategory.UNDERWEIGHT -> recommendForUnderweight(exerciseType)
            BmiCategory.NORMAL -> recommendForNormal(exerciseType)
            BmiCategory.OVERWEIGHT -> recommendForOverweight(exerciseType)
            BmiCategory.OBESE -> recommendForObese(exerciseType)
        }
    }

    private fun recommendForUnderweight(type: ExerciseType): WorkoutConfig {
        return when (type) {
            ExerciseType.SQUAT -> WorkoutConfig(type, repsPerSet = 10, sets = 3, restSeconds = 90)
            ExerciseType.PUSHUP -> WorkoutConfig(type, repsPerSet = 8, sets = 3, restSeconds = 90)
            ExerciseType.PLANK -> WorkoutConfig(type, sets = 3, holdSeconds = 30, restSeconds = 90)
            ExerciseType.BICEPS_CURL -> WorkoutConfig(type, repsPerSet = 10, sets = 3, restSeconds = 90)
        }
    }

    private fun recommendForNormal(type: ExerciseType): WorkoutConfig {
        return when (type) {
            ExerciseType.SQUAT -> WorkoutConfig(type, repsPerSet = 20, sets = 4, restSeconds = 60)
            ExerciseType.PUSHUP -> WorkoutConfig(type, repsPerSet = 15, sets = 4, restSeconds = 60)
            ExerciseType.PLANK -> WorkoutConfig(type, sets = 3, holdSeconds = 60, restSeconds = 60)
            ExerciseType.BICEPS_CURL -> WorkoutConfig(type, repsPerSet = 12, sets = 4, restSeconds = 60)
        }
    }

    private fun recommendForOverweight(type: ExerciseType): WorkoutConfig {
        return when (type) {
            ExerciseType.SQUAT -> WorkoutConfig(type, repsPerSet = 20, sets = 3, restSeconds = 45)
            ExerciseType.PUSHUP -> WorkoutConfig(type, repsPerSet = 12, sets = 3, restSeconds = 45)
            ExerciseType.PLANK -> WorkoutConfig(type, sets = 3, holdSeconds = 45, restSeconds = 45)
            ExerciseType.BICEPS_CURL -> WorkoutConfig(type, repsPerSet = 15, sets = 3, restSeconds = 45)
        }
    }

    private fun recommendForObese(type: ExerciseType): WorkoutConfig {
        return when (type) {
            ExerciseType.SQUAT -> WorkoutConfig(type, repsPerSet = 8, sets = 2, restSeconds = 120)
            ExerciseType.PUSHUP -> WorkoutConfig(type, repsPerSet = 8, sets = 2, restSeconds = 120)
            ExerciseType.PLANK -> WorkoutConfig(type, sets = 2, holdSeconds = 20, restSeconds = 120)
            ExerciseType.BICEPS_CURL -> WorkoutConfig(type, repsPerSet = 12, sets = 2, restSeconds = 120)
        }
    }
}
