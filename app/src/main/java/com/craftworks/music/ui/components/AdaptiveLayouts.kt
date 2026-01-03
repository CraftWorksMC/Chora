package com.craftworks.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.rememberFoldableState

@Composable
fun TwoPaneLayout(
    modifier: Modifier = Modifier,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    showSecondaryInCompact: Boolean = false
) {
    val foldableState = rememberFoldableState()

    when (foldableState.layoutMode) {
        LayoutMode.COMPACT -> {
            if (showSecondaryInCompact) {
                Column(modifier) {
                    Box(Modifier.weight(1f)) { primaryContent() }
                    Box(Modifier.weight(1f)) { secondaryContent() }
                }
            } else {
                Box(modifier) { primaryContent() }
            }
        }

        LayoutMode.TABLE_TOP -> {
            Column(modifier) {
                Box(Modifier.weight(1f)) { primaryContent() }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f)) { secondaryContent() }
            }
        }

        LayoutMode.BOOK_MODE, LayoutMode.EXPANDED, LayoutMode.MEDIUM -> {
            Row(modifier) {
                Box(Modifier.weight(0.4f)) { primaryContent() }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(0.6f)) { secondaryContent() }
            }
        }
    }
}

@Composable
fun MasterDetailLayout(
    modifier: Modifier = Modifier,
    masterContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    showDetailOnly: Boolean = false
) {
    val foldableState = rememberFoldableState()
    val useSplit = foldableState.layoutMode in listOf(
        LayoutMode.EXPANDED,
        LayoutMode.BOOK_MODE,
        LayoutMode.MEDIUM
    )

    if (useSplit) {
        Row(modifier.fillMaxSize()) {
            Box(Modifier.weight(0.35f)) { masterContent() }
            Spacer(Modifier.width(1.dp))
            Box(Modifier.weight(0.65f)) { detailContent() }
        }
    } else {
        if (showDetailOnly) {
            detailContent()
        } else {
            masterContent()
        }
    }
}

@Composable
fun TableTopLayout(
    modifier: Modifier = Modifier,
    topContent: @Composable () -> Unit,
    bottomContent: @Composable () -> Unit
) {
    val foldableState = rememberFoldableState()

    if (foldableState.layoutMode == LayoutMode.TABLE_TOP) {
        Column(modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { topContent() }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) { bottomContent() }
        }
    } else {
        Column(modifier.fillMaxSize()) {
            topContent()
            bottomContent()
        }
    }
}
