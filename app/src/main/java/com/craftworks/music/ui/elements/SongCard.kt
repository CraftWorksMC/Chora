package com.craftworks.music.ui.elements

import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.radioList
import com.craftworks.music.data.toSong
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
import com.craftworks.music.providers.navidrome.setNavidromeStar
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import com.craftworks.music.ui.elements.dialogs.songToAddToPlaylist
import com.craftworks.music.ui.screens.selectedRadioIndex
import com.craftworks.music.ui.screens.showRadioModifyDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Stable
@Composable
fun SongsCard(song: MediaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(12.dp, 48.dp, 0.dp, 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (song.mediaMetadata.extras?.getBoolean("isRadio") == false) return@combinedClickable
                    showRadioModifyDialog.value = true
                    selectedRadioIndex.intValue =
                        radioList.indexOf(radioList.firstOrNull { it.name == song.mediaMetadata.artist && it.media == song.mediaId })
                },
                onLongClickLabel = "Modify Radio"
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .width(128.dp)
                .height(172.dp)
        ) {
            SubcomposeAsyncImage(
                model = song.mediaMetadata.artworkUri,
                contentScale = ContentScale.FillHeight,
                contentDescription = "Album Image",
                modifier = Modifier
                    .height(128.dp)
                    .width(128.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = song.mediaMetadata.title.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = song.mediaMetadata.artist.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizontalSongCard(
    song: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

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
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.mediaMetadata.artworkUri)
                    .crossfade(true)
                    .size(64)
                    .build(),
                contentDescription = "Album Image",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp, 0.dp, 0.dp, 0.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                Modifier
                    .padding(end = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = song.mediaMetadata.title.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = song.mediaMetadata.artist.toString() + if (song.mediaMetadata.recordingYear != 0) " • " + song.mediaMetadata.recordingYear else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
            val formattedDuration by remember(song.mediaMetadata.durationMs) {
                derivedStateOf {
                    formatMilliseconds((song.mediaMetadata.durationMs?.div(1000))?.toInt() ?: 0)
                }
            }
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
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
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        disabledContentColor = MaterialTheme.colorScheme.onBackground
                    )
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
                            Text(
                                stringResource(R.string.Dialog_Add_To_Playlist).replace(
                                    "/ ",
                                    ""
                                )
                            )
                        },
                        onClick = {
                            println("Add Song To Playlist")
                            showAddSongToPlaylistDialog.value = true
                            songToAddToPlaylist.value = song.toSong()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add To Playlist Icon"
                            )
                        }
                    )
                    DropdownMenuItem(
                        enabled = !song.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_"),
                        text = {
                            Text(stringResource(R.string.Action_Download))
                        },
                        onClick = {
                            coroutineScope.launch {
                                downloadNavidromeSong(context, song.toSong())
                            }
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                                contentDescription = "Download Icon"
                            )
                        }
                    )
                    DropdownMenuItem(
                        enabled = !song.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_"),
                        text = {
                            Text(if (song.mediaMetadata.extras?.getString("starred").isNullOrEmpty()) "Star" else "Unstar")
                        },
                        onClick = {
                            coroutineScope.launch {
                                setNavidromeStar(
                                    !song.mediaMetadata.extras?.getString("starred").isNullOrEmpty(),
                                    song.mediaMetadata.extras?.getString("navidromeID")!!
                                )
                            }
                            song.mediaMetadata.buildUpon().setExtras(
                                Bundle().apply {
                                    putString("starred", System.currentTimeMillis().toString())
                                }
                            ).build()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (song.mediaMetadata.extras?.getString("starred").isNullOrEmpty())
                                        R.drawable.round_favorite_border_24
                                    else
                                        R.drawable.round_favorite_24
                                ),
                                contentDescription = "Star/Unstar Icon"
                            )
                        }
                    )
                }
            }
        }
    }
}