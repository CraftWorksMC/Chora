package com.craftworks.music.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media.utils.MediaConstants.METADATA_KEY_IS_EXPLICIT
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.StarRating
import androidx.media3.session.MediaController
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.AddToPlaylist
import com.craftworks.music.ui.elements.dialogs.RatingDialog
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import com.craftworks.music.utils.StringUtils
import kotlinx.coroutines.launch

@Composable
fun HorizontalSongCard(
    song: MediaItem,
    modifier: Modifier = Modifier,
    showTrackNumber: Boolean = false,
    onClick: () -> Unit,
    extraMenuItems: @Composable (onDismiss: () -> Unit) -> Unit = {},
    viewModel: SongsScreenViewModel = hiltViewModel(),
    mediaController: MediaController?
) {
    val context = LocalContext.current

    var showAddSongToPlaylistDialog by remember { mutableStateOf(false) }
    var showSongRatingDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .height(72.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (showTrackNumber) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(8.dp, 0.dp, 0.dp, 0.dp),
                    //.clip(CircleShape)
                    //.background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = song.mediaMetadata.trackNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song.mediaMetadata.getProvider()?.getImageUrl(
                            id = song.mediaMetadata.id ?: "",
                            itemType = LibraryType.SONG,
                            size = 128
                        ))
                        .crossfade(true)
                        .diskCacheKey(song.mediaMetadata.id)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .placeholderMemoryCacheKey(song.mediaMetadata.id)
                        .build(),
                    contentDescription = "Album Image",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp, 0.dp, 0.dp, 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                Modifier
                    .padding(end = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.mediaMetadata.title.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    if (song.mediaMetadata.extras?.getBoolean(METADATA_KEY_IS_EXPLICIT) == true)
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_explicit_24),
                            contentDescription = "Explicit"
                        )
                }

                Text(
                    text = song.mediaMetadata.artist.toString() + if (song.mediaMetadata.recordingYear != 0) " • " + song.mediaMetadata.recordingYear else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                if (song.mediaMetadata.userRating != null) {
                    Row {
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
            }
            val formattedDuration by remember(song.mediaMetadata.durationMs) {
                derivedStateOf {
                    StringUtils.formatSeconds((song.mediaMetadata.durationMs?.div(1000))?.toInt() ?: 0)
                }
            }
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = 12.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )

            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.width(48.dp)
            ) {
                IconButton(
                    modifier = Modifier,
                    onClick = { expanded = true },
//                    colors = IconButtonDefaults.iconButtonColors(
//                        containerColor = MaterialTheme.colorScheme.background,
//                        contentColor = MaterialTheme.colorScheme.primary,
//                        disabledContainerColor = MaterialTheme.colorScheme.background,
//                        disabledContentColor = MaterialTheme.colorScheme.onBackground
//                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "More menu"
                    )
                }

                val coroutineScope = rememberCoroutineScope()
                DropdownMenu(
                    modifier = Modifier,
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_set_rating))
                        },
                        onClick = {
                            showSongRatingDialog = true
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_star_outline_24),
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_add_to_queue))
                        },
                        onClick = {
                            SongHelper.enqueue(listOf(song), mediaController)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.outline_queue_add_24),
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_play_next))
                        },
                        onClick = {
                            SongHelper.playNext(listOf(song), mediaController)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.play_next_24px),
                                contentDescription = null
                            )
                        }
                    )
                    if (song.mediaMetadata.getProvider()?.featureFlags?.contains(ProviderFeature.PLAYLISTS) ?: false) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.action_add_to_playlist))
                            },
                            onClick = {
                                println("Add Song To Playlist")
                                showAddSongToPlaylistDialog = true
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    if (song.mediaMetadata.getProvider()?.featureFlags?.contains(ProviderFeature.DOWNLOADS) ?: false) {
                        DropdownMenuItem(
                            enabled = (song.mediaMetadata.getProvider()?.featureFlags?.contains(
                                ProviderFeature.DOWNLOADS)?:false),
                            text = {
                                Text(stringResource(R.string.action_download))
                            },
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.downloadSong(song)
                                }
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    extraMenuItems { expanded = false }
                }
            }
        }
    }

    if (showAddSongToPlaylistDialog) {
        AddToPlaylist(
            onDismissRequest = {showAddSongToPlaylistDialog = false},
            mediaToAddToPlaylist = listOf(song)
        )
    }
    if (showSongRatingDialog) {
        RatingDialog(
            currentRating = (song.mediaMetadata.userRating as? StarRating)?.starRating?.toInt() ?: 0,
            onDismiss = { showSongRatingDialog = false },
            onSetRating = { rating ->
                viewModel.setSongRating(song.mediaMetadata.id ?: "", rating)
            }
        )
    }
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
fun PreviewHorizontalSongCard() {
    HorizontalSongCard(
        song = MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Lololol")
                    .build()
            ).build(),
        onClick = {},
        mediaController = null
    )
}