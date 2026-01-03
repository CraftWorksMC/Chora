package com.craftworks.music.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Generates album artwork as a Bitmap for use in MediaMetadata (Bluetooth, notifications, etc.)
 * This mirrors the logic from GeneratedAlbumArt.kt but uses Android Canvas instead of Compose
 */
object GeneratedArtworkBitmap {

    private val materialYouPalette = listOf(
        intArrayOf(0xFF6750A4.toInt(), 0xFF9A82DB.toInt()),
        intArrayOf(0xFF006C51.toInt(), 0xFF4DB6AC.toInt()),
        intArrayOf(0xFFB3261E.toInt(), 0xFFEF5350.toInt()),
        intArrayOf(0xFF006493.toInt(), 0xFF4FC3F7.toInt()),
        intArrayOf(0xFF7D5260.toInt(), 0xFFF48FB1.toInt()),
        intArrayOf(0xFF5C6300.toInt(), 0xFFAED581.toInt()),
        intArrayOf(0xFF984061.toInt(), 0xFFE91E63.toInt()),
        intArrayOf(0xFF006874.toInt(), 0xFF26C6DA.toInt()),
        intArrayOf(0xFF8B5000.toInt(), 0xFFFFB74D.toInt()),
        intArrayOf(0xFF4A5568.toInt(), 0xFF90A4AE.toInt()),
    )

    private fun generateHash(title: String, artist: String?, album: String?): Long {
        var hash: Long = 0x2F5E2B3C4D5E6F7AL
        val prime: Long = 1099511628211L

        title.forEachIndexed { i, c ->
            hash = hash xor (c.code.toLong() * (i + 1))
            hash *= prime
        }
        artist?.forEachIndexed { i, c ->
            hash = hash xor (c.code.toLong() * (i + 17))
            hash *= prime
        }
        album?.forEachIndexed { i, c ->
            hash = hash xor (c.code.toLong() * (i + 31))
            hash *= prime
        }

        val lenMix = (title.length * 7919L) + ((artist?.length ?: 0) * 6427L) + ((album?.length ?: 0) * 8191L)
        hash = hash xor lenMix
        hash *= prime
        hash = hash xor (hash shr 33)
        hash *= 0x7F51AFD7ED558CCDL
        hash = hash xor (hash shr 33)

        return hash
    }

    /**
     * Generate a bitmap artwork for the given song metadata
     * @param title Song title
     * @param artist Artist name (optional)
     * @param album Album name (optional)
     * @param size Bitmap size in pixels (square)
     * @return Generated Bitmap
     */
    fun generate(
        title: String,
        artist: String?,
        album: String? = null,
        size: Int = 512
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val hash = generateHash(title, artist, album)
        val absHash = abs(hash)
        val colorIndex = (absHash % materialYouPalette.size).toInt()
        val gradientAngle = ((absHash shr 32) % 360).toFloat()

        val colors = materialYouPalette[colorIndex]
        val baseColor = colors[0]
        val accentColor = colors[1]

        // Draw gradient background
        drawGradientBackground(canvas, size, baseColor, accentColor, gradientAngle)

        // Draw text (artist and title)
        drawText(canvas, size, title, artist)

        // Draw border
        val borderPaint = Paint().apply {
            color = Color.argb(204, 0, 0, 0) // 0.8 alpha
            style = Paint.Style.STROKE
            strokeWidth = size * 0.02f
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), borderPaint)

        return bitmap
    }

    private fun drawGradientBackground(
        canvas: Canvas,
        size: Int,
        baseColor: Int,
        accentColor: Int,
        angle: Float
    ) {
        val angleRad = Math.toRadians(angle.toDouble())
        val cosAngle = cos(angleRad).toFloat()
        val sinAngle = sin(angleRad).toFloat()

        // Create contrast color
        val contrastColor = Color.rgb(
            (255 - Color.red(baseColor) * 0.6f).toInt().coerceIn(0, 255),
            (255 - Color.green(baseColor) * 0.6f).toInt().coerceIn(0, 255),
            (255 - Color.blue(baseColor) * 0.6f).toInt().coerceIn(0, 255)
        )

        // Main gradient
        val gradient = LinearGradient(
            size * (0.5f + cosAngle * 0.25f),
            size * (0.5f + sinAngle * 0.25f),
            size * (0.5f + cosAngle * 0.75f),
            size * (0.5f + sinAngle * 0.75f),
            intArrayOf(baseColor, accentColor, contrastColor, accentColor, baseColor),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        val gradientPaint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gradientPaint)

        // Radial highlight
        val radialGradient = RadialGradient(
            size * 0.25f,
            size * 0.25f,
            size * 0.6f,
            intArrayOf(Color.argb(64, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val radialPaint = Paint().apply {
            shader = radialGradient
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), radialPaint)

        // Accent circle
        val circlePaint = Paint().apply {
            color = Color.argb(89, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        }
        canvas.drawCircle(size * 0.8f, size * 0.8f, size * 0.35f, circlePaint)

        // Vignette
        val vignette = RadialGradient(
            size * 0.5f,
            size * 0.5f,
            size * 0.8f,
            intArrayOf(Color.TRANSPARENT, Color.argb(51, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val vignettePaint = Paint().apply {
            shader = vignette
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), vignettePaint)
    }

    private fun drawText(canvas: Canvas, size: Int, title: String, artist: String?) {
        val textColor = Color.argb(242, 255, 255, 255) // 0.95 alpha white
        val baseFontSize = size * 0.08f

        val artistPaint = Paint().apply {
            color = textColor
            textSize = baseFontSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = textColor
            textSize = baseFontSize * 1.1f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val separatorPaint = Paint().apply {
            color = Color.argb(128, 255, 255, 255) // 0.5 alpha
            textSize = baseFontSize * 0.7f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Calculate total height
        val spacing = size * 0.02f
        val artistHeight = if (artist != null) artistPaint.textSize + spacing else 0f
        val separatorHeight = if (artist != null) separatorPaint.textSize + spacing else 0f
        val titleHeight = titlePaint.textSize

        val totalHeight = artistHeight + separatorHeight + titleHeight
        var currentY = (size - totalHeight) / 2f

        // Draw artist
        if (!artist.isNullOrBlank()) {
            currentY += artistPaint.textSize
            canvas.drawText(
                artist.uppercase().take(30),
                size / 2f,
                currentY,
                artistPaint
            )
            currentY += spacing

            // Draw separator
            currentY += separatorPaint.textSize
            canvas.drawText("──────────", size / 2f, currentY, separatorPaint)
            currentY += spacing
        }

        // Draw title
        currentY += titlePaint.textSize
        canvas.drawText(
            title.uppercase().take(30),
            size / 2f,
            currentY,
            titlePaint
        )
    }

    /**
     * Check if a song needs generated artwork based on its artwork URI
     */
    fun needsGeneratedArt(artworkUri: String?): Boolean {
        if (artworkUri.isNullOrEmpty()) return true
        return artworkUri.contains("placeholder") ||
               artworkUri.endsWith("/coverArt") ||
               artworkUri.contains("coverArt?id=&") ||
               (artworkUri.contains("coverArt?size=") && !artworkUri.contains("id="))
    }
}
