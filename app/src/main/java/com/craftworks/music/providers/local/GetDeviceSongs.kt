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
import com.craftworks.music.data.Artist
import com.craftworks.music.data.Song
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.songsList
import java.io.FileNotFoundException

fun getSongsOnDevice(context: Context){
    if (localProviderList.isEmpty()) return

    val contentResolver: ContentResolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("%${localProviderList[selectedLocalProvider.intValue].directory}%")
    val cursor: Cursor? = contentResolver.query(uri, null, selection, selectionArgs, null)

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
                val thisArtist = cursor.getString(artistColumn).split("~")[0]
                val thisTitle = cursor.getString(titleColumn)
                val thisDuration = cursor.getInt(durationColumn)
                val thisDateAdded = cursor.getString(dateAddedColumn)
                val thisYear = cursor.getInt(yearColumn)
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
                    artist = thisArtist.split("~")[0],
                    album = thisAlbum,
                    imageUrl = imageUri,
                    media = contentUri,
                    duration = thisDuration,
                    dateAdded = thisDateAdded,
                    year = thisYear.toString().replace("null", "0"),
                    format = thisFormat.uppercase().drop(6),
                    bitrate = if (!thisBitrate.isNullOrBlank()) (thisBitrate.toInt() / 1000).toString() else "",
                    navidromeID = "Local"
                )
                if (!songsList.contains(songsList.firstOrNull { it.title == song.title && it.artist == song.artist })) {
                    songsList.add(song);
                }

                // Add songs to album
                val album = Album(
                    name = thisAlbum,
                    artist = thisArtist,
                    year = thisYear.toString(),
                    coverArt = imageUri
                )
                if (!albumList.contains(albumList.firstOrNull { it.name == album.name && it.artist == album.artist })){
                    albumList.add(album)
                }

                // Add artists to ArtistList
                val artist = Artist(
                    name = thisArtist,
                    imageUri = Uri.EMPTY,
                    navidromeID = "Local",
                    description = "",
                    similarArtistsID = ""
                )
                if (!artistList.contains(artistList.firstOrNull { it.name == artist.name })){
                    artistList.add(artist)
                }

            } while (cursor.moveToNext())
        }
    }
    cursor?.close()
}