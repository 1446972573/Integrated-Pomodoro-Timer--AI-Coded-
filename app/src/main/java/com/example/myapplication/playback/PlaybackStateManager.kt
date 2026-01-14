package com.example.myapplication.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton object to manage the global playback state, such as the currently active playlist.
 */
object PlaybackStateManager {
    private val _activePlaylistId = MutableStateFlow<Long?>(null)
    val activePlaylistId = _activePlaylistId.asStateFlow()

    fun setActivePlaylist(playlistId: Long) {
        _activePlaylistId.value = playlistId
    }
}
