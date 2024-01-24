package com.craftworks.music.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Song
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.navidrome.downloadNavidromeSong
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import com.craftworks.music.playingSong
import com.craftworks.music.repeatSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.sliderPos
import com.craftworks.music.songState
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.showMoreInfo
import com.craftworks.music.ui.screens.useNavidromeServer
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

var bitmap = mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=640dp,height=540dp,dpi=320", showSystemUi = true
)
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
    scaffoldState: BottomSheetScaffoldState? = rememberBottomSheetScaffoldState()
){

    var lyricsOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box (modifier = Modifier
        .wrapContentHeight()
        .fillMaxWidth()) {


        // BLURRED BACKGROUND
        if (backgroundType.value == "Static Blur"){
            AsyncImage(
                model = playingSong.selectedSong?.imageUrl,
                contentDescription = "Blurred Background",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(128.dp)
                    .alpha(0.5f))
        }

        // MOVING BLURRED BACKGROUND
        // BASED ON: https://gist.github.com/KlassenKonstantin/d5f6ed1d74b3ddbdca699d66c6b9a3b2
        if (backgroundType.value == "Animated Blur"){
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                var size by remember { mutableStateOf(Size.Zero) }

                if (playingSong.selectedSong?.imageUrl == Uri.EMPTY) return@Surface

                //var bitmap by remember { mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))}
                LaunchedEffect(playingSong.selectedSong?.imageUrl){
                    bitmap.value =
                        if (useNavidromeServer.value)
                            getNavidromeBitmap(context)
                        else //Don't crash if there's no album art!
                            try{ MediaStore.Images.Media.getBitmap(context.contentResolver, playingSong.selectedSong?.imageUrl) }
                            catch (_:FileNotFoundException) { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
                }

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
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.75f)){
                        drawRect(brushA)
                        drawRect(brushMask, blendMode = BlendMode.DstOut)
                        drawRect(brushB, blendMode = BlendMode.DstAtop)
                    }
                }
            }
        }

        // ANIMATED VALUES
        val miniPlayerAlpha: Float by animateFloatAsState(if (scaffoldState!!.bottomSheetState.targetValue == SheetValue.Expanded) 0f else 1f,
            label = "Animated Alpha"
        )
        val miniPlayerPadding: Float by animateFloatAsState(if (lyricsOpen && scaffoldState!!.bottomSheetState.targetValue == SheetValue.Expanded) 32f else 0f,
            label = "Animated Top Padding"
        )
        val topPaddingExpandedPlayer: Float by animateFloatAsState(if (scaffoldState!!.bottomSheetState.targetValue == SheetValue.Expanded) 48f else 84f,
            label = "Animated Height"
        )



        /* MINI-PLAYER UI */
        Box(modifier = Modifier
            .height(72.dp + miniPlayerPadding.dp)
            .padding(top = miniPlayerPadding.dp)
            .fillMaxWidth()
            .alpha(if (lyricsOpen && LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 1f else miniPlayerAlpha)
            .clickable {
                coroutineScope.launch {
                    if (scaffoldState?.bottomSheetState?.currentValue == SheetValue.PartiallyExpanded) {
                        scaffoldState.bottomSheetState.expand()
                    }
                }
            }, contentAlignment = Alignment.Center) {
            Row {
                AsyncImage(
                    model = playingSong.selectedSong?.imageUrl,
                    placeholder = painterResource(R.drawable.placeholder),
                    contentDescription = "Album Image",
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(72.dp)
                        .width(72.dp)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    playingSong.selectedSong?.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                    playingSong.selectedSong?.artist?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight(), verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (songState)
                            ImageVector.vectorResource(R.drawable.round_pause_24)
                        else
                            Icons.Rounded.PlayArrow,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Play/Pause",
                        modifier = Modifier
                            .height(48.dp)
                            .size(48.dp)
                            .clickable {
                                songState = !songState
                            }
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        /* EXPANDED-PLAYER UI */
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE){
            // VERTICAL PHONES
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(0.dp, topPaddingExpandedPlayer.dp, 0.dp, 0.dp), contentAlignment = Alignment.TopCenter) {
                Column {
                    Crossfade(targetState = lyricsOpen, label = "Lyrics View", modifier = Modifier.wrapContentHeight()) { screen ->
                        when (screen) {
                            true -> LyricsView()
                            false -> NormalSongView()
                        }
                    }


                    /* Progress Bar */
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        SliderUpdating()
                    }

                    /* Buttons */
                    Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {
                        /* MAIN ACTIONS */
                        Row(modifier = Modifier
                            .height(98.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {

                            /* Shuffle Button */
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                            {
                                Button(
                                    onClick = { shuffleSongs.value = !shuffleSongs.value
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    // Inner content including an icon and a text label
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(if (shuffleSongs.value) 1f else 0.5f),
                                        contentDescription = "Previous Song",
                                        modifier = Modifier
                                            .height(32.dp)
                                            .size(32.dp)
                                    )
                                }
                            }

                            /* Previous Song Button */
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                            {
                                Button(
                                    onClick = {
                                        if (playingSong.selectedSong?.isRadio == true) return@Button
                                        SongHelper.previousSong(song)
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    // Inner content including an icon and a text label
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
                                    onClick = { songState = !songState },
                                    shape = CircleShape,
                                    modifier = Modifier.size(92.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    // Inner content including an icon and a text label
                                    Icon(
                                        imageVector = if (songState)
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
                                        if (playingSong.selectedSong?.isRadio == true) return@Button
                                        SongHelper.nextSong(song)
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    // Inner content including an icon and a text label
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_skip_next_24),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        contentDescription = "Play/Pause",
                                        modifier = Modifier
                                            .height(48.dp)
                                            .size(48.dp)
                                    )
                                }
                            }

                            /* Repeat Button */
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                            {
                                Button(
                                    onClick = { repeatSong.value = !repeatSong.value },
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    // Inner content including an icon and a text label
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_repeat_28),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(if (repeatSong.value) 1f else 0.5f),
                                        contentDescription = "Previous Song",
                                        modifier = Modifier
                                            .height(32.dp)
                                            .size(32.dp)
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier
                            .height(64.dp)
                            .width(256.dp)
                            .weight(1f)
                            .padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                            /* Show/Hide Lyrics */
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                            {
                                Button(
                                    onClick = { lyricsOpen = !lyricsOpen },
                                    shape = CircleShape,
                                    modifier = Modifier.height(64.dp),
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
                                                    .height(52.dp)
                                                    .size(52.dp)
                                            )

                                            false -> Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.lyrics_inactive),
                                                tint = MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.5f
                                                ),
                                                contentDescription = "View Lyrics",
                                                modifier = Modifier
                                                    .height(52.dp)
                                                    .size(52.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            /* Download Song */
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                            {
                                Button(
                                    onClick = { downloadNavidromeSong("${navidromeServerIP.value}/rest/download.view?id=${playingSong.selectedSong?.navidromeID}&submission=true&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora") },
                                    shape = CircleShape,
                                    modifier = Modifier.height(64.dp),
                                    contentPadding = PaddingValues(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    // Inner content including an icon and a text label
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        contentDescription = "Download Song",
                                        modifier = Modifier
                                            .height(52.dp)
                                            .size(52.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            // LANDSCAPE TV-TABLET
            Box(modifier = Modifier
                .width(640.dp)
                .fillMaxHeight()
                .padding(0.dp, topPaddingExpandedPlayer.dp, 0.dp, 0.dp), contentAlignment = Alignment.TopCenter) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                        if (scaffoldState?.bottomSheetState?.currentValue == SheetValue.Expanded) {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                    },
                    modifier = Modifier
                        .offset(y = -(48).dp)
                        .alpha(1 - miniPlayerAlpha),
                    enabled = scaffoldState?.bottomSheetState?.currentValue == SheetValue.Expanded
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
                Row {
                    Crossfade(targetState = lyricsOpen, label = "Lyrics View", modifier = Modifier
                        .wrapContentHeight()
                        .width(256.dp)) { screen ->
                        when (screen) {
                            true -> LandscapeLyricsView()
                            false -> LandscapeNormalSongView()
                        }
                    }

                    /* Song Title + Artist*/
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // TITLE AND ARTIST
                        Column (Modifier.fillMaxWidth()) {
                            playingSong.selectedSong?.title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
                                )
                            }
                            playingSong.selectedSong?.artist?.let {
                                Text(
                                    text = it + " • " + playingSong.selectedSong?.year,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
                                )
                            }
                            if (showMoreInfo.value) {
                                playingSong.selectedSong?.format?.let {
                                    Text(
                                        text = it + " • " + playingSong.selectedSong?.bitrate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }

                        /* Progress Bar */
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            LandscapeSliderUpdating()
                        }

                        /* Buttons */
                        Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {
                            /* MAIN ACTIONS */
                            Row(modifier = Modifier
                                .height(96.dp)
                                .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {



                                /* Previous Song Button */
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center)
                                {
                                    Button(
                                        onClick = {
                                            if (playingSong.selectedSong?.isRadio == true) return@Button
                                            SongHelper.previousSong(song)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(72.dp),
                                        contentPadding = PaddingValues(2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                    ) {
                                        // Inner content including an icon and a text label
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
                                        onClick = { songState = !songState },
                                        shape = CircleShape,
                                        modifier = Modifier.size(92.dp),
                                        contentPadding = PaddingValues(2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent
                                        )
                                    ) {
                                        // Inner content including an icon and a text label
                                        Icon(
                                            imageVector = if (songState)
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
                                            if (playingSong.selectedSong?.isRadio == true) return@Button
                                            SongHelper.nextSong(song)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(72.dp),
                                        contentPadding = PaddingValues(2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                    ) {
                                        // Inner content including an icon and a text label
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.round_skip_next_24),
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            contentDescription = "Play/Pause",
                                            modifier = Modifier
                                                .height(48.dp)
                                                .size(48.dp)
                                        )
                                    }
                                }
                            }
                            // BUTTONS
                            Box(modifier = Modifier
                                .height(64.dp)
                                .width(256.dp)
                                .padding(horizontal = 24.dp)) {
                                /* Show/Hide Lyrics */
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.CenterStart), contentAlignment = Alignment.Center)
                                {
                                    Button(
                                        onClick = { lyricsOpen = !lyricsOpen },
                                        shape = CircleShape,
                                        modifier = Modifier.height(48.dp),
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
                                                        .height(52.dp)
                                                        .size(52.dp)
                                                )

                                                false -> Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.lyrics_inactive),
                                                    tint = MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    contentDescription = "View Lyrics",
                                                    modifier = Modifier
                                                        .height(52.dp)
                                                        .size(52.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                /* Shuffle Button */
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.Center), contentAlignment = Alignment.Center)
                                {
                                    Button(
                                        onClick = { shuffleSongs.value = !shuffleSongs.value
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        contentPadding = PaddingValues(2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                    ) {
                                        // Inner content including an icon and a text label
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                            tint = MaterialTheme.colorScheme.onBackground.copy(if (shuffleSongs.value) 1f else 0.5f),
                                            contentDescription = "Previous Song",
                                            modifier = Modifier
                                                .height(48.dp)
                                                .size(48.dp)
                                        )
                                    }
                                }
                                /* Repeat Button */
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.CenterEnd), contentAlignment = Alignment.Center)
                                {
                                    Button(
                                        onClick = { repeatSong.value = !repeatSong.value },
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp),
                                        contentPadding = PaddingValues(2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                    ) {
                                        // Inner content including an icon and a text label
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.round_repeat_28),
                                            tint = MaterialTheme.colorScheme.onBackground.copy(if (repeatSong.value) 1f else 0.5f),
                                            contentDescription = "Previous Song",
                                            modifier = Modifier
                                                .height(48.dp)
                                                .size(48.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderUpdating(){
    val animatedSliderValue by animateFloatAsState(targetValue = sliderPos.intValue.toFloat(),
        label = "Smooth Slider Update"
    )
    Slider(
        modifier = Modifier
            .width(320.dp)
            .height(12.dp),
        value = animatedSliderValue,
        onValueChange = {
            sliderPos.intValue = it.toInt()
            SongHelper.currentPosition = it.toLong()
            SongHelper.isSeeking = true },
        onValueChangeFinished = {
            SongHelper.isSeeking = false
            SongHelper.player.seekTo(sliderPos.intValue.toLong())},
        valueRange = 0f..(playingSong.selectedSong?.duration?.toFloat() ?: 0f),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.onBackground,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
        ),
        thumb = {},
        track = { sliderPositions ->
            SliderDefaults.Track(
                modifier = Modifier
                    .scale(scaleX = 1f, scaleY = 1.25f)
                    .clip(RoundedCornerShape(12.dp)),
                sliderPositions = sliderPositions,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                )
            )
        },
    )
    Box (modifier = Modifier.width(300.dp)) {
        Text(
            text = formatMilliseconds(sliderPos.intValue.toFloat()),
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = formatMilliseconds(playingSong.selectedSong?.duration?.toFloat() ?: 0f),
            fontWeight = FontWeight.Thin,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(64.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeSliderUpdating(){
    val animatedSliderValue by animateFloatAsState(targetValue = sliderPos.intValue.toFloat(),
        label = "Smooth Slider Update"
    )
    Slider(
        modifier = Modifier
            .width(480.dp)
            .height(24.dp),
        value = animatedSliderValue,
        onValueChange = {
            sliderPos.intValue = it.toInt()
            SongHelper.currentPosition = it.toLong()
            SongHelper.isSeeking = true },
        onValueChangeFinished = {
            SongHelper.isSeeking = false
            SongHelper.player.seekTo(sliderPos.intValue.toLong())},
        valueRange = 0f..(playingSong.selectedSong?.duration?.toFloat() ?: 0f),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.onBackground,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
        ),
        thumb = {},
        track = { sliderPositions ->
            SliderDefaults.Track(
                modifier = Modifier
                    .scale(scaleX = 1f, scaleY = 1.25f)
                    .clip(RoundedCornerShape(12.dp)),
                sliderPositions = sliderPositions,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                )
            )
        },
    )
    Box (modifier = Modifier.width(460.dp)) {
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
            text = formatMilliseconds(playingSong.selectedSong?.duration?.toFloat() ?: 0f),
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

@Composable
fun NormalSongView() {
    Column(modifier = Modifier.height(420.dp)) {
        /* Album Cover */
        Box(
            modifier = Modifier
                .height(320.dp)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = playingSong.selectedSong?.imageUrl,
                contentDescription = "Album Cover",
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .width(320.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    .background(MaterialTheme.colorScheme.background)
                    .clip(RoundedCornerShape(24.dp))
            )
        }
        /* Song Title + Artist*/
        Column(
            modifier = Modifier
                .padding(top = 4.dp)
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            playingSong.selectedSong?.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            playingSong.selectedSong?.artist?.let {
                Text(
                    text = it + " • " + playingSong.selectedSong?.year,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (showMoreInfo.value) {
                playingSong.selectedSong?.format?.let {
                    Text(
                        text = it + " • " + playingSong.selectedSong?.bitrate,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Thin,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}
@Composable
fun LandscapeNormalSongView() {
    Column(modifier = Modifier
        .fillMaxHeight()
        .padding(start = 24.dp)
        .width(256.dp), verticalArrangement = Arrangement.Top) {
        /* Album Cover */
        Box(
            modifier = Modifier
                .height(256.dp)
                .width(256.dp), contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = playingSong.selectedSong?.imageUrl,
                contentDescription = "Album Cover",
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .width(256.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    .background(MaterialTheme.colorScheme.background)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

    }

}
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LyricsView() {
    /* LYRICS BOX */
    val topBottomFade = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Red, 0.85f to Color.Red, 1f to Color.Transparent)
    val state = rememberScrollState()
    var currentLyricIndex by remember { mutableIntStateOf(0) }

    /* SCROLL VARS */
    val lineHeight = 30
    val dpToSp = lineHeight * LocalContext.current.resources.displayMetrics.density
    val pxValue = with(LocalDensity.current) { dpToSp.dp.toPx() }

    LaunchedEffect(currentLyricIndex) {
        state.animateScrollTo((pxValue * currentLyricIndex - 128).toInt())
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .height(420.dp)
        .padding(horizontal = 12.dp)
        .padding(top = 48.dp)
        .fadingEdge(topBottomFade)
        .verticalScroll(state), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        if (SyncedLyric.size > 1) {
            Box(
                modifier = Modifier
                    .height(dpToSp.dp)
                    .fillMaxWidth()
            )
            SyncedLyric.forEachIndexed { index, lyric ->
                val lyricAlpha: Float by animateFloatAsState(
                    if (lyric.isCurrentLyric) 1f else 0.5f,
                    label = "Current Lyric Alpha"
                )
                val lyricScale: Float by animateFloatAsState(
                    if (lyric.isCurrentLyric) 1f else 0.9f,
                    label = "Current Lyric Scale"
                )

                if (lyric.isCurrentLyric) currentLyricIndex = index

                Box(modifier = Modifier
                    .height((dpToSp).dp)
                    .clickable {
                        SongHelper.player.seekTo(lyric.timestamp.toLong())
                        currentLyricIndex = index
                        lyric.isCurrentLyric = false
                    }, contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyric.content == "") "• • •" else lyric.content,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(lyricAlpha),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(lyricScale),
                        textAlign = TextAlign.Center,
                        lineHeight = lineHeight.sp
                    )
                }
            }
        } else {
            Text(
                text = songLyrics.SongLyrics,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = lineHeight.sp
            )
        }
    }
}
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LandscapeLyricsView() {
    /* LYRICS BOX */
    val topBottomFade = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Red, 0.85f to Color.Red, 1f to Color.Transparent)
    val state = rememberScrollState()
    var currentLyricIndex by remember { mutableIntStateOf(0) }

    /* SCROLL VARS */
    val lineHeight = 30
    val dpToSp = lineHeight * LocalContext.current.resources.displayMetrics.density
    val pxValue = with(LocalDensity.current) { dpToSp.dp.toPx() }

    LaunchedEffect(currentLyricIndex) {
        state.animateScrollTo((pxValue * currentLyricIndex - 64).toInt())
    }

    Column(modifier = Modifier
        .width(256.dp)
        .fillMaxHeight()
        .padding(start = 24.dp)
        .fadingEdge(topBottomFade)
        .verticalScroll(state), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally){
        if (SyncedLyric.size > 1){
            Box(modifier = Modifier
                .height(dpToSp.dp)
                .fillMaxWidth())
            SyncedLyric.forEachIndexed { index, lyric ->
                val lyricAlpha: Float by animateFloatAsState(if (lyric.isCurrentLyric) 1f else 0.5f,
                    label = "Current Lyric Alpha"
                )

                if (lyric.isCurrentLyric) currentLyricIndex = index

                Box(modifier = Modifier
                    .height((dpToSp).dp)
                    .clickable {
                        SongHelper.player.seekTo(lyric.timestamp.toLong())
                        currentLyricIndex = index
                        lyric.isCurrentLyric = false
                    }, contentAlignment = Alignment.Center){
                    Text(
                        text = if (lyric.content == "") "• • •" else lyric.content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(lyricAlpha),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        lineHeight = lineHeight.sp
                    )
                }
            }
        }
        else{
            Text(
                text = songLyrics.SongLyrics,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = lineHeight.sp
            )
        }
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

suspend fun getNavidromeBitmap(context: Context): Bitmap {
    val loading = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data("${navidromeServerIP.value}/rest/getCoverArt.view?&id=${playingSong.selectedSong?.navidromeID}&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
        .build()
    val result = (loading.execute(request) as SuccessResult).drawable
    println("GOT NAVIDROME BITMAP!!!")
    return (result as BitmapDrawable).bitmap.copy(Bitmap.Config.RGBA_F16, true)
}