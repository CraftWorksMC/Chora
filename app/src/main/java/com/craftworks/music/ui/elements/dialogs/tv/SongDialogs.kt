package com.craftworks.music.ui.elements.dialogs.tv

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.htmlEncode
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideCardContainer
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.craftworks.music.R
import com.craftworks.music.data.model.favorite
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel

private enum class DialogMenu { MAIN, ADD_TO_PLAYLIST, NEW_PLAYLIST, SET_RATING }

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun SongDialog(
    song: MediaItem = MediaItem.EMPTY,
    onSetRating: (Int) -> Unit = { },
    onDownload: (MediaItem) -> Unit = { },
    setShowDialog: (Boolean) -> Unit = { },
    onPlayNext: (MediaItem) -> Unit = { },
    onEnqeue: (MediaItem) -> Unit = { },
    onSetFavorite: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    var dialogMenu by remember { mutableStateOf(DialogMenu.MAIN) }

    val (mainFocus, ratingFocus, playlistFocus, newPlaylistFocus) = remember { FocusRequester.createRefs() }
    LaunchedEffect(dialogMenu) {
        when (dialogMenu) {
            DialogMenu.MAIN -> mainFocus.requestFocus()
            DialogMenu.ADD_TO_PLAYLIST -> playlistFocus.requestFocus()
            DialogMenu.NEW_PLAYLIST -> newPlaylistFocus.requestFocus()
            DialogMenu.SET_RATING -> ratingFocus.requestFocus()
        }
    }

    var currentRating = (song.mediaMetadata.userRating as? StarRating)?.starRating?.toInt()?.takeIf { it > 0 } ?: 0

    var isFavorite = remember(song.mediaMetadata) { song.mediaMetadata.favorite ?: false }

    Dialog(
        onDismissRequest = {
            if (dialogMenu == DialogMenu.MAIN)
                setShowDialog(false)
            else
                dialogMenu = DialogMenu.MAIN
        },
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
                        .fillMaxWidth()
                        .focusProperties { canFocus = false },
                    imageCard = {
                        Card(
                            onClick = { },
                            modifier = Modifier,
                            content = {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(song.mediaMetadata.artworkUri)
                                        .crossfade(true)
                                        .size(64)
                                        .diskCacheKey(
                                            song.mediaMetadata.extras?.getString("id") ?: song.mediaId
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
                        Column(
                            Modifier.focusGroup().focusRestorer().focusRequester(mainFocus)
                        ) {
                            ListItem(
                                selected = false,
                                headlineContent = {
                                    Text(stringResource(R.string.action_play_next))
                                },
                                onClick = {
                                    onPlayNext(song)
                                }
                            )

                            ListItem(
                                selected = false,
                                headlineContent = {
                                    Text(stringResource(R.string.action_add_to_queue))
                                },
                                onClick = {
                                    onEnqeue(song)
                                }
                            )

                            ListItem(
                                selected = false,
                                headlineContent = {
                                    Text(stringResource(R.string.action_set_rating))
                                },
                                onClick = {
                                    dialogMenu = DialogMenu.SET_RATING
                                }
                            )

                            ListItem(
                                selected = false,
                                headlineContent = {
                                    if (isFavorite)
                                        Text(stringResource(R.string.action_remove_from_favorites))
                                    else
                                        Text(stringResource(R.string.action_add_to_favorites))
                                },
                                onClick = {
                                    onSetFavorite(!isFavorite)
                                }
                            )

                            ListItem(
                                selected = false,
                                headlineContent = { Text(stringResource(R.string.action_download)) },
                                onClick = {
                                    onDownload(song)
                                    setShowDialog(false)
                                }
                            )

                            ListItem(
                                selected = false,
                                headlineContent = {
                                    Text(
                                        text = AnnotatedString.fromHtml(
                                            stringResource(
                                                R.string.add_to_playlist_title,
                                                song.mediaMetadata.title.toString().htmlEncode()
                                            )
                                        )
                                    )
                                },
                                onClick = {
                                    dialogMenu = DialogMenu.ADD_TO_PLAYLIST
                                }
                            )
                        }
                    }
                    DialogMenu.ADD_TO_PLAYLIST -> {
                        Box(
                            Modifier.focusGroup().focusRestorer().focusRequester(playlistFocus)
                        ) {
                            AddSongToPlaylist(
                                song = song,
                                setShowDialog = { setShowDialog(it) },
                                setDialogMenu = { dialogMenu = it }
                            )
                        }
                    }
                    DialogMenu.NEW_PLAYLIST -> {
                        Box(
                            Modifier.focusGroup().focusRestorer().focusRequester(newPlaylistFocus)
                        ) {
                            NewPlaylist(
                                song = song,
                                setDialogMenu = { dialogMenu = it }
                            )
                        }
                    }

                    DialogMenu.SET_RATING -> {
                        Box(
                            Modifier.focusGroup().focusRestorer().focusRequester(ratingFocus)
                        ) {
                            SetRating(
                                currentRating = currentRating,
                                onSetRating = { rating ->
                                    onSetRating(rating)
                                    currentRating = rating
                                    dialogMenu = DialogMenu.MAIN
                                },
                                onDismiss = { dialogMenu = DialogMenu.MAIN }
                            )
                        }
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
                                        playlist.mediaMetadata.extras?.getString("id") ?: playlist.mediaId
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
                    text = stringResource(R.string.button_new_playlist),
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
                    text = stringResource(R.string.new_playlist_playlist_name),
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
            headlineContent = { Text(stringResource(R.string.action_add)) },
            onClick = {
                viewModel.createPlaylist(playlistName, listOf(song.mediaMetadata.extras?.getString("id") ?: ""), context)
                setDialogMenu(DialogMenu.MAIN)
            }
        )
    }
}

@Preview
@Composable
private fun SetRating(
    currentRating: Int = 0,
    onSetRating: (Int) -> Unit = { },
    onDismiss: () -> Unit = { }
) {
    var selectedRating by remember { mutableIntStateOf(currentRating) }

    Column (
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            onClick = { onSetRating(selectedRating) },
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            if (selectedRating > 1) {
                                selectedRating--
                                true
                            } else false
                        }

                        Key.DirectionRight -> {
                            if (selectedRating < 5) {
                                selectedRating++
                                true
                            } else false
                        }

                        Key.DirectionCenter -> {
                            onSetRating(selectedRating)
                            true
                        }

                        else -> false
                    }
                },
            shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.colors(
                containerColor = ListItemDefaults.colors().containerColor,
                contentColor = ListItemDefaults.colors().contentColor,
                focusedContainerColor = ListItemDefaults.colors().focusedContainerColor,
                focusedContentColor = ListItemDefaults.colors().focusedContentColor,
            ),
            scale = CardDefaults.scale(
                scale = 1f,
                focusedScale = ListItemDefaults.scale().focusedScale,
            ),
            border = CardDefaults.border(
                focusedBorder = Border.None,
                pressedBorder = Border.None
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (star in 1..5) {
                    Icon(
                        imageVector = if (star <= selectedRating) Icons.Rounded.Star
                        else ImageVector.vectorResource(R.drawable.rounded_star_outline_24),
                        contentDescription = "Star $star",
                        tint = if (star <= selectedRating) LocalContentColor.current
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.weight(1f))

                if (selectedRating > 0)
                    Text(
                        text = "$selectedRating / 5"
                    )
            }
        }
//
//        Row(
//            horizontalArrangement = Arrangement.Center,
//            modifier = Modifier
//                .fillMaxWidth()
//                .focusable()
//                .focusRequester(focusRequester)
//                .onKeyEvent { keyEvent ->
//                    if (keyEvent.type == KeyEventType.KeyDown) {
//                        when (keyEvent.key) {
//                            Key.DirectionLeft -> {
//                                if (selectedRating > 1) {
//                                    selectedRating--
//                                    true
//                                } else false
//                            }
//
//                            Key.DirectionRight -> {
//                                if (selectedRating < 5) {
//                                    selectedRating++
//                                    true
//                                } else false
//                            }
//
//                            Key.DirectionCenter -> {
//                                onSetRating(selectedRating)
//                                true
//                            }
//
//                            else -> false
//                        }
//                    } else {
//                        false
//                    }
//                }
//        ) {
//            for (star in 1..5) {
//                Icon(
//                    imageVector = if (star <= selectedRating) Icons.Rounded.Star
//                    else ImageVector.vectorResource(R.drawable.rounded_star_outline_24),
//                    contentDescription = "Star $star",
//                    tint = if (star <= selectedRating) MaterialTheme.colorScheme.primary
//                    else MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//        }

        ListItem(
            selected = false,
            headlineContent = { Text(stringResource(R.string.action_done)) },
            onClick = {
                    onSetRating(selectedRating)
                }
        )

//        Row {
//            OutlinedButton (onClick = onDismiss) {
//                Text("Cancel")
//            }
//
//            Button(
//                onClick = {
//                    onSetRating(selectedRating)
//                    onDismiss()
//                }
//            ) {
//                Text("Save")
//            }
//        }
    }
}