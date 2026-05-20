package com.example.posex.exercise

/**
 * Rep counter with five defences against miscounting:
 *
 * 1. CALIBRATION FROM FIRST REP
 *    Watches the user's actual range of motion on rep 1.
 *    Sets bottomThreshold = observedMin + THRESHOLD_BUFFER
 *    Sets topThreshold    = observedMax - THRESHOLD_BUFFER
 *    Falls back to constructor values if observed range is too small.
 *    First rep is observation only — not counted.
 *
 * 2. STABILITY WINDOWS (fixes noisy ML Kit landmarks)
 *    Angle must cross threshold for STABILITY_FRAMES = 2 consecutive
 *    frames before position flips. Single rogue frames are ignored.
 *
 * 3. TOLERANCE BAND (fixes inconsistent counting near threshold)
 *    Once the stability counter starts accumulating toward bottom,
 *    it only resets if the angle moves more than STABILITY_TOLERANCE
 *    degrees BACK above the threshold — not just 1-2° above it.
 *    This handles the natural oscillation that occurs when a user
 *    is near the threshold during a real movement.
 *    Same logic applies to the top stability counter.
 *
 * 4. FORM GATE
 *    If formValid = false on any frame during descent, rep is rejected.
 *
 * 5. MOVEMENT VALIDATION via [squatValidator]
 *    Optional external validator called on rep completion.
 *    Used to reject leg raises, partial movements, etc.
 *    Returns a [ValidationResult] — pass or reject with reason.
 */
class CalibratingRepCounter(
    private val fallbackBottom: Double,
    private val fallbackTop: Double
) {
    companion object {
        private const val THRESHOLD_BUFFER = 15.0
        private const val MIN_CALIBRATION_RANGE = 35.0
        const val STABILITY_FRAMES = 2

        // Tolerance band: stability counter only resets if angle moves
        // this many degrees back past the threshold in the wrong direction.
        // Handles natural oscillation near threshold during real movement.
        private const val STABILITY_TOLERANCE = 5.0
    }

    // ── Phase ─────────────────────────────────────────────────────────────

    enum class Phase { CALIBRATING, CALIBRATED }

    var phase: Phase = Phase.CALIBRATING
        private set

    // ── Thresholds ────────────────────────────────────────────────────────

    private var bottomThreshold = fallbackBottom
    private var topThreshold = fallbackTop

    // ── Rep counting state ────────────────────────────────────────────────

    private var repCount = 0
    private var isInBottomPosition = false

    private var consecutiveBottomFrames = 0
    private var consecutiveTopFrames = 0

    // Form gate
    private var hadCriticalThisRep = false

    // ── Calibration state ─────────────────────────────────────────────────

    private var observedMin = Double.MAX_VALUE
    private var observedMax = Double.MIN_VALUE
    private var calibrationReachedBottom = false

    // ── Result flags ──────────────────────────────────────────────────────

    var lastRepRejected: Boolean = false
        private set
    var rejectionReason: String = ""
        private set

    // ── External movement validator ───────────────────────────────────────

    data class ValidationResult(val passed: Boolean, val reason: String = "")

    /**
     * Optional validator called on every rep completion attempt.
     * Set by the analyzer (e.g. SquatsAnalyzer sets a leg-raise detector).
     * If null, no external validation is performed.
     */
    var movementValidator: (() -> ValidationResult)? = null

    // ── Public API ────────────────────────────────────────────────────────

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
        consecutiveBottomFrames = 0
        consecutiveTopFrames = 0
        hadCriticalThisRep = false
        lastRepRejected = false
        rejectionReason = ""
    }

    fun abortCurrentRep() {
        isInBottomPosition = false
        consecutiveBottomFrames = 0
        consecutiveTopFrames = 0
        hadCriticalThisRep = false
    }

    // ── Calibration ───────────────────────────────────────────────────────

    private fun calibrate(angle: Double): Int {
        if (angle < observedMin) observedMin = angle
        if (angle > observedMax) observedMax = angle

        val range = observedMax - observedMin

        if (!calibrationReachedBottom) {
            calibrationReachedBottom = angle <= fallbackBottom ||
                    range >= MIN_CALIBRATION_RANGE
        }

        val returnedToTop = angle >= fallbackTop ||
                angle >= observedMax - THRESHOLD_BUFFER

        if (calibrationReachedBottom && returnedToTop) {
            if (range >= MIN_CALIBRATION_RANGE) {
                // FIX: Run external validator before accepting the calibration!
                val validation = movementValidator?.invoke()
                if (validation != null && !validation.passed) {
                    lastRepRejected = true
                    rejectionReason = "Calibration failed: ${validation.reason}"

                    // Reset calibration state so they must try again
                    observedMin = Double.MAX_VALUE
                    observedMax = Double.MIN_VALUE
                    calibrationReachedBottom = false
                    return repCount
                }

                bottomThreshold = observedMin + THRESHOLD_BUFFER
                topThreshold = observedMax - THRESHOLD_BUFFER
            }
            phase = Phase.CALIBRATED
        }

        return repCount
    }

    // ── Rep counting ──────────────────────────────────────────────────────

    private fun countRep(angle: Double, formValid: Boolean): Int {
        // FIX 1: Catch bad form ANYTIME during the movement.
        // If they are below the topThreshold, they are actively in a rep.
        val isActivelyMoving = angle < topThreshold
        if (isActivelyMoving && !formValid) {
            hadCriticalThisRep = true
        }

        when {
            // ── Approaching bottom ────────────────────────────────────────
            !isInBottomPosition && angle <= bottomThreshold -> {
                consecutiveBottomFrames++

                if (consecutiveBottomFrames >= STABILITY_FRAMES) {
                    isInBottomPosition = true
                    consecutiveBottomFrames = 0
                }
            }

            // ── Returning to top ──────────────────────────────────────────
            isInBottomPosition && angle >= topThreshold -> {
                consecutiveTopFrames++

                if (consecutiveTopFrames >= STABILITY_FRAMES) {
                    isInBottomPosition = false
                    consecutiveTopFrames = 0
                    evaluateRep()
                }
            }

            // ── In middle zone — apply tolerance before resetting ─────────
            else -> {
                // If they bounce back UP slightly during descent, reset bottom stability
                if (!isInBottomPosition && angle > bottomThreshold + STABILITY_TOLERANCE) {
                    consecutiveBottomFrames = 0
                }
                // If they bounce back DOWN slightly during ascent, reset top stability
                if (isInBottomPosition && angle < topThreshold - STABILITY_TOLERANCE) {
                    consecutiveTopFrames = 0
                }
            }
        }

        // FIX 2: State Leak Prevention (Aborted Reps).
        // If they return to a fully upright standing position WITHOUT triggering
        // evaluateRep() (meaning they aborted a shallow rep), clear the bad form
        // flag so it doesn't unjustly ruin their next attempt.
        if (!isInBottomPosition && angle >= topThreshold + STABILITY_TOLERANCE) {
            hadCriticalThisRep = false
        }

        return repCount
    }

    private fun evaluateRep() {
        when {
            hadCriticalThisRep -> {
                lastRepRejected = true
                rejectionReason = "Rep not counted — fix your form first"
            }
            else -> {
                // Run external movement validator if set
                val validation = movementValidator?.invoke()
                if (validation != null && !validation.passed) {
                    lastRepRejected = true
                    rejectionReason = validation.reason
                } else {
                    repCount++
                }
            }
        }
        // Always reset per-rep tracking
        hadCriticalThisRep = false
    }
}