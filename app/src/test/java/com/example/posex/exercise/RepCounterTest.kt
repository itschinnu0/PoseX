package com.example.posex.exercise

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RepCounterTest {

    private lateinit var repCounter: RepCounter

    @Before
    fun setUp() {
        repCounter = RepCounter()
    }

    @Test
    fun testNoRepsCountedWhenStayingAtTop() {
        // Simulating user staying at top position (high knee angle)
        assertEquals(0, repCounter.updateReps(160.0))
        assertEquals(0, repCounter.updateReps(165.0))
        assertEquals(0, repCounter.updateReps(170.0))
    }

    @Test
    fun testNoRepsCountedWhenStayingAtBottom() {
        // Simulating user staying at bottom position (low knee angle)
        assertEquals(0, repCounter.updateReps(60.0))
        assertEquals(0, repCounter.updateReps(65.0))
        assertEquals(0, repCounter.updateReps(70.0))
    }

    @Test
    fun testOneRepCounted() {
        // Simulate a complete squat cycle: down and back up
        // Start at top
        repCounter.updateReps(165.0)
        assertEquals(0, repCounter.getRepCount())

        // Go down below bottom threshold
        repCounter.updateReps(80.0)
        assertEquals(0, repCounter.getRepCount())

        // Come back up above top threshold
        val reps = repCounter.updateReps(150.0)
        assertEquals(1, reps)
        assertEquals(1, repCounter.getRepCount())
    }

    @Test
    fun testMultipleRepsCounted() {
        // First squat: down and up
        repCounter.updateReps(165.0)  // Start at top
        repCounter.updateReps(80.0)   // Go down
        assertEquals(1, repCounter.updateReps(150.0))  // Come back up

        // Go back to top for next rep
        repCounter.updateReps(165.0)
        repCounter.updateReps(75.0)   // Go down again
        assertEquals(2, repCounter.updateReps(155.0))  // Come back up

        // Third rep
        repCounter.updateReps(170.0)
        repCounter.updateReps(85.0)   // Go down
        assertEquals(3, repCounter.updateReps(145.0))  // Come back up

        assertEquals(3, repCounter.getRepCount())
    }

    @Test
    fun testNoDoubleCountAtThresholdHysteresis() {
        // Simulate jitter around the top threshold
        repCounter.updateReps(165.0)  // Start at top
        repCounter.updateReps(75.0)   // Go down below BOTTOM_THRESHOLD (85)
        assertEquals(1, repCounter.updateReps(145.0))  // Cross TOP_THRESHOLD (140)

        // Simulate jitter - should not count again
        repCounter.updateReps(148.0)
        repCounter.updateReps(150.0)
        assertEquals(1, repCounter.getRepCount())

        // Go down again to start next rep
        repCounter.updateReps(120.0)
        repCounter.updateReps(75.0)
        assertEquals(2, repCounter.updateReps(150.0))

        assertEquals(2, repCounter.getRepCount())
    }

    @Test
    fun testResetFunctionality() {
        repCounter.updateReps(165.0)
        repCounter.updateReps(75.0)
        assertEquals(1, repCounter.updateReps(150.0))

        // Reset
        repCounter.reset()
        assertEquals(0, repCounter.getRepCount())

        // Should be able to count reps again
        repCounter.updateReps(165.0)
        repCounter.updateReps(80.0)
        assertEquals(1, repCounter.updateReps(150.0))
    }
}

