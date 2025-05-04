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
import com.craftworks.music.data.Genre
import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.LocalProviderManager
import java.io.FileNotFoundException

class LocalProvider private constructor() {
    private var applicationContext: Context? = null

    companion object {
        @Volatile
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
        Log.d("LOCAL PROVIDER", "Getting All Albums!")
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
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
            )
            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                        "${MediaStore.Audio.Media.DATA} LIKE ?" +
                        " OR ${MediaStore.Audio.Media.DATA} LIKE ?"
                            .repeat(LocalProviderManager.getAllFolders().size),
                LocalProviderManager.getAllFolders().map { "%$it%" }.toTypedArray(),
                sortOrder
            )

            when {
                cursor == null -> {
                    // query failed, handle error.
                }

                !cursor.moveToFirst() -> {
                    // no media on the device
                }

                else -> {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)

                    do {
                        val thisId = cursor.getLongOrNull(idColumn) ?: 0
                        val thisAlbum = cursor.getStringOrNull(albumColumn) ?: "Unknown"
                        val thisArtist = cursor.getStringOrNull(artistColumn) ?: "Unknown"

                        val imageUri: String = try {
                            "content://media/external/audio/media/$thisId/albumart"
                        } catch (_: FileNotFoundException) {
                            println("No Album Art!")
                        }.toString()

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(thisAlbum)
                            .setArtist(thisArtist)
                            .setAlbumTitle(thisAlbum)
                            .setAlbumArtist(thisAlbum)
                            .setArtworkUri(imageUri.toUri())
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

            cursor?.close()
        }

        return albums.toList()
    }

    fun getLocalAlbum(albumId: String): List<MediaItem>? {
        val numericId = try {
            albumId.removePrefix("Local_").toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        Log.d("LOCAL PROVIDER", "Getting album data for id $numericId!")

        var album = mutableListOf<MediaItem>()

        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ID,
                //MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Media.YEAR
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(numericId.toString()),
                null
            )

            when {
                cursor == null -> {
                    // query failed, handle error.
                }

                !cursor.moveToFirst() -> {
                    // no media on the device
                }

                else -> {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                    val albumIdColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                    val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                    do {
                        val thisId = cursor.getLongOrNull(idColumn) ?: 0
                        val thisAlbum = cursor.getString(albumColumn)
                        val thisArtist = cursor.getString(artistColumn)
                        val thisAlbumId = cursor.getInt(albumIdColumn)
                        val thisYear = cursor.getInt(yearColumn)

                        val imageUri: String = try {
                            "content://media/external/audio/media/$thisId/albumart"
                        } catch (_: FileNotFoundException) {
                            println("No Album Art!")
                        }.toString()

                        val songs = getLocalAlbumSongs(numericId)
                        //val songs = emptyList<MediaData.Song>()
                        val totalDuration = songs.sumOf { it.mediaMetadata.extras?.getLong("duration") ?: 0L }
                        val genre = songs.firstOrNull()?.mediaMetadata?.genre

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(thisAlbum)
                            .setArtist(thisArtist)
                            .setAlbumTitle(thisAlbum)
                            .setAlbumArtist(thisArtist)
                            .setArtworkUri(imageUri.toUri())
                            .setReleaseYear(thisYear)
                            .setGenre(genre)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                            .setExtras(
                                Bundle().apply {
                                    putString("navidromeID", "Local_$thisAlbumId")
                                    putInt("duration", totalDuration.toInt())
                                }
                            )
                            .build()

                        album.add(MediaItem.Builder()
                            .setMediaId("Local_$thisAlbumId")
                            .setUri("Local_$thisAlbumId")
                            .setMediaMetadata(mediaMetadata)
                            .build()
                        )

                        album.addAll(songs)
                        Log.d("LOCAL PROVIDER", "Got album data: $album")

                    } while (cursor.moveToNext())
                }
            }

            cursor?.close()
        }

        return album
    }

    private fun getLocalAlbumSongs(albumId: Long): List<MediaItem> {
        Log.d("LOCAL PROVIDER", "Getting Songs for album id: $albumId")

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
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(albumId.toString()),
                null
            )

            when {
                cursor == null -> {
                    // query failed, handle error.
                }

                !cursor.moveToFirst() -> {
                    // no media on the device
                }

                else -> {
                    val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
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

                        val imageUri: String = try {
                            "content://media/external/audio/media/$thisId/albumart"
                        } catch (_: FileNotFoundException) {
                            println("No Album Art!")
                        }.toString()

                        Log.d("LOCAL PROVIDER", "Added song: $thisTitle")

                        val genres = mutableListOf<Genre>()
                        thisGenre.split(",").forEach {
                            genres.add(Genre(it))
                        }

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(thisTitle)
                            .setArtist(thisArtist)
                            .setAlbumTitle(thisAlbum)
                            .setArtworkUri(imageUri.toUri())
                            .setReleaseYear(thisYear)
                            .setGenre(thisGenre.toString())
                            .setIsBrowsable(false).setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .setDurationMs(thisDuration.toLong())
                            .setExtras(Bundle().apply {
                                putString("navidromeID", "Local_$thisId")
                                putInt("duration", thisDuration)
                                putString("format", thisFormat)
                                putLong("bitrate", thisBitrate.toLong())
                                putBoolean("isRadio", false)
                            }).build()

                        songs.add(
                            MediaItem.Builder()
                            .setMediaId(thisId.toString())
                            .setUri(thisId.toString())
                            .setMediaMetadata(mediaMetadata)
                            .build()
                        )
                    }while (cursor.moveToNext())
                }
            }

            cursor?.close()
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

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media.DATA} LIKE ?" +
                        " OR ${MediaStore.Audio.Media.DATA} LIKE ?"
                            .repeat(LocalProviderManager.getAllFolders().size - 1),
                LocalProviderManager.getAllFolders().map {
                    "%$it%"
                }.toTypedArray(),
                "${MediaStore.Audio.Media.TITLE} DESC"
            )

            when {
                cursor == null -> {
                    // query failed, handle error.
                }

                !cursor.moveToFirst() -> {
                    // no media on the device
                }

                else -> {
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
                        val thisAlbumId = cursor.getStringOrNull(albumIdColumn) ?: ""
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

                        val imageUri: String = try {
                            "content://media/external/audio/media/$thisId/albumart"
                        } catch (_: FileNotFoundException) {
                            println("No Album Art!")
                        }.toString()

                        Log.d("LOCAL PROVIDER", "Added song to all songs list: $thisTitle")

                        val genres = mutableListOf<Genre>()
                        thisGenre.split(",").forEach {
                            genres.add(Genre(it))
                        }

                        songs.add(
                            MediaItem.fromUri(contentUri).buildUpon().apply {
                                setMediaId(contentUri.toString())
                                setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(thisTitle)
                                        .setAlbumTitle(thisAlbum)
                                        .setArtist(thisArtist)
                                        .setArtworkUri(imageUri.toUri())
                                        .setIsPlayable(true)
                                        .setTrackNumber(thisTrack)
                                        .setReleaseYear(thisYear)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        .setExtras(
                                            Bundle().apply {
                                                putString("navidromeID", "Local_$thisId")
                                                putString("parent", "")
                                                putString("format", thisFormat.split("/").last())
                                                putString("dateAdded", thisDateAdded)
                                                putString("albumId", "Local_$thisAlbumId")
                                                putString("path", thisPath)
                                                putString("contentType", thisFormat)
                                                putInt("duration", thisDuration / 1000)
                                                putInt("bitrate", thisBitrate / 1000)
                                                putString("artistId", "Local_$thisArtistId")
                                                putString("genre", thisGenre)
                                            }
                                        )
                                        .build()
                                )
                            }.build()
                        )
                    }while (cursor.moveToNext())
                }
            }

            cursor?.close()
        }
        return songs
    }

    fun getLocalArtists(): List<MediaData.Artist> {
        val artists = mutableListOf<MediaData.Artist>()
        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                "${MediaStore.Audio.Media.DATA} LIKE ?" +
                        " OR ${MediaStore.Audio.Media.DATA} LIKE ?"
                            .repeat(LocalProviderManager.getAllFolders().size - 1),
                LocalProviderManager.getAllFolders().map {
                    "%$it%"
                }.toTypedArray(),
                null
            )

            when {
                cursor == null -> {
                    // query failed, handle error.
                }

                !cursor.moveToFirst() -> {
                    // no media on the device
                }

                else -> {
                    val idColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
                    val nameColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)

                    do {
                        val thisId = cursor.getLong(idColumn)
                        val thisName = cursor.getString(nameColumn) ?: "Unknown"

                        Log.d("LOCAL PROVIDER", "Added artist: $thisName")

                        artists.add(
                            MediaData.Artist(
                                navidromeID = "Local_$thisId",
                                name = thisName,
                                description = "",
                                artistImageUrl = null
                            )
                        )
                    }while (cursor.moveToNext())
                }
            }

            cursor?.close()
        }
        return artists
    }
}