package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id

private val AlbumCardWidth = 128.dp
private const val ArtworkOversample = 2f
private const val MinArtworkSizePx = 300
private const val MaxArtworkSizePx = 1024

@Composable
fun TvAlbumCard(
    album: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val albumTitle = album.mediaMetadata.title?.toString() ?: ""
    val artistName = album.mediaMetadata.artist?.toString() ?: ""
    val density = LocalDensity.current
    val artworkSize = remember(density) {
        with(density) { (AlbumCardWidth * ArtworkOversample).roundToPx() }
            .coerceIn(MinArtworkSizePx, MaxArtworkSizePx)
    }
    val albumId = album.mediaMetadata.id ?: album.mediaId
    val imageId = album.mediaMetadata.extras?.getString("imageId")
    val provider = album.mediaMetadata.getProvider()
    val coverArtUrl = if (provider != null && !imageId.isNullOrBlank()) {
        provider.getImageUrl(imageId, LibraryType.ALBUM, artworkSize)
            .takeIf { it.isNotBlank() }
            ?: album.mediaMetadata.artworkUri
    } else {
        album.mediaMetadata.artworkUri
    }
    val artworkCacheKey = "$albumId@$artworkSize"

    StandardCardContainer(
        modifier = modifier.width(AlbumCardWidth),
        imageCard = {
            Card(
                onClick = onClick, interactionSource = it, content = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverArtUrl)
                            .placeholderMemoryCacheKey(artworkCacheKey)
                            .diskCacheKey(artworkCacheKey)
                            .memoryCacheKey(artworkCacheKey)
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