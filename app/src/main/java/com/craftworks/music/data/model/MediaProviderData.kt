package com.craftworks.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaProviderData (var libraries: List<Pair<MusicFolder, Boolean>>)