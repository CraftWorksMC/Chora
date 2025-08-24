package com.craftworks.music.ui.elements

import android.graphics.RuntimeShader
import android.os.Build
import android.view.animation.PathInterpolator
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.managers.SettingsManager
import kotlin.math.max
import kotlin.math.min

/*
 * Adapted from AOSP SystemUI:
 * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/animation/src/com/android/systemui/surfaceeffects/ripple/RippleShader.kt
*/
@Composable
fun RippleEffect(
    modifier: Modifier = Modifier,
    center: Offset,
    key: Int,
    color: Color = Color.White,
    durationMillis: Long = 800,
    distortionStrength: Float = 0.2f,
    sparkleStrength: Float = 0.3f,
    onFinished: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
        return

    val useRippleEffect by SettingsManager(LocalContext.current).refreshAnimationFlow.collectAsStateWithLifecycle(false)
    if (!useRippleEffect)
        return

    val shader = remember { StatefulRippleShader() }
    val density = LocalDensity.current

    val progressAnim = remember { Animatable(0f) }
    val timeAnim = remember { Animatable(0f) }

    LaunchedEffect(key) {
        if (key == 0) return@LaunchedEffect
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            1f,
            animationSpec = tween(durationMillis.toInt(), easing = LinearEasing)
        )
        onFinished()
    }

    LaunchedEffect(Unit) {
        while (true) {
            timeAnim.animateTo(
                timeAnim.value + 10000f,
                animationSpec = tween(10000, easing = LinearEasing)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxRadius = maxOf(center.x, center.y, size.width - center.x, size.height - center.y)

        shader.setMaxSize(maxRadius * 2f, maxRadius * 2f)

        shader.rawProgress = progressAnim.value
        shader.time = timeAnim.value
        shader.distortionStrength = distortionStrength
        shader.setCenter(center.x, center.y)
        shader.setPixelDensity(density.density)
        shader.setColor(color.toArgb())
        shader.setSparkleStrength(sparkleStrength)

        if (progressAnim.value > 0f && progressAnim.value < 1f) {
            drawRect(brush = ShaderBrush(shader))
        }
    }
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class StatefulRippleShader : RuntimeShader(CompleteRippleShaderCode.SHADER) {

    private val standardInterpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    private var _rawProgress: Float = 0f
    private var _distortionStrength: Float = 0f
    private var _maxSize: Pair<Float, Float> = Pair(0f, 0f)

    private data class FadeParams(
        val fadeInStart: Float,
        val fadeInEnd: Float,
        val fadeOutStart: Float,
        val fadeOutEnd: Float
    )
    private val baseRingFade = FadeParams(
        fadeInStart = 0f,
        fadeInEnd = 0.1f,
        fadeOutStart = 0.3f,
        fadeOutEnd = 1f
    )
    private val sparkleRingFade = FadeParams(
        fadeInStart = 0f,
        fadeInEnd = 0.1f,
        fadeOutStart = 0.4f,
        fadeOutEnd = 1f
    )
    private val centerFillFade = FadeParams(
        fadeInStart = 0f,
        fadeInEnd = 0f,
        fadeOutStart = 0f,
        fadeOutEnd = 0.6f
    )
    var time: Float = 0f
        set(value) {
            field = value
            setFloatUniform("in_time", value)
        }

    var rawProgress: Float
        get() = _rawProgress
        set(value) {
            _rawProgress = value
            updateFromProgress()
        }

    var distortionStrength: Float
        get() = _distortionStrength
        set(value) {
            _distortionStrength = value
            updateDistortion()
        }

    fun setMaxSize(width: Float, height: Float) {
        _maxSize = Pair(width, height)
    }

    fun setCenter(x: Float, y: Float) = setFloatUniform("in_center", x, y)
    fun setPixelDensity(density: Float) = setFloatUniform("in_pixelDensity", density)
    fun setColor(color: Int) = setColorUniform("in_color", color)
    fun setSparkleStrength(strength: Float) = setFloatUniform("in_sparkle_strength", strength)

    private fun updateFromProgress() {
        setFloatUniform("in_fadeSparkle", getFade(sparkleRingFade, _rawProgress))
        setFloatUniform("in_fadeRing", getFade(baseRingFade, _rawProgress))
        setFloatUniform("in_fadeFill", getFade(centerFillFade, _rawProgress))

        val interpolated = standardInterpolator.getInterpolation(_rawProgress)
        val currentWidth = interpolated * _maxSize.first
        val currentHeight = interpolated * _maxSize.second
        setFloatUniform("in_size", currentWidth, currentHeight)
        val blur = 1.25f + (0.5f - 1.25f) * interpolated
        setFloatUniform("in_blur", blur)
        updateDistortion()
    }

    private fun updateDistortion() {
        setFloatUniform("in_distort_radial", 75f * _rawProgress * _distortionStrength)
        setFloatUniform("in_distort_xy", 75f * _distortionStrength)
    }

    private fun getFade(params: FadeParams, progress: Float): Float {
        val fadeIn = subProgress(params.fadeInStart, params.fadeInEnd, progress)
        val fadeOut = 1f - subProgress(params.fadeOutStart, params.fadeOutEnd, progress)
        return min(fadeIn, fadeOut)
    }

    private fun subProgress(start: Float, end: Float, progress: Float): Float {
        if (start == end) return if (progress >= start) 1f else 0f
        val sub = progress.coerceIn(min(start, end), max(start, end))
        return (sub - start) / (end - start)
    }
}

private object CompleteRippleShaderCode {
    const val SHADER = """
        // Part 1: Uniforms from RippleShader.kt
        uniform vec2 in_center;
        uniform vec2 in_size;
        uniform float in_cornerRadius;
        uniform float in_thickness;
        uniform float in_time;
        uniform float in_distort_radial;
        uniform float in_distort_xy;
        uniform float in_fadeSparkle;
        uniform float in_fadeFill;
        uniform float in_fadeRing;
        uniform float in_blur;
        uniform float in_pixelDensity;
        layout(color) uniform vec4 in_color;
        uniform float in_sparkle_strength;

        float triangleNoise(vec2 n) {
            n  = fract(n * vec2(5.3987, 5.4421));
            n += dot(n.yx, n.xy + vec2(21.5351, 14.3137));
            float xy = n.x * n.y;
            return fract(xy * 95.4307) + fract(xy * 75.04961) - 1.0;
        }

        const float PI = 3.1415926535897932384626;

        float sparkles(vec2 uv, float t) {
            float n = triangleNoise(uv);
            float s = 0.0;
            for (float i = 0; i < 4; i += 1) {
                float l = i * 0.01;
                float h = l + 0.1;
                float o = smoothstep(n - l, h, n);
                o *= abs(sin(PI * o * (t + 0.55 * i)));
                s += o;
            }
            return s;
        }

        vec2 distort(vec2 p, float time, float distort_amount_radial,
            float distort_amount_xy) {
                float angle = atan(p.y, p.x);
                  return p + vec2(sin(angle * 8. + time * 0.003 + 1.641),
                            cos(angle * 5. + 2.14 + time * 0.00412)) * distort_amount_radial
                     + vec2(sin(p.x * 0.01 + time * 0.00215 + 0.8123),
                            cos(p.y * 0.01 + time * 0.005931)) * distort_amount_xy;
        }

        float subtract(float outer, float inner) {
            return max(outer, -inner);
        }
        
        float sdCircle(vec2 p, float r) {
            return (length(p)-r) / r;
        }

        float circleRing(vec2 p, float radius) {
            float thicknessHalf = radius * 0.25;

            float outerCircle = sdCircle(p, radius + thicknessHalf);
            float innerCircle = sdCircle(p, radius);

            return subtract(outerCircle, innerCircle);
        }

        float soften(float d, float blur) {
            float blurHalf = blur * 0.5;
            return smoothstep(-blurHalf, blurHalf, d);
        }

        vec4 main(vec2 p) {
                vec2 p_distorted = distort(p, in_time, in_distort_radial, in_distort_xy);
                float radius = in_size.x * 0.5;
                float sparkleRing = soften(circleRing(p_distorted-in_center, radius), in_blur);
                float inside = soften(sdCircle(p_distorted-in_center, radius * 1.25), in_blur);
                float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)
                    * (1.-sparkleRing) * in_fadeSparkle;

                float rippleInsideAlpha = (1.-inside) * in_fadeFill;
                float rippleRingAlpha = (1.-sparkleRing) * in_fadeRing;
                float rippleAlpha = max(rippleInsideAlpha, rippleRingAlpha) * in_color.a;
                vec4 ripple = vec4(in_color.rgb, 1.0) * rippleAlpha;
                return mix(ripple, vec4(sparkle), sparkle * in_sparkle_strength);
            }
    """
}