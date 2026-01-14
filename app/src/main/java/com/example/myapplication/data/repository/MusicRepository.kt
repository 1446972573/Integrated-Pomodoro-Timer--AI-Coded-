package com.example.myapplication.data.repository

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.database.entity.PlaylistEntity
import com.example.myapplication.data.database.entity.PlaylistSongCrossRef
import com.example.myapplication.data.database.entity.SongEntity
import com.example.myapplication.data.database.relation.PlaylistWithSongs
import com.example.myapplication.data.model.Song
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MusicRepository(private val context: Context) {

    private val playlistDao = AppDatabase.getDatabase(context).playlistDao()

    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>> = playlistDao.getAllPlaylistsWithSongs()

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs> = playlistDao.getPlaylistWithSongs(playlistId)

    fun getAllCrossRefs(): Flow<List<PlaylistSongCrossRef>> = playlistDao.getAllCrossRefs()

    suspend fun getCrossRef(playlistId: Long, songId: Long): PlaylistSongCrossRef? = playlistDao.getCrossRef(playlistId, songId)

    fun getBuiltInSongs(): List<Song> {
        return listOf(
            Song(id = -1, title = "Jazz In Paris", artist = "Media Right Productions", duration = 103000, contentUri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3".toUri()),
            Song(id = -2, title = "The Messenger", artist = "Silent Partner", duration = 130000, contentUri = "https://storage.googleapis.com/exoplayer-test-media-1/the-messenger.mp3".toUri()),
            Song(id = -3, title = "Talkies", artist = "Huma-Huma", duration = 100000, contentUri = "https://storage.googleapis.com/exoplayer-test-media-0/Talkies.mp3".toUri())
        )
    }

    suspend fun createPlaylist(name: String) {
        val playlistEntity = PlaylistEntity(name = name)
        playlistDao.insertPlaylist(playlistEntity)
    }

    suspend fun updatePlaylists(playlists: List<PlaylistEntity>) {
        playlistDao.updatePlaylists(playlists)
    }

    suspend fun updateCrossRefs(crossRefs: List<PlaylistSongCrossRef>) {
        playlistDao.updateCrossRefs(crossRefs)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        logDeletedPlaylists(listOf(playlist))
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun deletePlaylistsByIds(playlistIds: List<Long>) {
        val playlistsToDelete = playlistDao.getPlaylistsByIds(playlistIds)
        logDeletedPlaylists(playlistsToDelete)
        playlistDao.deletePlaylistsByIds(playlistIds)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        songs.forEachIndexed { index, song ->
            val songEntity = SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = "",
                duration = song.duration,
                uri = song.contentUri.toString()
            )
            playlistDao.insertSong(songEntity)
            val crossRef = PlaylistSongCrossRef(playlistId = playlistId, songId = song.id, displayOrder = index)
            playlistDao.addSongToPlaylist(crossRef)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val crossRef = PlaylistSongCrossRef(playlistId = playlistId, songId = songId, displayOrder = 0)
        playlistDao.removeSongFromPlaylist(crossRef)
    }

    private fun logDeletedPlaylists(playlists: List<PlaylistEntity>) {
        val logFile = File(context.filesDir, "deleted_playlists_log.txt")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        logFile.appendText("\n--- Deleted Playlists on $timestamp ---\n")
        playlists.forEach { 
            logFile.appendText("ID: ${it.playlistId}, Name: ${it.name}\n")
        }
    }

    fun getLocalSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        // ... (rest of the function remains the same)
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "${MediaStore.Audio.Media.DURATION} >= ? AND " +
                "${MediaStore.Audio.Media.DURATION} <= ?"
        val selectionArgs = arrayOf(
            TimeUnit.SECONDS.toMillis(30).toString(),
            TimeUnit.HOURS.toMillis(1).toString()
        )

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val duration = it.getLong(durationColumn)
                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString())
                    .build()

                songs.add(Song(id, title, artist, duration, contentUri))
            }
        }

        return songs
    }
}
