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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Stable
@Composable
fun NowPlaying_Background(
    colorPalette: List<Color> = emptyList(),
    backgroundStyle: String = "Animated Blur",
    overlayColor: Color = Color.Transparent
) {
    when (backgroundStyle){
        "Plain"         -> PlainBG()
        "Static Blur"   -> StaticBlurBG(colorPalette)
        "Animated Blur" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AnimatedGradientBG(
            color1 = colorPalette.elementAtOrNull(0) ?: Color.Black,
            color2 = colorPalette.elementAtOrNull(1) ?: Color.Black,
            color3 = colorPalette.elementAtOrNull(2) ?: Color.Black, // Sometimes palette doesn't generate all colors
            overlayColor = overlayColor,
            modifier = Modifier.fillMaxSize()
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
    colors: List<Color?>
) {
    Log.d("RECOMPOSITION", "Recomposing Static Blur BG")

    if (colors.isEmpty())
        return

    val animatedColors = colors.map { color ->
        animateColorAsState(
            targetValue = color ?: MaterialTheme.colorScheme.background,
            animationSpec = tween(durationMillis = 1500),
            label = "Animated Colors"
        ).value
    }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors.elementAtOrNull(0) ?: Color.Black, Color.Transparent),
                center = Offset(size.width * 0.05f, size.height / 2),
                radius = size.width * 2
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors.elementAtOrNull(1) ?: animatedColors.firstNotNullOf { it }, Color.Transparent),
                center = Offset(0f, size.height),
                radius = size.height
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors.elementAtOrNull(2) ?: animatedColors.firstNotNullOf { it }, Color.Transparent),
                center = Offset(size.width * 1.1f, size.height * 0.1f),
                radius = size.height
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(animatedColors.elementAtOrNull(3) ?: animatedColors.firstNotNullOf { it }, Color.Transparent),
                center = Offset(size.width, size.height * 0.9f),
                radius = size.height
            )
        )
    }
}

/*
@Composable
@Preview
@Stable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun AnimatedGradientBG(
    color1: Color  = Color.Blue,
    color2: Color = Color.Black,
    color3: Color = Color.Cyan,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    println("RECOMPOSITION: Animated Gradient BG")
    println("Colors: $color1, $color2, $color3")

    val shaderCode = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float3 color1;
    uniform float3 color2;
    uniform float3 color3;

    half4 main(vec2 fragCoord) {
        float mr = min(iResolution.x, iResolution.y);
        vec2 uv = (fragCoord * 2 - iResolution.xy) / mr;

        float d = -iTime * 0.5;
        float a = 0.0;
        for (float i = 0.0; i < 4; ++i) {
            a += cos(i - d - a * uv.x);
            d += sin(uv.y * i + a);
        }
        d += iTime * 0.5;
        
        //float mixFactor = (cos(uv.x * d) + sin(uv.y * a)) * 0.5 + 0.5;
        //float3 col = mix(color1, color2, mixFactor);

        vec3 col = vec3(cos(uv * vec2(d, a)).x * 0.6 + 0.4, cos(a + d) * 0.5 + 0.5, 0.0);
        float blendValue = (col.x + col.y) * 0.5;
        
        col = mix(color1, color2, blendValue);
        col = mix(col, color3, smoothstep(0.0, 1.0, sin(d) * 0.5 + 0.5));
        
        return half4(col, 1.0);
    }
"""

    var time by remember { mutableFloatStateOf(0f) }

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

    val currentColor3 by animateColorAsState(
        targetValue = color3,
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
            size.width * 2,
            size.height * 2
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
        shader.setFloatUniform(
            "color3",
            currentColor3.red,
            currentColor3.green,
            currentColor3.blue
        )
        drawRect(
            brush = ShaderBrush(shader),
            size = size
        )
    }
}
*/

@Composable
@Preview
@Stable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun AnimatedGradientBG(
    color1: Color = Color.Blue,
    color2: Color = Color.Black,
    color3: Color = Color.Cyan,
    overlayColor: Color = Color.Transparent,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val shader = remember { RuntimeShader(SHADER_CODE) }

    var time by remember { mutableFloatStateOf(0f) }

    val currentColor1 by animateColorAsState(color1, tween(1500), label = "color1")
    val currentColor2 by animateColorAsState(color2, tween(1500), label = "color2")
    val currentColor3 by animateColorAsState(color3, tween(1500), label = "color3")
    val currentOverlayColor by animateColorAsState(overlayColor, tween(1500), label = "overlay")


    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameTime ->
                time = frameTime / 10000f
            }
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            val shaderBrush = ShaderBrush(shader)

            onDrawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("color1", currentColor1.red, currentColor1.green, currentColor1.blue)
                shader.setFloatUniform("color2", currentColor2.red, currentColor2.green, currentColor2.blue)
                shader.setFloatUniform("color3", currentColor3.red, currentColor3.green, currentColor3.blue)

                drawRect(brush = shaderBrush)

                drawRect(
                    color = currentOverlayColor,
                    blendMode = BlendMode.SrcOver
                )
            }
        }
    )
}

// It's good practice to move large, static strings out of the composable function.
private const val SHADER_CODE = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float3 color1;
    uniform float3 color2;
    uniform float3 color3;

    half4 main(vec2 fragCoord) {
        float mr = min(iResolution.x, iResolution.y);
        vec2 uv = (fragCoord * 2 - iResolution.xy) / mr;

        float d = -iTime * 0.5;
        float a = 0.0;
        for (float i = 0.0; i < 4; ++i) {
            a += cos(i - d - a * uv.x);
            d += sin(uv.y * i + a);
        }
        d += iTime * 0.5;
        
        vec3 col = vec3(cos(uv * vec2(d, a)).x * 0.6 + 0.4, cos(a + d) * 0.5 + 0.5, 0.0);
        float blendValue = (col.x + col.y) * 0.5;
        
        col = mix(color1, color2, blendValue);
        col = mix(col, color3, smoothstep(0.0, 1.0, sin(d) * 0.5 + 0.5));
        
        return half4(col, 1.0);
    }
"""