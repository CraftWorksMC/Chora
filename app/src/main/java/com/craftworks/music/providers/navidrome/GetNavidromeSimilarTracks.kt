package com.craftworks.music.providers.navidrome

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable

fun parseNavidromeSimilarSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<MediaItem> {
    val subsonicResponse = parseSubsonicResponse(response)

    val passwordSaltMedia = NavidromeDataSource.generateSalt(8)
    val passwordHashMedia = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltMedia)

    val passwordSaltArt = NavidromeDataSource.generateSalt(8)
    val passwordHashArt = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltArt)

    val baseCoverArtUrl = "$navidromeUrl/rest/getCoverArt.view?u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora&size=128"
    subsonicResponse.sonicMatch?.forEach {
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"
    }

    val mediaDataSimilarSongs = subsonicResponse.sonicMatch?.map {
        val mediaItem = it.toMediaItem()
        mediaItem.buildUpon().setMediaMetadata(
            mediaItem.mediaMetadata.buildUpon()
                .setArtworkUri("$baseCoverArtUrl&id=${it.navidromeID}".toUri())
                .build()
        ).build()
    } ?: emptyList()

    return mediaDataSimilarSongs
}