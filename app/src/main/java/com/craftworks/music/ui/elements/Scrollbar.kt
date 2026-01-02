package com.craftworks.music.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * iPod-style alphabet fast scroller for grids
 */
@Composable
fun <T> AlphabetFastScroller(
    items: List<T>,
    getSectionLetter: (T) -> Char,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val letters = listOf('#') + ('A'..'Z').toList() + listOf('?')
    val coroutineScope = rememberCoroutineScope()

    // Track current visible letter based on scroll position
    val currentLetter by remember(items) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            items.getOrNull(firstVisible)?.let { getSectionLetter(it).uppercaseChar() }
        }
    }

    var showBubble by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Helper to scroll to letter
    fun scrollToLetter(letter: Char) {
        val targetIndex = items.indexOfFirst { item ->
            val itemLetter = getSectionLetter(item).uppercaseChar()
            when (letter) {
                '#' -> !itemLetter.isLetter() && itemLetter != '?'
                '?' -> itemLetter == '?'
                else -> itemLetter == letter
            }
        }
        if (targetIndex >= 0) {
            coroutineScope.launch {
                gridState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        // Letter column on right edge
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(20.dp)
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { showBubble = true },
                        onDragEnd = { showBubble = false },
                        onDragCancel = { showBubble = false },
                        onVerticalDrag = { change, _ ->
                            val letterIndex = (change.position.y / (size.height.toFloat() / letters.size))
                                .toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[letterIndex]
                            selectedLetter = letter
                            scrollToLetter(letter)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val letterIndex = (offset.y / (size.height.toFloat() / letters.size))
                            .toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[letterIndex]
                        selectedLetter = letter
                        showBubble = true
                        scrollToLetter(letter)
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = if (letter == currentLetter) FontWeight.Bold else FontWeight.Normal,
                    color = if (letter == currentLetter)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(16.dp)
                )
            }
        }

        // Large letter bubble when dragging
        AnimatedVisibility(
            visible = showBubble && selectedLetter != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (selectedLetter ?: '#').toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * iPod-style alphabet fast scroller for LazyColumn
 */
@Composable
fun <T> AlphabetFastScrollerList(
    items: List<T>,
    getSectionLetter: (T) -> Char,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val letters = listOf('#') + ('A'..'Z').toList() + listOf('?')
    val coroutineScope = rememberCoroutineScope()

    val currentLetter by remember(items) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            items.getOrNull(firstVisible)?.let { getSectionLetter(it).uppercaseChar() }
        }
    }

    var showBubble by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    fun scrollToLetter(letter: Char) {
        val targetIndex = items.indexOfFirst { item ->
            val itemLetter = getSectionLetter(item).uppercaseChar()
            when (letter) {
                '#' -> !itemLetter.isLetter() && itemLetter != '?'
                '?' -> itemLetter == '?'
                else -> itemLetter == letter
            }
        }
        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(20.dp)
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { showBubble = true },
                        onDragEnd = { showBubble = false },
                        onDragCancel = { showBubble = false },
                        onVerticalDrag = { change, _ ->
                            val letterIndex = (change.position.y / (size.height.toFloat() / letters.size))
                                .toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[letterIndex]
                            selectedLetter = letter
                            scrollToLetter(letter)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val letterIndex = (offset.y / (size.height.toFloat() / letters.size))
                            .toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[letterIndex]
                        selectedLetter = letter
                        showBubble = true
                        scrollToLetter(letter)
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = if (letter == currentLetter) FontWeight.Bold else FontWeight.Normal,
                    color = if (letter == currentLetter)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showBubble && selectedLetter != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (selectedLetter ?: '#').toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// Keep the old simple scrollbar for backwards compatibility on non-alphabetic lists
fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()

        val firstVisibleElement = state.layoutInfo.visibleItemsInfo.firstOrNull()
        val totalItems = state.layoutInfo.totalItemsCount

        if (firstVisibleElement != null && totalItems > 0) {
            val visibleItemCount = state.layoutInfo.visibleItemsInfo.size
            val scrollbarHeight = (this.size.height * visibleItemCount / totalItems)
                .coerceAtLeast(48.dp.toPx())
            val scrollbarOffsetY = state.firstVisibleItemIndex.toFloat() /
                    totalItems * (this.size.height - scrollbarHeight)

            drawRoundRect(
                color = color.copy(alpha = alpha * 0.5f),
                topLeft = Offset(this.size.width - width.toPx() - 4.dp.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(width.toPx() / 2)
            )
        }
    }
}

fun Modifier.drawVerticalScrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()

        val firstVisibleElement = state.layoutInfo.visibleItemsInfo.firstOrNull()
        val totalItems = state.layoutInfo.totalItemsCount

        if (firstVisibleElement != null && totalItems > 0) {
            val visibleItemCount = state.layoutInfo.visibleItemsInfo.size
            val scrollbarHeight = (this.size.height * visibleItemCount / totalItems)
                .coerceAtLeast(48.dp.toPx())
            val scrollbarOffsetY = state.firstVisibleItemScrollOffset.toFloat() /
                    totalItems * (this.size.height - scrollbarHeight) +
                    state.firstVisibleItemIndex.toFloat() / totalItems * (this.size.height - scrollbarHeight)

            drawRoundRect(
                color = color.copy(alpha = alpha * 0.5f),
                topLeft = Offset(this.size.width - width.toPx() - 4.dp.toPx(), scrollbarOffsetY.coerceIn(0f, this.size.height - scrollbarHeight)),
                size = Size(width.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(width.toPx() / 2)
            )
        }
    }
}
