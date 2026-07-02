package com.craftworks.music.providers.local

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.core.net.toUri
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaModel

object LocalNormalizer {
    fun cursorToAlbum(context: Context, cursor: Cursor, idIdx: Int, nameIdx: Int, artistIdx: Int): MediaModel.Album {

        val albumId = cursor.getLong(idIdx)
        val albumName = cursor.getString(nameIdx) ?: "Unknown"
        val artistName = cursor.getString(artistIdx) ?: "Unknown"

        val artworkUri = "${LocalMediaProvider.ALBUM_ART_PATH}/$albumId".toUri().let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.close()
                uri
            } catch (e: Exception) {
                "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
            }
        }

        val album = MediaModel.Album(
            albumArtistName = artistName,
            imageUrl = artworkUri.toString(),
            name = albumName
        )
        album.id = albumId.toString()

        return album
    }
    fun cursorToAlbums(context: Context, cursor: Cursor): List<MediaModel.Album> {
        val albums = mutableListOf<MediaModel.Album>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)

            while (it.moveToNext()) {
                albums.add(cursorToAlbum(context, cursor, idIdx, nameIdx, artistIdx))
            }
        }
        return albums
    }
}