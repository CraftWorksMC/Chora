package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.craftworks.music.data.model.Artists
import com.craftworks.music.data.model.Genre
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.ReplayGain

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["starred"]),
        Index(value = ["lastPlayed"]),
        Index(value = ["dateAdded"]),
        Index(value = ["timesPlayed"]),
        Index(value = ["title"]),
        Index(value = ["discNumber", "track"])
    ]
)
data class SongEntity(
    @PrimaryKey
    val navidromeID: String,
    val parent: String,
    val title: String,
    val album: String,
    val artist: String,
    val artistsJson: String, // JSON serialized List<Artists>
    val track: Int?,
    val year: Int?,
    val genre: String?,
    val imageUrl: String,
    val size: Int?,
    val contentType: String?,
    val format: String,
    val duration: Int,
    val bitrate: Int?,
    val path: String,
    val timesPlayed: Int?,
    val discNumber: Int?,
    val dateAdded: String,
    val albumId: String,
    val artistId: String?,
    val lastPlayed: String?,
    val bpm: Int,
    val genresJson: String?, // JSON serialized List<Genre>
    val trackGain: Float?,
    val channelCount: Int?,
    val samplingRate: Int?,
    val media: String?,
    val starred: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

fun SongEntity.toMediaDataSong(): MediaData.Song {
    val artists = try {
        kotlinx.serialization.json.Json.decodeFromString<List<Artists>>(artistsJson)
    } catch (e: Exception) {
        listOf(Artists("", artist))
    }

    val genres = try {
        genresJson?.let { kotlinx.serialization.json.Json.decodeFromString<List<Genre>>(it) }
    } catch (e: Exception) {
        null
    }

    return MediaData.Song(
        navidromeID = navidromeID,
        parent = parent,
        title = title,
        album = album,
        artist = artist,
        artists = artists,
        track = track,
        year = year,
        genre = genre,
        imageUrl = imageUrl,
        size = size,
        contentType = contentType,
        format = format,
        duration = duration,
        bitrate = bitrate,
        path = path,
        timesPlayed = timesPlayed,
        discNumber = discNumber,
        dateAdded = dateAdded,
        albumId = albumId,
        artistId = artistId,
        lastPlayed = lastPlayed,
        bpm = bpm,
        genres = genres,
        replayGain = trackGain?.let { ReplayGain(it) },
        channelCount = channelCount,
        samplingRate = samplingRate,
        media = media,
        starred = starred
    )
}

fun MediaData.Song.toEntity(): SongEntity {
    val artistsJson = try {
        kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Artists.serializer()), artists)
    } catch (e: Exception) {
        "[]"
    }

    val genresJson = try {
        genres?.let { kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Genre.serializer()), it) }
    } catch (e: Exception) {
        null
    }

    return SongEntity(
        navidromeID = navidromeID,
        parent = parent,
        title = title,
        album = album,
        artist = artist,
        artistsJson = artistsJson,
        track = track,
        year = year,
        genre = genre,
        imageUrl = imageUrl,
        size = size,
        contentType = contentType,
        format = format,
        duration = duration,
        bitrate = bitrate,
        path = path,
        timesPlayed = timesPlayed,
        discNumber = discNumber,
        dateAdded = dateAdded,
        albumId = albumId,
        artistId = artistId,
        lastPlayed = lastPlayed,
        bpm = bpm,
        genresJson = genresJson,
        trackGain = replayGain?.trackGain,
        channelCount = channelCount,
        samplingRate = samplingRate,
        media = media,
        starred = starred
    )
}
