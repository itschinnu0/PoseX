package com.example.posex.feedback

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class FeedbackEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenTime = 0L
    private val cooldownMs = 4000L

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(message: String) {
        if (!isTtsReady) return
        val now = System.currentTimeMillis()
        if (now - lastSpokenTime < cooldownMs) return
        lastSpokenTime = now
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}