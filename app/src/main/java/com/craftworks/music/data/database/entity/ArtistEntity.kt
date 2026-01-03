package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.craftworks.music.data.model.MediaData

@Entity(
    tableName = "artists",
    indices = [
        Index(value = ["starred"]),
        Index(value = ["name"])
    ]
)
data class ArtistEntity(
    @PrimaryKey
    val navidromeID: String,
    val name: String,
    val artistImageUrl: String?,
    val albumCount: Int?,
    val description: String,
    val starred: String?,
    val musicBrainzId: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

fun ArtistEntity.toMediaDataArtist(): MediaData.Artist {
    return MediaData.Artist(
        navidromeID = navidromeID,
        name = name,
        artistImageUrl = artistImageUrl,
        albumCount = albumCount,
        description = description,
        starred = starred,
        musicBrainzId = musicBrainzId,
        similarArtist = null,
        album = null
    )
}

fun MediaData.Artist.toEntity(): ArtistEntity {
    return ArtistEntity(
        navidromeID = navidromeID,
        name = name,
        artistImageUrl = artistImageUrl,
        albumCount = albumCount,
        description = description,
        starred = starred,
        musicBrainzId = musicBrainzId
    )
}
