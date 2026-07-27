package com.craftworks.music.data.repository

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.datasource.lrclib.LrclibDataSource
import com.craftworks.music.data.datasource.netease.NeteaseDataSource
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

object LyricsState {
    val lyrics = MutableStateFlow<List<Lyric>>(emptyList())
    val loading = MutableStateFlow(false)
    var open = mutableStateOf(false)
    var useLrcLib by mutableStateOf(true)
    var useNetEase by mutableStateOf(false)
}

@Singleton
class LyricsRepository @Inject constructor(
    val lrclibDataSource: LrclibDataSource,
    val neteaseDataSource: NeteaseDataSource
) {
    private var lyricsFetchJob: Job? = null

    suspend fun getLyrics(metadata: MediaMetadata?, ignoreCachedResponse: Boolean = false) {
        // Try getting lyrics through the media provider, first synced then plain.
        // If that fails, try LRCLIB.net or NetEase.
        // If we turned them off, or we cannot find lyrics, then return an empty list

        if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
            LyricsState.lyrics.value = listOf()
            return
        }

        lyricsFetchJob?.cancel()

        coroutineScope {
            lyricsFetchJob = launch {
                LyricsState.loading.value = true;

                coroutineScope {
                    val providerDeferred = async {
                        metadata?.id?.let { metadata.getProvider()?.getLyrics(it) }
                    }

                    val lrcLibDeferred = async {
                        if (LyricsState.useLrcLib) lrclibDataSource.getLrcLibLyrics(
                            metadata,
                            ignoreCachedResponse
                        ) else null
                    }

                    val netEaseDeferred = async {
                        if (LyricsState.useNetEase) neteaseDataSource.getNeteaseLyrics(metadata) else null
                    }

                    val provider = providerDeferred.await().orEmpty()
                    val lrcLib = lrcLibDeferred.await().orEmpty()
                    val netEase = netEaseDeferred.await().orEmpty()

                    val providerWordSynced = provider.firstOrNull { it.wordSynced }
                    if (providerWordSynced != null) {
                        Log.d("LYRICS", "Using provider word synced lyrics")
                        LyricsState.lyrics.value = providerWordSynced.lines
                        LyricsState.loading.value = false
                        return@coroutineScope
                    }

                    val providerSynced = provider.firstOrNull { it.synced }
                    if (providerSynced != null) {
                        Log.d("LYRICS", "Using provider synced lyrics")
                        LyricsState.lyrics.value = providerSynced.lines
                        LyricsState.loading.value = false
                        return@coroutineScope
                    }

                    if (lrcLib.size > 1) {
                        Log.d("LYRICS", "Using LRCLIB Synced Lyrics")
                        LyricsState.lyrics.value = lrcLib
                        LyricsState.loading.value = false
                        return@coroutineScope
                    }

                    if (netEase.size > 1) {
                        Log.d("LYRICS", "Using NetEase Synced Lyrics")
                        LyricsState.lyrics.value = netEase
                        LyricsState.loading.value = false
                        return@coroutineScope
                    }

                    // fallback to plain lyrics
                    val providerPlain = provider.firstOrNull { it.lines.isNotEmpty() }
                    when {
                        providerPlain != null -> {
                            Log.d("LYRICS", "Using provider Plain Lyrics")
                            LyricsState.lyrics.value = providerPlain.lines
                        }

                        lrcLib.isNotEmpty() -> {
                            Log.d("LYRICS", "Using LRCLIB Plain Lyrics")
                            LyricsState.lyrics.value = lrcLib
                        }

                        netEase.isNotEmpty() -> {
                            Log.d("LYRICS", "Using NetEase Plain Lyrics")
                            LyricsState.lyrics.value = netEase
                        }

                        else -> {
                            Log.d("LYRICS", "No lyrics found.")
                            LyricsState.lyrics.value = listOf()
                        }
                    }

                    LyricsState.loading.value = false
                }
            }
        }
    }
}