package com.craftworks.music.ui.playing

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.craftworks.music.managers.SettingsManager
import kotlinx.coroutines.launch

@Preview
@Stable
@Composable
fun NowPlaying_Background(
    colorPalette: List<Color> = emptyList(),
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

    val backgroundStyle by SettingsManager(context).npBackgroundFlow.collectAsState("Static Blur")

    when (backgroundStyle){
        "Plain"         -> PlainBG()
        "Static Blur"   -> StaticBlurBG(colorPalette)
        "Animated Blur" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && colorPalette.size >= 2) AnimatedGradientBG(
            color1 = colorPalette[0],
            color2 = colorPalette.elementAtOrNull(1) ?: Color.Gray // Sometimes palette doesn't generate all colors
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

    Canvas(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Draw all gradients in a single pass
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors[0], Color.Transparent),
                center = Offset(size.width * 0.05f, size.height / 2),
                radius = size.width * 2
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors[1], Color.Transparent),
                center = Offset(0f, size.height),
                radius = size.height
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors[2], Color.Transparent),
                center = Offset(size.width * 1.1f, size.height * 0.1f),
                radius = size.height
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors[3], Color.Transparent),
                center = Offset(size.width, size.height * 0.9f),
                radius = size.height
            )
        )
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
        animationSpec = tween(durationMillis = 1000),
        label = "Smooth color transition"
    )

    val currentColor2 by animateColorAsState(
        targetValue = color2,
        animationSpec = tween(durationMillis = 1000),
        label = "Smooth color transition"
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