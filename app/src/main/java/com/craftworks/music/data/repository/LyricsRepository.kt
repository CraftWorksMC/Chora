package com.craftworks.music.data.repository

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.datasource.lrclib.LrclibDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

object LyricsState {
    val lyrics = MutableStateFlow<List<Lyric>>(emptyList())
    val loading = MutableStateFlow<Boolean>(false)
    var open = mutableStateOf<Boolean>(false)
    var useLrcLib by mutableStateOf(true)
}

@Singleton
class LyricsRepository @Inject constructor(
    val lrclibDataSource: LrclibDataSource,
    val navidromeDataSource: NavidromeDataSource
) {
    suspend fun getLyrics(metadata: MediaMetadata?) {
        // Try getting lyrics through navidrome, first synced then plain.
        // If that fails, try LRCLIB.net.
        // If we turned it off or we cannot find lyrics, then return an empty list

        if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
            LyricsState.lyrics.value = listOf()
            //lyricsOpen = false
            return
        }

        LyricsState.loading.value = true;

        var foundNavidromePlainLyrics by mutableStateOf(false)

        if (NavidromeManager.checkActiveServers() && !(metadata?.extras?.getString("navidromeID")?.startsWith("Local_") ?: false)) {
            navidromeDataSource.getNavidromeSyncedLyrics(metadata?.extras?.getString("navidromeID") ?: "").takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true
                else {
                    Log.d("LYRICS", "Got Navidrome synced lyrics.")
                    LyricsState.lyrics.value = it
                    LyricsState.loading.value = false;
                    return
                }
            }

            navidromeDataSource.getNavidromePlainLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true

                Log.d("LYRICS", "Got Navidrome plain lyrics.")
                LyricsState.lyrics.value = it
            }
        }

        if (LyricsState.useLrcLib) {
            if (foundNavidromePlainLyrics) {
                Log.d("LYRICS", "Got Navidrome plain lyrics, trying LRCLIB.")
                lrclibDataSource.getLrcLibLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                    if (it.size != 1) LyricsState.lyrics.value = it
                    LyricsState.loading.value = false;
                    return
                }
            }

            lrclibDataSource.getLrcLibLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                Log.d("LYRICS", "Got LRCLIB lyrics.")
                LyricsState.lyrics.value = it
                LyricsState.loading.value = false;
                return
            }
        }

        LyricsState.loading.value = false;

        Log.d("LYRICS", "Didn't find any lyrics.")
        //lyricsOpen = false
        LyricsState.lyrics.value = listOf()
    }
}