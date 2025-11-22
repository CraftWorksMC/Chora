package com.craftworks.music.ui.playing.background

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
@Stable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun AnimatedGradientBackground(
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

                drawRect(brush = shaderBrush)

                drawRect(
                    color = currentOverlayColor,
                    blendMode = BlendMode.Companion.SrcOver
                )
            }
        }
    )
}

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