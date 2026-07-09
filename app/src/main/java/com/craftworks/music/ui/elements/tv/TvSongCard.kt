package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media.utils.MediaConstants.METADATA_KEY_IS_EXPLICIT
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideCardContainer
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R

@Preview(showBackground = true, device = "id:tv_1080p")
@Composable
fun TvHorizontalSongCard(
    song: MediaItem = MediaItem.EMPTY,
    modifier: Modifier = Modifier,
    showTrackNumber: Boolean = false,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { }
) {
    val context = LocalContext.current
    WideCardContainer(
        modifier = modifier,
        imageCard = {
            Card(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = it,
                content = {
                    if (showTrackNumber)
                        Box(
                            modifier = Modifier
                                .size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = song.mediaMetadata.trackNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    else
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(song.mediaMetadata.artworkUri)
                                .crossfade(true)
                                .size(64)
                                .diskCacheKey(
                                    song.mediaMetadata.extras?.getString("navidromeID") ?: song.mediaId
                                )
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                }
            )
        },
        title = {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.mediaMetadata.title.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
                if (song.mediaMetadata.extras?.getBoolean(METADATA_KEY_IS_EXPLICIT) == true)
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_explicit_24),
                        contentDescription = "Explicit"
                    )
            }
        },
        subtitle = {
            Text(
                text = song.mediaMetadata.artist.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        description = {
            if (song.mediaMetadata.userRating == null)
                return@WideCardContainer

            Row (
                modifier = Modifier.padding(start = 16.dp)
            ) {
                repeat((song.mediaMetadata.userRating as StarRating).starRating.toInt()) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
}