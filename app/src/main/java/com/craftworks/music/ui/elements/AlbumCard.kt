package com.craftworks.music.ui.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.managers.settings.ArtworkSettingsManager

import com.craftworks.music.ui.util.rememberAlbumPalette

@OptIn(ExperimentalFoundationApi::class)
@Stable
@Composable
fun AlbumCard(
    album: MediaItem,
    onClick: () -> Unit = { },
    onPlay: (album: MediaItem) -> Unit = { },
    modifier: Modifier = Modifier,
    cardWidth: Int = 128,
    isInSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onEnterSelectionMode: (() -> Unit)? = null
) {
    if (album.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_ALBUM) return
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    val artworkSettings = remember { ArtworkSettingsManager(context) }
    val generatedArtworkEnabled by artworkSettings.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by artworkSettings.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)

    // Palette colors
    val artworkUri = album.mediaMetadata.artworkUri?.toString()
    val paletteColors by rememberAlbumPalette(artworkUri)

    // Selection animations
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "selection_border"
    )

    Box {
        val cardHeight = (cardWidth * 1.34f).toInt()
        Column(
            modifier = modifier
                .padding(12.dp, 0.dp, 0.dp, 0.dp)
                .width(cardWidth.dp)
                .height(cardHeight.dp)
                .scale(selectionScale)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) Modifier.border(
                        width = 3.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ) else Modifier
                )
                .combinedClickable(
                    onClick = {
                        if (isInSelectionMode) {
                            onSelectionChange?.invoke(!isSelected)
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        if (!isInSelectionMode && onEnterSelectionMode != null) {
                            onEnterSelectionMode()
                        } else if (!isInSelectionMode) {
                            showContextMenu = true
                        }
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val hasArtwork = !artworkUri.isNullOrEmpty()

            // Check if we should use generated art based on fallback mode
            val useGeneratedArt = when {
                !generatedArtworkEnabled -> false
                fallbackMode == ArtworkSettingsManager.FallbackMode.ALWAYS -> true
                !hasArtwork -> true
                fallbackMode == ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT -> {
                    // Detect Navidrome placeholder patterns
                    artworkUri != null && (
                    artworkUri.contains("placeholder") ||
                    artworkUri.endsWith("/coverArt") ||
                    artworkUri.contains("coverArt?id=&") ||
                    artworkUri.contains("coverArt?size=") && !artworkUri.contains("id="))
                }
                else -> false
            }

            if (useGeneratedArt) {
                GeneratedAlbumArtStatic(
                    title = album.mediaMetadata.albumTitle?.toString() ?: "?",
                    artist = album.mediaMetadata.albumArtist?.toString(),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = paletteColors
                )
            } else if (hasArtwork) {
                val cacheKey = (album.mediaMetadata.extras?.getString("source") ?: "default") + "_" +
                    (album.mediaMetadata.extras?.getString("navidromeID") ?: album.mediaId)
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.mediaMetadata.artworkUri)
                        .crossfade(true)
                        .diskCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = "Album Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    error = {
                        if (generatedArtworkEnabled) {
                            GeneratedAlbumArtStatic(
                                title = album.mediaMetadata.albumTitle?.toString() ?: "?",
                                artist = album.mediaMetadata.albumArtist?.toString(),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                colors = paletteColors
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // Hide play button in selection mode
            if (!isInSelectionMode) {
                IconButton(
                    onClick = {
                        onPlay(album)
                    },
                    modifier = Modifier
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            shape = CircleShape
                        )
                        .height(36.dp)
                        .size(36.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Play Album",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Selection checkbox
            if (isInSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.mediaMetadata.albumTitle.toString(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = album.mediaMetadata.albumArtist.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Play") },
                onClick = {
                    onPlay(album)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play"
                    )
                }
            )
        }
    }
}