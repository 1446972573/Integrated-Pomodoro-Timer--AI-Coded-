package com.example.myapplication.data.model

import android.net.Uri

/**
 * Represents a single song in the music library.
 *
 * @param id The unique identifier for the song from MediaStore.
 * @param title The title of the song.
 * @param artist The artist of the song.
 * @param duration The duration of the song in milliseconds.
 * @param contentUri The URI to access the song's content.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val contentUri: Uri
)
