package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.craftworks.music.data.model.Genre
import com.craftworks.music.data.model.MediaData

@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["artistId"]),
        Index(value = ["starred"]),
        Index(value = ["created"]),
        Index(value = ["name"]),
        Index(value = ["year"])
    ]
)
data class AlbumEntity(
    @PrimaryKey
    val navidromeID: String,
    val parent: String?,
    val album: String?,
    val title: String?,
    val name: String?,
    val coverArt: String?,
    val songCount: Int,
    val played: String?,
    val created: String?,
    val duration: Int?,
    val playCount: Int?,
    val artistId: String?,
    val artist: String,
    val year: Int?,
    val genre: String?,
    val genresJson: String?,
    val starred: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

fun AlbumEntity.toMediaDataAlbum(): MediaData.Album {
    val genres = try {
        genresJson?.let { kotlinx.serialization.json.Json.decodeFromString<List<Genre>>(it) }
    } catch (e: Exception) {
        null
    }

    return MediaData.Album(
        navidromeID = navidromeID,
        parent = parent,
        album = album,
        title = title,
        name = name,
        coverArt = coverArt,
        songCount = songCount,
        played = played,
        created = created,
        duration = duration,
        playCount = playCount,
        artistId = artistId,
        artist = artist,
        year = year,
        genre = genre,
        genres = genres,
        starred = starred,
        songs = null
    )
}

fun MediaData.Album.toEntity(): AlbumEntity {
    val genresJson = try {
        genres?.let { kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Genre.serializer()), it) }
    } catch (e: Exception) {
        null
    }

    return AlbumEntity(
        navidromeID = navidromeID,
        parent = parent,
        album = album,
        title = title,
        name = name,
        coverArt = coverArt,
        songCount = songCount,
        played = played,
        created = created,
        duration = duration,
        playCount = playCount,
        artistId = artistId,
        artist = artist,
        year = year,
        genre = genre,
        genresJson = genresJson,
        starred = starred
    )
}
