package com.craftworks.music.data

import android.net.Uri

data class Radio (
    val imageUrl: Uri,
    val name: String,
    val media: Uri? = null,
    val format: String? = "MP3",
    val bitrate: String? = "320",
    val navidromeID: String? = "",
)