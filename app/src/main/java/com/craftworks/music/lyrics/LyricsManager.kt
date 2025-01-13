package com.craftworks.music.lyrics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.craftworks.music.data.Lyric
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromePlainLyrics
import com.craftworks.music.providers.navidrome.getNavidromeSyncedLyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LyricsManager {
    private val _Lyrics = MutableStateFlow(listOf(Lyric(-1, "No Lyrics Found")))
    val Lyrics: StateFlow<List<Lyric>> = _Lyrics.asStateFlow()

    private val useLrcLib: Boolean = true

    suspend fun getLyrics() {
        // Try getting lyrics through navidrome, first synced then plain.
        // If that fails, try LRCLIB.net.
        // If we turned it off or we cannot find lyrics, then return "No Lyrics Found"

        var foundNavidromePlainLyrics by mutableStateOf(false)

        if (NavidromeManager.checkActiveServers()) {
            getNavidromeSyncedLyrics().takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true
                else {
                    _Lyrics.value = it
                    return
                }
            }

            getNavidromePlainLyrics().takeIf { it.first().content.isNotEmpty() }?.let {
                if (it.size == 1)
                    foundNavidromePlainLyrics = true

                _Lyrics.value = it
            }
        }

        if (useLrcLib) {
            if (foundNavidromePlainLyrics) {
                getLrcLibLyrics().takeIf { it.isNotEmpty() }?.let {
                    if (it.size != 1) _Lyrics.value = it
                    return
                }
            }
            getLrcLibLyrics().takeIf { it.isNotEmpty() }?.let {
                _Lyrics.value = it
                return
            }
        }

        _Lyrics.value = listOf(Lyric(-1, "No Lyrics Found"))
    }
}