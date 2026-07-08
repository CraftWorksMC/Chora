package com.craftworks.music.providers.local

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaModel

object LocalNormalizer {
    fun cursorToAlbum(context: Context, providerId: String, cursor: Cursor, idIdx: Int, nameIdx: Int, artistIdx: Int): MediaModel.Album {

        val albumId = cursor.getLong(idIdx)
        val albumName = cursor.getString(nameIdx) ?: "Unknown"
        val artistName = cursor.getString(artistIdx) ?: "Unknown"

        val album = MediaModel.Album(
            albumArtistName = artistName,
            imageUrl = albumId.toString(),
            name = albumName
        ).apply {
            this.id = albumId.toString()
            this.providerId = providerId
        }

        return album
    }
    fun cursorToAlbums(context: Context, providerId: String, cursor: Cursor): List<MediaModel.Album> {
        val albums = mutableListOf<MediaModel.Album>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)

            while (it.moveToNext()) {
                albums.add(cursorToAlbum(context, providerId, it, idIdx, nameIdx, artistIdx))
            }
        }
        return albums
    }
}