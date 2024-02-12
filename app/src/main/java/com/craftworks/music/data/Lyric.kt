package com.craftworks.music.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

var SyncedLyric = mutableStateListOf<Lyric>()
var PlainLyrics by mutableStateOf("Getting Lyrics...")
var LyricStatus by mutableIntStateOf(200)

data class Lyric(
    val timestamp: Int,
    val content: String,
    var isCurrentLyric: Boolean)