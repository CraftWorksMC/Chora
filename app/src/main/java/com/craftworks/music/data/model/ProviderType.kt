package com.craftworks.music.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
@Parcelize
value class ProviderType(val id: Int) : Parcelable {
    companion object {
        val LOCAL_FOLDER = ProviderType(0)
        val SUBSONIC     = ProviderType(1)
        val NAVIDROME    = ProviderType(2)
    }
}