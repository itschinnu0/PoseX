package com.example.posex.audio

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class PoseXTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Throttling thresholds
    private val GLOBAL_COOLDOWN_MS = 2000L      // Minimum time between ANY speech
    private val SAME_CUE_COOLDOWN_MS = 5000L    // Minimum time before repeating the SAME cue

    // State tracking
    private var lastSpokenTimeAny: Long = 0L
    private val cueTimestamps = mutableMapOf<String, Long>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Default to US English, but you can check the device locale
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("PoseXTtsManager", "The Language specified is not supported!")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("PoseXTtsManager", "Initialization Failed!")
        }
    }

    /**
     * Attempts to speak a cue. Will silently ignore the request if cooldowns are active.
     */
    fun speakCue(message: String) {
        if (!isInitialized) return

        val currentTime = SystemClock.elapsedRealtime()

        // 1. Check Global Cooldown (Are we already talking?)
        if (currentTime - lastSpokenTimeAny < GLOBAL_COOLDOWN_MS) {
            return
        }

        // 2. Check Same-Cue Cooldown (Did we just say this exact warning?)
        val lastSpokenThisCue = cueTimestamps[message] ?: 0L
        if (currentTime - lastSpokenThisCue < SAME_CUE_COOLDOWN_MS) {
            return
        }

        // 3. Passed all checks! Update timestamps and speak.
        lastSpokenTimeAny = currentTime
        cueTimestamps[message] = currentTime

        // QUEUE_FLUSH drops anything currently playing and speaks immediately
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "Cue_$currentTime")
    }

    /**
     * Call this in the Activity/Fragment's onDestroy() to prevent memory leaks.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}