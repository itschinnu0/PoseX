package com.example.posex.data

object BmiCalculator {
    /**
     * Calculates BMI based on weight (kg) and height (cm).
     * Returns a Float rounded to one decimal place.
     */
    fun calculate(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f) return 0f // Safety check: Prevent division by zero
        
        val heightMeters = heightCm / 100f
        val rawBmi = weightKg / (heightMeters * heightMeters)
        
        // Round to 1 decimal place (e.g., 22.456 -> 22.5)
        return Math.round(rawBmi * 10f) / 10f
    }

    /**
     * Maps the numerical BMI to the WHO standard categories.
     */
    fun categorize(bmi: Float): BmiCategory {
        return when {
            bmi <= 0f -> BmiCategory.NORMAL // Fallback for missing data
            bmi < 18.5f -> BmiCategory.UNDERWEIGHT
            bmi <= 24.9f -> BmiCategory.NORMAL
            bmi <= 29.9f -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }
}
