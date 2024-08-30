package com.craftworks.music.ui.playing

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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

@Preview
@Composable
private fun AnimatedBG(
    mediaController: MediaController? = rememberManagedMediaController().value
) {
    //region Generate Clouds
    class CloudGenerator {
        val offset: Pair<Float, Float> = Pair(
            // Arbitrary Values
            Random.nextFloat() * 300f - 150f, Random.nextFloat() * 300f - 150f
        )
        val frameHeightRatio: Float = Random.nextFloat() * (1.0f - 0.8f) + 0.7f
    }

    @Composable
    fun Cloud(
        color: Color, rotationStart: Float, duration: Int, alignment: Alignment
    ) {
        val provider = remember { CloudGenerator() }
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .blur(64.dp), contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .size((maxHeight / provider.frameHeightRatio).value.dp)
                    .offset(provider.offset.first.dp, provider.offset.second.dp)
                    .rotate(animatedRotation)
                    .background(color.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
    //endregion

    // region Get Song Colors
    val isDarkMode = isSystemInDarkTheme()

    suspend fun extractColorsFromUri(uri: String, context: android.content.Context): List<Color> {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(uri)
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
                palette.mutedSwatch?.rgb?.let { Color(it) },
                if (isDarkMode)
                    palette.darkMutedSwatch?.rgb?.let { Color(it) }
                else
                    palette.lightMutedSwatch?.rgb?.let { Color(it) })
        } ?: listOf(Color.Gray)
    }

    val context = LocalContext.current
    var colors by remember { mutableStateOf(listOf(Color.Gray)) }

    LaunchedEffect(SongHelper.currentSong.imageUrl) {
        colors = withContext(Dispatchers.IO) {
            extractColorsFromUri(SongHelper.currentSong.imageUrl, context)
        }
    }

    val animatedColors = colors.map { targetColor ->
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 1000),
            label = "Animated Background Colors"
        )
    }
    //endregion

    //TODO: Change speed based on track volume

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedColors.firstOrNull()?.value ?: Color.White)
        )

        animatedColors.take(4).forEachIndexed { index, animatedColor ->
            Cloud(
                color = animatedColor.value,
                rotationStart = index * 90f,
                //duration = 50 + index * 10,
                duration = 30 + (SongHelper.currentSong.bpm / 10) + (index * 5),
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