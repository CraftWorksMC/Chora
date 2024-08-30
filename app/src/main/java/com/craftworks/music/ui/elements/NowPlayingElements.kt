package com.craftworks.music.ui.elements

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
import com.craftworks.music.repeatSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.sliderPos
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import com.craftworks.music.ui.playing.LyricsView
import com.craftworks.music.ui.playing.lyricsOpen
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingPortraitCover (
    navHostController: NavHostController = rememberNavController(),
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    mediaController: MediaController?
){
    val textFadingEdge = Brush.horizontalGradient(0.85f to Color.Red, 1f to Color.Transparent)

    Column(modifier = Modifier.heightIn(min=420.dp)) {
        /* Album Cover */
        Box(modifier = Modifier
            .heightIn(min = 320.dp)
            .fillMaxWidth(),
            contentAlignment = Alignment.Center){
            AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                if (it){
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                        contentAlignment = Alignment.Center){
                        LyricsView(false, mediaController)
                    }
                }
                else {
                    AsyncImage(
                        model = SongHelper.currentSong.imageUrl,
                        contentDescription = "Album Cover",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        /* Song Title + Artist*/
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            SongHelper.currentSong.title.let {
                Text(
                    text =
                    if (SongHelper.currentSong.isRadio == true)
                        it.split(" - ").last()
                    else
                        it,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Visible,
                    softWrap = false,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingEdge(textFadingEdge)
                )
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .fadingEdge(textFadingEdge)) {
                val coroutine = rememberCoroutineScope()
                SongHelper.currentSong.artist.let { artistName ->
                    Text(
                        text = artistName,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedArtist =
                                    artistList.firstOrNull() {
                                        it.name.equals(
                                            artistName,
                                            ignoreCase = true
                                        )
                                    }!!
                                coroutine.launch {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                                navHostController.navigate(Screen.AristDetails.route) {
                                    launchSingleTop = true
                                }
                            }
                    )
                }
                if (SongHelper.currentSong.year != 0){
                    SongHelper.currentSong.year?.let { year ->
                        Text(
                            text = " • $year",
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Visible,
                            softWrap = false,
                            textAlign = TextAlign.Start,
                            modifier = Modifier

                        )
                    }
                }
            }

            if (showMoreInfo.value) {
                Text(
                    text = "${SongHelper.currentSong.format} • ${SongHelper.currentSong.bitrate} • ${
                        if (SongHelper.currentSong.navidromeID == "Local")
                            stringResource(R.string.Source_Local)
                        else
                            stringResource(R.string.Source_Navidrome)
                    } ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingMiniPlayer(
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    mediaController: MediaController?
) {
    Log.d("RECOMPOSITION", "Mini Player")

    var offset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(scaffoldState.bottomSheetState.targetValue) {
        offset = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 72.dp.value.toFloat() else 0f
    }

    val animatedOffset by animateFloatAsState(
        targetValue = offset,
        label = "Animated Top Offset"
    )

    val coroutineScope = rememberCoroutineScope()

    Box (modifier = Modifier
        .graphicsLayer {
            translationY = -animatedOffset.dp.toPx()
        }
        .zIndex(1f)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        .height(72.dp)
        .fillMaxWidth()
        .clickable {
            coroutineScope.launch {
                if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded)
                    scaffoldState.bottomSheetState.expand()
                else
                    scaffoldState.bottomSheetState.partialExpand()
            }
        }) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)) {
            // Album Image
            AsyncImage(
                model = SongHelper.currentSong.imageUrl,
                contentDescription = "Album Cover",
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    //.shadow(4.dp, RoundedCornerShape(6.dp), clip = true)
                    .background(MaterialTheme.colorScheme.surfaceVariant)

            )

            // Title + Artist
            Column(modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f)) {
                SongHelper.currentSong.title.let {
                    Text(
                        text = //Limit the artist name length.
                        if (SongHelper.currentSong.isRadio == true)
                            it.split(" - ").last()
                        else
                            if (it.length > 24) it.substring(0, 21) + "..."
                            else it,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                SongHelper.currentSong.artist.let {
                    Text(
                        text = //Limit the artist name length.
                        if (it.isBlank()) ""
                        else if (it.length > 24)
                            it.substring(0, 21) + "..." + " • " + SongHelper.currentSong.year
                        else
                            it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                                 else "",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Play/Pause Icon
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                PlayPauseButtonUpdating(mediaController, 48.dp)
//                Icon(
//                    imageVector = if (isPlaying.value)
//                        ImageVector.vectorResource(R.drawable.media3_notification_pause)
//                    else
//                        Icons.Rounded.PlayArrow,
//                    tint = MaterialTheme.colorScheme.onBackground,
//                    contentDescription = "Play/Pause",
//                    modifier = Modifier
//                        .height(48.dp)
//                        .size(48.dp)
//                        .bounceClick()
//                        .clip(RoundedCornerShape(12.dp))
//                        .clickable {
//                            mediaController?.playWhenReady =
//                                !(mediaController?.playWhenReady ?: true)
//                        }
//                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderUpdating(isLandscape: Boolean? = false, mediaController: MediaController?){

    LaunchedEffect(mediaController) {
        while (true) {
            delay(1000)
            if (mediaController?.isPlaying == true) {
                sliderPos.intValue = mediaController.currentPosition.toInt()
            }
        }
    }

    val animatedSliderValue by animateFloatAsState(
        targetValue = sliderPos.intValue.toFloat(),
        label = "Smooth Slider Update"
    )
    val sliderHeight = if (isLandscape == true) 24.dp else 12.dp


    Slider(
        enabled = (transcodingBitrate.value == "No Transcoding" || SongHelper.currentSong.isRadio == false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(sliderHeight)
            .scale(scaleX = 1f, scaleY = 1.25f)
            .clip(RoundedCornerShape(12.dp))
            .focusable(false),
        value = animatedSliderValue,
        onValueChange = {
            SongHelper.isSeeking = true
            sliderPos.intValue = it.toInt()
        },
        onValueChangeFinished = {
            SongHelper.isSeeking = false
            mediaController?.seekTo(sliderPos.intValue.toLong())
        },
        valueRange = 0f..(SongHelper.currentSong.duration.toFloat() * 1000),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.onBackground,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
        ),
        thumb = {}
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = formatMilliseconds(sliderPos.intValue / 1000),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = formatMilliseconds(SongHelper.currentSong.duration),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable // Previous Song, Play/Pause, Next Song
fun MainButtons(mediaController: MediaController?,
                prev: FocusRequester = FocusRequester(),
                play: FocusRequester = FocusRequester(),
                next: FocusRequester = FocusRequester(),
                shuffle: FocusRequester = FocusRequester()
){

    // Previous Song
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                mediaController?.seekToPreviousMediaItem()
                println("MediaController has previous media item? ${mediaController?.hasPreviousMediaItem()}")
                println("MediaController previous item index? ${mediaController?.previousMediaItemIndex}")
            },
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .bounceClick()
                .moveClick(false)
                .focusRequester(prev)
                .focusProperties {
                    right = play
                    down = shuffle
                    up = FocusRequester.Cancel
                },
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_previous),

                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Previous Song",
                modifier = Modifier
                    .height(48.dp)
                    .size(48.dp)
            )
        }
    }

    PlayPauseButtonUpdating(mediaController, 92.dp)

    /* Next Song Button */
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                mediaController?.seekToNextMediaItem()
            },
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .bounceClick()
                .moveClick(true)
                .focusRequester(next)
                .focusProperties {
                    left = play
                    down = shuffle
                    up = FocusRequester.Cancel
                },
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_next),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Next Song",
                modifier = Modifier
                    .height(48.dp)
                    .size(48.dp)
            )
        }
    }
}

@Composable
fun PlayPauseButtonUpdating(mediaController: MediaController?, size: Dp){
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
        Log.d("RECOMPOSITION", "Registered new listener for mediaController!")

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center
    )
    {
        Button(
            onClick = {
                mediaController?.playWhenReady = !(mediaController?.playWhenReady ?: true)
            },
            shape = CircleShape,
            modifier = Modifier
                .size(size)
                .bounceClick(),
            contentPadding = PaddingValues(2.dp),
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
                    tint = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun LyricsButton(size: Dp){
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
                        tint = MaterialTheme.colorScheme.onBackground.copy(
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
fun DownloadButton(size: Dp){
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
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
fun ShuffleButton(size: Dp, mediaController: MediaController?,
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
                tint = MaterialTheme.colorScheme.onBackground.copy(if (shuffleSongs.value) 1f else 0.5f),
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
fun RepeatButton(size: Dp, mediaController: MediaController?,
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
                tint = MaterialTheme.colorScheme.onBackground.copy(if (repeatSong.value) 1f else 0.5f),
                contentDescription = "Toggle Repeat",
                modifier = Modifier
                    .height(size)
                    .size(size)
            )
        }
    }
}