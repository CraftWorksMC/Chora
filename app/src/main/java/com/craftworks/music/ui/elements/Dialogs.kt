package com.craftworks.music.ui.elements

import android.content.Context
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Radio
import com.craftworks.music.data.Song
import com.craftworks.music.data.bottomNavigationItems
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.fadingEdge
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.addSongToNavidromePlaylist
import com.craftworks.music.providers.navidrome.createNavidromePlaylist
import com.craftworks.music.providers.navidrome.createNavidromeRadioStation
import com.craftworks.music.providers.navidrome.deleteNavidromePlaylist
import com.craftworks.music.providers.navidrome.deleteNavidromeRadioStation
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.modifyNavidromeRadoStation
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.providers.navidrome.selectedNavidromeServerIndex
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.backgroundTypes
import kotlinx.coroutines.launch
import java.net.URL

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewBackgroundDialog(){
    BackgroundDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewNavbarItemsDialog(){
    NavbarItemsDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewProviderDialog(){
    CreateMediaProviderDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewAddRadioDialog(){
    AddRadioDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewModifyRadioDialog(){
    ModifyRadioDialog(setShowDialog = {}, radio = Radio("",Uri.EMPTY,"", Uri.EMPTY))
}

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

//region RADIO DIALOGS
@Composable
fun AddRadioDialog(setShowDialog: (Boolean) -> Unit) {
    var radioName by remember { mutableStateOf("") }
    var radioUrl by remember { mutableStateOf("") }
    var radioPage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.Dialog_Add_Radio),
                            style = TextStyle(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
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

                    OutlinedTextField(
                        value = radioName,
                        onValueChange = { radioName = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl,
                        onValueChange = { radioUrl = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_URL)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Homepage)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                        Button(
                            onClick = {
                                if (radioName.isBlank() && radioUrl.isBlank()) return@Button
                                setShowDialog(false)
                                radioList.add(
                                    Radio(
                                        name = radioName,
                                        media = Uri.parse(radioUrl),
                                        homepageUrl = radioPage,
                                        imageUrl = Uri.parse("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder),
                                        navidromeID = "Local"
                                    )
                                )
                                if (useNavidromeServer.value) createNavidromeRadioStation(
                                    radioName,
                                    radioUrl,
                                    radioPage
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(text = "Done")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ModifyRadioDialog(setShowDialog: (Boolean) -> Unit, radio: Radio) {
    var radioName by remember { mutableStateOf(radio.name) }
    var radioUrl by remember { mutableStateOf(radio.media.toString()) }
    var radioPage by remember { mutableStateOf(radio.homepageUrl) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.Dialog_Modify_Radio) + radioName,
                            style = TextStyle(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { setShowDialog(false) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = radioName,
                        onValueChange = { radioName = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl,
                        onValueChange = { radioUrl = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_URL)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Homepage)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                setShowDialog(false)
                                radioList.remove(radio)
                                if (useNavidromeServer.value) radio.navidromeID?.let {
                                    deleteNavidromeRadioStation(
                                        it
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Remove))
                        }
                        Button(
                            onClick = {
                                if (radioName.isBlank() && radioUrl.isBlank()) return@Button
                                if (useNavidromeServer.value) radio.navidromeID?.let {
                                    modifyNavidromeRadoStation(
                                        it,
                                        radioName,
                                        radioUrl,
                                        radioPage
                                    )
                                }
                                setShowDialog(false)
                                if (useNavidromeServer.value) {
                                    //radioList.clear()
                                    //getNavidromeRadios()
                                } else {
                                    radioList.remove(radio)
                                    radioList.add(
                                        Radio(
                                            name = radioName,
                                            imageUrl = Uri.parse("$radioUrl/favicon.ico"),
                                            media = Uri.parse(radioUrl)
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Done))
                        }
                    }
                }
            }
        }
    }
}

//endregion

//region SONG DIALOGS
var showAddSongToPlaylistDialog = mutableStateOf(false)
var showNewPlaylistDialog = mutableStateOf(false)
var songToAddToPlaylist = mutableStateOf(Song(Uri.EMPTY,"","","",0))
@Composable
fun AddSongToPlaylist(setShowDialog: (Boolean) -> Unit) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {

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
                    val listFadingEdge = Brush.verticalGradient(0.75f to Color.Red, 1f to Color.Transparent)

                    Column(modifier = Modifier
                        .height(256.dp)
                        .fadingEdge(listFadingEdge)
                        .verticalScroll(rememberScrollState())){
                        println("there are ${playlistList.size} playlists")

                        for (playlist in playlistList){
                            Row(modifier = Modifier
                                .padding(bottom = 12.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                //.background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (playlist.songs.contains(songToAddToPlaylist.value)) return@clickable

                                    if (useNavidromeServer.value)
                                        addSongToNavidromePlaylist(
                                            playlist.navidromeID.toString(),
                                            songToAddToPlaylist.value.navidromeID.toString()
                                        )
                                    else {
                                        playlist.songs += songToAddToPlaylist.value
                                    }
                                    setShowDialog(false)
                                }, verticalAlignment = Alignment.CenterVertically){
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
                                contentColor = MaterialTheme.colorScheme.onBackground),
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
                    Column{
                        /* Directory */
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.Label_Playlist_Name))},
                            singleLine = true
                        )

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                try {
                                    if (playlistList.firstOrNull { it.name == name } != null) return@Button

                                    if (useNavidromeServer.value)
                                        createNavidromePlaylist(name)
                                    else{
                                        var playlistImage = Uri.EMPTY
                                        coroutineScope.launch {
                                            playlistImage = localPlaylistImageGenerator(listOf(songToAddToPlaylist.value), context)?: Uri.EMPTY
                                        }
                                        playlistList.add(
                                            Playlist(
                                                name,
                                                playlistImage,
                                                listOf(songToAddToPlaylist.value),
                                                navidromeID = "Local"
                                            )
                                        )
                                        println("Added Playlist: $name")
                                        setShowDialog(false)
                                        showAddSongToPlaylistDialog.value = false
                                    }
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                                setShowDialog(false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.Action_CreatePlaylist), modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

        }
    }
}

var showDeletePlaylistDialog = mutableStateOf(false)
var playlistToDelete = mutableStateOf(Playlist("",Uri.EMPTY))
@Composable
fun DeletePlaylist(setShowDialog: (Boolean) -> Unit) {
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
                    Column{

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
                                            deleteNavidromePlaylist(it) }
                                    else {
                                        playlistList.remove(playlistToDelete.value)
                                    }
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                                setShowDialog(false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.Action_Remove), modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

        }
    }
}

//endregion

/* --- SETTINGS --- */

//region APPEARANCE DIALOGS
@Composable
fun BackgroundDialog(setShowDialog: (Boolean) -> Unit) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Setting_Background),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    for (option in backgroundTypes) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .height(48.dp)
                        ) {
                            RadioButton(
                                selected = option == backgroundType.value,
                                onClick = {
                                    backgroundType.value = option
                                    setShowDialog(false)
                                },
                                modifier = Modifier
                                    .semantics { contentDescription = option }
                                    .bounceClick()
                            )
                            Text(
                                text = option,
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun NavbarItemsDialog(setShowDialog: (Boolean) -> Unit) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Setting_Navbar_Items),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    for (navItem in bottomNavigationItems) {
                        val index = bottomNavigationItems.indexOf(navItem)
                        Row(
                            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .height(48.dp)
                        ) {
                            var checked by remember { mutableStateOf(true) }
                            Checkbox(
                                checked = navItem.enabled,
                                onCheckedChange = {
                                    checked = it
                                    bottomNavigationItems[index] = bottomNavigationItems[index].copy(enabled = it) },
                                modifier = Modifier
                                    .semantics { contentDescription = navItem.title }
                                    .bounceClick()
                            )
                            Text(
                                text = navItem.title,
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

//endregion

//region PROVIDERS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaProviderDialog(setShowDialog: (Boolean) -> Unit, context:Context = LocalContext.current) {
    var url: String by remember { mutableStateOf("") }
    var username: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }

    var dir: String by remember { mutableStateOf("/Music/") }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Settings_Header_Media),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    var expanded by remember { mutableStateOf(false) }

                    val options = listOf(stringResource(R.string.Source_Local), stringResource(R.string.Source_Navidrome))
                    var selectedOptionText by remember { mutableStateOf(options[0]) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        TextField(
                            shape = RoundedCornerShape(12.dp, 12.dp),
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            value = selectedOptionText,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.Dialog_Media_Source)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedOptionText = selectionOption
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    if (selectedOptionText == stringResource(R.string.Source_Local))
                        Column{
                            /* Directory */
                            OutlinedTextField(
                                value = dir,
                                onValueChange = { dir = it },
                                label = { Text(stringResource(R.string.Label_Local_Directory)) },
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    try {
                                        val localProvider = LocalProvider(dir, true)
                                        if (!localProviderList.contains(localProvider)){
                                            localProviderList.add(localProvider)
                                        }
                                        selectedLocalProvider.intValue = localProviderList.indexOf(localProvider)
                                        getSongsOnDevice(context)
                                        setShowDialog(false)
                                    } catch (_: Exception){
                                        // DO NOTHING
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .bounceClick(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.Action_Add), modifier = Modifier.height(24.dp))
                            }
                        }
                    else if (selectedOptionText == stringResource(R.string.Source_Navidrome))
                        Column{
                        /* SERVER URL */
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.Label_Navidrome_URL)) },
                            singleLine = true,
                            isError = navidromeStatus.value == "Invalid URL"
                        )
                        /* USERNAME */
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
                            singleLine = true
                        )
                        /* PASSWORD */
                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.Label_Navidrome_Password)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    R.drawable.round_visibility_24
                                else
                                    R.drawable.round_visibility_off_24

                                // Please provide localized description for accessibility services
                                val description = if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = {passwordVisible = !passwordVisible}){
                                    Icon(imageVector  = ImageVector.vectorResource(id = image), description)
                                }
                            }
                        )

                        Button(
                            onClick = {
                                try {
                                    //saveManager(context).saveSettings()
                                    val server = NavidromeProvider(url.removeSuffix("/"), username, password)

                                    if (!navidromeServersList.contains(server)){
                                        navidromeServersList.add(server)
                                    }
                                    selectedNavidromeServerIndex.intValue = navidromeServersList.indexOf(server)

                                    getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                                    getNavidromePlaylists()
                                    getNavidromeRadios()
                                    setShowDialog(false)
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                .height(50.dp)
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                                .bounceClick(),

                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Crossfade(targetState = navidromeStatus.value,
                                label = "Loading Button Animation") { status ->
                                when (status) {
                                    "" -> Text(stringResource(R.string.Action_Login), modifier = Modifier.height(24.dp))
                                    "Success" -> Text(stringResource(R.string.Action_Success), modifier = Modifier
                                        .height(24.dp)
                                        .wrapContentHeight(Alignment.CenterVertically))
                                    "Loading" -> CircularProgressIndicator(
                                        modifier = Modifier.size(size = 32.dp),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        strokeWidth = 4.dp,
                                        strokeCap = StrokeCap.Round)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//endregion