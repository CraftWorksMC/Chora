package com.craftworks.music.data.repository

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.datasource.lrclib.LrclibDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.datasource.netease.NeteaseDataSource
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    val neteaseDataSource: NeteaseDataSource,
    val navidromeDataSource: NavidromeDataSource
) {
    suspend fun getLyrics(metadata: MediaMetadata?) {
        // Try getting lyrics through navidrome, first synced then plain.
        // If that fails, try LRCLIB.net or NetEase.
        // If we turned them off, or we cannot find lyrics, then return an empty list

        if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
            LyricsState.lyrics.value = listOf()
            return
        }

        LyricsState.loading.value = true;

        coroutineScope {
            val isLocal = metadata?.extras?.getString("navidromeID")?.startsWith("Local_") ?: false

            val navidromeSyncedDeferred = async {
                if (NavidromeManager.checkActiveServers() && !isLocal) {
                    navidromeDataSource.getNavidromeSyncedLyrics(
                        metadata?.extras?.getString("navidromeID") ?: ""
                    )
                } else null
            }

            val navidromePlainDeferred = async {
                if (NavidromeManager.checkActiveServers() && !isLocal) {
                    navidromeDataSource.getNavidromePlainLyrics(metadata)
                } else null
            }

            val lrcLibDeferred = async {
                if (LyricsState.useLrcLib) lrclibDataSource.getLrcLibLyrics(metadata) else null
            }

            val netEaseDeferred = async {
                if (LyricsState.useNetEase) neteaseDataSource.getNeteaseLyrics(metadata) else null
            }

            val navidromeSynced = navidromeSyncedDeferred.await().orEmpty()
            val navidromePlain = navidromePlainDeferred.await().orEmpty()
            val lrcLib = lrcLibDeferred.await().orEmpty()
            val netEase = netEaseDeferred.await().orEmpty()

            if (navidromeSynced.size > 1) {
                Log.d("LYRICS", "Got Navidrome synced lyrics")
                LyricsState.lyrics.value = navidromeSynced
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
            when {
                navidromePlain.isNotEmpty() -> {
                    Log.d("LYRICS", "Using Navidrome Plain Lyrics")
                    LyricsState.lyrics.value = navidromePlain
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