package com.example.myapplication.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

sealed class MusicServiceErrorEvent {
    data class PlaybackError(val mediaId: String) : MusicServiceErrorEvent()
}

@UnstableApi // For MediaSessionService
class MusicService : MediaSessionService(), Player.Listener {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var bufferingTimeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        val errorEvents = MutableSharedFlow<MusicServiceErrorEvent>()
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        player = ExoPlayer.Builder(this).build()
        player.addListener(this)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Service",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Service")
            .setContentText("Running...")
            .setSmallIcon(R.mipmap.ic_launcher) // This is mandatory
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onPlayerError(error: PlaybackException) {
        player.currentMediaItem?.mediaId?.let {
            serviceScope.launch {
                errorEvents.emit(MusicServiceErrorEvent.PlaybackError(it))
            }
        }
        skipToNext()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                bufferingTimeoutJob?.cancel()
                bufferingTimeoutJob = serviceScope.launch {
                    delay(15_000) // 15 seconds timeout
                    player.currentMediaItem?.mediaId?.let {
                         errorEvents.emit(MusicServiceErrorEvent.PlaybackError(it))
                    }
                    skipToNext()
                }
            }
            Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> {
                bufferingTimeoutJob?.cancel()
            }
        }
    }
    
    private fun skipToNext(){
        if(player.hasNextMediaItem()){
            player.seekToNextMediaItem()
            player.play()
        } else {
            // Handle case where there are no more valid items to play
            player.stop()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(this@MusicService)
            player.release()
            release()
            mediaSession = null
        }
        bufferingTimeoutJob?.cancel()
        super.onDestroy()
    }
}
