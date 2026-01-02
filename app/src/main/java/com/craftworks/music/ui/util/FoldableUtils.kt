package com.craftworks.music.ui.util

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.craftworks.music.LocalFoldingFeatures
import com.craftworks.music.LocalWindowSizeClass

/**
 * Fold state for UI decisions
 */
enum class FoldState {
    FLAT,
    BOOK_MODE,
    TABLE_TOP
}

/**
 * Layout mode for screens
 */
enum class LayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
    TABLE_TOP,
    BOOK_MODE
}

data class FoldableState(
    val foldState: FoldState,
    val layoutMode: LayoutMode,
    val hingeBounds: HingeBounds?,
    val windowSizeClass: WindowSizeClass
)

data class HingeBounds(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
    val isVertical: Boolean
)

@Composable
fun rememberFoldableState(): FoldableState {
    val windowSizeClass = LocalWindowSizeClass.current
    val foldingFeatures = LocalFoldingFeatures.current
    val density = LocalDensity.current

    return remember(windowSizeClass, foldingFeatures, density) {
        val activeFold = foldingFeatures.firstOrNull {
            it.state == FoldingFeature.State.HALF_OPENED
        }

        val foldState = when {
            activeFold == null -> FoldState.FLAT
            activeFold.orientation == FoldingFeature.Orientation.HORIZONTAL -> FoldState.TABLE_TOP
            else -> FoldState.BOOK_MODE // VERTICAL orientation
        }

        val hingeBounds = activeFold?.bounds?.let { bounds ->
            with(density) {
                HingeBounds(
                    left = bounds.left.toDp(),
                    top = bounds.top.toDp(),
                    right = bounds.right.toDp(),
                    bottom = bounds.bottom.toDp(),
                    isVertical = activeFold.orientation == FoldingFeature.Orientation.VERTICAL
                )
            }
        }

        val layoutMode = when {
            foldState == FoldState.TABLE_TOP -> LayoutMode.TABLE_TOP
            foldState == FoldState.BOOK_MODE -> LayoutMode.BOOK_MODE
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> LayoutMode.EXPANDED
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> LayoutMode.MEDIUM
            else -> LayoutMode.COMPACT
        }

        FoldableState(foldState, layoutMode, hingeBounds, windowSizeClass)
    }
}

@Composable
fun shouldUseSplitLayout(): Boolean {
    val state = rememberFoldableState()
    return state.layoutMode in listOf(LayoutMode.EXPANDED, LayoutMode.BOOK_MODE, LayoutMode.TABLE_TOP)
}

@Composable
fun responsiveGridCells(): Int {
    val state = rememberFoldableState()
    return when (state.layoutMode) {
        LayoutMode.COMPACT -> 2
        LayoutMode.MEDIUM -> 3
        LayoutMode.TABLE_TOP -> 3
        LayoutMode.BOOK_MODE -> 4
        LayoutMode.EXPANDED -> 5
    }
}

@Composable
fun responsiveAlbumCardWidth(): Int {
    val state = rememberFoldableState()
    return when (state.layoutMode) {
        LayoutMode.COMPACT -> 128
        LayoutMode.MEDIUM -> 148
        LayoutMode.TABLE_TOP -> 148
        LayoutMode.BOOK_MODE -> 164
        LayoutMode.EXPANDED -> 180
    }
}
