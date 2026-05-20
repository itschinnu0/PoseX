package com.example.posex.exercise

import android.os.SystemClock

/**
 * A specialized timer for isometric holds (like planks).
 *
 * Features:
 * 1. Frame smoothing via consecutive frame thresholds to prevent jitter.
 * 2. Delta time tracking for accurate time regardless of FPS.
 * 3. Max delta capping to avoid large jumps after backgrounding.
 */
class ActiveHoldTimer {
    companion object {
        private const val BAD_FRAME_THRESHOLD = 15
        private const val GOOD_FRAME_THRESHOLD = 10
        private const val MAX_DELTA_MILLIS = 100L
    }

    var accumulatedTimeMillis: Long = 0L
        private set

    var isTimerRunning: Boolean = false
        private set

    private var consecutiveBadFrames = 0
    private var consecutiveGoodFrames = 0
    private var lastUpdateTimestamp: Long = 0L

    fun processFrame(isFormValid: Boolean) {
        val currentTimestamp = SystemClock.elapsedRealtime()
        if (lastUpdateTimestamp == 0L) {
            lastUpdateTimestamp = currentTimestamp
            return
        }

        val deltaMillis = currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
        val safeDelta = deltaMillis.coerceAtMost(MAX_DELTA_MILLIS)

        if (isFormValid) {
            consecutiveBadFrames = 0
            consecutiveGoodFrames++
            if (!isTimerRunning && consecutiveGoodFrames >= GOOD_FRAME_THRESHOLD) {
                isTimerRunning = true
            }
        } else {
            consecutiveGoodFrames = 0
            consecutiveBadFrames++
            if (isTimerRunning && consecutiveBadFrames >= BAD_FRAME_THRESHOLD) {
                isTimerRunning = false
            }
        }

        if (isTimerRunning) {
            accumulatedTimeMillis += safeDelta
        }
    }

    fun forcePause() {
        isTimerRunning = false
        consecutiveGoodFrames = 0
        lastUpdateTimestamp = 0L
    }

    fun reset() {
        accumulatedTimeMillis = 0L
        isTimerRunning = false
        consecutiveBadFrames = 0
        consecutiveGoodFrames = 0
        lastUpdateTimestamp = 0L
    }
}

