package com.craftworks.music.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.Song
import com.craftworks.music.data.SyncedLyric
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.providers.navidrome.downloadNavidromeSong
import com.craftworks.music.providers.navidrome.getNavidromeBitmap
import com.craftworks.music.providers.navidrome.selectedNavidromeServerIndex
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.repeatSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.sliderPos
import com.craftworks.music.ui.elements.NowPlayingLandscape
import com.craftworks.music.ui.elements.NowPlayingMiniPlayer
import com.craftworks.music.ui.elements.NowPlayingPortraitCover
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.moveClick
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.transcodingBitrate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


var bitmap = mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

var lyricsOpen by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = false, showBackground = true)
@Composable
fun NowPlayingContent(
    song: Song = Song(
        title = "Song Title",
        artist = "Song Artist",
        duration = 69420,
        imageUrl = Uri.EMPTY,
        dateAdded = "",
        year = "2023",
        album = "Album Name"
    ),
    context: Context = LocalContext.current,
    scaffoldState: BottomSheetScaffoldState? = rememberBottomSheetScaffoldState(),
    snackbarHostState: SnackbarHostState? = SnackbarHostState(),
    navHostController: NavHostController = rememberNavController()
) {
    println("Full Recompose NowPlaying.kt")

    if (scaffoldState == null) return

    Box {
        // UI PLAYING STATE
        var isPlaying by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val player = SongHelper.player
            val listener = object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    isPlaying = playWhenReady
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
            }
        }

        //region Update Content + Backgrounds
        // handle back presses
        val coroutineScope = rememberCoroutineScope()
        BackHandler(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            coroutineScope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
        }

        // Update Song Bitmap
        LaunchedEffect(SongHelper.currentSong) {
            println("Getting Cover Art Bitmap")
            bitmap.value =
                if (SongHelper.currentSong.navidromeID != "Local" &&
                    navidromeServersList.isNotEmpty() &&
                    SongHelper.currentSong.isRadio == false
                )
                    getNavidromeBitmap(context)
                else //Don't crash if there's no album art!
                    try {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                context.contentResolver,
                                SongHelper.currentSong.imageUrl
                            )
                        ).copy(Bitmap.Config.RGBA_F16, true)
                    } catch (_: Exception) {
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }
        }

        // BLURRED BACKGROUND
        if (backgroundType.value == "Static Blur") {
            Crossfade(
                SongHelper.currentSong.imageUrl,
                label = "Crossfade Static Background Image"
            ) { image ->
                AsyncImage(
                    model = image,
                    contentDescription = "Blurred Background",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(128.dp)
                        .alpha(0.5f)
                )
            }
        }

        //region MOVING BLURRED BACKGROUND
        //       BASED ON: https://gist.github.com/KlassenKonstantin/d5f6ed1d74b3ddbdca699d66c6b9a3b2
        if (backgroundType.value == "Animated Blur") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                var size by remember { mutableStateOf(Size.Zero) }

                if (SongHelper.currentSong.imageUrl == Uri.EMPTY) return@Surface

                val palette = Palette.from(bitmap.value).generate()

                val shaderA = LinearGradientShader(
                    Offset(size.width / 2f, 0f),
                    Offset(size.width / 2f, size.height),
                    listOf(
                        Color(palette.getLightVibrantColor(0)),
                        Color(palette.getDarkVibrantColor(0)),
                    ),
                    listOf(0f, 1f)
                )

                val shaderB = LinearGradientShader(
                    Offset(size.width / 2f, 0f),
                    Offset(size.width / 2f, size.height),
                    listOf(
                        Color(palette.getMutedColor(0)),
                        Color(palette.getDominantColor(0)),
                    ),
                    listOf(0f, 1f)
                )
                val shaderMask = LinearGradientShader(
                    Offset(size.width / 2f, 0f),
                    Offset(size.width / 2f, size.height),
                    listOf(
                        Color.White,
                        Color.Transparent
                    ),
                    listOf(0f, 1f)
                )

                val brushA by animateBrushRotation(shaderA, size, 20_000, true)
                val brushB by animateBrushRotation(shaderB, size, 12_000, false)
                val brushMask by animateBrushRotation(shaderMask, size, 15_000, true)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            size = Size(it.width.toFloat(), it.height.toFloat())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.75f)
                    ) {
                        drawRect(brushA)
                        drawRect(brushMask, blendMode = BlendMode.DstOut)
                        drawRect(brushB, blendMode = BlendMode.DstAtop)
                    }
                }
            }
        }
        //endregion

        //endregion

        // MINI-PLAYER
        val offsetY by animateFloatAsState(targetValue =
            if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded)
                dpToPx(72).toFloat()
            else
                0f,
            label = "Animated Top Offset")

        Box(modifier = Modifier
            .graphicsLayer { translationY = -offsetY }
            .zIndex(1f)) {
            NowPlayingMiniPlayer(scaffoldState, isPlaying)
        }

        // MAIN UI
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
        ) {
            if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                //region VERTICAL UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column {
                        // Album Art + Info
                        NowPlayingPortraitCover(navHostController, scaffoldState)

                        // Seek Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SliderUpdating()
                        }

                        //region Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // First Row
                            Row(
                                modifier = Modifier
                                    .height(98.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShuffleButton(32.dp)

                                MainButtons(song, isPlaying)

                                RepeatButton(32.dp)
                            }

                            Row(
                                modifier = Modifier
                                    .height(64.dp)
                                    .width(256.dp)
                                    .weight(1f)
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LyricsButton(64.dp)

                                DownloadButton(snackbarHostState, coroutineScope, 64.dp)
                            }
                        }
                        //endregion
                    }
                }
                //endregion

            }
            else {

                //region LANDSCAPE TV-TABLET UI
                Box(
                    modifier = Modifier
                        .width(640.dp)
                        .fillMaxHeight()
                        .padding(0.dp, 48.dp, 0.dp, 0.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Click to close
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            }
                        },
                        modifier = Modifier.offset(y = -(48).dp),
                        enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Previous Song",
                            modifier = Modifier
                                .height(32.dp)
                                .size(32.dp)
                        )
                    }

                    NowPlayingLandscape(
                        lyricsOpen || scaffoldState.bottomSheetState.targetValue != SheetValue.Expanded,
                        isPlaying,
                        song,
                        snackbarHostState,
                        coroutineScope
                    )
                }

                //endregion

            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun LyricsView(isLandscape: Boolean = false) {
    val topBottomFade = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Red, 0.85f to Color.Red, 1f to Color.Transparent)
    val state = rememberScrollState()
    var currentLyricIndex by remember { mutableIntStateOf(0) }

    /* SCROLL VARS */
    val dpToSp = 32 * LocalContext.current.resources.displayMetrics.density
    val pxValue = with(LocalDensity.current) { dpToSp.dp.toPx() }

    LaunchedEffect(currentLyricIndex) {
        state.animateScrollTo((pxValue * currentLyricIndex - if (isLandscape) 96 else pxValue.toInt()).toInt())
    }

    Column(
        modifier =
        if (isLandscape) {
            Modifier
                .width(256.dp)
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        }
            .fadingEdge(topBottomFade)
            .verticalScroll(state),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (SyncedLyric.size >= 1) {
            Box(modifier = Modifier.height(if (isLandscape) 32.dp else 60.dp))
            SyncedLyric.forEachIndexed { index, lyric ->
                val lyricAlpha: Float by animateFloatAsState(
                    if (lyric.isCurrentLyric) 1f else 0.5f,
                    label = "Current Lyric Alpha"
                )
                if (lyric.isCurrentLyric) currentLyricIndex = index

                Box(
                    modifier = Modifier
                        .height((dpToSp).dp)
                        .clickable {
                            SongHelper.player.seekTo(lyric.timestamp.toLong())
                            currentLyricIndex = index
                            lyric.isCurrentLyric = false
                        }
                        .graphicsLayer(
                            scaleX = if (lyric.isCurrentLyric) 1f else 0.9f,
                            scaleY = if (lyric.isCurrentLyric) 1f else 0.9f
                        )
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyric.content == "") "• • •" else lyric.content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(lyricAlpha),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }
            }
        }
        else {
            Box(modifier = Modifier.height(if (isLandscape) 32.dp else 60.dp))
            if (PlainLyrics == "Getting Lyrics..."){
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp),
                    strokeCap = StrokeCap.Round
                )
            }
            Text(
                text = PlainLyrics,
                style = if (isLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.2)
            )
            if (PlainLyrics == "No Lyrics / Instrumental"){
                Box(
                    modifier = Modifier
                        .height((dpToSp).dp)
                        .clickable {
                            getLyrics()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Retry",
                        style = if (isLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderUpdating(isLandscape: Boolean? = false){
    val animatedSliderValue by animateFloatAsState(targetValue = sliderPos.intValue.toFloat(),
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
            .clip(RoundedCornerShape(12.dp)),
        value = animatedSliderValue,
        onValueChange = {
            SongHelper.isSeeking = true
            sliderPos.intValue = it.toInt()
            SongHelper.currentPosition = it.toLong() },
        onValueChangeFinished = {
            SongHelper.isSeeking = false
            SongHelper.player.seekTo(sliderPos.intValue.toLong())},
        valueRange = 0f..(SongHelper.currentSong.duration.toFloat()),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.onBackground,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
        ),
        thumb = {}
    )
    Box (modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
        Text(
            text = formatMilliseconds(sliderPos.intValue.toFloat()),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = formatMilliseconds(SongHelper.currentSong.duration.toFloat()),
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

//region Reusable buttons

@Composable // Previous Song, Play/Pause, Next Song
fun MainButtons(song: Song, isPlaying: Boolean ? = false){
    // Previous Song
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                SongHelper.previousSong(song)
            },
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .bounceClick()
                .moveClick(false),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_skip_previous_24),

                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Previous Song",
                modifier = Modifier
                    .height(48.dp)
                    .size(48.dp)
            )
        }
    }

    /* Play/Pause Button */
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = { SongHelper.player.playWhenReady = !SongHelper.player.playWhenReady },
            shape = CircleShape,
            modifier = Modifier
                .size(92.dp)
                .bounceClick(),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = if (isPlaying == true)
                    ImageVector.vectorResource(R.drawable.round_pause_24)
                else
                    Icons.Rounded.PlayArrow,

                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Play/Pause",
                modifier = Modifier
                    .height(92.dp)
                    .size(92.dp)
            )
        }
    }

    /* Next Song Button */
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = {
                if (SongHelper.currentSong.isRadio == true) return@Button
                SongHelper.nextSong(song)
            },
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .bounceClick()
                .moveClick(true),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_skip_next_24),
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
fun LyricsButton(size: Dp){
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
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
fun DownloadButton(snackbarHostState: SnackbarHostState?,
                   coroutineScope: CoroutineScope,
                   size: Dp){
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = {
                if (navidromeServersList.isEmpty() || !useNavidromeServer.value || SongHelper.currentSong.navidromeID == "Local") return@Button
                if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
                    navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return@Button
                downloadNavidromeSong("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/download.view?id=${SongHelper.currentSong.navidromeID}&submission=true&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora",
                    snackbarHostState = snackbarHostState,
                    coroutineScope)
                coroutineScope.launch {
                    snackbarHostState?.showSnackbar("Downloading Started")
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
@Composable
fun ShuffleButton(size: Dp){
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = {
                shuffleSongs.value = !shuffleSongs.value
                SongHelper.player.shuffleModeEnabled = shuffleSongs.value
            },
            shape = CircleShape,
            modifier = Modifier.size(size),
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
@Composable
fun RepeatButton(size: Dp){
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
    {
        Button(
            onClick = {
                repeatSong.value = !repeatSong.value
                SongHelper.player.repeatMode =
                    if (repeatSong.value)
                        Player.REPEAT_MODE_ONE
                    else
                        Player.REPEAT_MODE_OFF
                      },
            shape = CircleShape,
            modifier = Modifier.size(size),
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

//endregion

@Composable
fun animateBrushRotation(
    shader: Shader,
    size: Size,
    duration: Int,
    clockwise: Boolean
): State<ShaderBrush> {
    val infiniteTransition = rememberInfiniteTransition(label = "Animated Blurred Background")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * if (clockwise) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Blur Animation"
    )

    return remember(shader, size) {
        derivedStateOf {
            val matrix = Matrix().apply {
                postRotate(angle, size.width / 2, size.height / 2)
            }
            shader.setLocalMatrix(matrix)
            ShaderBrush(shader)
        }
    }
}

@Composable
fun dpToPx(dp: Int): Int {
    return with(LocalDensity.current) { dp.dp.toPx() }.toInt()
}