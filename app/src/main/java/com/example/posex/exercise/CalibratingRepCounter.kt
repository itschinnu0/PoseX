package com.example.posex.exercise

/**
 * Rep counter that calibrates its thresholds from the user's first rep
 * instead of using hardcoded values.
 *
 * ## Calibration phase
 * On the first rep cycle, the counter watches the angle stream and records:
 *   - [observedMin] — deepest angle reached (bottom of movement)
 *   - [observedMax] — most extended angle reached (top of movement)
 *
 * Calibration completes when the user returns to the top position after
 * reaching the bottom. Thresholds are then set as:
 *   bottomThreshold = observedMin + THRESHOLD_BUFFER
 *   topThreshold    = observedMax - THRESHOLD_BUFFER
 *
 * The buffer (15°) prevents the threshold from sitting exactly at the
 * user's personal limit — they'd need to match it precisely every rep.
 * 15° gives realistic tolerance without being so loose that partial reps count.
 *
 * The first rep is NOT counted — it is observation only.
 *
 * ## Form gate
 * [updateReps] accepts a [formValid] parameter. If false when the rep
 * would complete (angle crosses topThreshold from bottom), the rep is
 * rejected and [lastRepRejected] is set to true. The caller (WorkoutScreen)
 * reads this flag to show the user why their rep didn't count.
 *
 * ## Fallback thresholds
 * If calibration produces a range smaller than MIN_CALIBRATION_RANGE degrees
 * (user barely moved), the counter falls back to the provided
 * [fallbackBottom] and [fallbackTop]. This handles cases where the user
 * twitched during calibration rather than doing a real rep.
 */
class CalibratingRepCounter(
    private val fallbackBottom: Double,
    private val fallbackTop: Double
) {
    companion object {
        private const val THRESHOLD_BUFFER = 15.0
        // Minimum angle range for calibration to be considered valid.
        // A squat should have at least 40° of range; pushup at least 50°.
        // If range is smaller, fall back to hardcoded thresholds.
        private const val MIN_CALIBRATION_RANGE = 35.0
    }

    // ── State ─────────────────────────────────────────────────────────────

    enum class Phase { CALIBRATING, CALIBRATED }

    var phase: Phase = Phase.CALIBRATING
        private set

    private var repCount = 0

    // Calibration tracking
    private var observedMin = Double.MAX_VALUE
    private var observedMax = Double.MIN_VALUE
    private var calibrationReachedBottom = false

    // Working thresholds — set after calibration or from fallback
    private var bottomThreshold = fallbackBottom
    private var topThreshold = fallbackTop

    // Rep cycle tracking
    private var isInBottomPosition = false

    /** True if the most recent completed rep cycle was rejected due to form. */
    var lastRepRejected: Boolean = false
        private set

    /** Human-readable reason for the last rejection. Empty if not rejected. */
    var rejectionReason: String = ""
        private set

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Feed the current angle each frame.
     *
     * [formValid] — pass false if a CRITICAL cue is active this frame.
     *               The rep will be blocked if form was invalid at any
     *               point during the current descent.
     *
     * Returns the current rep count.
     */
    fun updateReps(angle: Double, formValid: Boolean = true): Int {
        lastRepRejected = false
        rejectionReason = ""

        return when (phase) {
            Phase.CALIBRATING -> calibrate(angle)
            Phase.CALIBRATED  -> countRep(angle, formValid)
        }
    }

    fun getRepCount(): Int = repCount

    fun isCalibrating(): Boolean = phase == Phase.CALIBRATING

    fun reset() {
        phase = Phase.CALIBRATING
        repCount = 0
        observedMin = Double.MAX_VALUE
        observedMax = Double.MIN_VALUE
        calibrationReachedBottom = false
        bottomThreshold = fallbackBottom
        topThreshold = fallbackTop
        isInBottomPosition = false
        lastRepRejected = false
        rejectionReason = ""
        hadCriticalThisRep = false
    }

    // ── Private: calibration ──────────────────────────────────────────────

    private fun calibrate(angle: Double): Int {
        // Track full range during calibration rep
        if (angle < observedMin) observedMin = angle
        if (angle > observedMax) observedMax = angle

        // Wait for user to reach bottom
        if (!calibrationReachedBottom && angle < fallbackBottom + 20) {
            calibrationReachedBottom = true
        }

        // Calibration complete when user returns to top after reaching bottom
        if (calibrationReachedBottom && angle > fallbackTop - 20) {
            val range = observedMax - observedMin
            if (range >= MIN_CALIBRATION_RANGE) {
                bottomThreshold = observedMin + THRESHOLD_BUFFER
                topThreshold = observedMax - THRESHOLD_BUFFER
            }
            // If range too small, fallback thresholds remain unchanged
            phase = Phase.CALIBRATED
        }

        return repCount
    }

    // ── Private: rep counting with form gate ──────────────────────────────

    // Tracks whether a CRITICAL cue fired at any point during current descent
    private var hadCriticalThisRep = false

    private fun countRep(angle: Double, formValid: Boolean): Int {
        // Track critical cues during descent
        if (isInBottomPosition && !formValid) {
            hadCriticalThisRep = true
        }

        when {
            angle < bottomThreshold && !isInBottomPosition -> {
                isInBottomPosition = true
                hadCriticalThisRep = if (!formValid) true else hadCriticalThisRep
            }
            angle > topThreshold && isInBottomPosition -> {
                isInBottomPosition = false
                if (hadCriticalThisRep) {
                    // Rep blocked — form was critically wrong during this cycle
                    lastRepRejected = true
                    rejectionReason = "Rep not counted — fix your form first"
                } else {
                    repCount++
                }
                hadCriticalThisRep = false
            }
        }

        return repCount
    }
}