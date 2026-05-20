package com.example.posex.exercise

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RepCounterTest {

    private lateinit var repCounter: CalibratingRepCounter

    private val fallbackBottom = 100.0
    private val fallbackTop = 140.0

    private fun feedFrames(angle: Double, count: Int, formValid: Boolean = true) {
        repeat(count) { repCounter.updateReps(angle, formValid) }
    }

    private fun completeCalibration() {
        feedFrames(165.0, 2)   // stable at top
        feedFrames(95.0, 2)    // below fallbackBottom (100) → bottom anchor
        feedFrames(145.0, 2)   // above fallbackTop (140) → calibration completes
        assertEquals(CalibratingRepCounter.Phase.CALIBRATED, repCounter.phase)
        assertEquals(0, repCounter.getRepCount())
    }

    @Before
    fun setUp() {
        repCounter = CalibratingRepCounter(
            fallbackBottom = fallbackBottom,
            fallbackTop = fallbackTop
        )
        completeCalibration()
    }

    // ── Basic counting ────────────────────────────────────────────────────

    @Test
    fun testNoRepsCountedWhenStayingAtTop() {
        feedFrames(160.0, 4)
        assertEquals(0, repCounter.getRepCount())
    }

    @Test
    fun testNoRepsCountedWhenStayingAtBottom() {
        feedFrames(60.0, 4)
        assertEquals(0, repCounter.getRepCount())
    }

    @Test
    fun testOneRepCounted() {
        feedFrames(165.0, 2)
        feedFrames(80.0, 2)
        feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())
        assertFalse(repCounter.lastRepRejected)
    }

    @Test
    fun testMultipleRepsCounted() {
        feedFrames(165.0, 2); feedFrames(80.0, 2); feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())

        feedFrames(165.0, 2); feedFrames(75.0, 2); feedFrames(155.0, 2)
        assertEquals(2, repCounter.getRepCount())

        feedFrames(170.0, 2); feedFrames(85.0, 2); feedFrames(150.0, 2)
        assertEquals(3, repCounter.getRepCount())
    }

    // ── Stability window ──────────────────────────────────────────────────

    @Test
    fun testSingleFrameBelowThresholdDoesNotTriggerBottom() {
        repCounter.updateReps(165.0)
        repCounter.updateReps(80.0)   // 1 frame only — not enough
        repCounter.updateReps(165.0)  // back up before stability reached
        repCounter.updateReps(165.0)
        assertEquals(0, repCounter.getRepCount())
    }

    @Test
    fun testSingleFrameAboveThresholdDoesNotCompleteRep() {
        feedFrames(165.0, 2)
        feedFrames(80.0, 2)           // bottom confirmed
        repCounter.updateReps(150.0)  // 1 frame above top — not enough
        repCounter.updateReps(80.0)   // back down
        assertEquals(0, repCounter.getRepCount())
    }

    // ── Tolerance band — fixes inconsistent counting ───────────────────────

    @Test
    fun testOscillationNearBottomThresholdStillCountsRep() {
        // Simulate realistic squat descent: angle oscillates near threshold
        // before stabilising below it. With tolerance band, the stability
        // counter should survive brief excursions just above threshold.
        feedFrames(165.0, 2)          // at top
        repCounter.updateReps(98.0)   // below threshold (100)
        repCounter.updateReps(103.0)  // just above threshold — within tolerance (5°)
        repCounter.updateReps(97.0)   // back below — counter should NOT have reset
        feedFrames(150.0, 2)          // return to top → rep should count
        assertEquals("Rep should count despite oscillation near bottom threshold",
            1, repCounter.getRepCount())
    }

    @Test
    fun testLargeExcursionAboveThresholdResetsBottomCounter() {
        feedFrames(165.0, 2)
        repCounter.updateReps(98.0)   // below threshold — counter = 1
        repCounter.updateReps(120.0)  // 20° above threshold — well beyond tolerance
        // Counter should have reset — need 2 more clean frames below threshold
        repCounter.updateReps(98.0)   // counter = 1 again
        repCounter.updateReps(150.0)  // at top — should NOT count (bottom not confirmed)
        assertEquals("Large excursion above threshold should reset bottom counter",
            0, repCounter.getRepCount())
    }

    // ── Hysteresis ────────────────────────────────────────────────────────

    @Test
    fun testNoDoubleCountAtThresholdHysteresis() {
        feedFrames(165.0, 2); feedFrames(75.0, 2); feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())

        feedFrames(148.0, 2); feedFrames(150.0, 2)
        assertEquals("Jitter at top should not double count", 1, repCounter.getRepCount())

        feedFrames(75.0, 2); feedFrames(150.0, 2)
        assertEquals(2, repCounter.getRepCount())
    }

    // ── Form gate ─────────────────────────────────────────────────────────

    @Test
    fun testRepRejectedWhenFormInvalidDuringDescent() {
        feedFrames(165.0, 2, formValid = true)
        feedFrames(80.0, 2, formValid = false)
        feedFrames(150.0, 2, formValid = true)

        assertEquals(0, repCounter.getRepCount())
        assertTrue(repCounter.lastRepRejected)
        assertTrue(repCounter.rejectionReason.isNotEmpty())
    }

    @Test
    fun testRepCountedWhenFormValidThroughout() {
        feedFrames(165.0, 2, formValid = true)
        feedFrames(80.0, 2, formValid = true)
        feedFrames(150.0, 2, formValid = true)
        assertEquals(1, repCounter.getRepCount())
        assertFalse(repCounter.lastRepRejected)
    }

    @Test
    fun testFormGateResetsAfterRejection() {
        feedFrames(165.0, 2, formValid = true)
        feedFrames(80.0, 2, formValid = false)
        feedFrames(150.0, 2, formValid = true)
        assertEquals(0, repCounter.getRepCount())
        assertTrue(repCounter.lastRepRejected)

        feedFrames(165.0, 2, formValid = true)
        feedFrames(80.0, 2, formValid = true)
        feedFrames(150.0, 2, formValid = true)
        assertEquals(1, repCounter.getRepCount())
        assertFalse(repCounter.lastRepRejected)
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    @Test
    fun testResetFunctionality() {
        feedFrames(165.0, 2); feedFrames(75.0, 2); feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())

        repCounter.reset()
        assertEquals(0, repCounter.getRepCount())
        assertEquals(CalibratingRepCounter.Phase.CALIBRATING, repCounter.phase)

        completeCalibration()

        feedFrames(165.0, 2); feedFrames(80.0, 2); feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())
    }

    // ── External movement validator ───────────────────────────────────────

    @Test
    fun testMovementValidatorBlocksRep() {
        repCounter.movementValidator = {
            CalibratingRepCounter.ValidationResult(false, "Test rejection")
        }

        feedFrames(165.0, 2); feedFrames(80.0, 2); feedFrames(150.0, 2)

        assertEquals(0, repCounter.getRepCount())
        assertTrue(repCounter.lastRepRejected)
        assertEquals("Test rejection", repCounter.rejectionReason)
    }

    @Test
    fun testMovementValidatorAllowsRep() {
        repCounter.movementValidator = {
            CalibratingRepCounter.ValidationResult(true)
        }

        feedFrames(165.0, 2); feedFrames(80.0, 2); feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())
        assertFalse(repCounter.lastRepRejected)
    }

    @Test
    fun testAbortCurrentRepResetsState() {
        feedFrames(165.0, 2)
        feedFrames(80.0, 2) // enter bottom position
        repCounter.abortCurrentRep()

        // Returning to top should not count because rep was aborted
        feedFrames(150.0, 2)
        assertEquals(0, repCounter.getRepCount())

        // A fresh full rep should count normally
        feedFrames(165.0, 2)
        feedFrames(80.0, 2)
        feedFrames(150.0, 2)
        assertEquals(1, repCounter.getRepCount())
    }

    @Test
    fun testCalibrationRejectedByMovementValidator() {
        val counter = CalibratingRepCounter(
            fallbackBottom = fallbackBottom,
            fallbackTop = fallbackTop
        )
        counter.movementValidator = {
            CalibratingRepCounter.ValidationResult(false, "Leg raise")
        }

        // Attempt calibration
        repeat(2) { counter.updateReps(165.0) }
        repeat(2) { counter.updateReps(95.0) }
        counter.updateReps(145.0)

        assertEquals(CalibratingRepCounter.Phase.CALIBRATING, counter.phase)
        assertTrue(counter.lastRepRejected)
        assertTrue(counter.rejectionReason.contains("Calibration failed"))
    }

    @Test
    fun testCalibrationSucceedsWhenMovementValidatorPasses() {
        val counter = CalibratingRepCounter(
            fallbackBottom = fallbackBottom,
            fallbackTop = fallbackTop
        )
        counter.movementValidator = {
            CalibratingRepCounter.ValidationResult(true)
        }

        repeat(2) { counter.updateReps(165.0) }
        repeat(2) { counter.updateReps(95.0) }
        repeat(2) { counter.updateReps(145.0) }

        assertEquals(CalibratingRepCounter.Phase.CALIBRATED, counter.phase)
        assertFalse(counter.lastRepRejected)
    }
}