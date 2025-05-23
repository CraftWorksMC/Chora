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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.media3.common.MediaItem
import coil.compose.SubcomposeAsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.playlistList
import com.craftworks.music.fadingEdge
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.addSongToPlaylist
import com.craftworks.music.providers.createPlaylist
import com.craftworks.music.providers.deletePlaylist
import com.craftworks.music.providers.getPlaylists
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.launch

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewAddToPlaylistDialog(){
    AddSongToPlaylist(setShowDialog = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewNewPlaylistDialog(){
    NewPlaylist(setShowDialog = {})
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletePlaylistDialog(){
    DeletePlaylist(setShowDialog = {})
}
//endregion

var showAddSongToPlaylistDialog = mutableStateOf(false)
var showNewPlaylistDialog = mutableStateOf(false)
var songToAddToPlaylist = mutableStateOf(MediaItem.EMPTY)
var showDeletePlaylistDialog = mutableStateOf(false)
var playlistToDelete = mutableStateOf("")

@Composable
fun AddSongToPlaylist(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var playlists = remember { mutableStateListOf<MediaItem>() }

    LaunchedEffect(Unit) {
        playlists.addAll(getPlaylists(context, true))
    }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.Dialog_Add_To_Playlist).split("/")[0])
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(songToAddToPlaylist.value.mediaMetadata.title)
                            }
                            append(stringResource(R.string.Dialog_Add_To_Playlist).split("/")[1])
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
                            Row(modifier = Modifier
                                .padding(bottom = 12.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                //.background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (playlist.mediaMetadata.extras?.getString("navidromeID") == songToAddToPlaylist.value.mediaMetadata.extras?.getString("navidromeID"))
                                        return@clickable

                                    coroutineScope.launch {
                                        addSongToPlaylist(
                                            playlist.mediaMetadata.extras?.getString("navidromeID")
                                                ?: "",
                                            songToAddToPlaylist.value.mediaMetadata.extras?.getString("navidromeID") ?: "",
                                            context
                                        )
                                    }
                                    setShowDialog(false)
                                }, verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubcomposeAsyncImage (
                                    model = if (playlist.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local") == true)
                                        playlist.mediaMetadata.artworkData else playlist.mediaMetadata.artworkUri,
                                    contentScale = ContentScale.FillHeight,
                                    contentDescription = "Album Image",
                                    modifier = Modifier
                                        .height(64.dp)
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Text(
                                    text = playlist.mediaMetadata.title.toString(),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    color = MaterialTheme.colorScheme.onBackground,
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
                                showNewPlaylistDialog.value = true
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

                    if (showNewPlaylistDialog.value) {
                        NewPlaylist { showNewPlaylistDialog.value = it }
                    }
                }
            }
        }
    }
}


@Composable
fun NewPlaylist(setShowDialog: (Boolean) -> Unit) {
    var name: String by remember { mutableStateOf("") }

    val context = LocalContext.current

    var addToNavidrome by remember { mutableStateOf(NavidromeManager.checkActiveServers()) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
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
                            onValueChange = { name = it },
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

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (playlistList.firstOrNull { it.name == name } != null) return@Button

                                coroutineScope.launch { createPlaylist(name, addToNavidrome, context) }
                                showAddSongToPlaylistDialog.value = false
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

@Composable
fun DeletePlaylist(
    setShowDialog: (Boolean) -> Unit,
    onDeleted: () -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
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
                                coroutineScope.launch {
                                    deletePlaylist(playlistToDelete.value, context)
                                    onDeleted()
                                }
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