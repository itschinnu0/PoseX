package com.example.posex.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.posex.R

class PoseXSfxManager(context: Context) {
    private val soundPool: SoundPool
    private val repSoundId: Int
    private val tickSoundId: Int
    private var repLoaded = false
    private var tickLoaded = false
    private var activeTickStreamId: Int? = null

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()

        repSoundId = soundPool.load(context, R.raw.rep_increment, 1)
        tickSoundId = soundPool.load(context, R.raw.tick, 1)

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            if (sampleId == repSoundId) repLoaded = true
            if (sampleId == tickSoundId) tickLoaded = true
        }
    }

    fun playRepIncrement() {
        if (!repLoaded) return
        soundPool.play(repSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun playTick() {
        if (!tickLoaded) return
        activeTickStreamId?.let { soundPool.stop(it) }
        activeTickStreamId = soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
