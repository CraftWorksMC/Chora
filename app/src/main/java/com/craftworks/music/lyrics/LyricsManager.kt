package com.craftworks.music.lyrics

import com.craftworks.music.data.Lyric
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromePlainLyrics
import com.craftworks.music.providers.navidrome.getNavidromeSyncedLyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private var isGetLyricsRunning = false

object LyricsManager {
    private val _Lyrics = MutableStateFlow(emptyList<Lyric>())
    val Lyrics: StateFlow<List<Lyric>> = _Lyrics.asStateFlow()

    val useLrcLib: Boolean = true

    suspend fun getLyrics() {
        // Try getting lyrics through navidrome, first synced then plain.
        // If that fails, try LRCLIB.net.
        // If we turned it off or we cannot find lyrics, then return "No Lyrics Found"

        if (NavidromeManager.checkActiveServers()) {
            getNavidromeSyncedLyrics().takeIf { it.isNotEmpty() }?.let {
                _Lyrics.value = it
                return
            }

            getNavidromePlainLyrics().takeIf { it.first().content.isNotEmpty() }?.let {
                _Lyrics.value = it
                return
            }
        }

        if (useLrcLib) {
            getLrcLibLyrics().takeIf { it.isNotEmpty() }?.let {
                _Lyrics.value = it
                return
            }
        }

        _Lyrics.value = listOf(Lyric(-1, "No Lyrics Found"))
    }
}