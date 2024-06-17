package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.SongsHorizontalColumn
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongsScreen(
    mediaController: MediaController? = null
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    var isSearchFieldOpen by remember { mutableStateOf(false) }

    var searchFilter by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf("Name (A-Z)") }

    var allSongsList by remember { mutableStateOf<List<MediaData.Song>>(emptyList()) }

    if (allSongsList.isEmpty())
        allSongsList = songsList

    val coroutineScope = rememberCoroutineScope()

    /* SONGS ICON + TEXT */
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_music_note_24),
                contentDescription = "Songs Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.songs),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { isSearchFieldOpen = !isSearchFieldOpen },
                modifier = Modifier
                    .size(48.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Search all songs")
            }

            /* SORTING OPTIONS */
            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(48.dp)
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Sort songs")
                }

                DropdownMenu(
                    modifier = Modifier,
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = {
                            expanded = false
                            selectedSortOption = "Name (A-Z)"
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z-A)") },
                        onClick = {
                            expanded = false
                            selectedSortOption = "Name (Z-A)"
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added") },
                        onClick = {
                            expanded = false
                            selectedSortOption = "Date Added"
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Last Played") },
                        onClick = {
                            expanded = false
                            selectedSortOption = "Last Played"
                        }
                    )
                }
            }
        }

        HorizontalLineWithNavidromeCheck()

        AnimatedVisibility(
            visible = isSearchFieldOpen,
            enter = expandVertically(),
            exit = shrinkVertically()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
            ) {
                val focusRequester = remember { FocusRequester() }
                TextField(
                    value = searchFilter,
                    onValueChange = {
                        searchFilter = it
                        if (it.isBlank()){
                            coroutineScope.launch {
                                songsList.addAll(getNavidromeSongs())
                                //isSearchFieldOpen = false
                            }
                        } },
                    label = { Text(stringResource(R.string.Action_Search)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            coroutineScope.launch {
                                songsList.addAll(sendNavidromeGETRequest(
                                    navidromeServersList[selectedNavidromeServerIndex.intValue].url,
                                    navidromeServersList[selectedNavidromeServerIndex.intValue].username,
                                    navidromeServersList[selectedNavidromeServerIndex.intValue].password,
                                    "search3.view?query=${searchFilter}&songCount=500&artistCount=0&albumCount=0&f=json"
                                ).filterIsInstance<MediaData.Song>())

                                isSearchFieldOpen = false
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                        .focusRequester(focusRequester))

                LaunchedEffect(isSearchFieldOpen) {
                    if (isSearchFieldOpen)
                        focusRequester.requestFocus()
                    else
                        focusRequester.freeFocus()
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {

            LaunchedEffect(searchFilter) {
                if (searchFilter.isNotBlank()){
                    songsList.clear()
                }
            }

//            allSongsList = when (selectedSortOption) {
//                "Name (A-Z)" -> allSongsList.sortedBy { it.title }.toMutableList()
//                "Name (Z-A)" -> allSongsList.sortedByDescending { it.title }.toMutableList()
//                "Date Added" -> allSongsList.sortedBy { it.dateAdded }.toMutableList()
//                "Last Played" -> allSongsList.sortedByDescending { it.lastPlayed }.toMutableList()
//                else -> allSongsList.sortedBy { it.title }.toMutableList() // Default sorting or handle unknown option
//            }

            SongsHorizontalColumn(songList = allSongsList, onSongSelected = { song ->
                SongHelper.currentSong = song
                SongHelper.currentList = songsList.sortedBy { song.title }
                song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController) } },
                searchFilter.isNotBlank())
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )
}