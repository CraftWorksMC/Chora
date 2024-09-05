package com.craftworks.music.ui.playing

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
fun NowPlaying_Background(
    mediaController: MediaController? = rememberManagedMediaController().value
) {
    val context = LocalContext.current
    // region playerStatus
    val playerStatus = remember { mutableStateOf("") }

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
    //endregion

    val backgroundStyle by SettingsManager(context).npBackgroundFlow.collectAsState("Plain")

    when (backgroundStyle){
        "Plain"         -> PlainBG() //Keeping it in case i want to do more
        "Static Blur"   -> StaticBlurBG()
        "Animated Blur" -> AnimatedBG(mediaController)
    }
}

@Composable
private fun PlainBG() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {

    }
}

@Composable
private fun StaticBlurBG(){
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(SongHelper.currentSong.imageUrl)
                .size(64)
                .crossfade(true)
                .build(),
            contentDescription = "Blurred Background Image",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .fillMaxSize()
                .blur(64.dp)
                .alpha(0.5f)
        )
    }
}

// Apple Music - Like Animated Background.
// Heavily inspired by this implementation:
// https://www.cephalopod.studio/blog/swiftui-aurora-background-animation

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimatedBG(
    mediaController: MediaController? = null
) {
    Log.d("RECOMPOSITION", "Recomposing Animated BG.")

    // Get Song Colors
    val context = LocalContext.current
    var colors by remember {
        mutableStateOf(listOf(Color.Cyan, Color.Green, Color.Yellow, Color.Cyan))
    }
    val isDarkMode = isSystemInDarkTheme()

    LaunchedEffect(SongHelper.currentSong.imageUrl) {
        colors = withContext(Dispatchers.IO) {
            extractColorsFromUri(SongHelper.currentSong.imageUrl, context, isDarkMode)
        }
    }

    //TODO: Change speed based on track volume

    val backgroundColor by remember(colors) {
        derivedStateOf { colors.firstOrNull() ?: Color.Transparent }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        colors.take(4).forEachIndexed { index, animatedColor ->
            key(index) {
                Cloud(
                    color = animatedColor,
                    rotationStart = index * 120f,
                    duration = 30 + (index * 10),
                    alignment = when (index) {
                        0 -> Alignment.BottomEnd
                        1 -> Alignment.TopEnd
                        2 -> Alignment.BottomStart
                        else -> Alignment.TopStart
                    }
                )
            }
        }
    }
}

@Composable
private fun Cloud(
    color: Color, rotationStart: Float, duration: Int, alignment: Alignment
) {
    val offsets = remember {
        Pair(Random.nextFloat() * 300f - 150f, Random.nextFloat() * 300f - 150f)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = rotationStart,
        targetValue = rotationStart + 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration * 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )
    val animatedColor by animateColorAsState(
        targetValue = color, animationSpec = tween(1000, 0, EaseInOut), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(96.dp), contentAlignment = alignment
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = animatedRotation
                translationX = offsets.first.dp.toPx()
                translationY = offsets.second.dp.toPx()
            }
            .background(animatedColor.copy(alpha = 0.75f), CircleShape))
    }
}

suspend fun extractColorsFromUri(uri: String, context: android.content.Context, isDarkMode: Boolean): List<Color> {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .size(64)
        .data(uri)
        .allowHardware(false) // Disable hardware bitmaps.
        .build()

    val result = (loader.execute(request) as? SuccessResult)?.drawable
    val bitmap = result?.toBitmap()

    return bitmap?.let { bitmapImage ->
        val palette = Palette.from(bitmapImage).generate()
        listOfNotNull(
            palette.vibrantSwatch?.rgb?.let { Color(it) },
            if (isDarkMode)
                palette.darkVibrantSwatch?.rgb?.let { Color(it) }
            else
                palette.lightVibrantSwatch?.rgb?.let { Color(it) },
            palette.lightVibrantSwatch?.rgb?.let { Color(it) },
            if (isDarkMode)
                palette.darkMutedSwatch?.rgb?.let { Color(it) }
            else
                palette.lightMutedSwatch?.rgb?.let { Color(it) })
    } ?: listOf(Color.Gray)
}