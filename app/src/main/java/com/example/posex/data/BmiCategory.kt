package com.example.posex.data

enum class BmiCategory(val label: String) {
    UNDERWEIGHT("Underweight"), // < 18.5
    NORMAL("Fit"),              // 18.5 - 24.9
    OVERWEIGHT("Overweight"),   // 25.0 - 29.9
    OBESE("Obese")              // 30.0+
}
