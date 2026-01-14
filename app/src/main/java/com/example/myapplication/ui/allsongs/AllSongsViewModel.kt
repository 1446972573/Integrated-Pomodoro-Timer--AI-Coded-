package com.example.myapplication.ui.allsongs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Song
import com.example.myapplication.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AllSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

class AllSongsViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepository = MusicRepository(application)

    private val _uiState = MutableStateFlow(AllSongsUiState())
    val uiState: StateFlow<AllSongsUiState> = _uiState.asStateFlow()

    init {
        refreshSongs()
    }

    fun refreshSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = musicRepository.getLocalSongs()
            _uiState.value = AllSongsUiState(songs = songs, isLoading = false)
        }
    }
}
