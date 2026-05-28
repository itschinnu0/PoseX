package com.example.posex.exercise

object WorkoutConfigValidator {

    data class ValidationResult(
        val config: WorkoutConfig,
        val warnings: List<String>
    )

    fun validate(config: WorkoutConfig): ValidationResult {
        val warnings = mutableListOf<String>()

        return when (config.exerciseType) {
            ExerciseType.SQUAT  -> validateSquat(config, warnings)
            ExerciseType.PUSHUP -> validatePushup(config, warnings)
            ExerciseType.PLANK  -> validatePlank(config, warnings)
        }
    }

    private fun validateSquat(
        config: WorkoutConfig,
        warnings: MutableList<String>
    ): ValidationResult {
        var reps = config.repsPerSet
        var sets = config.sets
        var rest = config.restSeconds

        if (reps < 5) {
            warnings.add("Minimum 5 reps per set for squats. Adjusted to 5.")
            reps = 5
        }
        if (reps > 30) {
            warnings.add("30 reps is the safe maximum for bodyweight squats. Adjusted to 30.")
            reps = 30
        }
        if (sets < 1) {
            warnings.add("Minimum 1 set required. Adjusted to 1.")
            sets = 1
        }
        if (sets > 5) {
            warnings.add("More than 5 sets increases injury risk. Adjusted to 5.")
            sets = 5
        }
        if (rest < 30) {
            warnings.add("Minimum 30 seconds rest needed for muscle recovery. Adjusted to 30s.")
            rest = 30
        }
        if (rest > 180) {
            warnings.add("Rest over 3 minutes reduces training effectiveness. Adjusted to 3 minutes.")
            rest = 180
        }

        return ValidationResult(
            config.copy(repsPerSet = reps, sets = sets, restSeconds = rest),
            warnings
        )
    }

    private fun validatePushup(
        config: WorkoutConfig,
        warnings: MutableList<String>
    ): ValidationResult {
        var reps = config.repsPerSet
        var sets = config.sets
        var rest = config.restSeconds

        if (reps < 3) {
            warnings.add("Minimum 3 reps per set for push-ups. Adjusted to 3.")
            reps = 3
        }
        if (reps > 25) {
            warnings.add("25 reps is the safe maximum per set for push-ups. Adjusted to 25.")
            reps = 25
        }
        if (sets < 1) {
            warnings.add("Minimum 1 set required. Adjusted to 1.")
            sets = 1
        }
        if (sets > 5) {
            warnings.add("More than 5 sets increases injury risk. Adjusted to 5.")
            sets = 5
        }
        if (rest < 30) {
            warnings.add("Minimum 30 seconds rest needed for muscle recovery. Adjusted to 30s.")
            rest = 30
        }
        if (rest > 180) {
            warnings.add("Rest over 3 minutes reduces training effectiveness. Adjusted to 3 minutes.")
            rest = 180
        }

        return ValidationResult(
            config.copy(repsPerSet = reps, sets = sets, restSeconds = rest),
            warnings
        )
    }

    private fun validatePlank(
        config: WorkoutConfig,
        warnings: MutableList<String>
    ): ValidationResult {
        var hold = config.holdSeconds
        var sets = config.sets
        var rest = config.restSeconds

        if (hold < 10) {
            warnings.add("Minimum 10 seconds needed for an effective plank. Adjusted to 10s.")
            hold = 10
        }
        if (hold > 120) {
            warnings.add("Beyond 120 seconds offers diminishing returns and risks lower-back strain (McGill, 2010). Adjusted to 120s.")
            hold = 120
        }
        if (sets < 1) {
            warnings.add("Minimum 1 set required. Adjusted to 1.")
            sets = 1
        }
        if (sets > 3) {
            warnings.add("More than 3 plank sets per session is excessive. Adjusted to 3.")
            sets = 3
        }
        if (rest < 20) {
            warnings.add("Minimum 20 seconds rest needed between planks. Adjusted to 20s.")
            rest = 20
        }
        if (rest > 120) {
            warnings.add("Rest over 2 minutes is unnecessary between planks. Adjusted to 2 minutes.")
            rest = 120
        }

        return ValidationResult(
            config.copy(holdSeconds = hold, sets = sets, restSeconds = rest),
            warnings
        )
    }
}