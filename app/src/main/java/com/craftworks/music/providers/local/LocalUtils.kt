package com.craftworks.music.providers.local

import android.content.Context
import android.provider.MediaStore

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
}