package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.tv.material3.Card
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.craftworks.music.R
import com.craftworks.music.data.model.id

@Composable
fun TvAlbumCard(
    album: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val albumTitle = album.mediaMetadata.title?.toString() ?: ""
    val artistName = album.mediaMetadata.artist?.toString() ?: ""
    val coverArtUrl = album.mediaMetadata.artworkUri
    StandardCardContainer(
        modifier = modifier.width(128.dp),
        imageCard = {
            Card(
                onClick = onClick, interactionSource = it, content = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverArtUrl)
                            .placeholderMemoryCacheKey(album.mediaMetadata.id ?: album.mediaId)
                            .diskCacheKey(album.mediaMetadata.id ?: album.mediaId)
                            .memoryCacheKey(album.mediaMetadata.id ?: album.mediaId)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        fallback = painterResource(R.drawable.placeholder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().aspectRatio(1f)
                    )
                }
            )
        },
        title = {
            Text(
                text = albumTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        subtitle = {
            Text(
                text = artistName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}