package com.craftworks.music.ui.elements.dialogs.appearance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.ActionButton
import com.craftworks.music.ui.elements.ActionButtonType
import com.craftworks.music.ui.elements.getActionButtonIconText
import kotlinx.coroutines.delay
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// this code isn't the best, but works for now

private enum class DragZone { Row, Menu, Trash, None }

private fun LayoutCoordinates.rectRelativeTo(container: LayoutCoordinates): Rect {
    val topLeft = container.localPositionOf(this, Offset.Zero)
    return Rect(topLeft, size.toSize())
}

private val MoreSize = 64.dp
private val ChipSize = 64.dp
private val RowGap = 12.dp

private fun chipRowWidth(count: Int): Dp =
    if (count <= 0) 0.dp else ChipSize * count + RowGap * (count - 1)

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListActionButtonsDialog(
    title: String = "",
    actionButtons: List<ActionButton> = emptyList(),
    supportedButtonTypes: List<ActionButtonType> = emptyList(),
    onSet: (buttons: List<ActionButton>) -> Unit = { },
    onDismissRequest: () -> Unit = { },
) {
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val providerFeatures = currentProvider?.featureFlags ?: return

    val density = LocalDensity.current
    val morePx = with(density) { MoreSize.toPx() }
    val chipPx = with(density) { ChipSize.toPx() }
    val gapPx = with(density) { RowGap.toPx() }

    val ghostSpec = remember {
        spring<Offset>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        )
    }
    // Dedicated spring used after release: the ghost travels directly to the
    // fixed final slot rather than chasing the real chip's animateItem() motion.
    val releaseSpec = remember {
        spring<Offset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    }
    val chipsWidthSpec = remember {
        spring<Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    }

    val buttons = remember {
        mutableStateListOf<Pair<String, ActionButton>>().apply {
            addAll(
                actionButtons
                    .filter { it.isCompatible(providerFeatures) }
                    .map { Uuid.generateV4().toString() to it.copy() }
            )
        }
    }

    // Drag state
    var draggedButtonId by remember { mutableStateOf<String?>(null) }
    var currentZone by remember { mutableStateOf(DragZone.None) }
    var dragSession by remember { mutableStateOf(0) }
    var isReleasing by remember { mutableStateOf(false) }
    var releaseTarget by remember { mutableStateOf<Offset?>(null) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var floatingSize by remember { mutableStateOf(IntSize.Zero) }
    var ghostVisualPos by remember { mutableStateOf(Offset.Zero) }

    // Coordinate tracking
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chipCoords = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    val chipCenters = remember { mutableStateMapOf<String, Offset>() }
    var rowZoneCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuZoneCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuHeaderCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var trashZoneCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rowWidthPx by remember { mutableStateOf(0) }
    var menuWidthPx by remember { mutableStateOf(0) }
    var playMinWidthPx by remember { mutableStateOf(0) }

    fun currentChipRects(container: LayoutCoordinates): Map<String, Rect> =
        chipCoords.filterValues { it.isAttached }
            .mapValues { (_, coords) -> coords.rectRelativeTo(container) }

    fun finalChipCenter(
        id: String,
        container: LayoutCoordinates,
    ): Offset? {
        val pair = buttons.firstOrNull { it.first == id } ?: return null
        val inMenu = pair.second.inMenu
        val section = buttons.filter { it.second.inMenu == inMenu }
        val index = section.indexOfFirst { it.first == id }
        if (index < 0) return null

        return if (!inMenu) {
            val rowRect = rowZoneCoords
                ?.takeIf { it.isAttached }
                ?.rectRelativeTo(container)
                ?: return null

            val x =
                rowRect.left +
                        morePx +
                        gapPx +
                        index * (chipPx + gapPx) +
                        chipPx / 2f

            Offset(
                x = x,
                y = rowRect.center.y,
            )
        } else {
            val menuRect = menuZoneCoords
                ?.takeIf { it.isAttached }
                ?.rectRelativeTo(container)
                ?: return null

            val headerRect = menuHeaderCoords
                ?.takeIf { it.isAttached }
                ?.rectRelativeTo(container)
                ?: return null

            val itemGapPx = with(density) { 8.dp.toPx() }
            val chipHeightPx = with(density) { 48.dp.toPx() }

            // The LazyColumn's vertical arrangement and content padding are fixed.
            // Start from the actual header position, then place the target directly
            // at the requested final index. This is independent of animateItem().
            val y =
                headerRect.bottom +
                        itemGapPx +
                        index * (chipHeightPx + itemGapPx) +
                        chipHeightPx / 2f

            Offset(
                x = menuRect.center.x,
                y = y,
            )
        }
    }

    fun zoneFor(pos: Offset, container: LayoutCoordinates): DragZone {
        val menuRect = menuZoneCoords?.takeIf { it.isAttached }?.rectRelativeTo(container)
        val rowRect = rowZoneCoords?.takeIf { it.isAttached }?.rectRelativeTo(container)
        val trashRect = trashZoneCoords?.takeIf { it.isAttached }?.rectRelativeTo(container)
        return when {
            menuRect?.contains(pos) == true -> DragZone.Menu
            rowRect?.contains(pos) == true -> DragZone.Row
            trashRect?.contains(pos) == true -> DragZone.Trash
            else -> DragZone.None
        }
    }

    fun hasRoomForRowEntry(): Boolean {
        if (rowWidthPx <= 0 || playMinWidthPx <= 0) return true
        val n = buttons.count { !it.second.inMenu } + 1
        val newChipsPx = chipPx * n + gapPx * (n - 1)
        return rowWidthPx - morePx - 2 * gapPx - newChipsPx >= playMinWidthPx
    }

    fun updateLiveDragPosition(pos: Offset, zone: DragZone, container: LayoutCoordinates): Boolean {
        val activeId = draggedButtonId ?: return false
        val activeIndex = buttons.indexOfFirst { it.first == activeId }
        if (activeIndex == -1) return false
        val activePair = buttons[activeIndex]

        if (zone == DragZone.Trash) return activePair.second.inMenu

        val targetInMenu = when (zone) {
            DragZone.Menu -> true
            DragZone.Row -> {
                val alreadyInRow = !activePair.second.inMenu
                !(alreadyInRow || hasRoomForRowEntry())
            }
            else -> activePair.second.inMenu
        }

        val rects = currentChipRects(container)
        val targetSectionButtons =
            buttons.filter { it.first != activeId && it.second.inMenu == targetInMenu }

        val targetRelativeIndex = if (targetSectionButtons.isEmpty()) {
            0
        } else if (targetInMenu) {
            var idx = targetSectionButtons.size
            for (i in targetSectionButtons.indices) {
                val rect = rects[targetSectionButtons[i].first]
                if (rect != null && pos.y < rect.center.y) { idx = i; break }
            }
            idx
        } else {
            var idx = targetSectionButtons.size
            for (i in targetSectionButtons.indices) {
                val rect = rects[targetSectionButtons[i].first]
                if (rect != null && pos.x < rect.center.x) { idx = i; break }
            }
            idx
        }

        val remainingRow = buttons.filter { it.first != activeId && !it.second.inMenu }
        val remainingMenu = buttons.filter { it.first != activeId && it.second.inMenu }
        val updatedPair = activeId to activePair.second.copy(inMenu = targetInMenu)

        val newRowList = remainingRow.toMutableList()
        val newMenuList = remainingMenu.toMutableList()
        if (targetInMenu) {
            newMenuList.add(targetRelativeIndex.coerceIn(0, newMenuList.size), updatedPair)
        } else {
            newRowList.add(targetRelativeIndex.coerceIn(0, newRowList.size), updatedPair)
        }

        val newList = newRowList + newMenuList
        if (buttons != newList) {
            buttons.clear()
            buttons.addAll(newList)
        }

        return targetInMenu
    }

    // ── End-of-release detection ────────────────────────────────────────
    // Polls the dragged chip's position; once it has been stable for a few
    // frames, the item has finished its animateItem() spring and we can
    // drop the ghost. Cancels cleanly if a new drag starts (key change).
    LaunchedEffect(isReleasing, draggedButtonId) {
        val id = draggedButtonId ?: return@LaunchedEffect
        if (!isReleasing) return@LaunchedEffect

        // Phase 1: wait for the real chip to stop moving (animateItem settle).
        var last: Offset? = null
        var stableFrames = 0
        while (true) {
            delay(50)
            val pos = chipCenters[id] ?: continue
            if (last != null && (pos - last!!).getDistance() < 0.75f) {
                stableFrames++
                if (stableFrames >= 3) break
            } else {
                stableFrames = 0
            }
            last = pos
        }

        // Phase 2: wait for the ghost to actually reach its release target.
        // Without this, a same-position drop dismisses the ghost ~200ms in,
        // long before the StiffnessMediumLow release spring finishes.
        val target = releaseTarget
        if (target != null) {
            while ((ghostVisualPos - target).getDistance() > 1f) {
                delay(16)
            }
        }

        isReleasing = false
        draggedButtonId = null
        releaseTarget = null
    }

    var showAddButtonMenu by remember { mutableStateOf(false) }
    val isDragging = draggedButtonId != null

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            onSet(buttons.map { it.second })
                            onDismissRequest()
                        }) {
                            Text(stringResource(R.string.action_save))
                        }

                        AnimatedContent(targetState = isDragging, label = "ActionIconTransition") { dragging ->
                            if (dragging) {
                                val isHoveredOverTrash = currentZone == DragZone.Trash
                                val trashScale by animateFloatAsState(
                                    targetValue = if (isHoveredOverTrash) 1.25f else 1.0f,
                                    label = "trashScale"
                                )
                                val trashColor by animateColorAsState(
                                    targetValue = if (isHoveredOverTrash) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    label = "trashColor"
                                )
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { trashZoneCoords = it }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_remove),
                                        tint = trashColor,
                                        modifier = Modifier.size(28.dp).scale(trashScale)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.onGloballyPositioned { trashZoneCoords = it }) {
                                    IconButton(onClick = { showAddButtonMenu = true }) {
                                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                                    }
                                    DropdownMenu(
                                        expanded = showAddButtonMenu,
                                        onDismissRequest = { showAddButtonMenu = false }
                                    ) {
                                        supportedButtonTypes.forEach { type ->
                                            val (icon, text) = getActionButtonIconText(type)
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(icon, contentDescription = text) },
                                                text = { Text(text) },
                                                onClick = {
                                                    showAddButtonMenu = false
                                                    buttons.add(
                                                        Uuid.generateV4().toString() to
                                                                ActionButton(type = type, inMenu = true)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .onGloballyPositioned { containerCoords = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val container = containerCoords ?: return@awaitEachGesture
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pressPos = down.position

                                // Ignore new presses while a release is
                                // still settling.
                                if (draggedButtonId != null) return@awaitEachGesture

                                val hitEntry = currentChipRects(container).entries
                                    .firstOrNull { it.value.contains(pressPos) }
                                    ?: return@awaitEachGesture
                                val hitId = hitEntry.key
                                val hitBounds = hitEntry.value

                                down.consume()

                                // All synchronous writes. `dragSession++`
                                // recreates the ghost's animateOffsetAsState
                                // fresh at the current target on the next
                                // composition
                                dragSession++
                                draggedButtonId = hitId
                                isReleasing = false
                                releaseTarget = null
                                pointerPos = pressPos
                                floatingSize = IntSize(
                                    hitBounds.width.toInt(),
                                    hitBounds.height.toInt(),
                                )

                                val zone0 = zoneFor(pressPos, container)
                                currentZone = zone0
                                val inMenu0 = updateLiveDragPosition(pressPos, zone0, container)
                                if (inMenu0 && menuWidthPx > 0) {
                                    pointerPos = Offset(menuWidthPx / 2f, pressPos.y)
                                }

                                drag(down.id) { change ->
                                    change.consume()
                                    val pos = change.position
                                    val zone = zoneFor(pos, container)
                                    currentZone = zone
                                    val inMenu = updateLiveDragPosition(pos, zone, container)
                                    pointerPos = if (inMenu && menuWidthPx > 0) {
                                        Offset(menuWidthPx / 2f, pos.y)
                                    } else {
                                        pos
                                    }
                                }

                                if (currentZone == DragZone.Trash) {
                                    buttons.removeAll { it.first == hitId }
                                    draggedButtonId = null
                                    isReleasing = false
                                    releaseTarget = null
                                } else {
                                    // Capture the FINAL destination exactly once.
                                    // The real chip can now finish its animateItem()
                                    // independently; the ghost never chases it.
                                    releaseTarget = finalChipCenter(hitId, container) ?: pointerPos
                                    isReleasing = true
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Visible row", style = MaterialTheme.typography.labelLarge)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .onGloballyPositioned {
                                    rowZoneCoords = it
                                    rowWidthPx = it.size.width
                                },
                        ) {
                            val visibleChips = buttons.filter { !it.second.inMenu }
                            // Animated chips-row width. Because the Play button
                            // below uses weight(1f), it absorbs the difference,
                            // so as this animates the Play button visibly
                            // expands / contracts on its LEFT edge while its
                            // right edge stays pinned to the row's right wall.
                            val chipsWidth by animateDpAsState(
                                targetValue = chipRowWidth(visibleChips.size),
                                animationSpec = chipsWidthSpec,
                                label = "chipsWidth"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(RowGap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {},
                                    modifier = Modifier.size(MoreSize)
                                ) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                                }

                                LazyRow(
                                    modifier = Modifier
                                        .width(chipsWidth)
                                        .clipToBounds(),
                                    horizontalArrangement = Arrangement.spacedBy(RowGap),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(visibleChips, key = { it.first }) { (id, button) ->
                                        DraggableChip(
                                            modifier = Modifier.animateItem(),
                                            button = button,
                                            isBeingDragged = draggedButtonId == id,
                                            onCoordinatesChanged = { coords ->
                                                chipCoords[id] = coords
                                                val ctr = containerCoords
                                                if (ctr != null && coords.isAttached) {
                                                    chipCenters[id] = coords.rectRelativeTo(ctr).center
                                                }
                                            }
                                        )
                                    }
                                }

                                // Play button
                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.action_play)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.action_play))
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onGloballyPositioned {
                                    menuZoneCoords = it
                                    menuWidthPx = it.size.width
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            item {
                                Text(
                                    "Overflow menu",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.onGloballyPositioned {
                                        menuHeaderCoords = it
                                    },
                                )
                            }
                            items(buttons.filter { it.second.inMenu }, key = { it.first }) { (id, button) ->
                                DraggableChip(
                                    modifier = Modifier.animateItem(),
                                    button = button,
                                    isBeingDragged = draggedButtonId == id,
                                    onCoordinatesChanged = { coords ->
                                        chipCoords[id] = coords
                                        val ctr = containerCoords
                                        if (ctr != null && coords.isAttached) {
                                            chipCenters[id] = coords.rectRelativeTo(ctr).center
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // One-shot off-layout measurement of the Play button's
                    // natural width.
                    if (playMinWidthPx == 0) {
                        Box(
                            modifier = Modifier.layout { measurable, _ ->
                                val placeable = measurable.measure(Constraints())
                                if (playMinWidthPx != placeable.width) {
                                    playMinWidthPx = placeable.width
                                }
                                layout(0, 0) { }
                            }
                        ) {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(64.dp),
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.action_play))
                                }
                            }
                        }
                    }

                    // Floating ghost. The animateOffsetAsState is created
                    // inside key(dragSession), so a new drag begins with a fresh
                    // animation state seeded at the press position.
                    draggedButtonId?.let { activeId ->
                        val activePair = buttons.firstOrNull { it.first == activeId }
                        if (activePair != null) {
                            key(dragSession) {
                                // Fixed target on release: the final slot captured
                                // when the finger lifted. This is intentionally NOT
                                // chipCenters[activeId], because that position may
                                // still be moving due to animateItem().
                                val ghostTarget = if (isReleasing) {
                                    releaseTarget ?: pointerPos
                                } else {
                                    if (activePair.second.inMenu && menuWidthPx > 0) {
                                        Offset(menuWidthPx / 2f, pointerPos.y)
                                    } else {
                                        pointerPos
                                    }
                                }

                                val ghostPos by animateOffsetAsState(
                                    targetValue = ghostTarget,
                                    animationSpec = if (isReleasing) releaseSpec else ghostSpec,
                                    label = "ghostPos"
                                )

                                LaunchedEffect(ghostPos) { ghostVisualPos = ghostPos }

                                val cornerRadius by animateDpAsState(
                                    targetValue = if (activePair.second.inMenu) 12.dp else 32.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "cornerRadius"
                                )
                                val menuWidthDp = with(density) {
                                    (if (menuWidthPx > 0) menuWidthPx else 240).toDp()
                                }
                                Box(
                                    modifier = Modifier
                                        .zIndex(10f)
                                        .widthIn(max = menuWidthDp)
                                        .onGloballyPositioned { floatingSize = it.size }
                                        .graphicsLayer {
                                            val w = floatingSize.width
                                            val h = floatingSize.height
                                            translationX = ghostPos.x - w / 2f
                                            translationY = ghostPos.y - h / 2f
                                            shadowElevation = 12.dp.toPx()
                                            shape = RoundedCornerShape(cornerRadius)
                                            clip = false
                                        }
                                ) {
                                    ActionButtonChip(button = activePair.second)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableChip(
    button: ActionButton,
    isBeingDragged: Boolean,
    onCoordinatesChanged: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { onCoordinatesChanged(it) }
            .alpha(if (isBeingDragged) 0f else 1f)
    ) {
        ActionButtonChip(button = button)
    }
}

@Composable
private fun ActionButtonChip(button: ActionButton) {
    val (icon, text) = getActionButtonIconText(button.type)
    val inMenu = button.inMenu
    val tonalColors = IconButtonDefaults.filledTonalIconButtonColors()

    val expansionProgress by animateFloatAsState(
        targetValue = if (inMenu) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expansionProgress"
    )
    val containerColor by animateColorAsState(
        targetValue = if (inMenu) MaterialTheme.colorScheme.surfaceContainerHigh else tonalColors.containerColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (inMenu) MaterialTheme.colorScheme.onSurface else tonalColors.contentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "contentColor"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (inMenu) 12.dp else 32.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cornerRadius"
    )

    Surface(
        modifier = Modifier.layout { measurable, constraints ->
            val minSizePx = ChipSize.roundToPx()
            val targetHeightPx = 48.dp.roundToPx()
            val maxWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else minSizePx

            val currentWidth = (minSizePx + (maxWidthPx - minSizePx) * expansionProgress).toInt()
            val currentHeight = (minSizePx + (targetHeightPx - minSizePx) * expansionProgress).toInt()

            val placeable = measurable.measure(Constraints.fixed(currentWidth, currentHeight))
            layout(currentWidth, currentHeight) { placeable.placeRelative(0, 0) }
        },
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null)
            AnimatedVisibility(
                visible = inMenu,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}