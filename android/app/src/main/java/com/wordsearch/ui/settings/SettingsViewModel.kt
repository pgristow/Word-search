package com.wordsearch.ui.settings

import androidx.lifecycle.ViewModel
import com.wordsearch.data.audio.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sound: SoundManager
) : ViewModel() {

    private val _sfx = MutableStateFlow(sound.sfxEnabled)
    val sfx = _sfx.asStateFlow()
    private val _music = MutableStateFlow(sound.musicEnabled)
    val music = _music.asStateFlow()
    private val _track = MutableStateFlow(sound.musicTrack)
    val track = _track.asStateFlow()

    /** (id, displayName) for each selectable track. */
    val tracks: List<Pair<String, String>> = sound.tracks.map { it.first to it.second }

    fun setSfx(v: Boolean) {
        sound.sfxEnabled = v; _sfx.value = v
        if (v) sound.playCorrect()
    }

    fun setMusic(v: Boolean) {
        sound.musicEnabled = v; _music.value = v
        if (!v) sound.stopPreview()
    }

    fun selectTrack(id: String) {
        sound.musicTrack = id; _track.value = id
        sound.previewTrack(id)
    }

    fun preview(id: String) = sound.previewTrack(id)

    override fun onCleared() {
        sound.stopPreview()
        super.onCleared()
    }
}
