package com.craftworks.music.ui.elements.dialogs.tv

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.playing.NowPlayingBackground
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges

@Composable
fun <T> GenericListDialog(
    setShowDialog: (Boolean) -> Unit,
    titleRes: Int,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    helperText: (T) -> String = { "" },
    leftAligned: Boolean = false
) {
    AlertDialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = leftAligned
        ),
        modifier = modifier,
        onDismissRequest = { setShowDialog(false) },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(titleRes),
                color = MaterialTheme.colorScheme.onSurface,
                style =  MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (leftAligned) {
                val window = (LocalView.current.parent as? DialogWindowProvider)?.window
                SideEffect {
                    window?.let {
                        it.setGravity(Gravity.END)
                        it.setLayout(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            }

            val lazyColumnState = rememberLazyListState()

            LazyColumn(
                state = lazyColumnState,
                modifier = Modifier
                    .verticalFadingEdges(
                    FadingEdgesContentType.Dynamic.Lazy.List(
                        FadingEdgesScrollConfig.Dynamic(), lazyColumnState
                    ), FadingEdgesGravity.All, 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(options) { option ->
                    val isSelected = option == selectedOption

                    ListItem(
                        selected = isSelected,
                        enabled = !(option == NowPlayingBackground.ANIMATED_BLUR && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU),
                        headlineContent = {
                            Text(label(option) + helperText(option))
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onOptionSelected(option)
                                    setShowDialog(false)
                                }
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            setShowDialog(false)
                        },
                    )
                }
            }
        },
        confirmButton = { }
    )
}

@Composable
fun <T> GenericCheckDialog(
    setShowDialog: (Boolean) -> Unit,
    titleRes: Int,
    items: List<T>,
    label: @Composable (T) -> String,
    isEnabled: (T) -> Boolean,
    onCheckedChange: (index: Int, checked: Boolean) -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(titleRes),
                color = MaterialTheme.colorScheme.onSurface,
                style =  MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(items) { item ->
                    val index = items.indexOf(item)

                    ListItem(
                        selected = false,
                        onClick = { onCheckedChange(index, !isEnabled(item)) },
                        onLongClick = {

                        },
                        headlineContent = {
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        leadingContent = {
                            Checkbox(
                                checked = isEnabled(item),
                                onCheckedChange = null
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { setShowDialog(false) }
            ) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                onReset()
                setShowDialog(false)
            }) {
                Text(stringResource(R.string.action_reset))
            }
        }
    )
}