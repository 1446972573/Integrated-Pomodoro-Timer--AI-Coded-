package com.example.myapplication.data.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.myapplication.data.database.entity.PlaylistEntity
import com.example.myapplication.data.database.entity.PlaylistSongCrossRef
import com.example.myapplication.data.database.entity.SongEntity

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)
