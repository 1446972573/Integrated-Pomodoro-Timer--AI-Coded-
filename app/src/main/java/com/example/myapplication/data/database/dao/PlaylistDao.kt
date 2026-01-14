package com.example.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myapplication.data.database.entity.PlaylistEntity
import com.example.myapplication.data.database.entity.PlaylistSongCrossRef
import com.example.myapplication.data.database.entity.SongEntity
import com.example.myapplication.data.database.relation.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylists(playlists: List<PlaylistEntity>)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE playlistId IN (:playlistIds)")
    suspend fun deletePlaylistsByIds(playlistIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)
    
    @Update
    suspend fun updateCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Delete
    suspend fun removeSongFromPlaylist(crossRef: PlaylistSongCrossRef)

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY displayOrder ASC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Query("SELECT * FROM playlistsongcrossref")
    fun getAllCrossRefs(): Flow<List<PlaylistSongCrossRef>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs>

    @Query("SELECT * FROM playlists WHERE playlistId IN (:playlistIds)")
    suspend fun getPlaylistsByIds(playlistIds: List<Long>): List<PlaylistEntity>

    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM playlistsongcrossref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun getCrossRef(playlistId: Long, songId: Long): PlaylistSongCrossRef?
}
