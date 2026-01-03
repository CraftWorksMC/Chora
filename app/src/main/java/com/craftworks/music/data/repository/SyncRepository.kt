package com.craftworks.music.data.repository

import android.util.Log
import com.craftworks.music.data.database.ChoraDatabase
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.AlbumPaletteDao
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.dao.SyncMetadataDao
import com.craftworks.music.data.database.entity.SyncMetadata
import com.craftworks.music.data.database.entity.toEntity
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.managers.PaletteManager
import com.craftworks.music.managers.DataRefreshManager
import androidx.room.withTransaction
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.data.model.toSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncPhase {
    IDLE, FETCHING_COUNTS, ARTISTS, ALBUMS, SONGS, ARTWORKS, COMPLETE
}

data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val current: Int = 0,
    val total: Int = 0,
    val newSongs: Int = 0,
    val updatedSongs: Int = 0,
    val isPaused: Boolean = false,
    val message: String = ""
) {
    val percentage: Float
        get() = if (total > 0) (current.toFloat() / total * 100) else 0f

    val displayText: String
        get() = when (phase) {
            SyncPhase.IDLE -> ""
            SyncPhase.FETCHING_COUNTS -> "Fetching library info..."
            SyncPhase.ARTISTS -> if (total > 0) "Syncing artists ($current of $total)" else "Syncing artists..."
            SyncPhase.ALBUMS -> if (total > 0) "Syncing albums ($current of $total)" else "Syncing albums..."
            SyncPhase.SONGS -> {
                val songInfo = when {
                    newSongs > 0 && updatedSongs > 0 -> "$newSongs new, $updatedSongs updated"
                    newSongs > 0 -> "$newSongs new songs"
                    updatedSongs > 0 -> "$updatedSongs updated"
                    else -> "checking..."
                }
                if (total > 0) "Processing albums ($current of $total) • $songInfo" else "Syncing songs..."
            }
            SyncPhase.ARTWORKS -> if (total > 0) "Generating artwork palettes ($current of $total)" else "Generating artwork palettes..."
            SyncPhase.COMPLETE -> "Sync complete!"
        }
}

@Singleton
class SyncRepository @Inject constructor(
    private val database: ChoraDatabase,
    private val navidromeDataSource: NavidromeDataSource,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val albumPaletteDao: AlbumPaletteDao,
    private val paletteManager: PaletteManager
) {
    private val syncMutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow("")
    val syncProgress: StateFlow<String> = _syncProgress.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    @Volatile
    private var cancelRequested = false

    @Volatile
    private var pauseRequested = false

    // State for resuming
    private var pausedPhase: SyncPhase = SyncPhase.IDLE
    private var pausedAlbumIndex: Int = 0
    private var pausedAlbumOffset: Int = 0
    private var pausedForceRefresh: Boolean = false
    private var cachedAlbumIds: List<String> = emptyList()

    fun cancelSync() {
        if (_isSyncing.value) {
            cancelRequested = true
            pauseRequested = false
            _isPaused.value = false
            Log.d("SyncRepository", "Sync cancellation requested")
        }
    }

    fun pauseSync() {
        if (_isSyncing.value && !_isPaused.value) {
            pauseRequested = true
            Log.d("SyncRepository", "Sync pause requested")
        }
    }

    fun resumeSync() {
        // Note: This only resets the pause flag. The caller must also call
        // syncAll(resumeFromPause = true) to actually continue the sync.
        // The isPaused state will be properly cleared when syncAll acquires
        // the mutex and starts processing.
        if (_isPaused.value) {
            pauseRequested = false
            Log.d("SyncRepository", "Sync resume requested")
        }
    }

    suspend fun hasCachedData(): Boolean = withContext(Dispatchers.IO) {
        songDao.getCount() > 0 || albumDao.getCount() > 0 || artistDao.getCount() > 0
    }

    suspend fun syncAll(forceRefresh: Boolean = false, resumeFromPause: Boolean = false) = withContext(Dispatchers.IO) {
        // Use tryLock to prevent blocking if already syncing
        if (!syncMutex.tryLock()) {
            Log.d("SyncRepository", "Sync already in progress, skipping")
            return@withContext
        }

        // Track if we should preserve state (only true on clean pause)
        var preserveStateOnExit = false

        try {
            // If resuming, clear the paused state now that we have the mutex
            if (resumeFromPause && _isPaused.value) {
                _isPaused.value = false
            }

            _isSyncing.value = true
            cancelRequested = false

            val startPhase = if (resumeFromPause) pausedPhase else SyncPhase.FETCHING_COUNTS
            val effectiveForceRefresh = if (resumeFromPause) pausedForceRefresh else forceRefresh
            pausedForceRefresh = effectiveForceRefresh

            Log.d("SyncRepository", "Starting sync, forceRefresh=$effectiveForceRefresh, resumeFrom=$startPhase")

            // Phase 1: Fetch counts first for progress tracking
            if (startPhase == SyncPhase.FETCHING_COUNTS) {
                _syncState.value = SyncState(phase = SyncPhase.FETCHING_COUNTS)
                _syncProgress.value = "Fetching library info..."

                // Quick probe to estimate total albums
                val probeAlbums = navidromeDataSource.getNavidromeAlbums(
                    sort = "alphabeticalByName",
                    size = 1,
                    offset = 0,
                    ignoreCachedResponse = effectiveForceRefresh
                )
                // Fetch artists to count them
                val artists = navidromeDataSource.getNavidromeArtists(ignoreCachedResponse = effectiveForceRefresh)
                val artistCount = artists.size

                if (shouldPauseOrCancel()) {
                    preserveStateOnExit = handlePauseOrCancel(SyncPhase.ARTISTS)
                    return@withContext
                }

                // Sync artists (we already have them)
                _syncState.value = SyncState(phase = SyncPhase.ARTISTS, current = 0, total = artistCount)
                _syncProgress.value = "Syncing artists..."

                if (artists.isNotEmpty()) {
                    val entities = artists.map { it.toEntity() }
                    artistDao.insertAll(entities)
                    syncMetadataDao.upsert(
                        SyncMetadata(
                            key = SyncMetadata.KEY_ARTISTS,
                            lastSyncTimestamp = System.currentTimeMillis(),
                            itemCount = entities.size
                        )
                    )
                    _syncState.value = _syncState.value.copy(current = artistCount)
                    Log.d("SyncRepository", "Synced ${entities.size} artists")
                }

                if (shouldPauseOrCancel()) {
                    preserveStateOnExit = handlePauseOrCancel(SyncPhase.ALBUMS)
                    return@withContext
                }
            }

            // Phase 2: Sync albums with progress
            if (startPhase.ordinal <= SyncPhase.ALBUMS.ordinal) {
                val startOffset = if (resumeFromPause && startPhase == SyncPhase.ALBUMS) pausedAlbumOffset else 0
                preserveStateOnExit = syncAlbumsWithProgress(effectiveForceRefresh, startOffset)

                if (preserveStateOnExit || shouldPauseOrCancel()) {
                    return@withContext
                }
            }

            // Phase 3: Sync songs via albums
            if (startPhase.ordinal <= SyncPhase.SONGS.ordinal) {
                val startIndex = if (resumeFromPause && startPhase == SyncPhase.SONGS) pausedAlbumIndex else 0
                preserveStateOnExit = syncSongsWithProgress(effectiveForceRefresh, startIndex)

                if (preserveStateOnExit || shouldPauseOrCancel()) {
                    return@withContext
                }
            }

            // Phase 4: Cache Artworks
            if (startPhase.ordinal <= SyncPhase.ARTWORKS.ordinal) {
                preserveStateOnExit = cacheArtworks()

                if (preserveStateOnExit || shouldPauseOrCancel()) {
                    return@withContext
                }
            }

            // Complete
            _syncState.value = SyncState(phase = SyncPhase.COMPLETE)
            _syncProgress.value = "Sync complete!"
            Log.d("SyncRepository", "Full sync completed")

            // Record sync completion time for daily sync check
            recordFullSyncTime()

            // Brief delay to show completion, then reset
            kotlinx.coroutines.delay(1500)

        } catch (e: Exception) {
            Log.e("SyncRepository", "Sync failed", e)
            // On exception, never preserve state - always cleanup
            preserveStateOnExit = false
            _isPaused.value = false
        } finally {
            if (!preserveStateOnExit) {
                Log.d("SyncRepository", "Resetting sync state")
                _syncProgress.value = ""
                _syncState.value = SyncState()
                _isSyncing.value = false
                _isPaused.value = false
                cancelRequested = false
                pauseRequested = false
                pausedPhase = SyncPhase.IDLE
                pausedAlbumIndex = 0
                pausedAlbumOffset = 0
                cachedAlbumIds = emptyList()
            } else {
                Log.d("SyncRepository", "Preserving sync state for resume")
            }
            // Always unlock the mutex - we always acquired it at the start
            syncMutex.unlock()
        }
    }

    private fun shouldPauseOrCancel(): Boolean = cancelRequested || pauseRequested

    /**
     * Handles pause or cancel request.
     * @return true if paused (state should be preserved), false if cancelled (state should reset)
     */
    private fun handlePauseOrCancel(nextPhase: SyncPhase): Boolean {
        return if (pauseRequested) {
            _isPaused.value = true
            pausedPhase = nextPhase
            _syncState.value = _syncState.value.copy(isPaused = true)
            Log.d("SyncRepository", "Sync paused at phase $nextPhase")
            true // Preserve state
        } else if (cancelRequested) {
            Log.d("SyncRepository", "Sync cancelled at phase $nextPhase")
            false // Don't preserve state
        } else {
            false
        }
    }

    /**
     * Syncs albums with progress tracking.
     * @return true if paused (state should be preserved), false otherwise
     */
    private suspend fun syncAlbumsWithProgress(forceRefresh: Boolean, startOffset: Int = 0): Boolean {
        try {
            var offset = startOffset
            var totalAlbums = 0
            val pageSize = 500

            // First, estimate total by fetching until we hit the end
            // We'll update total as we go
            val estimatedTotal = if (startOffset == 0) {
                // Fetch first batch to estimate
                val firstBatch = navidromeDataSource.getNavidromeAlbums(
                    sort = "alphabeticalByName",
                    size = pageSize,
                    offset = 0,
                    ignoreCachedResponse = forceRefresh
                )
                if (firstBatch.size < pageSize) firstBatch.size else firstBatch.size * 3 // Rough estimate
            } else {
                _syncState.value.total
            }

            _syncState.value = SyncState(phase = SyncPhase.ALBUMS, current = startOffset, total = estimatedTotal)
            _syncProgress.value = "Syncing albums..."

            // Paginate to get all albums - insert each batch immediately
            while (!shouldPauseOrCancel()) {
                val albums = navidromeDataSource.getNavidromeAlbums(
                    sort = "alphabeticalByName",
                    size = pageSize,
                    offset = offset,
                    ignoreCachedResponse = forceRefresh
                )

                if (albums.isEmpty()) break

                // Insert this batch immediately - appears in UI right away
                val entities = albums.map { it.toAlbum().toEntity() }
                albumDao.insertAll(entities)
                totalAlbums += entities.size
                offset += albums.size

                _syncState.value = _syncState.value.copy(
                    current = offset,
                    total = if (albums.size < pageSize) offset else maxOf(offset + pageSize, estimatedTotal)
                )
                _syncProgress.value = "Syncing albums... ($offset)"

                if (albums.size < pageSize) break
            }

            if (shouldPauseOrCancel()) {
                pausedAlbumOffset = offset
                return handlePauseOrCancel(SyncPhase.ALBUMS)
            }

            if (totalAlbums > 0) {
                syncMetadataDao.upsert(
                    SyncMetadata(
                        key = SyncMetadata.KEY_ALBUMS,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        itemCount = totalAlbums
                    )
                )
                Log.d("SyncRepository", "Synced $totalAlbums albums progressively")
            }
            return false
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to sync albums", e)
            return false // Error occurred, don't preserve state
        }
    }

    /**
     * Syncs songs with progress tracking.
     * @return true if paused (state should be preserved), false otherwise
     */
    private suspend fun syncSongsWithProgress(forceRefresh: Boolean, startIndex: Int = 0): Boolean {
        try {
            val albums = if (cachedAlbumIds.isNotEmpty() && startIndex > 0) {
                // Resume with cached album list
                albumDao.getAllAlbumsOnce().filter { it.navidromeID in cachedAlbumIds }
            } else {
                albumDao.getAllAlbumsOnce().also {
                    cachedAlbumIds = it.map { album -> album.navidromeID }
                }
            }

            // Load existing song IDs for delta sync - use HashSet for O(1) lookup
            // and clear after use to prevent memory retention
            val existingSongIds: Set<String> = HashSet(songDao.getAllNavidromeIds())
            Log.d("SyncRepository", "Delta sync: ${existingSongIds.size} existing songs in database")

            val totalAlbums = albums.size
            val processedCount = AtomicInteger(startIndex)
            val newSongsCount = AtomicInteger(0)
            val updatedSongsCount = AtomicInteger(0)
            val failedAlbums = mutableListOf<String>()
            val failedAlbumsMutex = Mutex()

            _syncState.value = SyncState(
                phase = SyncPhase.SONGS,
                current = startIndex,
                total = totalAlbums,
                newSongs = 0,
                updatedSongs = 0
            )
            _syncProgress.value = "Syncing songs..."

            // Parallel sync with semaphore to limit concurrent requests
            val concurrency = 8 // Number of parallel requests
            val semaphore = Semaphore(concurrency)
            val songInsertMutex = Mutex()

            coroutineScope {
                val albumsToProcess = if (startIndex < albums.size) albums.subList(startIndex, albums.size) else emptyList()

                val jobs = albumsToProcess.mapIndexed { index, album ->
                    async {
                        if (shouldPauseOrCancel()) {
                            return@async
                        }

                        semaphore.withPermit {
                            if (shouldPauseOrCancel()) {
                                return@withPermit
                            }

                            val success = syncAlbumSongsWithDelta(
                                album.navidromeID,
                                forceRefresh,
                                existingSongIds,
                                songInsertMutex
                            ) { newCount, updatedCount ->
                                newSongsCount.addAndGet(newCount)
                                updatedSongsCount.addAndGet(updatedCount)
                            }

                            if (!success) {
                                failedAlbumsMutex.withLock {
                                    failedAlbums.add(album.navidromeID)
                                }
                            }

                            val current = processedCount.incrementAndGet()

                            // Update progress periodically
                            if (current % 5 == 0 || current == totalAlbums) {
                                _syncState.value = _syncState.value.copy(
                                    current = current,
                                    newSongs = newSongsCount.get(),
                                    updatedSongs = updatedSongsCount.get()
                                )
                                _syncProgress.value = "Syncing songs... ($current/$totalAlbums albums)"
                            }
                        }
                    }
                }

                jobs.awaitAll()
            }

            // Check for pause/cancel after parallel section
            if (shouldPauseOrCancel()) {
                pausedAlbumIndex = processedCount.get()
                return handlePauseOrCancel(SyncPhase.SONGS)
            }

            if (failedAlbums.isNotEmpty()) {
                Log.w("SyncRepository", "Failed to sync ${failedAlbums.size} albums after retries")
            }

            // Clear cached album IDs after sync to free memory
            if (!shouldPauseOrCancel()) {
                cachedAlbumIds = emptyList()
            }

            val finalNewCount = newSongsCount.get()
            val finalUpdatedCount = updatedSongsCount.get()
            val totalInDb = songDao.getCount()

            syncMetadataDao.upsert(
                SyncMetadata(
                    key = SyncMetadata.KEY_SONGS,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    itemCount = totalInDb
                )
            )
            Log.d("SyncRepository", "Delta sync complete: $finalNewCount new, $finalUpdatedCount updated, $totalInDb total in DB")
            return false
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to sync songs", e)
            return false // Error occurred, don't preserve state
        }
    }

    private suspend fun syncAlbumSongsWithDelta(
        albumId: String,
        forceRefresh: Boolean,
        existingSongIds: Set<String>,
        insertMutex: Mutex,
        onSongCounts: (newCount: Int, updatedCount: Int) -> Unit
    ): Boolean {
        var lastException: Exception? = null
        val maxRetries = 2

        repeat(maxRetries + 1) { attempt ->
            try {
                val albumSongs = navidromeDataSource.getNavidromeAlbum(
                    albumId = albumId,
                    ignoreCachedResponse = forceRefresh || attempt > 0
                )
                val songs = albumSongs?.drop(1)?.map { it.toSong() } ?: emptyList()

                if (songs.isNotEmpty()) {
                    val entities = songs.map { it.toEntity() }

                    // Separate new songs from updates
                    val (newSongs, existingSongs) = entities.partition { it.navidromeID !in existingSongIds }

                    // Only insert if we have songs to add/update
                    if (entities.isNotEmpty()) {
                        insertMutex.withLock {
                            // Only insert new songs to avoid unnecessary DB writes
                            if (newSongs.isNotEmpty()) {
                                songDao.insertAll(newSongs)
                            }
                            // For existing songs, only update if forceRefresh
                            if (forceRefresh && existingSongs.isNotEmpty()) {
                                songDao.insertAll(existingSongs)
                            }
                        }
                    }

                    onSongCounts(newSongs.size, if (forceRefresh) existingSongs.size else 0)
                }
                return true
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(50L * (1 shl attempt))
                }
            }
        }

        Log.e("SyncRepository", "Failed to sync album $albumId after $maxRetries retries", lastException)
        return false
    }

    private suspend fun syncAlbumSongsParallel(
        albumId: String,
        forceRefresh: Boolean,
        insertMutex: Mutex,
        onSongCount: (Int) -> Unit
    ): Boolean {
        var lastException: Exception? = null
        val maxRetries = 2

        repeat(maxRetries + 1) { attempt ->
            try {
                val albumSongs = navidromeDataSource.getNavidromeAlbum(
                    albumId = albumId,
                    ignoreCachedResponse = forceRefresh || attempt > 0
                )
                val songs = albumSongs?.drop(1)?.map { it.toSong() } ?: emptyList()

                if (songs.isNotEmpty()) {
                    val entities = songs.map { it.toEntity() }
                    insertMutex.withLock {
                        songDao.insertAll(entities)
                    }
                    onSongCount(entities.size)
                }
                return true
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(50L * (1 shl attempt))
                }
            }
        }

        Log.e("SyncRepository", "Failed to sync album $albumId after $maxRetries retries", lastException)
        return false
    }

    private suspend fun syncAlbumWithRetry(
        albumId: String,
        forceRefresh: Boolean,
        maxRetries: Int,
        onSuccess: suspend (List<MediaData.Song>) -> Unit
    ): Boolean {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                val albumSongs = navidromeDataSource.getNavidromeAlbum(
                    albumId = albumId,
                    ignoreCachedResponse = forceRefresh || attempt > 0
                )
                val songs = albumSongs?.drop(1)?.map { it.toSong() } ?: emptyList()
                onSuccess(songs)
                return true
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    // Exponential backoff: 100ms, 200ms, 400ms...
                    kotlinx.coroutines.delay(100L * (1 shl attempt))
                }
            }
        }

        Log.e("SyncRepository", "Failed to sync album $albumId after $maxRetries retries", lastException)
        return false
    }

    /**
     * Caches artwork palettes.
     * @return true if paused (state should be preserved), false otherwise
     */
    private suspend fun cacheArtworks(): Boolean {
        try {
            val albums = albumDao.getAllAlbumsOnce()
            val total = albums.size
            val processed = AtomicInteger(0)

            _syncState.value = SyncState(
                phase = SyncPhase.ARTWORKS,
                current = 0,
                total = total
            )
            _syncProgress.value = "Generating artwork palettes..."

            // Parallel processing
            val concurrency = 4
            val semaphore = Semaphore(concurrency)

            coroutineScope {
                albums.map { album ->
                    async {
                        if (shouldPauseOrCancel()) return@async

                        semaphore.withPermit {
                            if (shouldPauseOrCancel()) return@withPermit

                            album.coverArt?.let { url ->
                                // Only process if not null
                                try {
                                    paletteManager.getPaletteColors(url)
                                } catch (e: Exception) {
                                    Log.e("SyncRepository", "Failed to generate palette for ${album.title}", e)
                                }
                            }

                            val current = processed.incrementAndGet()
                            if (current % 10 == 0 || current == total) {
                                _syncState.value = _syncState.value.copy(current = current)
                                _syncProgress.value = "Generating artwork palettes ($current/$total)"
                            }
                        }
                    }
                }.awaitAll()
            }

            if (shouldPauseOrCancel()) {
                return handlePauseOrCancel(SyncPhase.ARTWORKS)
            }

            return false
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to cache artworks", e)
            return false // Error occurred, don't preserve state
        }
    }

    suspend fun getLastSyncTime(key: String): Long? = withContext(Dispatchers.IO) {
        syncMetadataDao.getLastSyncTime(key)
    }

    private val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    suspend fun shouldSyncToday(): Boolean = withContext(Dispatchers.IO) {
        val lastSync = syncMetadataDao.getLastSyncTime(SyncMetadata.KEY_LAST_FULL_SYNC)
        if (lastSync == null) return@withContext true
        val timeSinceLastSync = System.currentTimeMillis() - lastSync
        timeSinceLastSync >= ONE_DAY_MS
    }

    private suspend fun recordFullSyncTime() {
        syncMetadataDao.upsert(
            SyncMetadata(
                key = SyncMetadata.KEY_LAST_FULL_SYNC,
                lastSyncTimestamp = System.currentTimeMillis(),
                itemCount = 0
            )
        )
    }

    // Flag to prevent auto-sync after clearing cache
    @Volatile
    private var justCleared = false

    fun wasJustCleared(): Boolean {
        val result = justCleared
        justCleared = false
        return result
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        justCleared = true
        // Use transaction to ensure all-or-nothing deletion
        database.withTransaction {
            songDao.deleteAll()
            albumDao.deleteAll()
            artistDao.deleteAll()
            syncMetadataDao.deleteAll()
            albumPaletteDao.clear()
        }
        // Notify all observers that data has changed
        DataRefreshManager.notifyDataSourcesChanged()
    }
}
