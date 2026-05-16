package com.example.posex.exercise

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RepCounterTest {

    private lateinit var repCounter: CalibratingRepCounter

    // Squat fallback thresholds — matches SquatsAnalyzer
    private val fallbackBottom = 100.0
    private val fallbackTop = 140.0

    @Before
    fun setUp() {
        repCounter = CalibratingRepCounter(
            fallbackBottom = fallbackBottom,
            fallbackTop = fallbackTop
        )
        // Complete calibration before every test.
        // Calibration requires: reach bottom anchor (< fallbackBottom + 20 = 120)
        // then return to top anchor (> fallbackTop - 20 = 120).
        // We use a clean rep with good form (formValid = true) to complete it.
        repCounter.updateReps(165.0, formValid = true)  // start at top
        repCounter.updateReps(80.0, formValid = true)   // go below bottom anchor
        repCounter.updateReps(150.0, formValid = true)  // return above top anchor → calibrates
        // After calibration, first rep is NOT counted — counter still at 0.
        // Phase is now CALIBRATED and thresholds are set from observed range.
        assertEquals("Calibration should complete after first rep cycle",
            CalibratingRepCounter.Phase.CALIBRATED, repCounter.phase)
        assertEquals("No reps should be counted during calibration rep",
            0, repCounter.getRepCount())
    }

    @Test
    fun testNoRepsCountedWhenStayingAtTop() {
        assertEquals(0, repCounter.updateReps(160.0, formValid = true))
        assertEquals(0, repCounter.updateReps(165.0, formValid = true))
        assertEquals(0, repCounter.updateReps(170.0, formValid = true))
    }

    @Test
    fun testNoRepsCountedWhenStayingAtBottom() {
        assertEquals(0, repCounter.updateReps(60.0, formValid = true))
        assertEquals(0, repCounter.updateReps(65.0, formValid = true))
        assertEquals(0, repCounter.updateReps(70.0, formValid = true))
    }

    @Test
    fun testOneRepCounted() {
        repCounter.updateReps(165.0, formValid = true)  // start at top
        assertEquals(0, repCounter.getRepCount())

        repCounter.updateReps(80.0, formValid = true)   // go down
        assertEquals(0, repCounter.getRepCount())

        val reps = repCounter.updateReps(150.0, formValid = true)  // come back up
        assertEquals(1, reps)
        assertEquals(1, repCounter.getRepCount())
    }

    @Test
    fun testMultipleRepsCounted() {
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = true)
        assertEquals(1, repCounter.updateReps(150.0, formValid = true))

        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(75.0, formValid = true)
        assertEquals(2, repCounter.updateReps(155.0, formValid = true))

        repCounter.updateReps(170.0, formValid = true)
        repCounter.updateReps(85.0, formValid = true)
        assertEquals(3, repCounter.updateReps(145.0, formValid = true))

        assertEquals(3, repCounter.getRepCount())
    }

    @Test
    fun testNoDoubleCountAtThresholdHysteresis() {
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(75.0, formValid = true)
        assertEquals(1, repCounter.updateReps(145.0, formValid = true))

        // Jitter around top — should not count again
        repCounter.updateReps(148.0, formValid = true)
        repCounter.updateReps(150.0, formValid = true)
        assertEquals(1, repCounter.getRepCount())

        repCounter.updateReps(120.0, formValid = true)
        repCounter.updateReps(75.0, formValid = true)
        assertEquals(2, repCounter.updateReps(150.0, formValid = true))

        assertEquals(2, repCounter.getRepCount())
    }

    @Test
    fun testResetFunctionality() {
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(75.0, formValid = true)
        assertEquals(1, repCounter.updateReps(150.0, formValid = true))

        repCounter.reset()
        assertEquals(0, repCounter.getRepCount())
        assertEquals(CalibratingRepCounter.Phase.CALIBRATING, repCounter.phase)

        // Must re-calibrate after reset before reps count
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = true)
        repCounter.updateReps(150.0, formValid = true) // calibration rep
        assertEquals(0, repCounter.getRepCount())      // calibration rep not counted

        // Now reps count again
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = true)
        assertEquals(1, repCounter.updateReps(150.0, formValid = true))
    }

    @Test
    fun testRepRejectedWhenFormInvalidDuringDescent() {
        // Start rep with good form at top
        repCounter.updateReps(165.0, formValid = true)
        // Go down with bad form (CRITICAL cue active)
        repCounter.updateReps(80.0, formValid = false)
        // Return to top — rep should be rejected
        repCounter.updateReps(150.0, formValid = true)

        assertEquals("Rep should not be counted when form was invalid during descent",
            0, repCounter.getRepCount())
        assertTrue("lastRepRejected should be true", repCounter.lastRepRejected)
        assertTrue("rejectionReason should not be empty",
            repCounter.rejectionReason.isNotEmpty())
    }

    @Test
    fun testRepCountedWhenFormValidThroughout() {
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = true)
        repCounter.updateReps(150.0, formValid = true)

        assertEquals(1, repCounter.getRepCount())
        assertFalse("lastRepRejected should be false for clean rep",
            repCounter.lastRepRejected)
    }

    @Test
    fun testFormGateResetsAfterRejection() {
        // Rejected rep
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = false)
        repCounter.updateReps(150.0, formValid = true)
        assertEquals(0, repCounter.getRepCount())
        assertTrue(repCounter.lastRepRejected)

        // Clean rep immediately after — should count
        repCounter.updateReps(165.0, formValid = true)
        repCounter.updateReps(80.0, formValid = true)
        repCounter.updateReps(150.0, formValid = true)
        assertEquals(1, repCounter.getRepCount())
        assertFalse(repCounter.lastRepRejected)
    }
}