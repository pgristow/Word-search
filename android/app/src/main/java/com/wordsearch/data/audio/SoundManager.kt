package com.wordsearch.data.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.wordsearch.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central audio: short SFX via SoundPool (correct / incorrect), looping background
 * music via MediaPlayer, plus a preview player for the settings screen. All settings
 * persist in SharedPreferences.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ws_audio", Context.MODE_PRIVATE)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val correctId = soundPool.load(context, R.raw.correct, 1)
    private val incorrectId = soundPool.load(context, R.raw.incorrect, 1)

    private var music: MediaPlayer? = null
    private var preview: MediaPlayer? = null

    /** Selectable lo-fi tracks: id -> (display name, raw resource).
     *  Pluck/Beat/Bright are rhythmic with short note envelopes (no drawn-out pads). */
    val tracks: List<Triple<String, String, Int>> = listOf(
        Triple("lofi_pluck", "Pluck", R.raw.lofi_pluck),
        Triple("lofi_beat", "Beat", R.raw.lofi_beat),
        Triple("lofi_bright", "Bright", R.raw.lofi_bright),
        Triple("lofi_chill", "Chill", R.raw.lofi_chill),
        Triple("lofi_dreamy", "Dreamy", R.raw.lofi_dreamy),
        Triple("lofi_mellow", "Mellow", R.raw.lofi_mellow),
    )

    var sfxEnabled: Boolean
        get() = prefs.getBoolean("sfx", true)
        set(v) = prefs.edit().putBoolean("sfx", v).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music", true)
        set(v) {
            prefs.edit().putBoolean("music", v).apply()
            if (!v) stopMusic()
        }

    var musicTrack: String
        get() = prefs.getString("track", "lofi_pluck") ?: "lofi_pluck"
        set(v) = prefs.edit().putString("track", v).apply()

    /** Preferred default mode ("CLASSIC"/"CASUAL"), or null to always ask. */
    var defaultMode: String?
        get() = prefs.getString("mode", null)
        set(v) = prefs.edit().putString("mode", v).apply()

    private fun resFor(id: String) = tracks.firstOrNull { it.first == id }?.third ?: R.raw.lofi_pluck

    fun playCorrect() { if (sfxEnabled) soundPool.play(correctId, 0.9f, 0.9f, 1, 0, 1f) }
    fun playIncorrect() { if (sfxEnabled) soundPool.play(incorrectId, 1f, 1f, 1, 0, 1f) }

    fun startCasualMusic() {
        if (!musicEnabled) return
        stopMusic()
        music = MediaPlayer.create(context, resFor(musicTrack))?.apply {
            isLooping = true
            setVolume(0.45f, 0.45f)
            start()
        }
    }

    fun stopMusic() {
        music?.let { runCatching { it.stop() }; it.release() }
        music = null
    }

    /** Play a track once for previewing in settings. */
    fun previewTrack(id: String) {
        stopPreview()
        preview = MediaPlayer.create(context, resFor(id))?.apply {
            setVolume(0.6f, 0.6f)
            start()
        }
    }

    fun stopPreview() {
        preview?.let { runCatching { it.stop() }; it.release() }
        preview = null
    }
}
