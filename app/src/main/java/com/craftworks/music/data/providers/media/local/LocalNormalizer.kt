package com.craftworks.music.data.providers.media.local

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import java.util.Locale.getDefault

object LocalNormalizer {
    fun cursorToAlbum(context: Context, providerId: String, cursor: Cursor, idIdx: Int, nameIdx: Int, artistIdx: Int, yearIdx: Int): MediaModel.Album {

        val albumId = cursor.getLong(idIdx)
        val albumName = cursor.getString(nameIdx) ?: "Unknown"
        val artistName = cursor.getString(artistIdx) ?: "Unknown"
        val year = cursor.getInt(yearIdx)

        val album = MediaModel.Album(
            id = albumId.toString(),
            providerId = providerId,
            providerType = ProviderType.LOCAL_FOLDER,
            albumArtistName = artistName,
            artists = listOf(MediaModel.Artist(
                id = artistName.hashCode().toString(),
                providerId = providerId,
                providerType = ProviderType.LOCAL_FOLDER,
                name = artistName
            )),
            imageUrl = albumId.toString(),
            name = albumName,
            releaseYear = year,
            originalYear = year
        )

        return album
    }
    fun cursorToAlbums(context: Context, providerId: String, cursor: Cursor): List<MediaModel.Album> {
        val albums = mutableListOf<MediaModel.Album>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val yearIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR)

            while (it.moveToNext()) {
                albums.add(cursorToAlbum(context, providerId, it, idIdx, nameIdx, artistIdx, yearIdx))
            }
        }
        return albums
    }

    fun cursorToSong(context: Context, providerId: String, cursor: Cursor, idIdx: Int, albumIdIdx: Int, pathIdx: Int, titleIdx: Int, albumIdx: Int, artistIdx: Int, dateAddedIdx: Int, trackIdx: Int, yearIdx: Int, durationIdx: Int, bitrateIdx: Int, formatIdx: Int, genreIdx: Int) : MediaModel.Song {

        val id = cursor.getLong(idIdx)
        val albumId = cursor.getLong(albumIdIdx)
        val path = cursor.getString(pathIdx)
        val title = cursor.getString(titleIdx)
        val album = cursor.getStringOrNull(albumIdx) ?: "Unknown"
        val artist = cursor.getStringOrNull(artistIdx) ?: "Unknown"
        val dateAdded = cursor.getString(dateAddedIdx) ?: ""
        val rawTrack = cursor.getIntOrNull(trackIdx) ?: 0
        val year = cursor.getIntOrNull(yearIdx) ?: 0
        val duration = cursor.getIntOrNull(durationIdx) ?: 0
        val bitrate = cursor.getIntOrNull(bitrateIdx) ?: 0
        val format = cursor.getStringOrNull(formatIdx) ?: ""
        val genre = cursor.getStringOrNull(genreIdx) ?: ""

        val track = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack
        val discNumber = if (rawTrack >= 1000) rawTrack / 1000 else 1

        return MediaModel.Song(
            id = id.toString(),
            providerId = providerId,
            providerType = ProviderType.LOCAL_FOLDER,
            album = album,
            albumId = albumId.toString(),
            artistName = artist,
            artists = listOf(MediaModel.Artist(
                id = artist.hashCode().toString(),
                providerId = providerId,
                providerType = ProviderType.LOCAL_FOLDER,
                name = artist
            )),
            bitRate = bitrate / 1000,
            format = format.substringAfter("/").uppercase(getDefault()).removePrefix("X-"),
            createdAt = dateAdded,
            discNumber = discNumber,
            durationMs = duration,
            genres = listOf(MediaModel.Genre(name = genre)),
            imageId = albumId.toString(),
            name = title,
            path = path,
            trackNumber = track,
            releaseYear = year
        )
    }
    fun cursorToSongs(context: Context, providerId: String, cursor: Cursor) : List<MediaModel.Song> {
        val songs = mutableListOf<MediaModel.Song>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val pathIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dateAddedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val trackIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val bitrateIdx = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            val formatIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val genreIdx = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

            while (it.moveToNext()) {
                songs.add(cursorToSong(context, providerId, it, idIdx, albumIdIdx, pathIdx, titleIdx, albumIdx, artistIdx, dateAddedIdx, trackIdx, yearIdx, durationIdx, bitrateIdx, formatIdx, genreIdx))
            }
        }
        return songs
    }
}