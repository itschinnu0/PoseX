package com.example.posex.exercise

/**
 * Tracks rep counting state and rep count
 * Maintains state across multiple pose analyses to detect rep completion
 */
class RepCounter {
    private var repCount = 0
    private var isInBottomPosition = false
    private var hasReachedTopAfterBottom = false

    /**
     * Updates rep counter based on knee angle
     * A rep is counted when: bottom position detected → top position detected
     * Uses hysteresis to avoid counting jitter near thresholds
     */
    fun updateReps(kneeAngle: Double): Int {
        // Thresholds with hysteresis to avoid jitter
        val BOTTOM_THRESHOLD = 85.0   // Knee angle indicating bottom of squat
        val TOP_THRESHOLD = 140.0     // Knee angle indicating top of squat

        when {
            // User is going down - entering bottom position
            kneeAngle < BOTTOM_THRESHOLD && !isInBottomPosition -> {
                isInBottomPosition = true
                hasReachedTopAfterBottom = false
            }

            // User is coming up from bottom
            kneeAngle > TOP_THRESHOLD && isInBottomPosition -> {
                if (!hasReachedTopAfterBottom) {
                    hasReachedTopAfterBottom = true
                    isInBottomPosition = false
                    repCount++
                }
            }

            // Reset if user goes back to ambiguous middle range
            kneeAngle in 100.0..130.0 -> {
                if (hasReachedTopAfterBottom && kneeAngle < 110.0) {
                    hasReachedTopAfterBottom = false
                }
            }
        }

        return repCount
    }

    fun getRepCount(): Int = repCount

    fun reset() {
        repCount = 0
        isInBottomPosition = false
        hasReachedTopAfterBottom = false
    }
}

