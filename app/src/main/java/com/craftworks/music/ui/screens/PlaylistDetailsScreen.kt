package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.fadingEdge
import com.craftworks.music.playingSong
import com.craftworks.music.songState
import com.craftworks.music.ui.elements.SongsHorizontalColumn

@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistDetails(navHostController: NavHostController = rememberNavController()) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red, Color.Transparent))
    /* RADIO ICON + TEXT */
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding)) {
        Box (modifier = Modifier
            .padding(horizontal = 12.dp)
            .height(192.dp)
            .fillMaxWidth()) {
            AsyncImage(
                model = selectedPlaylist?.coverArt,
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Album Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingEdge(imageFadingEdge)
                    .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                    .alpha(0.85f)
            )
            selectedPlaylist?.name?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            Button(
                onClick = { navHostController.navigate(Screen.Playlists.route) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 12.dp, start = 12.dp).size(32.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Settings",
                    modifier = Modifier
                        .height(32.dp)
                        .size(32.dp)
                )
            }
        }
        Divider(
            modifier = Modifier.padding(12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(modifier = Modifier.padding(12.dp)) {
            selectedPlaylist?.songs?.let {
                SongsHorizontalColumn(it, onSongSelected = { song ->
                    playingSong.selectedSong = song
                    playingSong.selectedList = it
                    songState = true })
            }
        }
    }
}