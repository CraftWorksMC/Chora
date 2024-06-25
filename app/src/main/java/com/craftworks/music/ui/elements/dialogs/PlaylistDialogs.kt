package com.craftworks.music.ui.elements.dialogs

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.fadingEdge
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.addSongToNavidromePlaylist
import com.craftworks.music.providers.navidrome.createNavidromePlaylist
import com.craftworks.music.providers.navidrome.deleteNavidromePlaylist
import com.craftworks.music.saveManager
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
var songToAddToPlaylist = mutableStateOf(SongHelper.currentSong)
var showDeletePlaylistDialog = mutableStateOf(false)
var playlistToDelete = mutableStateOf(MediaData.Playlist("", "","","", true, "", "", 0, 0, ""))

@Composable
fun AddSongToPlaylist(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.Dialog_Add_To_Playlist).split("/")[0])
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(songToAddToPlaylist.value.title)
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
                                .padding(end = 30.dp)
                                .weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { setShowDialog(false) }
                        )
                    }

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
                        println("there are ${playlistList.size} playlists")

                        for (playlist in playlistList) {
                            Row(modifier = Modifier
                                .padding(bottom = 12.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                //.background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (playlist.songs?.contains(songToAddToPlaylist.value) == true) return@clickable

                                    if (useNavidromeServer.value)
                                        coroutineScope.launch { addSongToNavidromePlaylist(
                                            playlist.navidromeID.toString(),
                                            songToAddToPlaylist.value.navidromeID.toString()
                                        ) }
                                    else {
                                        playlist.songs = playlist.songs?.plus(songToAddToPlaylist.value)
                                        saveManager(context).saveLocalPlaylists()
                                    }
                                    setShowDialog(false)
                                }, verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = playlist.coverArt,
                                    placeholder = painterResource(R.drawable.placeholder),
                                    fallback = painterResource(R.drawable.placeholder),
                                    contentScale = ContentScale.FillHeight,
                                    contentDescription = "Album Image",
                                    modifier = Modifier
                                        .height(64.dp)
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Text(
                                    text = playlist.name,
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

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                try {
                                    if (playlistList.firstOrNull { it.name == name } != null) return@Button

                                    if (useNavidromeServer.value)
                                        coroutineScope.launch { createNavidromePlaylist(name) }
                                    else {
                                        var playlistImage = Uri.EMPTY
                                        coroutineScope.launch {
                                            playlistImage = localPlaylistImageGenerator(
                                                listOf(songToAddToPlaylist.value), context
                                            ) ?: Uri.EMPTY
                                        }
                                        playlistList.add(
                                            MediaData.Playlist(
                                                "Local",
                                                name,
                                                playlistImage.toString(),
                                                changed = "", created = "", duration = 0, songCount = 0,
                                                songs = listOf(songToAddToPlaylist.value)
                                            )
                                        )
                                        println("Added Playlist: $name")
                                        saveManager(context).saveLocalPlaylists()
                                        setShowDialog(false)
                                        showAddSongToPlaylistDialog.value = false
                                    }
                                } catch (_: Exception) {
                                    // DO NOTHING
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
fun DeletePlaylist(setShowDialog: (Boolean) -> Unit) {
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
                                try {
                                    if (useNavidromeServer.value)
                                        playlistToDelete.value.navidromeID?.let {
                                            coroutineScope.launch { deleteNavidromePlaylist(it) }
                                        }
                                    else {
                                        playlistList.remove(playlistToDelete.value)
                                        saveManager(context).saveLocalPlaylists()
                                    }
                                } catch (_: Exception) {
                                    // DO NOTHING
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