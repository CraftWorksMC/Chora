package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Makes the current dialog a focus group with a [FocusRequester] and restricts the focus from
 * exiting its bounds while it's visible.
 */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun Modifier.dialogFocusable() = composed {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        focusManager.moveFocus(FocusDirection.Enter)
    }
    this.then(
        Modifier
            .focusRequester(focusRequester)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup()
    )
}