package com.craftworks.music.ui.elements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

// Color palettes
private val materialYouPalette = listOf(
    Color(0xFF6750A4) to Color(0xFF9A82DB), // Purple
    Color(0xFF006C51) to Color(0xFF4DB6AC), // Teal
    Color(0xFFB3261E) to Color(0xFFEF5350), // Red
    Color(0xFF006493) to Color(0xFF4FC3F7), // Blue
    Color(0xFF7D5260) to Color(0xFFF48FB1), // Pink
    Color(0xFF5C6300) to Color(0xFFAED581), // Lime
    Color(0xFF984061) to Color(0xFFE91E63), // Magenta
    Color(0xFF006874) to Color(0xFF26C6DA), // Cyan
    Color(0xFF8B5000) to Color(0xFFFFB74D), // Orange
    Color(0xFF4A5568) to Color(0xFF90A4AE), // Slate
)

private val vibrantPalette = listOf(
    Color(0xFFFF1744) to Color(0xFFFF8A80), // Red
    Color(0xFFD500F9) to Color(0xFFEA80FC), // Purple
    Color(0xFF00E676) to Color(0xFF69F0AE), // Green
    Color(0xFF2979FF) to Color(0xFF82B1FF), // Blue
    Color(0xFFFFEA00) to Color(0xFFFFFF8D), // Yellow
    Color(0xFFFF9100) to Color(0xFFFFD180), // Orange
    Color(0xFF00E5FF) to Color(0xFF84FFFF), // Cyan
    Color(0xFFF50057) to Color(0xFFFF80AB), // Pink
)

private val pastelPalette = listOf(
    Color(0xFFE8D5E0) to Color(0xFFF3E5F5), // Lavender
    Color(0xFFD5E8D4) to Color(0xFFE8F5E9), // Mint
    Color(0xFFD4E5F7) to Color(0xFFE3F2FD), // Sky
    Color(0xFFF7E8D4) to Color(0xFFFFF3E0), // Peach
    Color(0xFFF7D4D4) to Color(0xFFFFEBEE), // Rose
    Color(0xFFD4F7F4) to Color(0xFFE0F7FA), // Aqua
    Color(0xFFF7F4D4) to Color(0xFFFFFDE7), // Cream
    Color(0xFFE0D4F7) to Color(0xFFEDE7F6), // Lilac
)

private val monochromePalette = listOf(
    Color(0xFF212121) to Color(0xFF424242),
    Color(0xFF37474F) to Color(0xFF546E7A),
    Color(0xFF455A64) to Color(0xFF607D8B),
    Color(0xFF263238) to Color(0xFF37474F),
    Color(0xFF1A237E) to Color(0xFF283593),
    Color(0xFF311B92) to Color(0xFF4527A0),
)

fun getPaletteColors(palette: ArtworkSettingsManager.ColorPalette): List<Pair<Color, Color>> {
    return when (palette) {
        ArtworkSettingsManager.ColorPalette.MATERIAL_YOU -> materialYouPalette
        ArtworkSettingsManager.ColorPalette.VIBRANT -> vibrantPalette
        ArtworkSettingsManager.ColorPalette.PASTEL -> pastelPalette
        ArtworkSettingsManager.ColorPalette.MONOCHROME -> monochromePalette
        ArtworkSettingsManager.ColorPalette.DYNAMIC -> materialYouPalette
    }
}

/**
 * Parsed artist info - cached for performance
 */
@Immutable
private data class ParsedArtistInfo(
    val mainArtist: String,
    val featuringArtists: String? // With brackets preserved
)

/**
 * Parsed title info - cached for performance
 */
@Immutable
private data class ParsedTitleInfo(
    val cleanTitle: String,
    val bracketContent: String?
)

/**
 * Pre-parsed content for rendering - computed once and cached
 */
@Immutable
private data class ArtworkContent(
    val artistInfo: ParsedArtistInfo,
    val titleInfo: ParsedTitleInfo,
    val album: String?,
    val colorIndex: Int,
    val colorIndex2: Int,      // Second color for mixing
    val patternSeed: Int,      // Separate seed for pattern variations
    val patternType: Int,      // Which pattern to use
    val gradientAngle: Float,  // Angle for gradient direction
    val layoutVariant: Int,    // Layout variation within pattern
    val scaleVariant: Float,   // Scale variation
    val positionSeed: Int      // Position variation seed
)

/**
 * Generate a more varied hash using multiple string properties
 */
private fun generateVariedHash(title: String, artist: String?, album: String?): Long {
    // Use a custom mixing hash for better distribution
    var hash: Long = 0x2F5E2B3C4D5E6F7AL // Starting seed
    val prime: Long = 1099511628211L // FNV prime

    // Mix in title characters with position weighting
    title.forEachIndexed { i, c ->
        hash = hash xor (c.code.toLong() * (i + 1))
        hash = hash * prime
    }

    // Mix in artist with different offset
    artist?.forEachIndexed { i, c ->
        hash = hash xor (c.code.toLong() * (i + 17))
        hash = hash * prime
    }

    // Mix in album with another offset
    album?.forEachIndexed { i, c ->
        hash = hash xor (c.code.toLong() * (i + 31))
        hash = hash * prime
    }

    // Additional mixing based on string lengths and content
    val lenMix = (title.length * 7919L) + ((artist?.length ?: 0) * 6427L) + ((album?.length ?: 0) * 8191L)
    hash = hash xor lenMix
    hash = hash * prime

    // Final mixing to spread bits
    hash = hash xor (hash shr 33)
    hash = hash * 0x7F51AFD7ED558CCDL
    hash = hash xor (hash shr 33)

    return hash
}

// Regex patterns compiled once
private val featuringRegex = """(?i)\s*[\[(]?\s*(?:ft\.?|feat\.?|featuring)\s*([^)\]]+)[\])]?\s*$""".toRegex()
private val bracketRegex = """[\[(]([^)\]]+)[\])]""".toRegex()

/**
 * Parse artist name to extract main artist and featuring artists
 */
@Stable
private fun parseArtist(artist: String?): ParsedArtistInfo {
    if (artist.isNullOrBlank()) return ParsedArtistInfo("", null)

    val match = featuringRegex.find(artist)
    return if (match != null) {
        val mainArtist = artist.substring(0, match.range.first).trim()
        val featuring = match.groupValues[1].trim()
        ParsedArtistInfo(mainArtist, "(ft. $featuring)")
    } else {
        ParsedArtistInfo(artist, null)
    }
}

/**
 * Parse title to extract bracket content
 */
@Stable
private fun parseTitle(title: String): ParsedTitleInfo {
    val bracketContent = bracketRegex.findAll(title)
        .map { "(${it.groupValues[1]})" }
        .joinToString(" ")
        .takeIf { it.isNotEmpty() }
    val cleanTitle = title.replace(bracketRegex, "").trim()
    return ParsedTitleInfo(cleanTitle, bracketContent)
}

/**
 * Smart album art that reads settings and displays accordingly
 */
@Composable
fun GeneratedAlbumArt(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animate: Boolean = true,
    album: String? = null,
    colors: List<Color>? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { ArtworkSettingsManager(context) }

    val style by settingsManager.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT)
    val palette by settingsManager.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU)
    val showInitials by settingsManager.showInitialsFlow.collectAsStateWithLifecycle(true)
    val shouldAnimate by settingsManager.animateArtworkFlow.collectAsStateWithLifecycle(false)

    GeneratedAlbumArtInternal(
        title = title,
        artist = artist,
        album = album,
        modifier = modifier,
        size = size,
        animate = animate && shouldAnimate,
        style = style,
        palette = palette,
        showInitials = showInitials,
        passedColors = colors
    )
}

/**
 * Static version without animation for lists where performance is critical
 * Optimized for lazy lists - minimizes recomposition and caches all computed values
 */
@Composable
fun GeneratedAlbumArtStatic(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    album: String? = null,
    colors: List<Color>? = null,
    // Performance: Pass from parent to avoid per-item settings creation
    artworkStyle: ArtworkSettingsManager.ArtworkStyle? = null,
    colorPalette: ArtworkSettingsManager.ColorPalette? = null,
    showInitialsOverride: Boolean? = null
) {
    // Use passed values or fall back to reading settings (for standalone usage)
    val style: ArtworkSettingsManager.ArtworkStyle
    val palette: ArtworkSettingsManager.ColorPalette
    val showInitials: Boolean

    if (artworkStyle != null && colorPalette != null && showInitialsOverride != null) {
        style = artworkStyle
        palette = colorPalette
        showInitials = showInitialsOverride
    } else {
        val context = LocalContext.current
        val settingsManager = remember { ArtworkSettingsManager(context) }
        style = artworkStyle ?: settingsManager.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT).value
        palette = colorPalette ?: settingsManager.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU).value
        showInitials = showInitialsOverride ?: settingsManager.showInitialsFlow.collectAsStateWithLifecycle(true).value
    }

    // Cache content parsing - only recompute when inputs change
    val content = remember(title, artist, album, palette) {
        val paletteColors = getPaletteColors(palette)
        val hash = generateVariedHash(title, artist, album)
        val absHash = abs(hash)
        ArtworkContent(
            artistInfo = parseArtist(artist),
            titleInfo = parseTitle(title),
            album = album,
            colorIndex = (absHash % paletteColors.size).toInt(),
            colorIndex2 = ((absHash shr 8) % paletteColors.size).toInt(),
            patternSeed = ((absHash shr 16) % 1000000).toInt(),
            patternType = ((absHash shr 24) % 20).toInt(),  // 20 pattern types
            gradientAngle = ((absHash shr 32) % 360).toFloat(),
            layoutVariant = ((absHash shr 40) % 8).toInt(),
            scaleVariant = 0.7f + ((absHash shr 48) % 60).toFloat() / 100f,  // 0.7 to 1.3
            positionSeed = ((absHash shr 56) % 10000).toInt()
        )
    }

    GeneratedAlbumArtOptimized(
        content = content,
        modifier = modifier,
        size = size,
        style = style,
        palette = palette,
        showInitials = showInitials,
        passedColors = colors
    )
}

/**
 * Optimized renderer for static artwork - uses cached content
 */
@Composable
private fun GeneratedAlbumArtOptimized(
    content: ArtworkContent,
    modifier: Modifier,
    size: Dp,
    style: ArtworkSettingsManager.ArtworkStyle,
    palette: ArtworkSettingsManager.ColorPalette,
    showInitials: Boolean,
    passedColors: List<Color>?
) {
    val textMeasurer = rememberTextMeasurer()
    val paletteColors = remember(palette) { getPaletteColors(palette) }
    
    val (baseColor, accentColor) = if (passedColors != null && passedColors.size >= 2) {
        passedColors[0] to passedColors[1]
    } else {
        paletteColors[content.colorIndex]
    }

    val textColor = remember(palette) {
        if (palette == ArtworkSettingsManager.ColorPalette.PASTEL)
            Color.Black.copy(alpha = 0.8f)
        else
            Color.White.copy(alpha = 0.95f)
    }

    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val maxWidth = this.size.width * 0.9f
                val baseFontSize = (this.size.minDimension.toDp().value * 0.08f).sp

                // Build text layouts with constraints for wrapping
                val layouts = if (showInitials) {
                    buildTextLayouts(
                        textMeasurer = textMeasurer,
                        content = content,
                        textColor = textColor,
                        baseFontSize = baseFontSize.value,
                        maxWidth = maxWidth.toInt()
                    )
                } else {
                    emptyList()
                }

                onDrawBehind {
                    // Draw background
                    when (style) {
                        ArtworkSettingsManager.ArtworkStyle.GRADIENT -> {
                            drawGradientStyle(baseColor, accentColor, 0.5f, content.gradientAngle)
                        }
                        ArtworkSettingsManager.ArtworkStyle.SOLID -> {
                            drawSolidStyle(baseColor, content.patternSeed)
                        }
                        ArtworkSettingsManager.ArtworkStyle.PATTERN -> {
                            drawPatternStyle(baseColor, accentColor, content)
                        }
                        ArtworkSettingsManager.ArtworkStyle.WAVEFORM -> {
                            drawWaveformStyle(baseColor, accentColor, content.patternSeed, 0.5f)
                        }
                        ArtworkSettingsManager.ArtworkStyle.MINIMAL -> {
                            drawMinimalStyle(baseColor)
                        }
                    }

                    // Draw text layouts
                    if (layouts.isNotEmpty()) {
                        drawTextLayouts(layouts, this.size)
                    }

                    // Draw uniform black border for album cover look
                    drawRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset.Zero,
                        size = this.size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = this.size.minDimension * 0.02f)
                    )
                }
            }
    )
}

@Composable
private fun GeneratedAlbumArtInternal(
    title: String,
    artist: String?,
    album: String?,
    modifier: Modifier,
    size: Dp,
    animate: Boolean,
    style: ArtworkSettingsManager.ArtworkStyle,
    palette: ArtworkSettingsManager.ColorPalette,
    showInitials: Boolean,
    passedColors: List<Color>?
) {
    val textMeasurer = rememberTextMeasurer()
    val paletteColors = getPaletteColors(palette)

    // Cache content parsing
    val content = remember(title, artist, album, palette) {
        val hash = generateVariedHash(title, artist, album)
        val absHash = abs(hash)
        ArtworkContent(
            artistInfo = parseArtist(artist),
            titleInfo = parseTitle(title),
            album = album,
            colorIndex = (absHash % paletteColors.size).toInt(),
            colorIndex2 = ((absHash shr 8) % paletteColors.size).toInt(),
            patternSeed = ((absHash shr 16) % 1000000).toInt(),
            patternType = ((absHash shr 24) % 20).toInt(),
            gradientAngle = ((absHash shr 32) % 360).toFloat(),
            layoutVariant = ((absHash shr 40) % 8).toInt(),
            scaleVariant = 0.7f + ((absHash shr 48) % 60).toFloat() / 100f,
            positionSeed = ((absHash shr 56) % 10000).toInt()
        )
    }

    val (baseColor, accentColor) = if (passedColors != null && passedColors.size >= 2) {
        passedColors[0] to passedColors[1]
    } else {
        paletteColors[content.colorIndex]
    }

    val shimmerOffset = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val offset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )
        offset
    } else {
        0.5f
    }

    val textColor = if (palette == ArtworkSettingsManager.ColorPalette.PASTEL)
        Color.Black.copy(alpha = 0.8f)
    else
        Color.White.copy(alpha = 0.95f)

    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val maxWidth = this.size.width * 0.9f
                val baseFontSize = (this.size.minDimension.toDp().value * 0.08f).sp

                val layouts = if (showInitials) {
                    buildTextLayouts(
                        textMeasurer = textMeasurer,
                        content = content,
                        textColor = textColor,
                        baseFontSize = baseFontSize.value,
                        maxWidth = maxWidth.toInt()
                    )
                } else {
                    emptyList()
                }

                onDrawBehind {
                    when (style) {
                        ArtworkSettingsManager.ArtworkStyle.GRADIENT -> {
                            drawGradientStyle(baseColor, accentColor, shimmerOffset, content.gradientAngle)
                        }
                        ArtworkSettingsManager.ArtworkStyle.SOLID -> {
                            drawSolidStyle(baseColor, content.patternSeed)
                        }
                        ArtworkSettingsManager.ArtworkStyle.PATTERN -> {
                            drawPatternStyle(baseColor, accentColor, content)
                        }
                        ArtworkSettingsManager.ArtworkStyle.WAVEFORM -> {
                            drawWaveformStyle(baseColor, accentColor, content.patternSeed, shimmerOffset)
                        }
                        ArtworkSettingsManager.ArtworkStyle.MINIMAL -> {
                            drawMinimalStyle(baseColor)
                        }
                    }

                    if (layouts.isNotEmpty()) {
                        drawTextLayouts(layouts, this.size)
                    }

                    // Draw uniform black border for album cover look
                    drawRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset.Zero,
                        size = this.size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = this.size.minDimension * 0.02f)
                    )
                }
            }
    )
}

/**
 * Build all text layouts with proper wrapping and line management
 */
private fun buildTextLayouts(
    textMeasurer: TextMeasurer,
    content: ArtworkContent,
    textColor: Color,
    baseFontSize: Float,
    maxWidth: Int
): List<TextLayoutResult> {
    val layouts = mutableListOf<TextLayoutResult>()
    val constraints = Constraints(maxWidth = maxWidth)

    val separator = "──────────"

    val artistStyle = TextStyle(
        color = textColor,
        fontSize = baseFontSize.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    val featuringStyle = TextStyle(
        color = textColor.copy(alpha = 0.75f),
        fontSize = (baseFontSize * 0.85f).sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    val titleStyle = TextStyle(
        color = textColor,
        fontSize = (baseFontSize * 1.1f).sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
    )

    val bracketStyle = TextStyle(
        color = textColor.copy(alpha = 0.7f),
        fontSize = (baseFontSize * 0.85f).sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    val lineStyle = TextStyle(
        color = textColor.copy(alpha = 0.5f),
        fontSize = (baseFontSize * 0.7f).sp,
        textAlign = TextAlign.Center
    )

    val albumStyle = TextStyle(
        color = textColor.copy(alpha = 0.6f),
        fontSize = (baseFontSize * 0.75f).sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    )

    var lineCount = 0

    // Main artist (with wrapping)
    if (content.artistInfo.mainArtist.isNotBlank()) {
        layouts.add(
            textMeasurer.measure(
                text = content.artistInfo.mainArtist.uppercase(),
                style = artistStyle,
                constraints = constraints,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        )
        lineCount++
    }

    // Featuring artists (with brackets)
    if (content.artistInfo.featuringArtists != null) {
        layouts.add(
            textMeasurer.measure(
                text = content.artistInfo.featuringArtists,
                style = featuringStyle,
                constraints = constraints,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        )
        lineCount++
    }

    // Separator after artist
    if (content.artistInfo.mainArtist.isNotBlank()) {
        layouts.add(textMeasurer.measure(text = separator, style = lineStyle))
    }

    // Title (with wrapping)
    if (content.titleInfo.cleanTitle.isNotBlank()) {
        layouts.add(
            textMeasurer.measure(
                text = content.titleInfo.cleanTitle.uppercase(),
                style = titleStyle,
                constraints = constraints,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        )
        lineCount++
    }

    // Bracket content from title (with brackets preserved)
    if (content.titleInfo.bracketContent != null) {
        layouts.add(
            textMeasurer.measure(
                text = content.titleInfo.bracketContent,
                style = bracketStyle,
                constraints = constraints,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        )
        lineCount++
    }

    // Add album if less than 3 content lines and album is provided
    if (lineCount < 3 && !content.album.isNullOrBlank()) {
        // Add separator before album
        layouts.add(textMeasurer.measure(text = separator, style = lineStyle))
        layouts.add(
            textMeasurer.measure(
                text = content.album,
                style = albumStyle,
                constraints = constraints,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        )
    }

    return layouts
}

/**
 * Draw all text layouts centered vertically
 */
private fun DrawScope.drawTextLayouts(layouts: List<TextLayoutResult>, canvasSize: androidx.compose.ui.geometry.Size) {
    val spacing = canvasSize.height * 0.02f
    val totalHeight = layouts.sumOf { it.size.height.toDouble() }.toFloat() +
                      (layouts.size - 1) * spacing

    var currentY = (canvasSize.height - totalHeight) / 2f

    layouts.forEach { layout ->
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                (canvasSize.width - layout.size.width) / 2f,
                currentY
            )
        )
        currentY += layout.size.height + spacing
    }
}

private fun DrawScope.drawGradientStyle(baseColor: Color, accentColor: Color, shimmerOffset: Float, angle: Float) {
    // Convert angle to radians and calculate gradient direction
    val angleRad = Math.toRadians(angle.toDouble())
    val cosAngle = cos(angleRad).toFloat()
    val sinAngle = sin(angleRad).toFloat()

    // Create high-contrast complementary color
    val contrastColor = Color(
        red = 1f - baseColor.red * 0.6f,
        green = 1f - baseColor.green * 0.6f,
        blue = 1f - baseColor.blue * 0.6f,
        alpha = 1f
    )

    // Create richer gradient with more color stops
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            accentColor.copy(alpha = 0.9f),
            contrastColor.copy(alpha = 0.5f),
            accentColor,
            baseColor.copy(alpha = 0.95f)
        ),
        start = Offset(
            size.width * (0.5f + cosAngle * shimmerOffset * 0.5f),
            size.height * (0.5f + sinAngle * shimmerOffset * 0.5f)
        ),
        end = Offset(
            size.width * (0.5f + cosAngle * (shimmerOffset + 1f) * 0.5f),
            size.height * (0.5f + sinAngle * (shimmerOffset + 1f) * 0.5f)
        )
    )
    drawRect(brush = gradientBrush)

    // Add radial overlay for depth - more pronounced
    val radialBrush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
        center = Offset(size.width * (0.25f + cosAngle * 0.15f), size.height * (0.25f - sinAngle * 0.15f)),
        radius = size.minDimension * 0.6f
    )
    drawRect(brush = radialBrush)

    // Add multiple accent shapes for visual interest
    drawCircle(
        color = accentColor.copy(alpha = 0.35f),
        radius = size.minDimension * 0.35f,
        center = Offset(size.width * (0.8f + cosAngle * 0.1f), size.height * (0.8f + sinAngle * 0.1f))
    )

    // Contrasting accent on opposite corner
    drawCircle(
        color = contrastColor.copy(alpha = 0.2f),
        radius = size.minDimension * 0.25f,
        center = Offset(size.width * (0.15f - cosAngle * 0.05f), size.height * (0.85f - sinAngle * 0.05f))
    )

    // Add subtle geometric accent
    val accentPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * 0.7f, size.height * 0.1f)
        lineTo(size.width * 0.9f, size.height * 0.3f)
        lineTo(size.width * 0.85f, size.height * 0.05f)
        close()
    }
    drawPath(accentPath, Color.White.copy(alpha = 0.15f))

    // Dark vignette for depth
    val vignetteBrush = Brush.radialGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        radius = size.maxDimension * 0.8f
    )
    drawRect(brush = vignetteBrush)
}

private fun DrawScope.drawSolidStyle(baseColor: Color, seed: Int) {
    drawRect(color = baseColor)

    // Vary highlight position based on seed
    val random = kotlin.random.Random(seed)
    val highlightX = 0.6f + random.nextFloat() * 0.3f
    val highlightY = 0.1f + random.nextFloat() * 0.3f
    val highlightRadius = 0.3f + random.nextFloat() * 0.2f

    drawCircle(
        color = Color.White.copy(alpha = 0.1f + random.nextFloat() * 0.1f),
        radius = size.minDimension * highlightRadius,
        center = Offset(size.width * highlightX, size.height * highlightY)
    )
}

private fun DrawScope.drawPatternStyle(baseColor: Color, accentColor: Color, content: ArtworkContent) {
    val seed = content.patternSeed
    val patternType = content.patternType
    val layoutVariant = content.layoutVariant
    val scale = content.scaleVariant
    val posSeed = content.positionSeed

    // Create multiple contrast colors for variety
    val contrastColor = Color(
        red = 1f - baseColor.red * 0.5f,
        green = 1f - baseColor.green * 0.5f,
        blue = 1f - baseColor.blue * 0.5f,
        alpha = 1f
    )

    val mixedColor = Color(
        red = (baseColor.red + accentColor.red) / 2f,
        green = (baseColor.green + accentColor.green) / 2f,
        blue = (baseColor.blue + accentColor.blue) / 2f,
        alpha = 1f
    )

    // Background with gradient based on layout variant
    when (layoutVariant % 4) {
        0 -> drawRect(color = baseColor)
        1 -> {
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, mixedColor),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            drawRect(brush = brush)
        }
        2 -> {
            val brush = Brush.radialGradient(
                colors = listOf(mixedColor, baseColor),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.maxDimension * 0.7f
            )
            drawRect(brush = brush)
        }
        else -> {
            val brush = Brush.verticalGradient(
                colors = listOf(baseColor, accentColor.copy(alpha = 0.7f), baseColor)
            )
            drawRect(brush = brush)
        }
    }

    val random = kotlin.random.Random(seed)
    val posRandom = kotlin.random.Random(posSeed)

    when (patternType) {
        0 -> {
            // Concentric circles - varied position and count
            val centerX = 0.15f + posRandom.nextFloat() * 0.7f
            val centerY = 0.15f + posRandom.nextFloat() * 0.7f
            val ringCount = (4 + random.nextInt(6)) * scale.toInt().coerceAtLeast(1)
            val ringSpacing = 0.06f + posRandom.nextFloat() * 0.06f

            repeat(ringCount.coerceAtMost(12)) { i ->
                val ringColor = when (i % 3) {
                    0 -> accentColor
                    1 -> contrastColor
                    else -> mixedColor
                }
                drawCircle(
                    color = ringColor.copy(alpha = 0.2f + (i * 0.06f)),
                    radius = size.minDimension * (ringSpacing + i * ringSpacing * scale),
                    center = Offset(size.width * centerX, size.height * centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f + random.nextFloat() * 6f)
                )
            }
            drawCircle(
                color = contrastColor.copy(alpha = 0.7f),
                radius = size.minDimension * 0.04f * scale,
                center = Offset(size.width * centerX, size.height * centerY)
            )
        }
        1 -> {
            // Spiral - varied center, turns, direction
            val centerX = size.width * (0.3f + posRandom.nextFloat() * 0.4f)
            val centerY = size.height * (0.3f + posRandom.nextFloat() * 0.4f)
            val spiralPoints = (80 + random.nextInt(80))
            val spiralTurns = 2f + random.nextFloat() * 4f
            val direction = if (layoutVariant % 2 == 0) 1f else -1f

            for (i in 0 until spiralPoints) {
                val t = i.toFloat() / spiralPoints
                val angle = t * spiralTurns * 2f * PI.toFloat() * direction
                val radius = t * size.minDimension * 0.45f * scale
                val x = centerX + cos(angle) * radius
                val y = centerY + sin(angle) * radius
                val dotSize = size.minDimension * (0.01f + t * 0.03f) * scale
                val dotColor = when (i % 4) {
                    0 -> contrastColor
                    1 -> accentColor
                    2 -> mixedColor
                    else -> Color.White
                }
                drawCircle(
                    color = dotColor.copy(alpha = 0.3f + t * 0.5f),
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }
        }
        2 -> {
            // Wave interference - varied wave sources
            val waveCount = 2 + random.nextInt(4)
            val step = (3 + layoutVariant).coerceAtMost(8)
            val waveFreq = 0.03f + posRandom.nextFloat() * 0.05f

            for (py in 0 until size.height.toInt() step step) {
                for (px in 0 until size.width.toInt() step step) {
                    var intensity = 0f
                    for (w in 0 until waveCount) {
                        val cx = size.width * (posRandom.nextFloat())
                        val cy = size.height * (posRandom.nextFloat())
                        val dist = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
                        intensity += sin(dist * waveFreq + w * 0.5f).toFloat()
                    }
                    intensity = (intensity / waveCount + 1f) / 2f
                    if (intensity > 0.45f) {
                        val pixelColor = if (intensity > 0.7f) accentColor else contrastColor
                        drawRect(
                            color = pixelColor.copy(alpha = intensity * 0.6f),
                            topLeft = Offset(px.toFloat(), py.toFloat()),
                            size = Size(step.toFloat(), step.toFloat())
                        )
                    }
                }
            }
        }
        3 -> {
            // Triangles tessellation - varied size and rotation
            val cellSize = size.minDimension / ((3 + random.nextInt(4)) * scale)
            val rows = (size.height / cellSize).toInt() + 2
            val cols = (size.width / cellSize).toInt() + 2
            val rotated = layoutVariant % 2 == 1

            for (row in -1..rows) {
                for (col in -1..cols) {
                    val xOffset = if (row % 2 == 0) 0f else cellSize * 0.5f
                    val x = col * cellSize + xOffset
                    val y = row * cellSize * 0.866f

                    val colorChoice = (row * 7 + col * 13 + seed) % 5
                    val triColor = when (colorChoice) {
                        0 -> baseColor.copy(alpha = 0.9f)
                        1 -> accentColor.copy(alpha = 0.7f)
                        2 -> contrastColor.copy(alpha = 0.5f)
                        3 -> mixedColor.copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.2f)
                    }

                    val path = androidx.compose.ui.graphics.Path().apply {
                        if (rotated) {
                            moveTo(x, y + cellSize * 0.866f)
                            lineTo(x + cellSize * 0.5f, y)
                            lineTo(x - cellSize * 0.5f, y)
                        } else {
                            moveTo(x, y)
                            lineTo(x + cellSize * 0.5f, y + cellSize * 0.866f)
                            lineTo(x - cellSize * 0.5f, y + cellSize * 0.866f)
                        }
                        close()
                    }
                    drawPath(path, triColor)
                }
            }
        }
        4 -> {
            // Diagonal stripes - varied angle and width
            val stripeCount = (5 + random.nextInt(6))
            val stripeWidth = size.minDimension / stripeCount * (1f + scale * 0.5f)
            val angle = when (layoutVariant % 4) {
                0 -> 0f  // top-left to bottom-right
                1 -> 1f  // top-right to bottom-left
                2 -> 0.5f // steeper
                else -> -0.5f
            }

            repeat(stripeCount * 4) { i ->
                val stripeColor = when (i % 4) {
                    0 -> accentColor.copy(alpha = 0.8f)
                    1 -> contrastColor.copy(alpha = 0.6f)
                    2 -> mixedColor.copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.25f)
                }
                val startX = -size.width * 2 + i * stripeWidth
                drawLine(
                    color = stripeColor,
                    start = Offset(startX, if (angle >= 0) 0f else size.height),
                    end = Offset(startX + size.width * (1f + abs(angle)), if (angle >= 0) size.height else 0f),
                    strokeWidth = stripeWidth * 0.4f
                )
            }
        }
        5 -> {
            // Hexagonal/circular grid - varied density
            val hexRadius = size.minDimension / ((6 + random.nextInt(6)) * scale)
            val hexHeight = hexRadius * sqrt(3f)

            for (row in -1..(size.height / hexHeight).toInt() + 2) {
                for (col in -1..(size.width / (hexRadius * 1.5f)).toInt() + 2) {
                    val xOffset = if (row % 2 == 0) 0f else hexRadius * 0.75f
                    val cx = col * hexRadius * 1.5f + xOffset
                    val cy = row * hexHeight * 0.5f

                    val hexColor = when ((row * 5 + col * 7 + seed) % 5) {
                        0 -> accentColor.copy(alpha = 0.6f)
                        1 -> contrastColor.copy(alpha = 0.5f)
                        2 -> baseColor.copy(alpha = 0.8f)
                        3 -> mixedColor.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.2f)
                    }

                    val radiusMult = 0.35f + posRandom.nextFloat() * 0.2f
                    drawCircle(
                        color = hexColor,
                        radius = hexRadius * radiusMult,
                        center = Offset(cx, cy)
                    )
                    if (layoutVariant % 2 == 0) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = hexRadius * radiusMult,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )
                    }
                }
            }
        }
        6 -> {
            // Radial burst - varied ray count and center
            val rayCount = 8 + random.nextInt(20)
            val centerX = size.width * (0.25f + posRandom.nextFloat() * 0.5f)
            val centerY = size.height * (0.25f + posRandom.nextFloat() * 0.5f)
            val startAngle = posRandom.nextFloat() * PI.toFloat()

            for (i in 0 until rayCount) {
                val angle = startAngle + (i.toFloat() / rayCount) * 2f * PI.toFloat()
                val rayLength = size.maxDimension * (0.6f + posRandom.nextFloat() * 0.4f)
                val rayColor = when (i % 4) {
                    0 -> accentColor
                    1 -> contrastColor
                    2 -> mixedColor
                    else -> Color.White
                }

                drawLine(
                    color = rayColor.copy(alpha = 0.35f + posRandom.nextFloat() * 0.2f),
                    start = Offset(centerX, centerY),
                    end = Offset(
                        centerX + cos(angle) * rayLength,
                        centerY + sin(angle) * rayLength
                    ),
                    strokeWidth = size.minDimension / rayCount * (0.5f + posRandom.nextFloat() * 0.5f)
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = size.minDimension * 0.06f * scale,
                center = Offset(centerX, centerY)
            )
        }
        7 -> {
            // Overlapping circles - varied positions and sizes
            val circleCount = 3 + random.nextInt(5)
            for (i in 0 until circleCount) {
                val cx = size.width * (0.1f + posRandom.nextFloat() * 0.8f)
                val cy = size.height * (0.1f + posRandom.nextFloat() * 0.8f)
                val circleRadius = size.minDimension * (0.12f + posRandom.nextFloat() * 0.2f) * scale
                val circleColor = when (i % 4) {
                    0 -> accentColor
                    1 -> contrastColor
                    2 -> mixedColor
                    else -> baseColor
                }
                val filled = layoutVariant % 2 == 0
                if (filled) {
                    drawCircle(
                        color = circleColor.copy(alpha = 0.4f + posRandom.nextFloat() * 0.3f),
                        radius = circleRadius,
                        center = Offset(cx, cy)
                    )
                } else {
                    drawCircle(
                        color = circleColor.copy(alpha = 0.6f),
                        radius = circleRadius,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = circleRadius * 0.12f)
                    )
                }
            }
        }
        8 -> {
            // Noise dots - varied density and clustering
            val dotCount = (150 + random.nextInt(200))
            val clusterX = posRandom.nextFloat()
            val clusterY = posRandom.nextFloat()
            val clusterStrength = 0.3f + posRandom.nextFloat() * 0.4f

            for (i in 0 until dotCount) {
                var x = random.nextFloat() * size.width
                var y = random.nextFloat() * size.height

                // Cluster towards a point
                x = x * (1f - clusterStrength) + size.width * clusterX * clusterStrength
                y = y * (1f - clusterStrength) + size.height * clusterY * clusterStrength

                val distFromCluster = sqrt(
                    (x - size.width * clusterX).let { it * it } +
                    (y - size.height * clusterY).let { it * it }
                ) / size.maxDimension

                val dotSize = size.minDimension * (0.004f + random.nextFloat() * 0.025f) * scale
                val dotColor = when (random.nextInt(4)) {
                    0 -> accentColor
                    1 -> contrastColor
                    2 -> mixedColor
                    else -> Color.White
                }
                val alpha = (1f - distFromCluster * 0.8f) * 0.7f

                drawCircle(
                    color = dotColor.copy(alpha = alpha.coerceIn(0.1f, 0.8f)),
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }
        }
        9 -> {
            // Scattered bold shapes - mixed types
            val shapeCount = 6 + random.nextInt(8)
            repeat(shapeCount) { idx ->
                val x = posRandom.nextFloat() * size.width
                val y = posRandom.nextFloat() * size.height
                val shapeSize = size.minDimension * (0.08f + random.nextFloat() * 0.18f) * scale
                val shapeType = (idx + layoutVariant) % 4
                val shapeColor = when (random.nextInt(4)) {
                    0 -> accentColor.copy(alpha = 0.65f)
                    1 -> contrastColor.copy(alpha = 0.55f)
                    2 -> mixedColor.copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.35f)
                }

                when (shapeType) {
                    0 -> drawCircle(color = shapeColor, radius = shapeSize, center = Offset(x, y))
                    1 -> drawRect(color = shapeColor, topLeft = Offset(x - shapeSize/2, y - shapeSize/2), size = Size(shapeSize, shapeSize))
                    2 -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x, y - shapeSize)
                            lineTo(x + shapeSize * 0.866f, y + shapeSize * 0.5f)
                            lineTo(x - shapeSize * 0.866f, y + shapeSize * 0.5f)
                            close()
                        }
                        drawPath(path, shapeColor)
                    }
                    else -> {
                        // Diamond
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x, y - shapeSize)
                            lineTo(x + shapeSize, y)
                            lineTo(x, y + shapeSize)
                            lineTo(x - shapeSize, y)
                            close()
                        }
                        drawPath(path, shapeColor)
                    }
                }
            }
        }
        10 -> {
            // Concentric squares
            val squareCount = 4 + random.nextInt(5)
            val centerX = size.width * (0.3f + posRandom.nextFloat() * 0.4f)
            val centerY = size.height * (0.3f + posRandom.nextFloat() * 0.4f)

            repeat(squareCount) { i ->
                val squareSize = size.minDimension * (0.1f + i * 0.12f) * scale
                val squareColor = when (i % 3) {
                    0 -> accentColor
                    1 -> contrastColor
                    else -> mixedColor
                }
                drawRect(
                    color = squareColor.copy(alpha = 0.4f + i * 0.08f),
                    topLeft = Offset(centerX - squareSize, centerY - squareSize),
                    size = Size(squareSize * 2, squareSize * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f + random.nextFloat() * 4f)
                )
            }
        }
        11 -> {
            // Crosshatch pattern
            val lineCount = 8 + random.nextInt(8)
            val spacing = size.minDimension / lineCount * scale

            // First direction
            repeat(lineCount * 2) { i ->
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(-size.width + i * spacing, 0f),
                    end = Offset(i * spacing, size.height),
                    strokeWidth = 1.5f + random.nextFloat() * 2f
                )
            }
            // Second direction
            repeat(lineCount * 2) { i ->
                drawLine(
                    color = contrastColor.copy(alpha = 0.3f),
                    start = Offset(size.width * 2 - i * spacing, 0f),
                    end = Offset(size.width - i * spacing, size.height),
                    strokeWidth = 1.5f + random.nextFloat() * 2f
                )
            }
        }
        12 -> {
            // Gradient circles at corners
            val corners = listOf(
                Offset(0f, 0f),
                Offset(size.width, 0f),
                Offset(0f, size.height),
                Offset(size.width, size.height)
            )
            val colors = listOf(accentColor, contrastColor, mixedColor, baseColor)

            corners.forEachIndexed { idx, corner ->
                val radiusBase = size.minDimension * (0.3f + posRandom.nextFloat() * 0.3f) * scale
                drawCircle(
                    color = colors[idx].copy(alpha = 0.5f),
                    radius = radiusBase,
                    center = corner
                )
                drawCircle(
                    color = colors[(idx + 1) % 4].copy(alpha = 0.3f),
                    radius = radiusBase * 0.7f,
                    center = corner
                )
            }
        }
        13 -> {
            // Horizontal bars/lines
            val barCount = 5 + random.nextInt(8)
            val barHeight = size.height / (barCount * 2)

            repeat(barCount) { i ->
                val y = i * size.height / barCount + barHeight / 2
                val barColor = when (i % 3) {
                    0 -> accentColor
                    1 -> contrastColor
                    else -> mixedColor
                }
                val barWidth = size.width * (0.3f + posRandom.nextFloat() * 0.7f)
                val startX = posRandom.nextFloat() * (size.width - barWidth)

                drawRect(
                    color = barColor.copy(alpha = 0.5f + posRandom.nextFloat() * 0.3f),
                    topLeft = Offset(startX, y),
                    size = Size(barWidth, barHeight * scale)
                )
            }
        }
        14 -> {
            // Starburst from edge
            val edgePos = layoutVariant % 4
            val (cx, cy) = when (edgePos) {
                0 -> 0f to size.height / 2
                1 -> size.width to size.height / 2
                2 -> size.width / 2 to 0f
                else -> size.width / 2 to size.height
            }
            val rayCount = 15 + random.nextInt(15)

            repeat(rayCount) { i ->
                val angle = (i.toFloat() / rayCount - 0.5f) * PI.toFloat()
                val length = size.maxDimension * (0.8f + posRandom.nextFloat() * 0.4f)
                val rayColor = if (i % 2 == 0) accentColor else contrastColor

                drawLine(
                    color = rayColor.copy(alpha = 0.4f),
                    start = Offset(cx, cy),
                    end = Offset(cx + cos(angle) * length, cy + sin(angle) * length),
                    strokeWidth = 2f + posRandom.nextFloat() * 4f
                )
            }
        }
        15 -> {
            // Bubble/circle field
            val bubbleCount = 20 + random.nextInt(30)
            repeat(bubbleCount) {
                val x = posRandom.nextFloat() * size.width
                val y = posRandom.nextFloat() * size.height
                val r = size.minDimension * (0.02f + posRandom.nextFloat() * 0.1f) * scale
                val bubbleColor = when (random.nextInt(4)) {
                    0 -> accentColor
                    1 -> contrastColor
                    2 -> mixedColor
                    else -> Color.White
                }

                drawCircle(
                    color = bubbleColor.copy(alpha = 0.15f + posRandom.nextFloat() * 0.35f),
                    radius = r,
                    center = Offset(x, y)
                )
                // Highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = r * 0.3f,
                    center = Offset(x - r * 0.3f, y - r * 0.3f)
                )
            }
        }
        16 -> {
            // Chevron/arrow pattern
            val chevronCount = 4 + random.nextInt(4)
            val chevronHeight = size.height / chevronCount

            repeat(chevronCount) { i ->
                val y = i * chevronHeight
                val chevronColor = when (i % 3) {
                    0 -> accentColor
                    1 -> contrastColor
                    else -> mixedColor
                }
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, y + chevronHeight * 0.5f)
                    lineTo(size.width * 0.5f, y)
                    lineTo(size.width, y + chevronHeight * 0.5f)
                    lineTo(size.width, y + chevronHeight)
                    lineTo(size.width * 0.5f, y + chevronHeight * 0.5f)
                    lineTo(0f, y + chevronHeight)
                    close()
                }
                drawPath(path, chevronColor.copy(alpha = 0.5f + i * 0.1f))
            }
        }
        17 -> {
            // Mosaic squares
            val gridSize = 4 + random.nextInt(4)
            val cellWidth = size.width / gridSize
            val cellHeight = size.height / gridSize

            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val cellColor = when ((row * 7 + col * 11 + seed) % 5) {
                        0 -> accentColor
                        1 -> contrastColor
                        2 -> mixedColor
                        3 -> baseColor
                        else -> Color.White
                    }
                    val alpha = 0.3f + posRandom.nextFloat() * 0.5f
                    val inset = cellWidth * 0.05f

                    drawRect(
                        color = cellColor.copy(alpha = alpha),
                        topLeft = Offset(col * cellWidth + inset, row * cellHeight + inset),
                        size = Size(cellWidth - inset * 2, cellHeight - inset * 2)
                    )
                }
            }
        }
        18 -> {
            // Flowing curves
            val curveCount = 3 + random.nextInt(4)
            repeat(curveCount) { idx ->
                val startY = size.height * posRandom.nextFloat()
                val amplitude = size.height * (0.1f + posRandom.nextFloat() * 0.2f) * scale
                val frequency = 1f + posRandom.nextFloat() * 2f
                val curveColor = when (idx % 3) {
                    0 -> accentColor
                    1 -> contrastColor
                    else -> mixedColor
                }

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, startY)
                    for (x in 0..size.width.toInt() step 4) {
                        val y = startY + sin(x * frequency * 0.02f + idx) * amplitude
                        lineTo(x.toFloat(), y.toFloat())
                    }
                }
                drawPath(
                    path = path,
                    color = curveColor.copy(alpha = 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f + posRandom.nextFloat() * 5f)
                )
            }
        }
        else -> {
            // Layered abstract shapes
            val layerCount = 4 + random.nextInt(4)
            repeat(layerCount) { layer ->
                val shapeType = (layer + layoutVariant) % 3
                val x = posRandom.nextFloat() * size.width
                val y = posRandom.nextFloat() * size.height
                val shapeSize = size.minDimension * (0.15f + posRandom.nextFloat() * 0.25f) * scale
                val shapeColor = when (layer % 4) {
                    0 -> accentColor
                    1 -> contrastColor
                    2 -> mixedColor
                    else -> Color.White
                }

                when (shapeType) {
                    0 -> {
                        // Overlapping circles
                        drawCircle(color = shapeColor.copy(alpha = 0.4f), radius = shapeSize, center = Offset(x, y))
                        drawCircle(color = shapeColor.copy(alpha = 0.3f), radius = shapeSize * 0.7f, center = Offset(x + shapeSize * 0.3f, y - shapeSize * 0.2f))
                    }
                    1 -> {
                        // Rotated square
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x, y - shapeSize)
                            lineTo(x + shapeSize, y)
                            lineTo(x, y + shapeSize)
                            lineTo(x - shapeSize, y)
                            close()
                        }
                        drawPath(path, shapeColor.copy(alpha = 0.45f))
                    }
                    else -> {
                        // Pentagon-ish
                        val path = androidx.compose.ui.graphics.Path().apply {
                            for (i in 0 until 5) {
                                val angle = (i * 72 - 90) * PI.toFloat() / 180f
                                val px = x + cos(angle) * shapeSize
                                val py = y + sin(angle) * shapeSize
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(path, shapeColor.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    // Vignette with varied intensity
    val vignetteAlpha = 0.15f + (layoutVariant % 4) * 0.05f
    val vignetteBrush = Brush.radialGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha)),
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        radius = size.maxDimension * 0.75f
    )
    drawRect(brush = vignetteBrush)
}

private fun DrawScope.drawWaveformStyle(baseColor: Color, accentColor: Color, seed: Int, offset: Float) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(baseColor.copy(alpha = 0.9f), accentColor.copy(alpha = 0.7f))
    )
    drawRect(brush = gradientBrush)

    val random = kotlin.random.Random(seed)

    // Vary bar count and width based on seed
    val barCount = 8 + random.nextInt(8)  // 8-15 bars
    val barWidthMultiplier = 0.6f + random.nextFloat() * 0.4f  // Vary bar width
    val barWidth = size.width / (barCount * 2f)
    val barAlpha = 0.5f + random.nextFloat() * 0.3f

    repeat(barCount) { i ->
        val baseHeight = 0.15f + random.nextFloat() * 0.65f
        val barHeight = size.height * baseHeight
        val animatedHeight = barHeight * (0.8f + sin((offset + i * 0.5f) * 3.14f).toFloat() * 0.2f)

        drawRect(
            color = Color.White.copy(alpha = barAlpha),
            topLeft = Offset(
                x = barWidth + i * barWidth * 2,
                y = (size.height - animatedHeight) / 2
            ),
            size = Size(barWidth * barWidthMultiplier, animatedHeight)
        )
    }
}

private fun DrawScope.drawMinimalStyle(baseColor: Color) {
    drawRect(color = baseColor.copy(alpha = 0.15f))

    drawRect(
        color = baseColor.copy(alpha = 0.3f),
        topLeft = Offset.Zero,
        size = size,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
}
