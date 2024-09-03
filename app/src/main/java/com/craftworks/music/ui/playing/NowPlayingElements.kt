package com.craftworks.music.ui.playing

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
import com.craftworks.music.repeatSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import com.craftworks.music.ui.elements.moveClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderUpdating(color: Color, mediaController: MediaController?){
    var currentValue by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (mediaController?.isPlaying == true) {
            currentValue = mediaController.currentPosition
            println("PLAYER POS: $currentValue")
            delay(1.seconds)
        }
    }

    val animatedValue by animateFloatAsState(
        targetValue = currentValue.toFloat(),
        animationSpec = tween(200, 0, EaseInOut),
        label = "Animated slider value"
    )

    Slider(
        enabled = (transcodingBitrate.value == "No Transcoding" || SongHelper.currentSong.isRadio == false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(12.dp)
            .scale(scaleX = 1f, scaleY = 1.25f)
            .clip(RoundedCornerShape(12.dp))
            .focusable(false),
        value = animatedValue,
        onValueChange = {
            SongHelper.isSeeking = true
            currentValue = it.toLong()
        },
        onValueChangeFinished = {
            SongHelper.isSeeking = false
            mediaController?.seekTo(currentValue)
        },
        valueRange = 0f..(SongHelper.currentSong.duration.toFloat() * 1000),
        colors = SliderDefaults.colors(
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.25f),
        ),
        thumb = {}
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = formatMilliseconds(currentValue.toInt() * 1000),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Start,
            color = color.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = formatMilliseconds(SongHelper.currentSong.duration),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            color = color.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PreviousSongButton(color: Color, mediaController: MediaController?, size: Dp) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                mediaController?.seekToPreviousMediaItem()
            },
            shape = CircleShape,
            modifier = Modifier
                .size(size)
                .bounceClick()
                .moveClick(false),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_previous),
                tint = color,
                contentDescription = "Previous Song",
                modifier = Modifier
                    .size(size)
            )
        }
    }
}

@Composable
fun PlayPauseButtonUpdating(color: Color, mediaController: MediaController?, size: Dp){
    Log.d("RECOMPOSITION", "PlayPauseButtonUpdating")
    val playerStatus = remember { mutableStateOf("") }

    // Update playerStatus from mediaController
    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                playerStatus.value = "loading"
                super.onIsLoadingChanged(isLoading)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playerStatus.value = if (playWhenReady) "playing" else "paused"
                super.onPlayWhenReadyChanged(playWhenReady, reason)
            }
        }

        mediaController?.addListener(listener)
        Log.d("RECOMPOSITION", "Registered play/pause button mediaController!")

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    Button(
        onClick = {
            mediaController?.playWhenReady = !(mediaController?.playWhenReady ?: true)
        },
        shape = CircleShape,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .size(size)
            .bounceClick(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        val icon = when (playerStatus.value) {
            "playing" -> ImageVector.vectorResource(R.drawable.media3_notification_pause)
            "paused" -> Icons.Rounded.PlayArrow
            else -> null // Return null for loading state
        }

        if (icon != null) {
            Icon(
                imageVector = icon,
                tint = color,
                contentDescription = "Play/Pause button",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(size / 8)
                    .size(size),
                strokeCap = StrokeCap.Round,
                strokeWidth = size / 12,
                color = color
            )
        }
    }
}

@Composable
fun NextSongButton(color: Color, mediaController: MediaController?, size: Dp) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                mediaController?.seekToNextMediaItem()
            },
            shape = CircleShape,
            modifier = Modifier
                .size(size)
                .bounceClick()
                .moveClick(true),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_next),
                tint = color,
                contentDescription = "Next Song",
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun LyricsButton(color: Color, size: Dp){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = { lyricsOpen = !lyricsOpen },
            shape = CircleShape,
            modifier = Modifier
                .height(size)
                .bounceClick(),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Crossfade(targetState = lyricsOpen, label = "Lyrics Icon Crossfade") { open ->
                when (open) {
                    true -> Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.lyrics_active),
                        tint = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = 0.5f
                        ),
                        contentDescription = "Close Lyrics",
                        modifier = Modifier
                            .height(size)
                            .size(size)
                    )

                    false -> Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.lyrics_inactive),
                        tint = color.copy(
                            alpha = 0.5f
                        ),
                        contentDescription = "View Lyrics",
                        modifier = Modifier
                            .height(size)
                            .size(size)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadButton(color: Color, size: Dp){
    val snackbarHostState = SnackbarHostState()
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                if (navidromeServersList.isEmpty() || !NavidromeManager.checkActiveServers() || SongHelper.currentSong.navidromeID == "Local") return@Button
                if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
                    navidromeServersList[selectedNavidromeServerIndex.intValue].url == ""
                ) return@Button

                downloadNavidromeSong(
                    "${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/download.view?id=${SongHelper.currentSong.navidromeID}&submission=true&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora",
                    snackbarHostState = snackbarHostState,
                    coroutineScope
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Downloading Started. Do not close the app or start another download until complete.")
                }
            },
            shape = CircleShape,
            modifier = Modifier
                .height(size)
                .bounceClick(),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                tint = color.copy(alpha = 0.5f),
                contentDescription = "Download Song",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShuffleButton(color: Color, mediaController: MediaController?, size: Dp,
                  play: FocusRequester = FocusRequester(),
                  shuffle: FocusRequester = FocusRequester()
){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                shuffleSongs.value = !shuffleSongs.value
                mediaController?.shuffleModeEnabled = shuffleSongs.value
            },
            shape = CircleShape,
            modifier = Modifier
                .size(size)
                .focusRequester(shuffle)
                .focusProperties {
                    up = play
                    down = FocusRequester.Cancel
                },
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
                tint = color.copy(if (shuffleSongs.value) 1f else 0.5f),
                contentDescription = "Toggle Shuffle",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RepeatButton(color: Color, mediaController: MediaController?, size: Dp,
                 play: FocusRequester = FocusRequester(),
                 replay: FocusRequester = FocusRequester()
){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                repeatSong.value = !repeatSong.value
                mediaController?.repeatMode =
                    if (repeatSong.value)
                        Player.REPEAT_MODE_ONE
                    else
                        Player.REPEAT_MODE_OFF
            },
            shape = CircleShape,
            modifier = Modifier
                .size(size)
                .focusRequester(replay)
                .focusProperties {
                    up = play
                    down = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                },
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_repeat_28),
                tint = color.copy(if (repeatSong.value) 1f else 0.5f),
                contentDescription = "Toggle Repeat",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        }
    }
}
