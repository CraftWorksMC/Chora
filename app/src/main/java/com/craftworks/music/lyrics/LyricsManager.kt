package com.craftworks.music.lyrics

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromePlainLyrics
import com.craftworks.music.providers.navidrome.getNavidromeSyncedLyrics
import com.craftworks.music.ui.playing.lyricsOpen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LyricsManager {
    private val _Lyrics = MutableStateFlow(listOf<Lyric>())
    val Lyrics: StateFlow<List<Lyric>> = _Lyrics.asStateFlow()

    var useLrcLib by mutableStateOf(true)

    suspend fun getLyrics(metadata: MediaMetadata?) {
        // Try getting lyrics through navidrome, first synced then plain.
        // If that fails, try LRCLIB.net.
        // If we turned it off or we cannot find lyrics, then return an empty list

        if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
            _Lyrics.value = listOf()
            return
        }

        var foundNavidromePlainLyrics by mutableStateOf(false)

        if (NavidromeManager.checkActiveServers()) {
            getNavidromeSyncedLyrics(metadata?.extras?.getString("navidromeID") ?: "").takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true
                else {
                    Log.d("LYRICS", "Got Navidrome synced lyrics.")
                    _Lyrics.value = it
                    return
                }
            }

            getNavidromePlainLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true

                Log.d("LYRICS", "Got Navidrome plain lyrics.")
                _Lyrics.value = it
            }
        }

        if (useLrcLib) {
            if (foundNavidromePlainLyrics) {
                Log.d("LYRICS", "Got Navidrome plain lyrics, trying LRCLIB.")
                getLrcLibLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                    if (it.size != 1) _Lyrics.value = it
                    return
                }
            }

            getLrcLibLyrics(metadata).takeIf { it.isNotEmpty() }?.let {
                Log.d("LYRICS", "Got LRCLIB lyrics.")
                _Lyrics.value = it
                return
            }
        }

        Log.d("LYRICS", "Didn't find any lyrics.")
        // Hide lyrics panel if we cannot find lyrics.
        lyricsOpen = false
        _Lyrics.value = listOf()
    }
}