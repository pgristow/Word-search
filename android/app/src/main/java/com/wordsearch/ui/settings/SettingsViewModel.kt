package com.wordsearch.ui.settings

import androidx.lifecycle.ViewModel
import com.wordsearch.data.audio.SoundManager
import com.wordsearch.data.local.ModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sound: SoundManager,
    private val modeStore: ModeStore
) : ViewModel() {

    private val _hazards = MutableStateFlow(modeStore.casualHazards)
    val hazards = _hazards.asStateFlow()
    fun setHazards(v: Boolean) { modeStore.casualHazards = v; _hazards.value = v }

    private val _sfx = MutableStateFlow(sound.sfxEnabled)
    val sfx = _sfx.asStateFlow()
    private val _music = MutableStateFlow(sound.musicEnabled)
    val music = _music.asStateFlow()
    private val _track = MutableStateFlow(sound.musicTrack)
    val track = _track.asStateFlow()
    private val _shuffle = MutableStateFlow(sound.shuffleMusic)
    val shuffle = _shuffle.asStateFlow()
    private val _volume = MutableStateFlow(sound.musicVolume)
    val volume = _volume.asStateFlow()

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

    fun setShuffle(v: Boolean) {
        sound.shuffleMusic = v; _shuffle.value = v
    }

    /** Live-update volume while dragging; previews the current track so the change is audible. */
    fun setVolume(v: Float) {
        sound.musicVolume = v; _volume.value = v
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
