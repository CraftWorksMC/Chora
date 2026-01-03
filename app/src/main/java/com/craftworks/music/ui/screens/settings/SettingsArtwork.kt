package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import com.craftworks.music.ui.elements.GeneratedAlbumArtStatic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_ArtworkScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { ArtworkSettingsManager(context.applicationContext) }

    val enabled by settingsManager.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by settingsManager.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)
    val style by settingsManager.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT)
    val palette by settingsManager.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU)
    val showInitials by settingsManager.showInitialsFlow.collectAsStateWithLifecycle(true)
    val animate by settingsManager.animateArtworkFlow.collectAsStateWithLifecycle(false)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Artwork") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Preview section
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GeneratedAlbumArtStatic(
                    title = "Album One",
                    artist = "Artist Name",
                    size = 80.dp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
                GeneratedAlbumArtStatic(
                    title = "Different",
                    artist = "Someone",
                    size = 80.dp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
                GeneratedAlbumArtStatic(
                    title = "Third",
                    artist = "Various",
                    size = 80.dp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }

            HorizontalDivider()

            // Main toggle
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingsToggleItem(
                title = "Generated Artwork",
                description = "Show generated artwork when album art is unavailable",
                checked = enabled,
                onCheckedChange = {
                    coroutineScope.launch { settingsManager.setGeneratedArtworkEnabled(it) }
                }
            )

            if (enabled) {
                // Fallback mode
                Text(
                    text = "When to Show",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                FallbackModeSelector(
                    selectedMode = fallbackMode,
                    onModeSelected = {
                        coroutineScope.launch { settingsManager.setFallbackMode(it) }
                    }
                )

                // Style
                Text(
                    text = "Style",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                StyleSelector(
                    selectedStyle = style,
                    onStyleSelected = {
                        coroutineScope.launch { settingsManager.setArtworkStyle(it) }
                    }
                )

                // Color palette
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                PaletteSelector(
                    selectedPalette = palette,
                    onPaletteSelected = {
                        coroutineScope.launch { settingsManager.setColorPalette(it) }
                    }
                )

                // Additional options
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                SettingsToggleItem(
                    title = "Show Initials",
                    description = "Display album/artist initials on generated artwork",
                    checked = showInitials,
                    onCheckedChange = {
                        coroutineScope.launch { settingsManager.setShowInitials(it) }
                    }
                )

                SettingsToggleItem(
                    title = "Animate Artwork",
                    description = "Enable shimmer animation on Now Playing screen",
                    checked = animate,
                    onCheckedChange = {
                        coroutineScope.launch { settingsManager.setAnimateArtwork(it) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private data class FallbackModeItem(
    val mode: ArtworkSettingsManager.FallbackMode,
    val title: String,
    val description: String
)

@Composable
private fun FallbackModeSelector(
    selectedMode: ArtworkSettingsManager.FallbackMode,
    onModeSelected: (ArtworkSettingsManager.FallbackMode) -> Unit
) {
    val modes = remember {
        listOf(
            FallbackModeItem(
                ArtworkSettingsManager.FallbackMode.ALWAYS,
                "Always",
                "Always use generated artwork"
            ),
            FallbackModeItem(
                ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT,
                "Smart Detection",
                "Detect and replace placeholder images"
            ),
            FallbackModeItem(
                ArtworkSettingsManager.FallbackMode.NO_ARTWORK,
                "No Artwork Only",
                "Only when no artwork URL exists"
            ),
            FallbackModeItem(
                ArtworkSettingsManager.FallbackMode.ON_ERROR,
                "On Error",
                "Only when image fails to load"
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { item ->
            SelectableCard(
                title = item.title,
                description = item.description,
                selected = selectedMode == item.mode,
                onClick = { onModeSelected(item.mode) }
            )
        }
    }
}

@Composable
private fun StyleSelector(
    selectedStyle: ArtworkSettingsManager.ArtworkStyle,
    onStyleSelected: (ArtworkSettingsManager.ArtworkStyle) -> Unit
) {
    val styles = listOf(
        ArtworkSettingsManager.ArtworkStyle.GRADIENT to "Gradient",
        ArtworkSettingsManager.ArtworkStyle.SOLID to "Solid",
        ArtworkSettingsManager.ArtworkStyle.PATTERN to "Pattern",
        ArtworkSettingsManager.ArtworkStyle.WAVEFORM to "Waveform",
        ArtworkSettingsManager.ArtworkStyle.MINIMAL to "Minimal"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        styles.forEach { (style, name) ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStyleSelected(style) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedStyle == style)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedStyle == style)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSelector(
    selectedPalette: ArtworkSettingsManager.ColorPalette,
    onPaletteSelected: (ArtworkSettingsManager.ColorPalette) -> Unit
) {
    val palettes = listOf(
        ArtworkSettingsManager.ColorPalette.MATERIAL_YOU to "Material You",
        ArtworkSettingsManager.ColorPalette.VIBRANT to "Vibrant",
        ArtworkSettingsManager.ColorPalette.PASTEL to "Pastel",
        ArtworkSettingsManager.ColorPalette.MONOCHROME to "Monochrome",
        ArtworkSettingsManager.ColorPalette.DYNAMIC to "Dynamic"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        palettes.forEach { (palette, name) ->
            SelectableCard(
                title = name,
                description = when (palette) {
                    ArtworkSettingsManager.ColorPalette.MATERIAL_YOU -> "Material Design inspired colors"
                    ArtworkSettingsManager.ColorPalette.VIBRANT -> "Bright, saturated colors"
                    ArtworkSettingsManager.ColorPalette.PASTEL -> "Soft, light colors"
                    ArtworkSettingsManager.ColorPalette.MONOCHROME -> "Grayscale tones"
                    ArtworkSettingsManager.ColorPalette.DYNAMIC -> "Based on album title"
                },
                selected = selectedPalette == palette,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

@Composable
private fun SelectableCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
