package com.craftworks.music.ui.playing

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Preview
@Stable
@Composable
fun NowPlaying_Background(
    mediaController: MediaController? = null
) {
    val context = LocalContext.current
    //region Player Status
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

    //region Get Song Colors
    var colors by remember {
        mutableStateOf(listOf(Color.Gray, Color.Red, Color.Blue, Color.Cyan))
    }
    LaunchedEffect(SongHelper.currentSong.imageUrl) {
        if (SongHelper.currentSong.imageUrl.isBlank()) return@LaunchedEffect
        launch {
            colors = extractColorsFromUri(SongHelper.currentSong.imageUrl, context)
        }

    }
    //endregion
    val backgroundStyle by SettingsManager(context).npBackgroundFlow.collectAsState("Static Blur")

    when (backgroundStyle){
        "Plain"         -> PlainBG() //Keeping it in case i want to do more
        "Static Blur"   -> StaticBlurBG(colors)
        "Animated Blur" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AnimatedGradientBG(
            color1 = colors[0],
            color2 = colors[1]
        )
    }
}

@Stable
@Composable
private fun PlainBG() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    )
}

@Stable
@Composable
private fun StaticBlurBG(
    colors: List<Color>
) {
    Log.d("RECOMPOSITION", "Recomposing Static Blur BG")

    if (colors.size < 4) {
        println("Colors list is less than 4 colors! weird...")
        return
    }

    val animatedColors = colors.map { color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = tween(durationMillis = 1500),
            label = "Animated Colors"
        ).value
    }

//    Box(
//        Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
//    ) {
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(SongHelper.currentSong.imageUrl)
//                .size(64)
//                .crossfade(true)
//                .build(),
//            contentDescription = "Blurred Background Image",
//            contentScale = ContentScale.FillHeight,
//            modifier = Modifier
//                .fillMaxSize()
//                .blur(64.dp)
//                .alpha(0.5f)
//        )
//    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Layout(content = {
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColors[0], Color.Transparent
                            ),
                            center = Offset(this.size.width * 0.05f, this.size.height / 2),
                            radius = size.width * 2,
                        )
                    )
                })
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColors[1], Color.Transparent
                            ),
                            center = Offset(0f, this.size.height),
                            radius = size.height,
                        )
                    )
                })
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColors[2], Color.Transparent
                            ),
                            center = Offset(
                                this.size.width * 1.1f, this.size.height * .100f
                            ),
                            radius = size.height,
                        )
                    )
                })
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColors[3], Color.Transparent
                            ),
                            center = Offset(
                                this.size.width, this.size.height * 0.9f
                            ),
                            radius = size.height,
                        )
                    )
                })

        }, measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { it.place(0, 0) }
            }
        })
    }
}

@Composable
@Preview
@Stable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun AnimatedGradientBG(
    color1: Color  = Color.Red,
    color2: Color = Color.Black,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Log.d("RECOMPOSITION", "Recomposing Animated Gradient")

    val shaderCode = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float3 color1;
    uniform float3 color2;

    half4 main(vec2 fragCoord) {
        float mr = min(iResolution.x, iResolution.y);
        vec2 uv = (fragCoord * 0.75 - iResolution.xy) / mr;

        float d = -iTime * 0.5;
        float a = 0.0;
        for (float i = 0.0; i < 8.0; ++i) {
            a += cos(i - d - a * uv.x);
            d += sin(uv.y * i + a);
        }
        d += iTime * 0.5;
        
        float mixFactor = (cos(uv.x * d) + sin(uv.y * a)) * 0.5 + 0.5;
        float3 col = mix(color1, color2, mixFactor);
        
        return half4(col, 1.0);
    }
"""

    var time by remember { mutableFloatStateOf(0f) }

//    val currentColor1 by rememberUpdatedState(color1)
//    val currentColor2 by rememberUpdatedState(color2)

    val currentColor1 by animateColorAsState(
        targetValue = color1,
        animationSpec = tween(durationMillis = 1000)
    )

    val currentColor2 by animateColorAsState(
        targetValue = color2,
        animationSpec = tween(durationMillis = 1000)
    )

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            while (true) {
                withFrameMillis { frameTime ->
                    time = frameTime / 10000f
                }
            }
        }
    }

    val shader = remember { RuntimeShader(shaderCode) }

    Canvas(modifier = modifier) {
        shader.setFloatUniform(
            "iResolution",
            size.width,
            size.height
        )
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform(
            "color1",
            currentColor1.red,
            currentColor1.green,
            currentColor1.blue
        )
        shader.setFloatUniform(
            "color2",
            currentColor2.red,
            currentColor2.green,
            currentColor2.blue
        )
        drawRect(
            brush = ShaderBrush(shader),
            size = size
        )
    }
}


suspend fun extractColorsFromUri(uri: String, context: android.content.Context): List<Color> {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .size(64)
        .data(uri)
        .allowHardware(false) // Disable hardware bitmaps.
        .build()

    val result = (loader.execute(request) as? SuccessResult)?.drawable
    val bitmap = result?.toBitmap()

//    return bitmap?.let { bitmapImage ->
//        val palette = Palette.from(bitmapImage).generate()
//        listOfNotNull(
//            palette.vibrantSwatch?.rgb?.let { Color(it) },
//            if (isDarkMode)
//                palette.darkVibrantSwatch?.rgb?.let { Color(it) }
//            else
//                palette.lightVibrantSwatch?.rgb?.let { Color(it) },
//            palette.lightVibrantSwatch?.rgb?.let { Color(it) },
//            if (isDarkMode)
//                palette.darkMutedSwatch?.rgb?.let { Color(it) }
//            else
//                palette.lightMutedSwatch?.rgb?.let { Color(it) })
//    } ?: listOf(Color.Gray)

    return bitmap?.let { bitmapImage ->
        val palette = Palette.Builder(bitmapImage).generate()
        listOfNotNull(
            palette.vibrantSwatch?.rgb?.let { Color(it) } ?: Color.Transparent,
            palette.darkVibrantSwatch?.rgb?.let { Color(it) } ?: Color.Transparent,
            palette.lightVibrantSwatch?.rgb?.let { Color(it) } ?: Color.Transparent,
            palette.lightMutedSwatch?.rgb?.let { Color(it) } ?: Color.Transparent
        )
    } ?: listOf(Color.Blue, Color.Red, Color.Cyan, Color.Magenta)
}