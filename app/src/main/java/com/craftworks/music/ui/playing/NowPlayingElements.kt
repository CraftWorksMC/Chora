package com.craftworks.music.ui.playing

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.sliderPos
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.moveClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PlaybackProgressSlider(
    color: Color = MaterialTheme.colorScheme.onBackground,
    mediaController: MediaController? = null,
) {
    var currentValue by remember { mutableIntStateOf(0) }

    val animatedValue by animateFloatAsState(
        targetValue = currentValue.toFloat(), label = "Smooth Slider Update"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val focused = remember { mutableStateOf(false) }

    LaunchedEffect(mediaController) {
        mediaController?.let {
            while (isActive) {
                if (it.isPlaying) {
                    val position = it.currentPosition.toInt()
                    currentValue = position
                }
                delay(1000)
            }
        }
    }

    Column(
        Modifier.focusable(false)
    ) {
        Slider(
            enabled = (SongHelper.currentSong.isRadio == false),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .onFocusChanged {
                    focused.value = it.isFocused
                }
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                            currentValue = (currentValue + 5000)
                            mediaController?.seekTo(currentValue.toLong())
                            true
                        }

                        keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                            currentValue = (currentValue - 5000)
                            mediaController?.seekTo(currentValue.toLong())
                            true
                        }

                        else -> false
                    }
                },
            value = animatedValue,
            onValueChange = {
                sliderPos.intValue = it.toInt()
                currentValue = it.toInt()
            },
            onValueChangeFinished = {
                mediaController?.seekTo(currentValue.toLong())
            },
            valueRange = 0f..(SongHelper.currentSong.duration.toFloat() * 1000),
            colors = SliderDefaults.colors(
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.25f),
                thumbColor = color
            ),
            interactionSource = interactionSource,
            thumb = {
                AnimatedVisibility(
                    focused.value
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors().copy(thumbColor = color)
                    )
                }
            }
        )

        // Time thingies
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = formatMilliseconds(currentValue / 1000),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Start,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(64.dp),
                maxLines = 1
            )
            Text(
                text = formatMilliseconds(SongHelper.currentSong.duration),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(64.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun PreviousSongButton(color: Color, mediaController: MediaController?, size: Dp) {
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
        contentPadding = PaddingValues(0.dp),
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

@Stable
@Composable
fun PlayPauseButtonUpdating(color: Color, mediaController: MediaController?, size: Dp){
    val playerStatus = remember {
        mutableStateOf(
            if (mediaController?.isLoading == true) "loading"
            else if (mediaController?.isPlaying == true) "playing"
            else "paused"
        )
    }

    // Update playerStatus from mediaController
    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                playerStatus.value = if (isLoading) "loading" else playerStatus.value
                super.onIsLoadingChanged(isLoading)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerStatus.value = if (isPlaying) "playing" else "paused"
                super.onIsPlayingChanged(isPlaying)
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
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = color
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
        contentPadding = PaddingValues(0.dp),
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

@Composable
fun LyricsButton(color: Color, size: Dp){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = { lyricsOpen = !lyricsOpen },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(size)
                .bounceClick(),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Crossfade(targetState = lyricsOpen, label = "Lyrics Icon Crossfade") { open ->
                when (open) {
                    true -> Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.lyrics_active),
                        tint = color.copy(
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DownloadButton(color: Color, size: Dp) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    downloadNavidromeSong(context, SongHelper.currentSong)
                }
            },
            enabled = !SongHelper.currentSong.navidromeID.startsWith("Local_"),
            shape = CircleShape,
            modifier = Modifier
                .height(size)
                .bounceClick(),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                tint = if (SongHelper.currentSong.navidromeID.startsWith("Local_")) color.copy(alpha = 0.25f) else color.copy(alpha = 0.5f),
                contentDescription = "Download Song",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        }
    }
}

@Composable
fun ShuffleButton(
    color: Color, mediaController: MediaController?, size: Dp
){
    Button(
        onClick = {
            shuffleSongs.value = !shuffleSongs.value
            mediaController?.shuffleModeEnabled = shuffleSongs.value
        },
        shape = CircleShape,
        modifier = Modifier
            .size(size),
        contentPadding = PaddingValues(0.dp),
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

@Composable
fun RepeatButton(
    color: Color, mediaController: MediaController?, size: Dp,
) {
    Button(
        onClick = {
            mediaController?.repeatMode =
                if (mediaController.repeatMode == Player.REPEAT_MODE_OFF)
                    Player.REPEAT_MODE_ONE
                else
                    Player.REPEAT_MODE_OFF
        },
        shape = CircleShape,
        modifier = Modifier
            .size(size),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.round_repeat_28),
            tint = color.copy(if (mediaController?.repeatMode == Player.REPEAT_MODE_ONE) 1f else 0.5f),
            contentDescription = "Toggle Repeat",
            modifier = Modifier
                .height(size)
                .size(size)
        )
    }
}