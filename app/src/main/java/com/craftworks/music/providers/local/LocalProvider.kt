package com.craftworks.music.providers.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.craftworks.music.data.Genre
import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.LocalProviderManager
import java.io.FileNotFoundException

class LocalProvider private constructor() {
    private var applicationContext: Context? = null

    companion object {
        @Volatile
        private var instance: LocalProvider? = null
        private const val LOCAL_PREFIX = "Local_"

        fun getInstance(): LocalProvider {
            return instance ?: synchronized(this) {
                instance ?: LocalProvider().also { instance = it }
            }
        }
    }

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    //region Albums
    fun getLocalAlbums(sort: String?): List<MediaData.Album> {
        Log.d("LOCAL PROVIDER", "Getting All Albums!")
        val albums = mutableListOf<MediaData.Album>()

        val sortOrder = when(sort) {
            "alphabeticalByName" -> "${MediaStore.Audio.Albums.ALBUM} DESC"
            "recent" -> "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            "newest" -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            "random" -> "RANDOM()"
            else -> {
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            }
        }

        //for (dir in LocalProviderManager.getAllFolders()){
            applicationContext?.let { context ->
                val contentResolver: ContentResolver = context.contentResolver
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ARTIST,
                    //MediaStore.Audio.Media.DATE_ADDED,
                    //MediaStore.Audio.Media.YEAR
                )
                val cursor: Cursor? = contentResolver.query(
                    uri,
                    projection,
                    "${MediaStore.Audio.Media.DATA} LIKE ?",
                    //arrayOf("%$dir%"),
                    LocalProviderManager.getAllFolders().map {
                        "%$it%"
                    }.toTypedArray(),
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
                        val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                        val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                        //val dateAddedColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                        //val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                        do {
                            val thisId = cursor.getLongOrNull(idColumn) ?: 0
                            val thisAlbum = cursor.getString(albumColumn)
                            val thisArtist = cursor.getString(artistColumn)
                            //val thisDateAdded = cursor.getString(dateAddedColumn)
                            //val thisYear = cursor.getInt(yearColumn)

                            val imageUri: String = try {
                                "content://media/external/audio/media/$thisId/albumart"
                            } catch (_: FileNotFoundException) {
                                println("No Album Art!")
                            }.toString()

                            val album = MediaData.Album(
                                navidromeID = "Local_$thisId",
                                name = thisAlbum,
                                album = thisAlbum,
                                title = thisAlbum,
                                artist = thisArtist,
                                coverArt = imageUri,
                                songCount = 0,
                                duration = 0,
                                artistId = "",
                            )

                            if (!albums.contains(album))
                                albums.add(album)
                        } while (cursor.moveToNext())
                    }
                }

                cursor?.close()
            }
        //}

        return albums
    }

    fun getLocalAlbum(albumId: String): MediaData.Album? {
        val numericId = try {
            albumId.removePrefix("Local_").toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        Log.d("LOCAL PROVIDER", "Getting album data for id $numericId!")

        var album: MediaData.Album? = null

        applicationContext?.let { context ->
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ARTIST_ID,
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
                    val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val artistIdColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST_ID)
                    val yearColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                    do {
                        val thisId = cursor.getLongOrNull(idColumn) ?: 0
                        val thisAlbum = cursor.getString(albumColumn)
                        val thisArtist = cursor.getString(artistColumn)
                        val thisArtistId = cursor.getInt(artistIdColumn)
                        val thisYear = cursor.getInt(yearColumn)

                        val imageUri: String = try {
                            "content://media/external/audio/media/$thisId/albumart"
                        } catch (_: FileNotFoundException) {
                            println("No Album Art!")
                        }.toString()

                        val songs = getLocalAlbumSongs(numericId)
                        //val songs = emptyList<MediaData.Song>()
                        val totalDuration = songs.sumOf { it.duration }
                        val genre = songs.firstOrNull()?.genres ?: emptyList()

                        album = MediaData.Album(
                            navidromeID = albumId,
                            name = thisAlbum,
                            album = thisAlbum,
                            title = thisAlbum,
                            coverArt = imageUri,
                            songCount = songs.size,
                            duration = totalDuration,
                            artistId = "Local_$thisArtistId",
                            artist = thisArtist,
                            year = thisYear,
                            //genre = genre,
                            genres = genre,
                            songs = songs
                        )
                        Log.d("LOCAL PROVIDER", "Got album data: $album")

                    } while (cursor.moveToNext())
                }
            }

            cursor?.close()
        }

        return album
    }

    private fun getLocalAlbumSongs(albumId: Long): List<MediaData.Song> {
        Log.d("LOCAL PROVIDER", "Getting Songs for album id: $albumId")

        val songs = mutableListOf<MediaData.Song>()

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
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId)

                    val imageUri: String = try {
                        "content://media/external/audio/media/$thisId/albumart"
                    } catch (_: FileNotFoundException) {
                        println("No Album Art!")
                    }.toString()

                    Log.d("LOCAL PROVIDER", "Added song: $thisTitle")

                    val genres = mutableListOf<Genre>()
                    thisGenre.split(",").forEach{
                        genres.add(Genre(it))
                    }

                    songs.add(
                        MediaData.Song(
                            navidromeID = "Local_$thisId",
                            parent = "",
                            title = thisTitle,
                            album = thisAlbum,
                            artist = thisArtist,
                            imageUrl = imageUri,
                            format = thisFormat.split("/").last(),
                            dateAdded = thisDateAdded,
                            albumId = "Local_$albumId",
                            bpm = 0,
                            path = thisPath,
                            media = contentUri.toString(),

                            track = thisTrack,
                            year = thisYear,
                            contentType = thisFormat,
                            duration = thisDuration / 1000,
                            bitrate = thisBitrate / 1000,
                            artistId = "Local_$thisArtistId",
                            genre = thisGenre,
                            genres = genres
                        )
                    )
                }
            }

            cursor?.close()
        }

        return songs
    }
    //endregion

    fun getLocalSongs(): List<MediaData.Song> {
        val songs = mutableListOf<MediaData.Song>()
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
                "${MediaStore.Audio.Media.DATA} LIKE ?",
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
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, thisId)

                    val imageUri: String = try {
                        "content://media/external/audio/media/$thisId/albumart"
                    } catch (_: FileNotFoundException) {
                        println("No Album Art!")
                    }.toString()

                    Log.d("LOCAL PROVIDER", "Added song to all songs list: $thisTitle")

                    val genres = mutableListOf<Genre>()
                    thisGenre.split(",").forEach{
                        genres.add(Genre(it))
                    }

                    songs.add(
                        MediaData.Song(
                            navidromeID = "Local_$thisId",
                            parent = "",
                            title = thisTitle,
                            album = thisAlbum,
                            artist = thisArtist,
                            imageUrl = imageUri,
                            format = thisFormat.split("/").last(),
                            dateAdded = thisDateAdded,
                            albumId = "Local_$thisAlbumId",
                            bpm = 0,
                            path = thisPath,
                            media = contentUri.toString(),

                            track = thisTrack,
                            year = thisYear,
                            contentType = thisFormat,
                            duration = thisDuration / 1000,
                            bitrate = thisBitrate / 1000,
                            artistId = "Local_$thisArtistId",
                            genre = thisGenre,
                            genres = genres
                        )
                    )
                }
            }

            cursor?.close()
        }
        return songs
    }

    fun getLocalArtists(): List<MediaData.Artist> {
        val artists = mutableListOf<MediaData.Artist>()
        applicationContext?.let { context ->
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
            )

            context.contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID))
                    artists.add(
                        MediaData.Artist(
                            navidromeID = "Local_$id",
                            name = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)) ?: "Unknown",
                            description = "",

                            albumCount = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS))
                        )
                    )
                }
            }
        }
        return artists
    }
}