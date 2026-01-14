package com.example.myapplication.data.model

/**
 * Represents a playlist of songs.
 *
 * @param id The unique identifier for the playlist.
 * @param name The name of the playlist.
 * @param songs The list of songs in this playlist.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song>
)
