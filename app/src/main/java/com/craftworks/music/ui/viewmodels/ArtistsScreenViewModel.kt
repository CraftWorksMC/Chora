package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.entity.toMediaDataArtist
import com.craftworks.music.data.database.entity.toMediaDataSong
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.data.repository.SyncRepository
import com.craftworks.music.ui.util.TextDisplayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistsScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val syncRepository: SyncRepository,
    private val artistDao: ArtistDao,
    private val songDao: SongDao
) : ViewModel() {

    // Observe Room database directly for instant UI updates
    // Sort using TextDisplayUtils.getSortKey to handle leading quotes/punctuation properly
    val allArtists: StateFlow<List<MediaData.Artist>> = artistDao.getAllArtists()
        .map { entities ->
            entities.map { it.toMediaDataArtist() }
                .sortedBy { TextDisplayUtils.getSortKey(it.name) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedArtist = MutableStateFlow<MediaData.Artist?>(null)
    val selectedArtist: StateFlow<MediaData.Artist?> = _selectedArtist.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val artistAlbums: StateFlow<List<MediaItem>> = _artistAlbums.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Load cached data instantly, sync in background (once per day)
        viewModelScope.launch {
            try {
                // Skip auto-sync if cache was just cleared
                if (syncRepository.wasJustCleared()) {
                    return@launch
                }

                if (!syncRepository.hasCachedData()) {
                    _isLoading.value = true
                    syncRepository.syncAll()
                    _isLoading.value = false
                } else if (syncRepository.shouldSyncToday()) {
                    launch { syncRepository.syncAll() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }

    }

    fun refreshArtists() {
        viewModelScope.launch {
            try {
                syncRepository.syncAll(forceRefresh = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<MediaData.Artist>> = searchQuery
        .debounce(300L)
        .combine(allArtists) { query, artists ->
            if (query.isBlank()) {
                emptyList()
            } else {
                artists.filter { artist ->
                    artist.name.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getSongsForArtists(artists: List<MediaData.Artist>): List<MediaItem> {
        // Use batch query to avoid N+1 problem
        val localArtists = artists.filter { it.navidromeID.startsWith("Local_") }
        val navidromeArtists = artists.filter { !it.navidromeID.startsWith("Local_") }

        val songs = mutableListOf<MediaItem>()

        // Get cached songs from Room for Navidrome artists
        if (navidromeArtists.isNotEmpty()) {
            val artistIds = navidromeArtists.map { it.navidromeID }
            val cachedSongs = songDao.getSongsByArtistIds(artistIds)
            songs.addAll(cachedSongs.map { it.toMediaDataSong().toMediaItem() })
        }

        // Fallback to repository for local artists
        for (artist in localArtists) {
            val albums = artistRepository.getArtistAlbums(artist.navidromeID)
            for (album in albums) {
                val albumSongs = albumRepository.getAlbum(album.mediaId) ?: emptyList()
                songs.addAll(albumSongs.drop(1)) // Drop album header
            }
        }

        return songs
    }

    fun setSelectedArtist(artist: MediaData.Artist) {
        _selectedArtist.value = artist
        viewModelScope.launch {
            val loadingJob = launch {
                delay(1000)
                if (_artistAlbums.value.isEmpty()) {
                    _isLoading.value = true
                }
            }
            try {
                coroutineScope {
                    // Run both requests in parallel
                    val artistAlbumsDeferred = async { artistRepository.getArtistAlbums(artist.navidromeID) }
                    val artistDetailsDeferred = async { artistRepository.getArtistInfo(artist.navidromeID) }

                    _artistAlbums.value = artistAlbumsDeferred.await()
                    val artistDetails = artistDetailsDeferred.await()
                    _selectedArtist.value = _selectedArtist.value?.copy(
                        description = artistDetails?.biography ?: "",
                        musicBrainzId = artistDetails?.musicBrainzId,
                        similarArtist = artistDetails?.similarArtist
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingJob.cancel()
                _isLoading.value = false
            }
        }
    }
}