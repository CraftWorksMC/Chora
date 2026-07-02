package com.craftworks.music.ui.elements.dialogs.tv

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideCardContainer
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.ui.screens.tv.settings.SettingsSwitchItem
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import kotlinx.coroutines.launch

private enum class DialogMenu { MAIN, ADD_TO_PLAYLIST, NEW_PLAYLIST }

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun SongDialog(
    song: MediaItem = MediaItem.EMPTY,
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    var dialogMenu by remember { mutableStateOf(DialogMenu.MAIN) }

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WideCardContainer(
                    modifier = Modifier
                        .fillMaxWidth(),
                    imageCard = {
                        Card(
                            onClick = { },
                            content = {
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .focusable(false),
                                )
                            }
                        )
                    },
                    title = {
                        Text(
                            text = song.mediaMetadata.title.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    subtitle = {
                        Text(
                            text = song.mediaMetadata.artist.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                )

                when (dialogMenu) {
                    DialogMenu.MAIN -> {
                        ListItem(
                            selected = false,
                            headlineContent = { Text(stringResource(R.string.Action_Download)) },
                            onClick = {
                                coroutineScope.launch {
                                    TODO("Download song")
                                    //downloadNavidromeSong(context, song.mediaMetadata)

                                    setShowDialog(false)
                                }
                            }
                        )

                        ListItem(
                            selected = false,
                            headlineContent = {
                                Text(
                                    stringResource(R.string.Dialog_Add_To_Playlist)
                                        .replace("/", song.mediaMetadata.title.toString())
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    dialogMenu = DialogMenu.ADD_TO_PLAYLIST
                                }
                            }
                        )
                    }
                    DialogMenu.ADD_TO_PLAYLIST -> {
                        AddSongToPlaylist(
                            song = song,
                            setShowDialog = { setShowDialog(it) },
                            setDialogMenu = { dialogMenu = it }
                        )
                    }
                    DialogMenu.NEW_PLAYLIST -> {
                        NewPlaylist(
                            song = song,
                            setDialogMenu = { dialogMenu = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSongToPlaylist(
    song: MediaItem,
    setShowDialog: (Boolean) -> Unit = { },
    setDialogMenu: (DialogMenu) -> Unit = { },
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        playlists.forEach { playlist ->
            WideCardContainer(
                modifier = Modifier
                    .fillMaxWidth(),
                imageCard = {
                    Card(
                        onClick = {
                            viewModel.addSongsToPlaylist(
                                playlist.mediaMetadata.extras?.getString("id") ?: "",
                                listOf(song.mediaMetadata.extras?.getString("id") ?: "")
                            )
                            setShowDialog(false)
                        },
                        interactionSource = null,
                        content = {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(playlist.mediaMetadata.artworkUri)
                                    .crossfade(true)
                                    .size(64)
                                    .diskCacheKey(
                                        playlist.mediaMetadata.extras?.getString("navidromeID") ?: playlist.mediaId
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
                    Text(
                        text = playlist.mediaMetadata.title.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            )
        }

        WideCardContainer(
            modifier = Modifier
                .fillMaxWidth(),
            imageCard = {
                Card(
                    onClick = {
                        setDialogMenu(DialogMenu.NEW_PLAYLIST)
                    },
                    interactionSource = null,
                    content = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.rounded_add_24),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.Dialog_New_Playlist),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        )
    }
}

@Composable
private fun NewPlaylist(
    song: MediaItem = MediaItem.EMPTY,
    setDialogMenu: (DialogMenu) -> Unit = { },
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var playlistName by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorTextColor = MaterialTheme.colorScheme.error
    )

    Column(
        modifier = Modifier
            .widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            label = {
                Text(
                    text = stringResource(R.string.Label_Playlist_Name),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    viewModel.createPlaylist(playlistName, listOf(song.mediaMetadata.extras?.getString("id") ?: ""), context)
                    setDialogMenu(DialogMenu.MAIN)
                }
            ),
            colors = textFieldColors,
        )

        ListItem(
            selected = false,
            headlineContent = { Text(stringResource(R.string.Action_Add)) },
            onClick = {
                viewModel.createPlaylist(playlistName, listOf(song.mediaMetadata.extras?.getString("id") ?: ""), context)
                setDialogMenu(DialogMenu.MAIN)
            }
        )
    }
}