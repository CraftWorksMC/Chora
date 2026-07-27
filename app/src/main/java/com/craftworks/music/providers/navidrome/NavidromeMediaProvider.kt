package com.craftworks.music.providers.navidrome

import com.craftworks.music.R
import com.craftworks.music.providers.subsonic.SubsonicMediaProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("navidrome")
class NavidromeMediaProvider() : SubsonicMediaProvider() {
    override val providerIcon: Int
        get() = R.drawable.s_m_navidrome
    override val providerName: Int
        get() = R.string.Source_Navidrome
}