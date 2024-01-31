package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.Song
import com.craftworks.music.data.songsList
import com.craftworks.music.playingSong
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.songState
import com.craftworks.music.ui.elements.SongsRow
import com.craftworks.music.ui.elements.bounceClick


@Composable
@Preview(showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
fun HomeScreen(navHostController: NavHostController = rememberNavController()) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    Column(modifier = Modifier
        .padding(start = leftPadding)
        .fillMaxWidth()
        .wrapContentHeight()
        .verticalScroll(rememberScrollState())) {

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                )
                .height(72.dp)
        ) {
            /* GREETING */
            Box(Modifier.weight(1f)) {
                if (useNavidromeServer.value){
                    var rotation by remember { mutableFloatStateOf(-10f) }
                    val animatedRotation by animateFloatAsState(
                        targetValue = rotation,
                        animationSpec = tween(durationMillis = 1000),
                        label = "Navidrome Logo Rotate"
                    )

                    Image(
                        painter = painterResource(R.drawable.s_m_navidrome),
                        contentDescription = "Navidrome Icon",
                        modifier = Modifier
                            .size(72.dp)
                            .offset(x = (-36).dp)
                            //.rotate(-10f) // Make it look just a tad bit nicer
                            .shadow(24.dp, CircleShape)
                            .graphicsLayer(
                                rotationZ = animatedRotation
                            )
                            .bounceClick()
                            .clickable {
                                rotation += 360f
                            }
                    )
                }
                Text(
                    text = "${stringResource(R.string.welcome_text)},\n${username.value}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier
                        .padding(start =
                        if (useNavidromeServer.value)
                            42.dp
                        else
                            12.dp),
                    lineHeight = 32.sp
                )
            }
            Box(Modifier.padding(end = 12.dp)) {
                Button(
                    onClick = { navHostController.navigate(Screen.Setting.route) },
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .height(32.dp)
                            .size(32.dp)
                    )
                }
            }
        }


        Divider(
            modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
        /* RECENTLY PLAYED */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
        ) {
            Text(
                text = stringResource(R.string.recently_played) + ":",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                modifier = Modifier.padding(start = 12.dp)
            )
            val recentlyPlayedSongsList = songsList.sortedByDescending { song: Song -> song.lastPlayed }.take(10)
            /* SONGS ROW */
            SongsRow(songsList = recentlyPlayedSongsList, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = recentlyPlayedSongsList
                songState = true
            })
        }


        /* RECENTLY ADDED SONGS */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
        ) {
            Text(
                text = stringResource(R.string.recently_added) + ":",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                modifier = Modifier.padding(start = 12.dp)
            )
            val recentSongsList = songsList.sortedByDescending { song: Song -> song.dateAdded }
            /* SONGS ROW */
            SongsRow(songsList = recentSongsList, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = recentSongsList
                songState = true
            })
        }

        /* MOST PLAYED SONGS */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
        ) {
            Text(
                text = stringResource(R.string.most_played) + ":",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                modifier = Modifier.padding(start = 12.dp)
            )
            val mostPlayedList = songsList.sortedByDescending { song: Song -> song.timesPlayed }
            /* SONGS ROW */
            SongsRow(songsList = mostPlayedList, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = mostPlayedList
                songState = true
            })
        }

        /* EXPLORE FROM YOUR LIBRARY */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
        ) {
            Text(
                text = stringResource(R.string.random_songs) + ":",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                modifier = Modifier.padding(start = 12.dp)
            )
            val shuffledSongsList = remember { songsList.take(10).shuffled() }
            /* SONGS ROW */
            SongsRow(songsList = shuffledSongsList, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = shuffledSongsList
                songState = true
            })
        }

        Spacer(modifier = Modifier.height(72.dp + 80.dp))
    }
}


