package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Radio (
    val name: String,
    val media: Uri? = null,
    val homepageUrl: String = "",
    val imageUrl: Uri,
    val navidromeID: String? = "",
)

var radioList:SnapshotStateList<MediaData.Radio> = mutableStateListOf()