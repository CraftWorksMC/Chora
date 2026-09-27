package com.craftworks.music.ui.elements

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderFeature
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.EnumSet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListActionButtons(
    buttons: List<ActionButton>,
    providerFeatures: EnumSet<ProviderFeature>?,
    playAction: () -> Unit,
    isStarred: Boolean = false
) {
    if (providerFeatures == null)
        return

    val rowButtons = buttons.filter { !it.inMenu && it.isCompatible(providerFeatures) }
    val menuButtons = buttons.filter { it.inMenu && it.isCompatible(providerFeatures) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val buttonSizePx = remember(density) { with(density) { 72.dp.roundToPx() } }

    var availableButtons by remember { mutableIntStateOf(0) }

    val interactionSources = remember(rowButtons) {
        List(rowButtons.size + 2) { MutableInteractionSource() }
    }

    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier
            .height(64.dp)
            .widthIn(max = 640.dp)
            .onSizeChanged { size ->
                val availableSpace =
                    size.width - buttonSizePx * 2 // Remove the more and play buttons
                val newAvailableButtons =
                    0.coerceAtLeast(availableSpace / buttonSizePx).coerceAtMost(rowButtons.size)
                if (newAvailableButtons != availableButtons) {
                    availableButtons = newAvailableButtons
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier
                        .height(64.dp)
                        .animateWidth(interactionSources[1]),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.action_more)
                    )
                }
            },
            menuContent = {}
        )

        for ((index, button) in rowButtons.withIndex()) {
            val interactionSource = interactionSources[index + 2]
            when (button.type) {
                ActionButtonType.SEPARATOR -> {}
                ActionButtonType.FAVORITE -> {
                    customItem(
                        buttonGroupContent = {
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val cornerRadius by animateDpAsState(
                                targetValue = if (isStarred || isPressed) 12.dp else 32.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "Star Button Shape Animation"
                            )

                            if (index >= rowButtons.size - availableButtons) {
                                FilledTonalIconButton(
                                    onClick = button.onClick,
                                    interactionSource = interactionSource,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSource),
                                    shape = RoundedCornerShape(cornerRadius),

                                    ) {
                                    Crossfade(
                                        targetState = isStarred
                                    ) {
                                        if (it) Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.round_favorite_24),
                                            contentDescription = stringResource(R.string.action_remove_from_favorites)
                                        )
                                        else
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.round_favorite_border_24),
                                                contentDescription = stringResource(R.string.action_add_to_favorites)
                                            )
                                    }
                                }
                            }
                        },
                        menuContent = { },
                    )
                }

                else -> {
                    customItem(
                        buttonGroupContent = {
                            val (icon, text) = getActionButtonIconText(button.type)

                            if (index >= rowButtons.size - availableButtons) {
                                FilledTonalIconButton(
                                    onClick = button.onClick,
                                    interactionSource = interactionSource,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSource),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = text
                                    )
                                }
                            }
                        },
                        menuContent = { },
                    )
                }
            }
        }

        customItem(
            buttonGroupContent = {
                Button(
                    onClick = playAction,
                    interactionSource = interactionSources[0],
                    modifier = Modifier
                        .height(64.dp)
                        .animateWidth(interactionSources[0])
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.action_play)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.action_play),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            menuContent = { },
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                @Composable
                fun menuButton(button: ActionButton) {
                    when (button.type) {
                        ActionButtonType.SEPARATOR -> HorizontalDivider()
                        ActionButtonType.FAVORITE -> {
                            ListItem (
                                onClick = {
                                    scope.launch { sheetState.hide() }
                                    button.onClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                leadingContent = {
                                    Crossfade(
                                        targetState = isStarred
                                    ) {
                                        if (it) Icon(
                                            imageVector = ImageVector.vectorResource(
                                                R.drawable.round_favorite_24
                                            ),
                                            contentDescription = stringResource(
                                                R.string.action_remove_from_favorites
                                            )
                                        )
                                        else
                                            Icon(
                                                imageVector = ImageVector.vectorResource(
                                                    R.drawable.round_favorite_border_24
                                                ),
                                                contentDescription = stringResource(
                                                    R.string.action_add_to_favorites
                                                )
                                            )
                                    }
                                },
                                content = {
                                    Text(
                                        if (isStarred) stringResource(R.string.action_remove_from_favorites) else stringResource(
                                            R.string.action_add_to_favorites
                                        )
                                    )
                                }
                            )
                        }

                        else -> {
                            val (icon, text) = getActionButtonIconText(button.type)
                            ListItem(
                                onClick = {
                                    scope.launch { sheetState.hide() }
                                    button.onClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                leadingContent = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text
                                    )
                                },
                                content = { Text(text) }
                            )
                        }
                    }
                }

                if (availableButtons < rowButtons.size) {
                    for (button in rowButtons.slice(0..<rowButtons.size-availableButtons)) {
                        menuButton(button)
                    }
                    HorizontalDivider()
                }

                for (button in menuButtons) {
                    menuButton(button)
                }
            }
        }
    }
}

@Composable
fun getActionButtonIconText(type: ActionButtonType): Pair<ImageVector, String> = when (type) {
    ActionButtonType.SEPARATOR -> ImageVector.vectorResource(R.drawable.horizontal_rule_24px) to
            stringResource(R.string.label_separator)

    ActionButtonType.SHUFFLE -> ImageVector.vectorResource(R.drawable.round_shuffle_28) to
            stringResource(R.string.action_shuffle)

    ActionButtonType.FAVORITE -> ImageVector.vectorResource(R.drawable.round_favorite_24) to
            stringResource(R.string.action_add_to_favorites)

    ActionButtonType.ADD_TO_QUEUE -> ImageVector.vectorResource(R.drawable.outline_queue_add_24) to
            stringResource(R.string.action_add_to_queue)

    ActionButtonType.PLAY_NEXT -> ImageVector.vectorResource(R.drawable.play_next_24px) to
            stringResource(R.string.action_play_next)

    ActionButtonType.ADD_TO_PLAYLIST -> ImageVector.vectorResource(R.drawable.rounded_add_24) to
            stringResource(R.string.action_add_to_playlist)

    ActionButtonType.DOWNLOAD -> ImageVector.vectorResource(R.drawable.rounded_download_24) to
            stringResource(R.string.action_download)
}

enum class ActionButtonType {
    SHUFFLE, FAVORITE, PLAY_NEXT, ADD_TO_QUEUE, ADD_TO_PLAYLIST, DOWNLOAD, SEPARATOR
}

@Serializable
data class ActionButton(
    var type: ActionButtonType,
    var inMenu: Boolean,
    @Transient
    var onClick: () -> Unit = {}
) {
    fun isCompatible(flags: EnumSet<ProviderFeature>): Boolean =
        when (type) {
            ActionButtonType.FAVORITE -> flags.contains(ProviderFeature.FAVORITES)
            ActionButtonType.ADD_TO_PLAYLIST -> flags.contains(ProviderFeature.PLAYLISTS)
            ActionButtonType.DOWNLOAD -> flags.contains(ProviderFeature.DOWNLOADS)
            else -> true
        }
}