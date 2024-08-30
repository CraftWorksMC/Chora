package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.SyncedLyric
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.fadingEdge
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.getNavidromeBitmap
import com.craftworks.music.sliderPos
import com.craftworks.music.ui.elements.DownloadButton
import com.craftworks.music.ui.elements.LyricsButton
import com.craftworks.music.ui.elements.MainButtons
import com.craftworks.music.ui.elements.RepeatButton
import com.craftworks.music.ui.elements.ShuffleButton
import com.craftworks.music.ui.elements.SliderUpdating
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


var bitmap = mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

var lyricsOpen by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_TELEVISION,
    device = "id:tv_1080p"
)
@Composable
fun NowPlayingContent(
    song: MediaData.Song = MediaData.Song(
        title = "Song Title",
        artist = "Song Artist",
        duration = 69420,
        imageUrl = "",
        dateAdded = "",
        year = 2024,
        album = "Album Name",
        albumId = "",
        bpm = 0,
        navidromeID = "Local",
        format = "MP3",
        parent = "",
        path = "", size = 0
    ),
    context: Context = LocalContext.current,
    scaffoldState: BottomSheetScaffoldState? = rememberBottomSheetScaffoldState(),
    snackbarHostState: SnackbarHostState? = SnackbarHostState(),
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController?
) {
    if (scaffoldState == null) return

    Log.d("RECOMPOSITION", "NowPlayingContent")
    Box {
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

            if (SongHelper.currentSong.imageUrl == "") return@LaunchedEffect

            bitmap.value =
                if (SongHelper.currentSong.navidromeID != "Local" &&
                    navidromeServersList.isNotEmpty() &&
                    SongHelper.currentSong.isRadio == false
                )
                    getNavidromeBitmap(context)
                else //Don't crash if there's no album art!
                    try {
                        Log.d("LOCAL", "Getting Local Cover Art Bitmap")
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                context.contentResolver,
                                Uri.parse(SongHelper.currentSong.imageUrl)
                            )
                        ).copy(Bitmap.Config.RGBA_F16, true)
                    } catch (_: Exception) {
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }
        }

        val backgroundType by SettingsManager(context).npBackgroundFlow.collectAsState("")

        // BLURRED BACKGROUND
        if (backgroundType == "Static Blur") {
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
        if (backgroundType == "Animated Blur") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                var size by remember { mutableStateOf(Size.Zero) }

                if (SongHelper.currentSong.imageUrl == "") return@Surface

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

        // MAIN UI
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                NowPlayingPortrait(mediaController, scaffoldState, navHostController)
            }
            else {
                //region LANDSCAPE TABLET UI
                if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION){
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
                            snackbarHostState,
                            coroutineScope,
                            mediaController
                        )
                    }
                }
                //endregion

                else {
                    NowPlaying_TV(lyricsOpen, mediaController)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NowPlaying_TV(
    collapsed: Boolean? = false,
    mediaController: MediaController?
) {
    val (prev, play, next, shuffle, replay) = remember { FocusRequester.createRefs() }

    Row(){
        Column(modifier = Modifier
            .width(512.dp)
            .padding(start = 80.dp + 6.dp),
            horizontalAlignment = Alignment.Start) {

            /* Album Cover */
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(256.dp),
                contentAlignment = Alignment.Center){
                AsyncImage(
                    model = SongHelper.currentSong.imageUrl,
                    contentDescription = "Album Cover",
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
//                LyricsView(true, mediaController)
            }

            /* Song Title + Artist*/
            Column(
                modifier = Modifier
                    .width(512.dp)
                    .fillMaxHeight()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Column(modifier = Modifier
                    .height(72.dp)
                    .padding(start = 24.dp)){
                    SongHelper.currentSong.title.let {
                        Text(
                            text = // Limit Song Title Length (if not collapsed).
                            if (it.length > 24 && collapsed == false) it.substring(0, 21) + "..."
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
                            text = //Limit the artist name length (if not collapsed).
                            if (it.length > 20 && collapsed == false)
                                it.substring(0, 17) + "..." + " • " + SongHelper.currentSong.year
                            else
                                it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                                else "",
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
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

                /* Progress Bar */
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SliderUpdating(true, mediaController)
                }

                //region BUTTONS
                Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {

                    LaunchedEffect(Unit) {
                        play.requestFocus()
                    }

                    /* MAIN ACTIONS */
                    Row(modifier = Modifier
                        .height(96.dp)
                        .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        MainButtons(mediaController, prev, play, next, shuffle)
                    }
                    // BUTTONS
                    Row(modifier = Modifier
                        .height(64.dp)
                        //.width(256.dp)
                        .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        ShuffleButton(48.dp, mediaController, play, shuffle)

                        RepeatButton(48.dp, mediaController, play, replay)

                        //DownloadButton(snackbarHostState, coroutineScope, 48.dp)
                    }
                }
                //endregion
            }
        }

//        AsyncImage(
//            model = SongHelper.currentSong.imageUrl,
//            contentDescription = "Album Cover",
//            placeholder = painterResource(R.drawable.placeholder),
//            fallback = painterResource(R.drawable.placeholder),
//            contentScale = ContentScale.FillHeight,
//            alignment = Alignment.Center,
//            modifier = Modifier
//                .padding(32.dp)
//                .weight(1f)
//                .aspectRatio(1f)
//                .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
//                .background(MaterialTheme.colorScheme.surfaceVariant)
//        )
        LyricsView(true, mediaController)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LyricsView(isLandscape: Boolean = false, mediaController: MediaController?,
               retryLyricsFocus: FocusRequester = FocusRequester(),
               playFocus: FocusRequester = FocusRequester()) {

    var currentLyricIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val lyrics by LyricsManager.Lyrics.collectAsState()

    LaunchedEffect(lyrics, sliderPos) {
        snapshotFlow { sliderPos.intValue.toLong() + 750 }.collect { currentPosition ->
                currentLyricIndex =
                    ((lyrics.indexOfFirst { it.timestamp > currentPosition }.takeIf { it >= 0 }
                        ?: lyrics.size) - 1).coerceAtLeast(0)

                // Calculate the delay between lyrics. If the nextLyricIndex is -1 then it means we've reached the end of the lyrics.
                val nextLyricIndex =
                    lyrics.indexOfFirst { it.timestamp > currentPosition }.takeIf { it >= 0 }
                        ?: lyrics.size
                val delayMillis = if (nextLyricIndex in lyrics.indices) {
                    (lyrics[nextLyricIndex].timestamp - currentPosition).coerceAtLeast(0)
                } else {
                    1000L
                }
                delay(delayMillis)
            }
    }

    val topBottomFade = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Red, 0.85f to Color.Red, 1f to Color.Transparent)
    val state = rememberLazyListState()

    LaunchedEffect(currentLyricIndex) {
        delay(100)
        state.animateScrollToItem(currentLyricIndex)
    }

    LazyColumn(
        modifier = if (isLandscape) {
            Modifier
                .widthIn(min = 256.dp)
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        }
            .fadingEdge(topBottomFade),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = state,
    ) {
        item { // For the spacing + loading indicator
            Box(modifier = Modifier.height(if (isLandscape) 32.dp else 60.dp)) {
                if (PlainLyrics == "Getting Lyrics..." && SyncedLyric.size == 0) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // For displaying Synced Lyrics
        if (lyrics.size > 1) {
            itemsIndexed(lyrics) { index, lyric ->
                val lyricAlpha: Float by animateFloatAsState(
                    if (currentLyricIndex == index) 1f else 0.5f,
                    label = "Current Lyric Alpha",
                    animationSpec = tween(1000, 0, FastOutSlowInEasing)
                )
                val scale by animateFloatAsState(
                    targetValue = if (currentLyricIndex == index) 1f else 0.9f,
                    label = "Lyric Scale Animation",
                    animationSpec = tween(1000, 0, FastOutSlowInEasing)
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .heightIn(min = 48.dp)
                        .clickable {
                            mediaController?.seekTo(lyric.timestamp.toLong())
                            currentLyricIndex = index
                        }
                        .focusable(false)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
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
        } else if (lyrics[0].timestamp == -1) {
            item { // For displaying plain lyrics
                Text(
                    text = lyrics[0].content,
                    style = if (isLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.2f)
                )
            }
        } /*else {
            item { // For the "Retry" section
                Box(
                    modifier = Modifier
                        //.height((dpToSp).dp)
                        .height(64.dp)
                        .clickable {
                            coroutineScope.launch {
                                LyricsManager.getLyrics()
                            }
                            //getLyrics()
                        }
                        .focusRequester(retryLyricsFocus)
                        .focusProperties {
                            down = playFocus
                            right = FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            up = FocusRequester.Cancel
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
        } */
    }
}

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

// Returns the normalized center item offset (-1,1)
fun LazyListLayoutInfo.normalizedItemPosition(key: Any) : Float =
    visibleItemsInfo
        .firstOrNull { it.index == key }
        ?.let {
            val center = (viewportEndOffset + viewportStartOffset - it.size) / 2F
            (it.offset.toFloat() - center) / center
        }
        ?: 0F

@Composable
fun NowPlayingLandscape(
    collapsed: Boolean? = false,
    //isPlaying: Boolean? = false,
    //song: MediaData.Song = SongHelper.currentSong,
    snackbarHostState: SnackbarHostState? = SnackbarHostState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    mediaController: MediaController?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 6.dp),
        verticalAlignment = Alignment.Top
    ) {

        /* Album Cover */
        Box(
            modifier = Modifier
                .heightIn(min = 256.dp)
                .width(256.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                if (it) {
                    LyricsView(true, mediaController)
                } else {
                    AsyncImage(
                        model = SongHelper.currentSong.imageUrl,
                        contentDescription = "Album Cover",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .size(256.dp)
                            .padding(12.dp)
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
                .fillMaxHeight()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Column(modifier = Modifier.height(72.dp)) {
                SongHelper.currentSong.title.let {
                    Text(
                        text = // Limit Song Title Length (if not collapsed).
                        if (it.length > 24 && collapsed == false) it.substring(0, 21) + "..."
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
                        text = //Limit the artist name length (if not collapsed).
                        if (it.length > 20 && collapsed == false)
                            it.substring(0, 17) + "..." + " • " + SongHelper.currentSong.year
                        else
                            it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                            else "",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                if (showMoreInfo.value) {
                    SongHelper.currentSong.format.let { format ->
                        Text(
                            text = format.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            /* Progress Bar */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SliderUpdating(true, mediaController)
            }

            //region BUTTONS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /* MAIN ACTIONS */
                Row(
                    modifier = Modifier
                        .height(96.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainButtons(mediaController)
                }
                // BUTTONS
                Row(
                    modifier = Modifier
                        .height(64.dp)
                        //.width(256.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LyricsButton(48.dp)

                    ShuffleButton(48.dp, mediaController)

                    RepeatButton(48.dp, mediaController)

                    DownloadButton(48.dp)
                }
            }
            //endregion
        }
    }
}