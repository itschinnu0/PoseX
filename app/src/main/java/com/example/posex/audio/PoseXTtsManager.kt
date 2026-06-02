package com.example.posex.audio

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.posex.exercise.WorkoutState
import java.util.Locale

class PoseXTtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val GLOBAL_COOLDOWN_MS = 1500L
        private const val SAME_CUE_COOLDOWN_MS = 4000L
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false

    private var lastSpokenTimeAny: Long = 0L
    private var lastMessage: String? = null
    private var lastMessageAtMs: Long = 0L

    private var workoutState: WorkoutState = WorkoutState.Idle

    init {
        tts = TextToSpeech(context, this)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("PoseXTtsManager", "The Language specified is not supported!")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("PoseXTtsManager", "Initialization Failed!")
        }
    }

    fun setWorkoutState(state: WorkoutState) {
        workoutState = state
        if (state !is WorkoutState.Active) {
            tts?.stop()
            isSpeaking = false
        }
    }

    fun speakCue(message: String) {
        if (!isInitialized || isSpeaking) return
        if (workoutState !is WorkoutState.Active) return

        val trimmed = message.trim()
        if (trimmed.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastSpokenTimeAny < GLOBAL_COOLDOWN_MS) return

        if (lastMessage != null && lastMessage == trimmed &&
            now - lastMessageAtMs < SAME_CUE_COOLDOWN_MS
        ) {
            return
        }

        lastSpokenTimeAny = now
        lastMessage = trimmed
        lastMessageAtMs = now

        tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, "posex_cue")
    }

    fun speakCountdown(number: Int) {
        if (!isInitialized) return
        tts?.speak(number.toString(), TextToSpeech.QUEUE_FLUSH, null, "posex_countdown")
    }

    fun speakRestStart(seconds: Int) {
        val safeSeconds = seconds.coerceAtLeast(1)
        speakAnnouncement(
            message = "Take rest for $safeSeconds seconds",
            utteranceId = "posex_rest_start"
        )
    }

    fun speakRestComplete() {
        speakAnnouncement(
            message = "Rest time is over. Get ready to continue.",
            utteranceId = "posex_rest_end"
        )
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    private fun speakAnnouncement(message: String, utteranceId: String) {
        if (!isInitialized) return
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        lastSpokenTimeAny = now
        lastMessage = trimmed
        lastMessageAtMs = now

        tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
}
