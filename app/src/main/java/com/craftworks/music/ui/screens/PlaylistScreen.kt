package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.Playlist
import com.craftworks.music.playingSong
import com.craftworks.music.songState
import com.craftworks.music.ui.elements.PlaylistGrid

var playlistList:MutableList<Playlist> = mutableStateListOf(
    /*Playlist(
        "My Awesome Playlist",
        coverArt = Uri.parse(R.drawable.placeholder.toString()),
        songs = songsList),
    Playlist(
        "Second Playlist!",
        coverArt = Uri.parse(R.drawable.placeholder.toString()),
        songs = songsList),
    Playlist(
        "Second Playlist!",
        coverArt = Uri.parse(R.drawable.placeholder.toString()),
        songs = songsList)*/
)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistScreen() {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    /* RADIO ICON + TEXT */
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.placeholder),
                contentDescription = "Songs Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.playlists),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )
        }
        Divider(
            modifier = Modifier.padding(12.dp,56.dp,12.dp,0.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(modifier = Modifier.padding(12.dp,64.dp,12.dp,12.dp)) {
            PlaylistGrid(playlistList , onSongSelected = { song ->
                playingSong.selectedSong = song
                songState = true })
        }
    }
}