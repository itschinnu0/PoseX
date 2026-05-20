package com.example.posex.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushupAnalyzerTest {

    @Test
    fun testBodyLineCuePiking() {
        val cue = PushupAnalyzer.bodyLineCue(140.0)
        assertEquals(FormCue.Severity.CRITICAL, cue?.severity)
        assertEquals("Lower your hips, your body is piking up", cue?.message)
    }

    @Test
    fun testBodyLineCueSagging() {
        val cue = PushupAnalyzer.bodyLineCue(190.0)
        assertEquals(FormCue.Severity.CRITICAL, cue?.severity)
        assertEquals("Lift your hips, your core is sagging", cue?.message)
    }

    @Test
    fun testBodyLineCueNeutral() {
        val cue = PushupAnalyzer.bodyLineCue(170.0)
        assertNull(cue)
    }
}

