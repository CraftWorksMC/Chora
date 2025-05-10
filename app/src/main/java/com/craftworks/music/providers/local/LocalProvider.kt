package com.craftworks.music.providers.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.R
import com.craftworks.music.data.Genre
import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.LocalProviderManager

class LocalProvider {
    private var applicationContext: Context? = null

    companion object {
        private const val TAG = "LOCAL PROVIDER"

        private var instance: LocalProvider? = null

        fun getInstance(): LocalProvider {
            return instance ?: synchronized(this) {
                instance ?: LocalProvider().also { instance = it }
            }
        }
    }

    fun init(context: Context) {
        applicationContext = context.applicationContext

        scanLocalFiles()
    }

    fun scanLocalFiles() {
        MediaScannerConnection.scanFile(
            applicationContext, arrayOf(Environment.getExternalStorageDirectory().path), null
        ) { _, _ -> Log.i("Scan For Files", "Media Scan Completed") }
    }

    //region Albums
    fun getLocalAlbums(sort: String?): List<MediaItem> {
        Log.d(TAG, "Getting All Albums!")
        val albums = mutableSetOf<MediaItem>()

        val sortOrder = when (sort) {
            "alphabeticalByName" -> "${MediaStore.Audio.Albums.ALBUM} DESC"
            "recent" -> "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            "newest" -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            "random" -> "RANDOM()"
            else -> {
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            }
        }

        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
            )

            val folders = LocalProviderManager.getAllFolders()
            val selectionBuilder = StringBuilder("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            val selectionArgs = mutableListOf<String>()

            if (folders.isNotEmpty()) {
                selectionBuilder.append(" AND (")
                folders.forEachIndexed { index, folder ->
                    if (index > 0) selectionBuilder.append(" OR ")
                    selectionBuilder.append("${MediaStore.Audio.Media.DATA} LIKE ?")
                    selectionArgs.add("%$folder%")
                }
                selectionBuilder.append(")")
            }

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                selectionBuilder.toString(),
                selectionArgs.toTypedArray(),
                sortOrder
            )

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val albumIdColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)

                    do {
                        val thisId = cursor.getLongOrNull(idColumn) ?: 0
                        val thisAlbumId = cursor.getLongOrNull(albumIdColumn) ?: 0
                        val thisAlbum = cursor.getStringOrNull(albumColumn) ?: "Unknown"
                        val thisArtist = cursor.getStringOrNull(artistColumn) ?: "Unknown"

                        var imageUri = "content://media/external/audio/media/$thisAlbumId/albumart".toUri()

                        try {
                            context.contentResolver.openInputStream(imageUri)?.close()
                        }
                        catch(_: Exception) {
                            imageUri = ("android.resource://com.craftworks.music/" + R.drawable.albumplaceholder).toUri()
                        }

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(thisAlbum)
                            .setArtist(thisArtist)
                            .setAlbumTitle(thisAlbum)
                            .setAlbumArtist(thisArtist)
                            .setArtworkUri(imageUri)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                            .setExtras(
                                Bundle().apply {
                                    putString("navidromeID", "Local_$thisId")
                                }
                            )
                            .build()

                        val album = MediaItem.Builder()
                            .setMediaId(
                                "Local_$thisId"
                            )
                            .setUri("Local_$thisId")
                            .setMediaMetadata(mediaMetadata)
                            .build()

                        albums.add(album)
                    } while (cursor.moveToNext())
                }
            }
        }

        return albums.toList()
    }

    fun getLocalAlbum(albumId: String): List<MediaItem>? {
        val numericId = try {
            albumId.removePrefix("Local_").toLong()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Invalid album ID format: $albumId", e)
            return null
        }

        Log.d(TAG, "Getting album data for id $numericId!")

        val albumWithSongs = mutableListOf<MediaItem>()

        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(numericId.toString()),
                null
            )

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val albumIdColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                    val thisId = cursor.getLongOrNull(idColumn) ?: 0
                    val thisAlbumId = cursor.getLong(albumIdColumn)
                    val thisAlbum = cursor.getStringOrNull(albumColumn) ?: "Unknown"
                    val thisArtist = cursor.getStringOrNull(artistColumn) ?: "Unknown"
                    val thisYear = cursor.getIntOrNull(yearColumn) ?: 0

                    var imageUri = "content://media/external/audio/media/$thisAlbumId/albumart".toUri()

                    try {
                        context.contentResolver.openInputStream(imageUri)?.close()
                    }
                    catch(_: Exception) {
                        imageUri = ("android.resource://com.craftworks.music/" + R.drawable.albumplaceholder).toUri()
                    }


                    val songs = getLocalAlbumSongs(thisAlbumId)
                    val totalDuration = songs.sumOf { it.mediaMetadata.durationMs ?: 0L }
                    val genre = songs.firstOrNull()?.mediaMetadata?.genre

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(thisAlbum)
                        .setArtist(thisArtist)
                        .setAlbumTitle(thisAlbum)
                        .setAlbumArtist(thisArtist)
                        .setArtworkUri(imageUri)
                        .setRecordingYear(thisYear)
                        .setGenre(genre)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setDurationMs(totalDuration)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                        .setExtras(
                            Bundle().apply {
                                putString("navidromeID", "Local_$thisId")
                            }
                        )
                        .build()

                    albumWithSongs.add(
                        MediaItem.Builder()
                            .setMediaId("$thisId")
                            .setUri("$thisId")
                            .setMediaMetadata(mediaMetadata)
                            .build()
                    )

                    albumWithSongs.addAll(songs)
                    Log.d(TAG, "Got album data with ${songs.size} songs")
                }
            }
        }

        return albumWithSongs
    }

    private fun getLocalAlbumSongs(albumId: Long): List<MediaItem> {
        Log.d(TAG, "Getting Songs for album id: $albumId")

        val songs = mutableListOf<MediaItem>()

        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.BITRATE,
                MediaStore.Audio.Media.GENRE
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media.ALBUM_ID} = ?",
                arrayOf(albumId.toString()),
                "${MediaStore.Audio.Media.TRACK} ASC"
            )

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val albumIdColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val pathColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val titleColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val artistIdColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                    val formatColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val dateAddedColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                    val trackIndex: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                    val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    val durationColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val bitrateColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                    val genreColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

                    do {
                        val thisId = cursor.getLong(idColumn)
                        val thisAlbumId = cursor.getLongOrNull(albumIdColumn) ?: 0
                        val thisPath = cursor.getString(pathColumn)
                        val thisTitle = cursor.getString(titleColumn)
                        val thisAlbum = cursor.getStringOrNull(albumColumn) ?: "Unknown"
                        val thisArtist = cursor.getStringOrNull(artistColumn) ?: "Unknown"
                        val thisArtistId = cursor.getStringOrNull(artistIdColumn) ?: "Unknown"
                        val thisFormat = cursor.getString(formatColumn)
                        val thisDateAdded = cursor.getString(dateAddedColumn) ?: ""
                        val thisTrack = cursor.getIntOrNull(trackIndex) ?: 0
                        val thisYear = cursor.getIntOrNull(yearColumn) ?: 0
                        val thisDuration = cursor.getIntOrNull(durationColumn) ?: 0
                        val thisBitrate = cursor.getIntOrNull(bitrateColumn) ?: 0
                        val thisGenre = cursor.getStringOrNull(genreColumn) ?: ""

                        val contentUri: Uri =
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                thisId
                            )

                        var imageUri = ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(),
                            thisAlbumId
                        )

                        try {
                            context.contentResolver.openInputStream(imageUri)?.close()
                        }
                        catch(_: Exception) {
                            imageUri = ("android.resource://com.craftworks.music/" + R.drawable.albumplaceholder).toUri()
                        }

                        Log.d(TAG, "Added song: $thisTitle")

                        val genres = mutableListOf<Genre>()
                        thisGenre.split(",").forEach { genreName ->
                            if (genreName.isNotBlank()) {
                                genres.add(Genre(genreName.trim()))
                            }
                        }

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(thisTitle)
                            .setArtist(thisArtist)
                            .setAlbumTitle(thisAlbum)
                            .setArtworkUri(imageUri)
                            .setRecordingYear(thisYear)
                            .setGenre(thisGenre)
                            .setIsBrowsable(false).setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .setTrackNumber(thisTrack)
                            .setDurationMs(thisDuration.toLong())
                            .setExtras(Bundle().apply {
                                putString("navidromeID", "Local_$thisId")
                                putString("format", thisFormat.drop(6))
                                putLong("bitrate", thisBitrate.toLong() / 1000)
                                putBoolean("isRadio", false)
                            }).build()

                        songs.add(
                            MediaItem.Builder()
                                .setMediaId(contentUri.toString())
                                .setUri(contentUri.toString())
                                .setMediaMetadata(mediaMetadata)
                                .build()
                        )
                    } while (cursor.moveToNext())
                }
            }
        }

        return songs
    }
    //endregion

    fun getLocalSongs(): List<MediaItem> {
        val songs = mutableListOf<MediaItem>()
        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.BITRATE,
                MediaStore.Audio.Media.GENRE
            )

            val folders = LocalProviderManager.getAllFolders()
            val selectionBuilder = StringBuilder()
            val selectionArgs = mutableListOf<String>()

            if (folders.isNotEmpty()) {
                selectionBuilder.append("(")
                folders.forEachIndexed { index, folder ->
                    if (index > 0) selectionBuilder.append(" OR ")
                    selectionBuilder.append("${MediaStore.Audio.Media.DATA} LIKE ?")
                    selectionArgs.add("%$folder%")
                }
                selectionBuilder.append(")")
            } else {
                return emptyList()
            }

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                selectionBuilder.toString(),
                selectionArgs.toTypedArray(),
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val albumIdColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val pathColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val titleColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val artistIdColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                    val formatColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val dateAddedColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                    val trackIndex: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                    val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    val durationColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val bitrateColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                    val genreColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

                    do {
                        val thisId = cursor.getLong(idColumn)
                        val thisAlbumId = cursor.getLongOrNull(albumIdColumn) ?: 0
                        val thisPath = cursor.getString(pathColumn)
                        val thisTitle = cursor.getString(titleColumn)
                        val thisAlbum = cursor.getStringOrNull(albumColumn) ?: "Unknown"
                        val thisArtist = cursor.getStringOrNull(artistColumn) ?: "Unknown"
                        val thisArtistId = cursor.getStringOrNull(artistIdColumn) ?: "Unknown"
                        val thisFormat = cursor.getString(formatColumn)
                        val thisDateAdded = cursor.getString(dateAddedColumn) ?: ""
                        val thisTrack = cursor.getIntOrNull(trackIndex) ?: 0
                        val thisYear = cursor.getIntOrNull(yearColumn) ?: 0
                        val thisDuration = cursor.getIntOrNull(durationColumn) ?: 0
                        val thisBitrate = cursor.getIntOrNull(bitrateColumn) ?: 0
                        val thisGenre = cursor.getStringOrNull(genreColumn) ?: ""

                        val contentUri: Uri =
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                thisId
                            )

                        var imageUri = ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(),
                            thisAlbumId
                        )

                        try {
                            context.contentResolver.openInputStream(imageUri)?.close()
                        }
                        catch(_: Exception) {
                            imageUri = ("android.resource://com.craftworks.music/" + R.drawable.albumplaceholder).toUri()
                        }


                        Log.d(TAG, "Added song to all songs list: $thisTitle, released: $thisYear")

                        songs.add(
                            MediaItem.fromUri(contentUri.toString()).buildUpon().apply {
                                setMediaId(contentUri.toString())
                                setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(thisTitle)
                                        .setAlbumTitle(thisAlbum)
                                        .setArtist(thisArtist)
                                        .setArtworkUri(imageUri)
                                        .setIsPlayable(true)
                                        .setTrackNumber(thisTrack)
                                        .setRecordingYear(thisYear)
                                        .setDurationMs(thisDuration.toLong())
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .setExtras(
                                            Bundle().apply {
                                                putString("navidromeID", "Local_$thisId")
                                                putString("format", thisFormat.drop(6))
                                                putInt("bitrate", thisBitrate / 1000)
                                            }
                                        )
                                        .build()
                                )
                            }.build()
                        )
                    } while (cursor.moveToNext())
                }
            }
        }
        return songs
    }

    fun getLocalArtists(): List<MediaData.Artist> {
        val artists = mutableListOf<MediaData.Artist>()
        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
            )

            val folders = LocalProviderManager.getAllFolders()
            val selectionBuilder = StringBuilder()
            val selectionArgs = mutableListOf<String>()

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST
            )

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
                    val nameColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)

                    do {
                        val thisId = cursor.getLong(idColumn)
                        val thisName = cursor.getString(nameColumn) ?: "Unknown"

                        Log.d(TAG, "Added artist: $thisName")

                        artists.add(
                            MediaData.Artist(
                                navidromeID = "Local_$thisId",
                                name = thisName,
                                description = "",
                                artistImageUrl = null
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        }

        // Filter artists based on the songs we have in our folders
        return if (LocalProviderManager.getAllFolders().isNotEmpty()) {
            val songs = getLocalSongs()
            val artistIds = songs.mapNotNull {
                it.mediaMetadata.extras?.getString("artistId")
            }.toSet()

            artists.filter { artist ->
                artistIds.contains(artist.navidromeID.removePrefix("Local_"))
            }
        } else {
            artists
        }
    }
}