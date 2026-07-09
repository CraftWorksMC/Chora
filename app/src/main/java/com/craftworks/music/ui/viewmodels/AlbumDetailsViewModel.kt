package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.data.repository.StarredRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val starredRepository: StarredRepository
) : ViewModel() {
    private val _songsInAlbum = MutableStateFlow<List<MediaItem>>(listOf())
    val songsInAlbum: StateFlow<List<MediaItem>> = _songsInAlbum.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            val loadingJob = launch {
                if (_songsInAlbum.value.isEmpty()) {
                    _isLoading.value = true
                }
            }

            _songsInAlbum.value = albumRepository.getAlbum(albumId) ?: listOf(MediaItem.EMPTY)

            loadingJob.cancel()
            _isLoading.value = false
        }
    }

    fun starAlbum(id: String) {
        viewModelScope.launch {
            starredRepository.starItem(albumId = id, ignoreCachedResponse = true)
        }
    }
    fun unstarAlbum(id: String) {
        viewModelScope.launch {
            starredRepository.unStarItem(albumId = id, ignoreCachedResponse = true)
        }
    }

    fun setSongRating(
        songId: String,
        rating: Int,
    ) {
        val song =_songsInAlbum.value.first {
            it.mediaMetadata.extras?.getString("navidromeID") == songId
        }
        val maxStars = (song.mediaMetadata.userRating as? StarRating)?.maxStars ?: 5

        val updatedSong = song.buildUpon().setMediaMetadata(
            song.mediaMetadata.buildUpon()
                .setUserRating(StarRating(maxStars, rating.toFloat()))
                .build()
        ).build()

        _songsInAlbum.value = _songsInAlbum.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }

        viewModelScope.launch {
            songRepository.setSongRating(songId, rating)
        }
    }
}