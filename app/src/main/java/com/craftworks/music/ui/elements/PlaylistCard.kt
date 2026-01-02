package com.craftworks.music.ui.elements

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.managers.settings.ArtworkSettingsManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(playlist: MediaItem,
                 onClick: () -> Unit,
                 onDeletePlaylist: (String) -> Unit) {
    val metadata = playlist.mediaMetadata
    val context = LocalContext.current
    // Cache the settings manager to avoid creating new instance on each recomposition
    val artworkSettingsManager = remember(context) { ArtworkSettingsManager(context) }
    val generatedArtworkEnabled by artworkSettingsManager.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)

    val imageSize = with(LocalDensity.current) { 256.dp.toPx().toInt() }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    if (playlist.mediaMetadata.extras?.getString("navidromeID") != "favourites") {
                        onDeletePlaylist(playlist.mediaMetadata.extras?.getString("navidromeID") ?: "")
                    }
                },
                onLongClickLabel = "Delete Playlist"
            )
            .widthIn(min = 128.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val hasArtwork = metadata.artworkUri != null || metadata.artworkData != null

        if (hasArtwork) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(
                        if (metadata.extras?.getString("navidromeID")?.startsWith("Local") == true)
                            metadata.artworkData else
                            metadata.artworkUri
                    )
                    .size(imageSize)
                    .crossfade(true)
                    .diskCacheKey(metadata.extras?.getString("navidromeID") ?: playlist.mediaId)
                    .memoryCacheKey(metadata.extras?.getString("navidromeID") ?: playlist.mediaId)
                    .build(),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Playlist Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                error = {
                    if (generatedArtworkEnabled) {
                        GeneratedAlbumArtStatic(
                            title = metadata.title?.toString() ?: "?",
                            artist = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            )
        } else {
            if (generatedArtworkEnabled) {
                GeneratedAlbumArtStatic(
                    title = metadata.title?.toString() ?: "?",
                    artist = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Text(
            text = metadata.title.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}