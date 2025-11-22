package com.craftworks.music.providers.local

import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.managers.LocalProviderManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        private const val TAG = "LOCAL_PROVIDER"
        private const val LOCAL_PREFIX = "Local_"
        private const val ALBUM_ART_PATH = "content://media/external/audio/albumart"
    }

    //region Albums
    suspend fun getLocalAlbums(sort: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting All Albums")

        val sortOrder = when (sort) {
            "alphabeticalByName" -> "${MediaStore.Audio.Albums.ALBUM} ASC"
            "random" -> "RANDOM()"
            else -> "${MediaStore.Audio.Albums.ALBUM} DESC"
        }

        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST
        )

        // Get all folders first
        val folders = LocalProviderManager.getAllFolders()
        val albumIdsInFolders = if (folders.isNotEmpty()) {
            getAlbumIdsInFolders(folders)
        } else {
            emptySet()
        }

        val selection = if (albumIdsInFolders.isNotEmpty()) {
            "${MediaStore.Audio.Albums._ID} IN (${albumIdsInFolders.joinToString(",")})"
        } else {
            null
        }

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )

        val albums = mutableListOf<MediaItem>()

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)

            while (it.moveToNext()) {
                val albumId = it.getLong(idIdx)
                val albumName = it.getString(nameIdx) ?: "Unknown"
                val artistName = it.getString(artistIdx) ?: "Unknown"

                val artworkUri = "$ALBUM_ART_PATH/$albumId".toUri().let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        uri
                    } catch (e: Exception) {
                        "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
                    }
                }

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(albumName)
                    .setArtist(artistName)
                    .setAlbumTitle(albumName)
                    .setAlbumArtist(artistName)
                    .setArtworkUri(artworkUri)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .setExtras(Bundle().apply {
                        putString("navidromeID", "$LOCAL_PREFIX$albumId")
                    })
                    .build()

                albums += MediaItem.Builder()
                    .setMediaId("$LOCAL_PREFIX$albumId")
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }
        }

        albums
    }

    private fun getAlbumIdsInFolders(folders: List<String>): Set<Long> {
        val albumIds = mutableSetOf<Long>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selectionBuilder = StringBuilder("${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (")
        folders.forEachIndexed { index, _ ->
            if (index > 0) selectionBuilder.append(" OR ")
            selectionBuilder.append("${MediaStore.Audio.Media.DATA} LIKE ?")
        }
        selectionBuilder.append(")")

        val selectionArgs = folders.map { "%$it%" }.toTypedArray()

        contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.ALBUM_ID),
            selectionBuilder.toString(),
            selectionArgs,
            null
        )?.use { cursor ->
            val albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                albumIds.add(cursor.getLong(albumIdIdx))
            }
        }

        return albumIds
    }

    fun getLocalAlbum(albumId: String): List<MediaItem>? {
        val albumIdLong = albumId.removePrefix(LOCAL_PREFIX).toLongOrNull() ?: run {
            Log.e(TAG, "Invalid album ID format: $albumId")
            return null
        }

        Log.d(TAG, "Getting album data for id $albumIdLong")

        val albumWithSongs = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver

        val albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val albumProjection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        contentResolver.query(
            albumUri,
            albumProjection,
            "${MediaStore.Audio.Albums._ID} = ?",
            arrayOf(albumIdLong.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

                val albumName = cursor.getString(nameIdx) ?: "Unknown"
                val artistName = cursor.getString(artistIdx) ?: "Unknown"
                val year = cursor.getInt(yearIdx)

                val artworkUri = "$ALBUM_ART_PATH/$albumIdLong".toUri().let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        uri
                    } catch (e: Exception) {
                        "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
                    }
                }

                val songs = getLocalAlbumSongs(albumIdLong)
                val totalDuration = songs.sumOf { it.mediaMetadata.durationMs ?: 0L }
                val genre = songs.firstOrNull()?.mediaMetadata?.genre

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(albumName)
                    .setArtist(artistName)
                    .setAlbumTitle(albumName)
                    .setAlbumArtist(artistName)
                    .setArtworkUri(artworkUri)
                    .setRecordingYear(year)
                    .setGenre(genre)
                    .setIsBrowsable(true)
                    .setIsPlayable(true)
                    .setDurationMs(totalDuration)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .setExtras(Bundle().apply {
                        putString("navidromeID", "$LOCAL_PREFIX$albumIdLong")
                    })
                    .build()

                albumWithSongs += MediaItem.Builder()
                    .setMediaId("$LOCAL_PREFIX$albumIdLong")
                    .setUri("$ALBUM_ART_PATH/$albumIdLong")
                    .setMediaMetadata(mediaMetadata)
                    .build()

                albumWithSongs.addAll(songs)
                Log.d(TAG, "Got album data with ${songs.size} songs")
            }
        }

        return albumWithSongs
    }

    private fun getLocalAlbumSongs(albumId: Long): List<MediaItem> {
        Log.d(TAG, "Getting Songs for album id: $albumId")

        val songs = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.GENRE
        )

        contentResolver.query(
            uri,
            projection,
            "${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0",
            arrayOf(albumId.toString()),
            "${MediaStore.Audio.Media.TRACK} ASC, ${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val pathIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val formatIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val trackIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val bitrateIdx = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            val genreIdx = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val path = cursor.getString(pathIdx)
                val title = cursor.getString(titleIdx)
                val album = cursor.getStringOrNull(albumIdx) ?: "Unknown"
                val artist = cursor.getStringOrNull(artistIdx) ?: "Unknown"
                val format = cursor.getString(formatIdx)
                val dateAdded = cursor.getString(dateAddedIdx) ?: ""
                val track = cursor.getIntOrNull(trackIdx) ?: 0
                val year = cursor.getIntOrNull(yearIdx) ?: 0
                val duration = cursor.getIntOrNull(durationIdx) ?: 0
                val bitrate = cursor.getIntOrNull(bitrateIdx) ?: 0
                val genre = cursor.getStringOrNull(genreIdx) ?: ""

                val artworkUri = "$ALBUM_ART_PATH/$albumId".toUri().let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        uri
                    } catch (e: Exception) {
                        "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
                    }
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                if (yearIdx != -1) {
                    val rawYearString = cursor.getString(yearIdx)
                    Log.d(TAG, "Raw year string from MediaStore: $rawYearString")
                }

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setAlbumTitle(album)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTrackNumber(track)
                    .setRecordingYear(year)
                    .setDurationMs(duration.toLong())
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(Bundle().apply {
                        putString("navidromeID", "$LOCAL_PREFIX$id")
                        putString("artistId", "$LOCAL_PREFIX${artist.hashCode()}")
                        putString("format", format.drop(6))
                        putInt("bitrate", bitrate / 1000)
                        putString("path", path)
                    })
                    .build()

                songs += MediaItem.Builder()
                    .setMediaId(contentUri.toString())
                    .setUri(contentUri.toString())
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }
        }

        return songs
    }
    //endregion

    fun getLocalSongs(): List<MediaItem> {
        val songs = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
        )

        val folders = LocalProviderManager.getAllFolders()
        if (folders.isEmpty()) return emptyList()

        val selectionBuilder = StringBuilder("${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (")
        folders.forEachIndexed { index, folder ->
            if (index > 0) selectionBuilder.append(" OR ")
            selectionBuilder.append("${MediaStore.Audio.Media.DATA} LIKE ?")
        }
        selectionBuilder.append(")")

        val selectionArgs = folders.map { "%$it%" }.toTypedArray()

        contentResolver.query(
            uri,
            projection,
            selectionBuilder.toString(),
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val pathIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val formatIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val trackIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val bitrateIdx = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            val genreIdx = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val albumId = cursor.getLongOrNull(albumIdIdx) ?: 0
                val path = cursor.getString(pathIdx)
                val title = cursor.getString(titleIdx)
                val album = cursor.getStringOrNull(albumIdx) ?: "Unknown"
                val artist = cursor.getStringOrNull(artistIdx) ?: "Unknown"
                val format = cursor.getString(formatIdx)
                val dateAdded = cursor.getString(dateAddedIdx) ?: ""
                val track = cursor.getIntOrNull(trackIdx) ?: 0
                val year = cursor.getIntOrNull(yearIdx) ?: 0
                val duration = cursor.getIntOrNull(durationIdx) ?: 0
                val bitrate = cursor.getIntOrNull(bitrateIdx) ?: 0
                val genre = cursor.getStringOrNull(genreIdx) ?: ""

                val artworkUri = "$ALBUM_ART_PATH/$albumId".toUri().let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        uri
                    } catch (e: Exception) {
                        "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
                    }
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setAlbumTitle(album)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .setTrackNumber(track)
                    .setRecordingYear(year)
                    .setDurationMs(duration.toLong())
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(Bundle().apply {
                        putString("navidromeID", "$LOCAL_PREFIX$id")
                        putString("artistId", "$LOCAL_PREFIX${artist.hashCode()}")
                        putString("format", format.drop(6))
                        putInt("bitrate", bitrate / 1000)
                        putString("path", path)
                    })
                    .build()

                songs += MediaItem.Builder()
                    .setMediaId(contentUri.toString())
                    .setUri(contentUri.toString())
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }
        }

        return songs
    }

    fun getLocalArtists(): List<MediaData.Artist> {
        val artists = mutableSetOf<MediaData.Artist>()
        val songs = getLocalSongs()

        songs.forEach { song ->
            val artistName = song.mediaMetadata.artist ?: "Unknown"
            val artistId = song.mediaMetadata.extras?.getString("artistId") ?:
            "$LOCAL_PREFIX${artistName.hashCode()}"

            artists.add(
                MediaData.Artist(
                    navidromeID = artistId,
                    name = artistName.toString(),
                    description = "",
                    artistImageUrl = null
                )
            )
        }

        return artists.sortedBy { it.name }
    }

    suspend fun getAlbumsByArtistId(artistId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val albumsUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        contentResolver.query(
            albumsUri,
            projection,
            null, // get all albums
            null,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(idIdx)
                val albumName = cursor.getString(nameIdx) ?: "Unknown"
                val artistName = cursor.getString(artistIdx) ?: "Unknown"
                val year = cursor.getInt(yearIdx)

                // match the artistId against our synthetic ID scheme
                val thisArtistId = "$LOCAL_PREFIX${artistName.hashCode()}"
                if (thisArtistId != artistId) continue

                val artworkUri = "$ALBUM_ART_PATH/$albumId".toUri().let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                        uri
                    } catch (e: Exception) {
                        "android.resource://com.craftworks.music/${R.drawable.albumplaceholder}".toUri()
                    }
                }

                albums += MediaItem.Builder()
                    .setMediaId("$LOCAL_PREFIX$albumId")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(albumName)
                            .setArtist(artistName)
                            .setAlbumTitle(albumName)
                            .setAlbumArtist(artistName)
                            .setArtworkUri(artworkUri)
                            .setRecordingYear(year)
                            .setReleaseYear(year)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                            .setExtras(Bundle().apply {
                                putString("navidromeID", "$LOCAL_PREFIX$albumId")
                                putString("artistId", artistId)
                            })
                            .build()
                    )
                    .build()
            }
        }

        albums
    }

}