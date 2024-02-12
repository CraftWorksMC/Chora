package com.craftworks.music.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

var SyncedLyric = mutableStateListOf(Lyric(0, "Getting Lyrics... \n No Lyrics Found", false))
var PlainLyrics by mutableStateOf("Getting Lyrics... \n No Lyrics Found")

data class Lyric(
    val timestamp: Int,
    val content: String,
    var isCurrentLyric: Boolean)