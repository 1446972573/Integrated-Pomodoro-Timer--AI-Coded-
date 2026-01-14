package com.example.myapplication.ui.player

import android.app.Application
import android.content.ComponentName
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myapplication.data.database.entity.SongEntity
import com.example.myapplication.data.repository.MusicRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.playback.PlaybackStateManager
import com.example.myapplication.services.MusicService
import com.example.myapplication.services.MusicServiceErrorEvent
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentMediaItem: MediaItem? = null,
    val shuffleModeEnabled: Boolean = false,
    @Player.RepeatMode val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val failedSongIds = MutableStateFlow<Set<String>>(emptySet())

    private var mediaController: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val musicRepository = MusicRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateState() }
        override fun onPlaybackStateChanged(playbackState: Int) { updateState() }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updateState() }
        override fun onRepeatModeChanged(repeatMode: Int) { updateState() }
    }

    init {
        connectToService()
        viewModelScope.launch {
            while (true) {
                if (mediaController?.isPlaying == true) { updateState() }
                delay(1000)
            }
        }

        MusicService.errorEvents.onEach { event ->
            if (event is MusicServiceErrorEvent.PlaybackError) markSongAsFailed(event.mediaId)
        }.launchIn(viewModelScope)

        PlaybackStateManager.activePlaylistId.onEach { loadMusic() }.launchIn(viewModelScope)
        settingsRepository.isBuiltInMusicEnabled.onEach { loadMusic() }.launchIn(viewModelScope)
    }

    private fun markSongAsFailed(mediaId: String) {
        val currentFailedIds = failedSongIds.value.toMutableSet()
        currentFailedIds.add(mediaId)
        failedSongIds.value = currentFailedIds
        loadMusic()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), MusicService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.addListener(playerListener)
                loadMusic()
                updateState()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun loadMusic() {
        viewModelScope.launch {
            val finalSongList = mutableListOf<SongEntity>()

            if (settingsRepository.isBuiltInMusicEnabled.value) {
                finalSongList.addAll(musicRepository.getBuiltInSongs().map { 
                    SongEntity(it.id, it.title, it.artist, "", it.duration, it.contentUri.toString()) 
                })
            }

            val activePlaylistId = PlaybackStateManager.activePlaylistId.value
            val userSongs = if (activePlaylistId != null) {
                musicRepository.getPlaylistWithSongs(activePlaylistId).first()?.songs ?: emptyList()
            } else {
                musicRepository.getLocalSongs().map { 
                    SongEntity(it.id, it.title, it.artist, "", it.duration, it.contentUri.toString())
                }
            }
            finalSongList.addAll(userSongs)

            val validSongs = finalSongList.filter { !failedSongIds.value.contains(it.id.toString()) }
            val mediaItems = validSongs.map { 
                MediaItem.Builder()
                    .setUri(it.uri.toUri())
                    .setMediaId(it.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(it.title)
                            .setArtist(it.artist)
                            .build()
                    )
                    .build()
            }
            mediaController?.setMediaItems(mediaItems)
            mediaController?.prepare()
        }
    }

    private fun updateState() {
        mediaController?.let {
            _uiState.value = PlayerUiState(
                isPlaying = it.isPlaying,
                currentPosition = it.currentPosition,
                duration = it.duration.coerceAtLeast(0),
                currentMediaItem = it.currentMediaItem,
                shuffleModeEnabled = it.shuffleModeEnabled,
                repeatMode = it.repeatMode
            )
        }
    }

    fun playPause() {
        if (mediaController?.isPlaying == true) mediaController?.pause() else mediaController?.play()
    }

    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }
    
    fun toggleShuffleMode() {
        mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }
    }
}
