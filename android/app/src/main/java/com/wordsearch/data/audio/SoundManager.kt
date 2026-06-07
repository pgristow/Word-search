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

    /** Selectable music tracks: id -> (display name, raw resource).
     *  All by Kevin MacLeod (incompetech.com), CC BY 4.0 — see the in-app Credits screen. */
    val tracks: List<Triple<String, String, Int>> = listOf(
        Triple("journey_ascend", "Journey To Ascend", R.raw.journey_ascend),
        Triple("whimsy_groove", "Whimsy Groove", R.raw.whimsy_groove),
        Triple("foxtale_waltz", "Fox Tale Waltz", R.raw.foxtale_waltz),
        Triple("half_mystery", "Half Mystery", R.raw.half_mystery),
    )

    /** The track currently playing in shuffle mode, so we don't repeat it back-to-back. */
    private var nowPlayingId: String? = null

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
        get() = prefs.getString("track", "journey_ascend") ?: "journey_ascend"
        set(v) = prefs.edit().putString("track", v).apply()

    /** When true, casual music shuffles through all tracks instead of looping one. */
    var shuffleMusic: Boolean
        get() = prefs.getBoolean("shuffle", true)
        set(v) = prefs.edit().putBoolean("shuffle", v).apply()

    /** Background music volume, 0f..1f. */
    var musicVolume: Float
        get() = prefs.getFloat("music_vol", 0.6f)
        set(v) {
            val vol = v.coerceIn(0f, 1f)
            prefs.edit().putFloat("music_vol", vol).apply()
            music?.setVolume(vol, vol)
            preview?.setVolume(vol, vol)
        }

    /** Preferred default mode ("CLASSIC"/"CASUAL"), or null to always ask. */
    var defaultMode: String?
        get() = prefs.getString("mode", null)
        set(v) = prefs.edit().putString("mode", v).apply()

    private fun resFor(id: String) = tracks.firstOrNull { it.first == id }?.third ?: R.raw.journey_ascend

    fun playCorrect() { if (sfxEnabled) soundPool.play(correctId, 0.9f, 0.9f, 1, 0, 1f) }
    fun playIncorrect() { if (sfxEnabled) soundPool.play(incorrectId, 1f, 1f, 1, 0, 1f) }

    /**
     * Start casual background music. In shuffle mode (default) it starts on a random
     * track and, when each finishes, continues to another random track endlessly. With
     * shuffle off it loops the single selected track.
     */
    fun startCasualMusic() {
        if (!musicEnabled) return
        if (shuffleMusic) {
            playTrack(randomTrackId(exclude = null), loopSingle = false)
        } else {
            playTrack(musicTrack, loopSingle = true)
        }
    }

    private fun randomTrackId(exclude: String?): String {
        val ids = tracks.map { it.first }
        return ids.filter { it != exclude }.randomOrNull() ?: ids.random()
    }

    private fun playTrack(id: String, loopSingle: Boolean) {
        stopMusic()
        nowPlayingId = id
        val vol = musicVolume
        music = MediaPlayer.create(context, resFor(id))?.apply {
            setVolume(vol, vol)
            if (loopSingle) {
                isLooping = true
            } else {
                isLooping = false
                // When this track ends, roll on to a different random one (shuffle loop).
                setOnCompletionListener {
                    if (musicEnabled) playTrack(randomTrackId(exclude = id), loopSingle = false)
                }
            }
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
        val vol = musicVolume
        preview = MediaPlayer.create(context, resFor(id))?.apply {
            setVolume(vol, vol)
            start()
        }
    }

    fun stopPreview() {
        preview?.let { runCatching { it.stop() }; it.release() }
        preview = null
    }
}
