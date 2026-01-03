package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.fadingEdge
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.ExperimentalFoundationApi

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewAddToPlaylistDialog(){
    AddSongToPlaylist(song = MediaItem.EMPTY, onDismiss = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewNewPlaylistDialog(){
    NewPlaylist(hiltViewModel(), song = MediaItem.EMPTY, onDismiss = {}, onDone = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletePlaylistDialog(){
    DeletePlaylist(playlistName = "My Playlist", setShowDialog = {})
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddSongToPlaylist(
    song: MediaItem,
    onDismiss: () -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    var showNewPlaylistDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.dialogFocusable()
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header
                    val dialogText = stringResource(R.string.Dialog_Add_To_Playlist)
                    val parts = dialogText.split("/")
                    Text(
                        text = buildAnnotatedString {
                            append(parts.getOrElse(0) { "" })
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(song.mediaMetadata.title)
                            }
                            append(parts.getOrElse(1) { "" })
                        },
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // List
                    val listFadingEdge =
                        Brush.verticalGradient(0.75f to Color.Red, 1f to Color.Transparent)

                    Column(
                        modifier = Modifier
                            .height(256.dp)
                            .fadingEdge(listFadingEdge)
                            .verticalScroll(rememberScrollState())
                    ) {
                        println("there are ${playlists.size} playlists")

                        for (playlist in playlists) {
                            // Allow ONLY adding local songs to local playlists and navidrome songs to navidrome playlists.
                            val disabled = song.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == true xor
                                    (playlist.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == true)

                            Row(modifier = Modifier
                                .padding(bottom = 12.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    enabled = !disabled
                                ) {
                                    if (playlist.mediaMetadata.extras?.getString("navidromeID") ==
                                        song.mediaMetadata.extras?.getString("navidromeID"))
                                        return@clickable

                                    viewModel.addSongToPlaylist(playlist.mediaMetadata.extras?.getString("navidromeID")
                                        ?: "",
                                        song.mediaMetadata.extras?.getString(
                                            "navidromeID"
                                        ) ?: "")
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                            ) {
                                val artwork = if (playlist.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local") == true)
                                    playlist.mediaMetadata.artworkData
                                else
                                    playlist.mediaMetadata.artworkUri

                                SubcomposeAsyncImage (
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(artwork)
                                        .crossfade(true)
                                        .diskCacheKey(
                                            playlist.mediaMetadata.extras?.getString("navidromeID") ?: playlist.mediaId
                                        )
                                        .build(),
                                    contentScale = ContentScale.FillHeight,
                                    contentDescription = "Album Image",
                                    alpha = if (disabled) 0.5f else 1f,
                                    modifier = Modifier
                                        .height(64.dp)
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Text(
                                    text = playlist.mediaMetadata.title.toString(),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    color = if (disabled) MaterialTheme.colorScheme.onBackground.copy(0.5f) else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                        Button(
                            onClick = {
                                showNewPlaylistDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Dialog_New_Playlist))
                        }
                    }

                    if (showNewPlaylistDialog) {
                        NewPlaylist(
                            viewModel = viewModel,
                            song = song,
                            onDismiss = { showNewPlaylistDialog = false },
                            onDone = {
                                showNewPlaylistDialog = false
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewPlaylist(
    viewModel: PlaylistScreenViewModel,
    song: MediaItem,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var name: String by remember { mutableStateOf("") }
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    var addToNavidrome by remember { mutableStateOf(NavidromeManager.checkActiveServers()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.dialogFocusable()
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Dialog_New_Playlist),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Column {
                        /* Directory */
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 128) name = it },
                            label = { Text(stringResource(R.string.Label_Playlist_Name)) },
                            singleLine = true
                        )

                        if (NavidromeManager.checkActiveServers()) {
                            Row (
                                modifier = Modifier.selectable(
                                    selected = addToNavidrome,
                                    onClick = {
                                        addToNavidrome = !addToNavidrome
                                    },
                                    role = Role.Checkbox,
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = addToNavidrome,
                                    onCheckedChange = { addToNavidrome = it }
                                )

                                Text(
                                    text = stringResource(R.string.Label_Radio_Add_To_Navidrome),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (playlists.firstOrNull { it.mediaMetadata.title == name } != null) return@Button

                                viewModel.createPlaylist(
                                    name,
                                    song.mediaMetadata.extras?.getString("navidromeID") ?: "",
                                    addToNavidrome
                                )

                                onDone()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_CreatePlaylist),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun DeletePlaylist(
    playlistName: String,
    setShowDialog: (Boolean) -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.dialogFocusable()
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Dialog_Delete_Playlist),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Column {

                        Text(
                            text = stringResource(R.string.Label_Confirm_Delete_Playlist),
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.deletePlaylist(playlistName)
                                setShowDialog(false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.Action_Remove),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}
