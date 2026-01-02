package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.craftworks.music.data.database.entity.DownloadEntity
import com.craftworks.music.data.database.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY queuedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY queuedAt ASC")
    fun getDownloadsByStatus(statuses: List<DownloadStatus>): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY queuedAt ASC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY queuedAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'FAILED' ORDER BY queuedAt DESC")
    fun getFailedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity?

    @Query("SELECT mediaId FROM downloads WHERE mediaId IN (:mediaIds)")
    suspend fun getExistingMediaIds(mediaIds: List<String>): List<String>

    @Query("SELECT * FROM downloads WHERE mediaId IN (:mediaIds)")
    suspend fun getDownloadsByMediaIds(mediaIds: List<String>): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveDownloadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun getActiveDownloadCountOnce(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<DownloadEntity>)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)

    @Query("UPDATE downloads SET status = :status, progress = :progress, bytesDownloaded = :bytesDownloaded WHERE id = :id")
    suspend fun updateProgress(id: String, status: DownloadStatus, progress: Float, bytesDownloaded: Long)

    @Query("UPDATE downloads SET status = 'COMPLETED', completedAt = :completedAt, localFilePath = :localFilePath, progress = 1.0 WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long, localFilePath: String)

    @Query("UPDATE downloads SET status = 'FAILED', failureReason = :reason, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, reason: String)

    @Query("UPDATE downloads SET status = 'QUEUED', failureReason = NULL WHERE id = :id")
    suspend fun resetForRetry(id: String)

    @Query("UPDATE downloads SET status = 'PAUSED' WHERE status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun pauseAllActive()

    @Query("SELECT * FROM downloads WHERE status = 'PAUSED' ORDER BY queuedAt ASC")
    suspend fun getPausedDownloads(): List<DownloadEntity>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM downloads WHERE status = 'FAILED'")
    suspend fun deleteFailed()

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
