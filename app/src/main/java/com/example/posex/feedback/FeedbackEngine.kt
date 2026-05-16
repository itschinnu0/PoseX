package com.example.posex.feedback

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.posex.exercise.WorkoutState
import java.util.Locale

/**
 * Wraps Android TTS with two guards:
 *  1. 4-second cooldown between spoken messages (unchanged).
 *  2. State gate — only speaks when workoutState is Active.
 *     This prevents the engine from speaking during countdown, pause,
 *     or after completion regardless of what WorkoutScreen passes in.
 *
 * [setWorkoutState] must be called by WorkoutScreen on every state transition.
 */
class FeedbackEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenTime = 0L
    private val cooldownMs = 4000L

    // Defaults to Idle so nothing is spoken before the user starts
    private var workoutState: WorkoutState = WorkoutState.Idle

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun setWorkoutState(state: WorkoutState) {
        workoutState = state
        // Flush any queued speech immediately when leaving Active state
        if (state !is WorkoutState.Active) {
            tts?.stop()
        }
    }

    fun speak(message: String) {
        if (!isTtsReady) return
        // State gate: only speak during active workout
        if (workoutState !is WorkoutState.Active) return
        val now = System.currentTimeMillis()
        if (now - lastSpokenTime < cooldownMs) return
        lastSpokenTime = now
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Speaks countdown numbers without the state gate or cooldown.
     * Called directly by WorkoutScreen during Countdown state.
     */
    fun speakCountdown(number: Int) {
        if (!isTtsReady) return
        tts?.speak(number.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}