package com.example.posex.exercise

class RepCounter(
    private val bottomThreshold: Double,
    private val topThreshold: Double
) {
    private var repCount = 0
    private var isInBottomPosition = false

    fun updateReps(angle: Double): Int {
        when {
            angle < bottomThreshold && !isInBottomPosition -> {
                isInBottomPosition = true
            }
            angle > topThreshold && isInBottomPosition -> {
                isInBottomPosition = false
                repCount++
            }
        }
        return repCount
    }

    fun getRepCount(): Int = repCount

    fun reset() {
        repCount = 0
        isInBottomPosition = false
    }
}