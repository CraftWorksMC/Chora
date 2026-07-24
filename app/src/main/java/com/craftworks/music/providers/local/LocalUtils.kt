package com.craftworks.music.providers.local

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.craftworks.music.data.model.MediaModel

object LocalUtils {
    fun getAlbumIdsInFolders(context: Context, folders: List<String>): Set<Long> {
        val albumIds = mutableSetOf<Long>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


        val selectionBuilder = StringBuilder("${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (")
        folders.forEachIndexed { index, _ ->
            if (index > 0) selectionBuilder.append(" OR ")
            selectionBuilder.append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?")
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

    fun getLocalAlbumSongs(context: Context, albumId: String, providerId: String): List<MediaModel.Song> {
        val songs = mutableListOf<MediaModel.Song>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.BITRATE)
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        contentResolver.query(
            uri,
            projection,
            "${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0",
            arrayOf(albumId),
            "${MediaStore.Audio.Media.TRACK} ASC, ${MediaStore.Audio.Media.TITLE} ASC"
        )?.use {
            return LocalNormalizer.cursorToSongs(context, providerId, it)
        }
        return emptyList()
    }
    fun getLocalSongs(context: Context, providerId: String, sort: String, folders: List<String>): List<MediaModel.Song> {
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.BITRATE)
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

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
            sort
        )?.use {
            return LocalNormalizer.cursorToSongs(context, providerId, it)
        }

        return emptyList()
    }
}