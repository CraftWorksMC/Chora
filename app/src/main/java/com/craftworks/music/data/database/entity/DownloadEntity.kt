package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class MediaType {
    SONG,
    ALBUM
}

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["status"]),
        Index(value = ["mediaId"], unique = true),
        Index(value = ["queuedAt"])
    ]
)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val mediaId: String,
    val mediaType: MediaType,
    val title: String,
    val artist: String,
    val albumTitle: String?,
    val imageUrl: String?,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localFilePath: String? = null,
    val workRequestId: String? = null,
    val queuedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val failureReason: String? = null,
    val retryCount: Int = 0,
    val format: String = "mp3"
)
