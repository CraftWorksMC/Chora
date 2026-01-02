package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.database.entity.DownloadEntity
import com.craftworks.music.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val activeDownloads: Flow<List<DownloadEntity>> = downloadRepository.activeDownloads
    val completedDownloads: Flow<List<DownloadEntity>> = downloadRepository.completedDownloads
    val failedDownloads: Flow<List<DownloadEntity>> = downloadRepository.failedDownloads
    val activeDownloadCount: Flow<Int> = downloadRepository.activeDownloadCount

    val hasActiveDownloads: StateFlow<Boolean> = downloadRepository.hasActiveDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun queueDownload(song: MediaMetadata) {
        viewModelScope.launch {
            try {
                downloadRepository.queueSongDownload(song)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun queueDownloads(songs: List<MediaMetadata>) {
        viewModelScope.launch {
            try {
                downloadRepository.queueSongsDownload(songs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.cancelDownload(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.pauseDownload(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.resumeDownload(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.retryDownload(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDownload(downloadId: String, mediaId: String) {
        viewModelScope.launch {
            try {
                // Delete the offline song file and record
                downloadRepository.deleteOfflineSong(mediaId)
                // Also remove the download record
                downloadRepository.cancelDownload(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            try {
                downloadRepository.clearCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            try {
                downloadRepository.pauseAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            try {
                downloadRepository.resumeAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun isOfflineAvailable(songId: String): Boolean {
        return downloadRepository.isOfflineAvailable(songId)
    }

    fun isOfflineAvailableFlow(songId: String): Flow<Boolean> {
        return downloadRepository.isOfflineAvailableFlow(songId)
    }
}
