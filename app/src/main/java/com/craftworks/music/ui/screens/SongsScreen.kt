package com.craftworks.music.ui.screens

import android.content.res.Configuration
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
import com.craftworks.music.data.Song
import com.craftworks.music.playingSong
import com.craftworks.music.songState
import com.craftworks.music.songsList
import com.craftworks.music.ui.elements.SongsHorizontalColumn

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongsScreen() {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    /* SONGS ICON + TEXT */
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding)) {
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
        }
        Divider(
            modifier = Modifier.padding(12.dp,56.dp,12.dp,0.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(modifier = Modifier.padding(0.dp,64.dp,0.dp,0.dp)) {
            val allSongsList = songsList.sortedBy { song: Song -> song.title }
            SongsHorizontalColumn(songsList = allSongsList, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = allSongsList
                songState = true })
        }
    }

}