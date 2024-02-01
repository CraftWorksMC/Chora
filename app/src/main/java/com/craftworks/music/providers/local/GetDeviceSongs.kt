package com.craftworks.music.providers.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.Song
import com.craftworks.music.data.albumList
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.songsList
import java.io.FileNotFoundException

fun getSongsOnDevice(context: Context){
    val contentResolver: ContentResolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("%${localProviderList[selectedLocalProvider.intValue].directory}%")
    val cursor: Cursor? = contentResolver.query(uri, null, selection, selectionArgs, null)


    // Clear everything!
    //songsList.clear()
    //albumList.clear()
    radioList.clear()
    playlistList.clear()

    MediaScannerConnection.scanFile(
        context, arrayOf(Environment.getExternalStorageDirectory().path), null
    ) { _, _ -> Log.i("Scan For Files", "Media Scan Completed") }

    when {
        cursor == null -> {
            // query failed, handle error.
        }
        !cursor.moveToFirst() -> {
            // no media on the device
        }
        else -> {
            val idColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val dateAddedColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val durationColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val formatColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val bitrateColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            do {
                val thisId = cursor.getLong(idColumn)
                val thisArtist = cursor.getString(artistColumn)
                val thisTitle = cursor.getString(titleColumn)
                val thisDuration = cursor.getInt(durationColumn)
                val thisDateAdded = cursor.getString(dateAddedColumn)
                val thisYear = cursor.getString(yearColumn)
                val thisFormat = cursor.getString(formatColumn)
                val thisBitrate = cursor.getString(bitrateColumn)
                val thisAlbum = cursor.getString(albumColumn)

                val contentUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId)

                var imageUri: Uri = Uri.EMPTY
                try {
                    imageUri = Uri.parse("content://media/external/audio/media/$thisId/albumart")
                } catch (_: FileNotFoundException){
                    println("No Album Art!")
                }

                // Add Song
                val song = Song(
                    title = thisTitle,
                    artist = thisArtist,
                    album = thisAlbum,
                    imageUrl = imageUri,
                    media = contentUri,
                    duration = thisDuration,
                    dateAdded = thisDateAdded,
                    year = thisYear,
                    format = thisFormat.uppercase().drop(6),
                    bitrate = if (!thisBitrate.isNullOrBlank()) (thisBitrate.toInt() / 1000).toString() else ""
                )
                if (songsList.isEmpty() || !songsList.contains(songsList.firstOrNull { it.title == song.title && it.artist == song.artist })) {
                    songsList.add(song);
                }

                // Add songs to album
                val album = Album(
                    name = thisAlbum,
                    artist = thisArtist,
                    year = if (!thisYear.isNullOrBlank()) thisYear else "",
                    coverArt = imageUri
                )
                if (albumList.isEmpty() || !albumList.contains(albumList.firstOrNull { it.name == album.name && it.artist == album.artist })){
                    albumList.add(album)
                }

            } while (cursor.moveToNext())
        }
    }
    cursor?.close()
}