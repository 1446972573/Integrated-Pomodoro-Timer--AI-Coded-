package com.example.myapplication.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.entity.PlaylistEntity
import com.example.myapplication.data.database.entity.PlaylistSongCrossRef
import com.example.myapplication.data.database.relation.PlaylistWithSongs
import com.example.myapplication.data.model.Song
import com.example.myapplication.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PlaylistDialogState {
    object Hidden : PlaylistDialogState()
    object ShowCreate : PlaylistDialogState()
    data class ShowRename(val playlist: PlaylistEntity) : PlaylistDialogState()
    data class ShowDeleteConfirm(val playlist: PlaylistEntity) : PlaylistDialogState()
    data class ShowAddSongs(val playlistId: Long) : PlaylistDialogState()
}

data class PlaylistUiState(
    val playlists: List<PlaylistWithSongs> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val isEditMode: Boolean = false,
    val isInMultiSelectMode: Boolean = false,
    val selectedPlaylistIds: Set<Long> = emptySet(),
    val dialogState: PlaylistDialogState = PlaylistDialogState.Hidden,
    val expandedPlaylistId: Long? = null
)

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepository = MusicRepository(application)

    private val _playlists = musicRepository.getAllPlaylistsWithSongs()
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isEditMode = MutableStateFlow(false)
    private val _isInMultiSelectMode = MutableStateFlow(false)
    private val _selectedPlaylistIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _dialogState = MutableStateFlow<PlaylistDialogState>(PlaylistDialogState.Hidden)
    private val _expandedPlaylistId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PlaylistUiState> = combine(
        _playlists, _localSongs, _isEditMode, _isInMultiSelectMode,
        _selectedPlaylistIds, _dialogState, _expandedPlaylistId
    ) {
        @Suppress("UNCHECKED_CAST")
        PlaylistUiState(
            playlists = it[0] as List<PlaylistWithSongs>,
            localSongs = it[1] as List<Song>,
            isEditMode = it[2] as Boolean,
            isInMultiSelectMode = it[3] as Boolean,
            selectedPlaylistIds = it[4] as Set<Long>,
            dialogState = it[5] as PlaylistDialogState,
            expandedPlaylistId = it[6] as Long?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState()
    )

    init {
        loadLocalSongs()
    }

    private fun loadLocalSongs() {
        viewModelScope.launch {
            _localSongs.value = musicRepository.getLocalSongs()
        }
    }

    fun onEditModeChange(isEditing: Boolean) {
        _isEditMode.value = isEditing
        if (!isEditing) {
            _isInMultiSelectMode.value = false
            _selectedPlaylistIds.value = emptySet()
        }
    }

    fun toggleMultiSelectMode() {
        _isInMultiSelectMode.value = !_isInMultiSelectMode.value
        _selectedPlaylistIds.value = emptySet()
    }

    fun togglePlaylistSelection(playlistId: Long) {
        val currentSelection = _selectedPlaylistIds.value.toMutableSet()
        if (currentSelection.contains(playlistId)) {
            currentSelection.remove(playlistId)
        } else {
            currentSelection.add(playlistId)
        }
        _selectedPlaylistIds.value = currentSelection
    }

    fun deleteSelectedPlaylists() {
        viewModelScope.launch {
            musicRepository.deletePlaylistsByIds(_selectedPlaylistIds.value.toList())
            toggleMultiSelectMode()
        }
    }

    fun onDialogStateChange(newState: PlaylistDialogState) {
        _dialogState.value = newState
    }

    fun onPlaylistExpanded(playlistId: Long) {
        _expandedPlaylistId.value = if (_expandedPlaylistId.value == playlistId) null else playlistId
    }

    fun onPlaylistMoved(from: Int, to: Int) {
        viewModelScope.launch {
            val currentPlaylists = uiState.value.playlists.toMutableList()
            val movedItem = currentPlaylists.removeAt(from)
            currentPlaylists.add(to, movedItem)
            val updatedPlaylists = currentPlaylists.mapIndexed { index, playlist -> playlist.playlist.copy(displayOrder = index) }
            musicRepository.updatePlaylists(updatedPlaylists)
        }
    }

    fun onSongMoved(playlistId: Long, from: Int, to: Int) {
        viewModelScope.launch {
            val playlistWithSongs = _playlists.first().find { it.playlist.playlistId == playlistId } ?: return@launch
            val songsInPlaylist = playlistWithSongs.songs.toMutableList()
            val movedSong = songsInPlaylist.removeAt(from)
            songsInPlaylist.add(to, movedSong)
            val updatedSongs = songsInPlaylist.mapIndexedNotNull { index, song ->
                musicRepository.getCrossRef(playlistId, song.id)?.copy(displayOrder = index)
            }
            musicRepository.updateCrossRefs(updatedSongs)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
            onDialogStateChange(PlaylistDialogState.Hidden)
        }
    }

    fun updatePlaylistName(playlist: PlaylistEntity, newName: String) {
        viewModelScope.launch {
            musicRepository.updatePlaylist(playlist.copy(name = newName))
            onDialogStateChange(PlaylistDialogState.Hidden)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlist)
            onDialogStateChange(PlaylistDialogState.Hidden)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            musicRepository.addSongsToPlaylist(playlistId, songs)
            onDialogStateChange(PlaylistDialogState.Hidden)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
