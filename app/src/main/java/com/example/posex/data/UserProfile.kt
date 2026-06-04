package com.example.posex.data

import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val age: Int,
    val gender: String,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
) {
    // These properties evaluate instantly whenever called in your app.
    // They are NOT stored in SharedPreferences, saving storage space.
    
    val currentBmi: Float
        get() = BmiCalculator.calculate(weightKg, heightCm)
        
    val healthStatus: BmiCategory
        get() = BmiCalculator.categorize(currentBmi)
}
