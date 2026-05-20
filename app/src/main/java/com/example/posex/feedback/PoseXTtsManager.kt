package com.example.posex.feedback

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.SystemClock
import com.example.posex.exercise.WorkoutState
import java.util.Locale

class PoseXTtsManager(context: Context) {
    companion object {
        private const val GLOBAL_COOLDOWN_MS = 1500L
        private const val SAME_MESSAGE_COOLDOWN_MS = 4000L
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isSpeaking = false

    private var lastSpokenAtMs = 0L
    private var lastMessage: String? = null
    private var lastMessageAtMs = 0L

    private var workoutState: WorkoutState = WorkoutState.Idle

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    fun setWorkoutState(state: WorkoutState) {
        workoutState = state
        if (state !is WorkoutState.Active) {
            tts?.stop()
            isSpeaking = false
        }
    }

    fun speakCue(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        if (!isTtsReady || isSpeaking) return
        if (workoutState !is WorkoutState.Active) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastSpokenAtMs < GLOBAL_COOLDOWN_MS) return

        if (lastMessage != null && lastMessage == trimmed &&
            now - lastMessageAtMs < SAME_MESSAGE_COOLDOWN_MS
        ) {
            return
        }

        lastSpokenAtMs = now
        lastMessage = trimmed
        lastMessageAtMs = now

        tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, "posex_cue")
    }

    fun speakCountdown(number: Int) {
        if (!isTtsReady) return
        tts?.speak(number.toString(), TextToSpeech.QUEUE_FLUSH, null, "posex_countdown")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
