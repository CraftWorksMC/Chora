package com.craftworks.music.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.data.model.SyncType
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import com.craftworks.music.data.providers.lyrics.binimum.BiniLyricsDataSource
import com.craftworks.music.data.providers.lyrics.lrclib.LrclibDataSource
import com.craftworks.music.data.providers.lyrics.netease.NeteaseDataSource
import com.craftworks.music.data.providers.lyrics.unison.UnisonLyricsDataSource
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

object LyricsState {
    val lyrics = MutableStateFlow<Lyrics?>(null)
    val loading = MutableStateFlow(false)
    var open = mutableStateOf(false)
    var useLrcLib by mutableStateOf(true)
    var useBiniLyrics by mutableStateOf(false)
    var useNetEase by mutableStateOf(false)
}

@Singleton
class LyricsRepository @Inject constructor(
    val mediaProviderSettingsManager: MediaProviderSettingsManager,
    val lrclibDataSource: LrclibDataSource,
    val biniLyricsDataSource: BiniLyricsDataSource,
    val unisonLyricsDataSource: UnisonLyricsDataSource,
    val neteaseDataSource: NeteaseDataSource
) {
    private var lyricsFetchJob: Job? = null

    suspend fun getLyrics(metadata: MediaMetadata?, ignoreCachedResponse: Boolean = false) {
        if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
            LyricsState.lyrics.value = null
            return
        }

        val providers = mediaProviderSettingsManager.lyricProvidersFlow.first().filter { it.enabled }

        lyricsFetchJob?.cancel()

        coroutineScope {
            lyricsFetchJob = launch {
                LyricsState.loading.value = true;

                coroutineScope {
                    val results = providers.map {
                        async {
                            when (it.source) {
                                LyricSource.MEDIA_PROVIDER -> metadata?.id?.let { metadata.getProvider()?.getLyrics(it) }?.firstOrNull()
                                LyricSource.LRCLIB -> lrclibDataSource.getLyrics(metadata, ignoreCachedResponse)
                                LyricSource.BINI_LYRICS -> biniLyricsDataSource.getLyrics(metadata, ignoreCachedResponse)
                                LyricSource.UNISON -> unisonLyricsDataSource.getLyrics(metadata, ignoreCachedResponse)
                                LyricSource.NETEASE -> neteaseDataSource.getLyrics(metadata)
                            }
                        }
                    }.awaitAll().filterNotNull()

                    LyricsState.lyrics.value = results.firstOrNull { it.syncType == SyncType.WORD }
                        ?: results.firstOrNull { it.syncType == SyncType.LINE }
                        ?: results.firstOrNull { it.lines.isNotEmpty() }

                    LyricsState.loading.value = false
                }
            }
        }
    }
}