@file:androidx.annotation.OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import com.craftworks.music.R
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
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
    metadata: MediaMetadata? = null
) {
    var currentValue by remember { mutableLongStateOf(1L) }
    val currentDuration by derivedStateOf {
        mediaController?.duration?.coerceAtLeast(0L)
    }

    val animatedValue by animateFloatAsState(
        targetValue = currentValue.toFloat(), label = "Smooth Slider Update"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val focused = remember { mutableStateOf(false) }

    LaunchedEffect(mediaController) {
        mediaController?.let {
            while (isActive) {
                if (it.isPlaying)
                    currentValue = it.currentPosition
                delay(1000)
            }
        }
    }

    Column(
        Modifier.focusable(false)
    ) {
        Slider(
            enabled = metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION,
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
                currentValue = it.toLong()
            },
            onValueChangeFinished = {
                mediaController?.seekTo(currentValue.toLong())
            },
            valueRange = 0f..(currentDuration?.toFloat() ?: 0f),
            colors = SliderDefaults.colors(
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.25f),
                thumbColor = color
            ),
            interactionSource = interactionSource,
        )

        // Time thingies
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = remember(currentValue) { formatMilliseconds(currentValue.toInt() / 1000) },
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Start,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(64.dp),
                maxLines = 1
            )
            Text(
                text = remember(currentDuration) { formatMilliseconds(currentDuration?.toInt()?.div(1000) ?: (currentValue/1000).toInt()) },
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun PreviousSongButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberPreviousButtonState(player)
    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(state.isEnabled).moveClick(false, state.isEnabled), enabled = state.isEnabled) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_previous),
            contentDescription = "Previous song",
            modifier = modifier,
            tint = if (state.isEnabled) color else color.copy(0.5f)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun PlayPauseButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberPlayPauseButtonState(player)
    val icon = if (state.showPlay) Icons.Rounded.PlayArrow else ImageVector.vectorResource(R.drawable.media3_notification_pause)
    val contentDescription =
        if (state.showPlay) "play"
        else "pause"
    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(state.isEnabled), enabled = state.isEnabled) {
        Icon(icon, contentDescription = contentDescription, modifier = modifier, tint = if (state.isEnabled) color else color.copy(0.5f))
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun NextSongButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberNextButtonState(player)
    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(state.isEnabled).moveClick(true, state.isEnabled), enabled = state.isEnabled) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_next),
            contentDescription = "Next song",
            modifier = modifier,
            tint = if (state.isEnabled) color else color.copy(0.5f)
        )
    }
}

@Composable
@Preview
fun LyricsButton(
    color: Color = Color.Black,
    size: Dp = 64.dp
){
    val lyrics by LyricsManager.Lyrics.collectAsStateWithLifecycle()

    Button(
        onClick = { lyricsOpen = !lyricsOpen },
        shape = RoundedCornerShape(12.dp),
        modifier = // Disable bounce click if no lyrics are present
        if (lyrics.isNotEmpty())
            Modifier
                .bounceClick()
                .height(size + 6.dp)
        else
            Modifier.height(size + 6.dp),
        contentPadding = PaddingValues(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = color.copy(0.5f),
            disabledContentColor = color.copy(0.25f)
        ),
        enabled = lyrics.isNotEmpty()
    ) {
        Crossfade(targetState = lyricsOpen, label = "Lyrics Icon Crossfade") { open ->
            when (open) {
                true -> Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.lyrics_active),
                    contentDescription = "Close Lyrics",
                    modifier = Modifier
                        .height(size)
                        .size(size)
                )

                false -> Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.lyrics_inactive),
                    contentDescription = "View Lyrics",
                    modifier = Modifier
                        .height(size)
                        .size(size)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DownloadButton(color: Color, size: Dp) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Button(
        onClick = {
            coroutineScope.launch {
                downloadNavidromeSong(context, SongHelper.currentSong)
            }
        },
        enabled = !SongHelper.currentSong.navidromeID.startsWith("Local_"),
        shape = RoundedCornerShape(12.dp),
        modifier = if (!SongHelper.currentSong.navidromeID.startsWith("Local_")) // Disable bounce click if song is local
            Modifier
                .bounceClick()
                .height(size + 6.dp)
        else
            Modifier.height(size + 6.dp),
        contentPadding = PaddingValues(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = color.copy(alpha = 0.5f),
            disabledContentColor = color.copy(alpha = 0.25f)
        )
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
            contentDescription = "Download Song",
            modifier = Modifier
                .height(size)
                .size(size)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ShuffleButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberShuffleButtonState(player)
    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(), enabled = state.isEnabled) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
            contentDescription = "Shuffle",
            modifier = modifier,
            tint = color.copy(if (state.shuffleOn) 1f else 0.5f)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun RepeatButton(player: Player, color: Color, modifier: Modifier = Modifier) {
    val state = rememberRepeatButtonState(player)
    val icon = repeatModeIcon(state.repeatModeState)
    IconButton(onClick = state::onClick, modifier = modifier.bounceClick(), enabled = state.isEnabled) {
        Icon(
            imageVector = icon,
            contentDescription = "Repeat",
            modifier = modifier,
            tint = color.copy(if (state.repeatModeState == Player.REPEAT_MODE_OFF) 0.5f else 1f)
        )
    }
}
@Composable
private fun repeatModeIcon(repeatMode: @Player.RepeatMode Int): ImageVector {
    return when (repeatMode) {
        Player.REPEAT_MODE_OFF -> ImageVector.vectorResource(R.drawable.rounded_repeat_24)
        Player.REPEAT_MODE_ONE -> ImageVector.vectorResource(R.drawable.rounded_repeat1_24)
        else -> ImageVector.vectorResource(R.drawable.rounded_repeat_24)
    }
}