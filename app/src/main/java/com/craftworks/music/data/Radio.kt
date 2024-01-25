package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

data class Radio (
    val imageUrl: Uri,
    val name: String,
    val media: Uri? = null,
    val homepageUrl: String = "",
    val format: String? = "MP3",
    val bitrate: String? = "320",
    val navidromeID: String? = "",
)

var radioList:MutableList<Radio> = mutableStateListOf()