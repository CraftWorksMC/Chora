package com.craftworks.music.ui.elements.dialogs.appearance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.screens.HomeItem
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Preview(showBackground = true)
@Composable
fun PreviewHomeItemsDialog(){
    HomeItemsDialog(setShowDialog = { })
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemsDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeItems =
        (AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(null).value ?: emptyList()).toMutableList()

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.appearance_home_items)) },
        text = {
            val lazyListState = rememberLazyListState()
            val reorderableLazyColumnState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    AppearanceSettingsManager(context).setHomeItems(homeItems.toMutableList()
                        .apply {
                            add(to.index, removeAt(from.index))
                        })
                }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState
            ) {
                items(homeItems, key = { it.key }) { item ->
                    ReorderableItem(reorderableLazyColumnState, item.key) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val index = homeItems.indexOf(item)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(48.dp)
                        ) {
                            Checkbox(
                                checked = homeItems[index].enabled,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        homeItems[index] = homeItems[index].copy(enabled = it)
                                        AppearanceSettingsManager(context).setHomeItems(homeItems)
                                    }
                                },
                                modifier = Modifier
                                    .bounceClick()
                            )
                            val titleMap = remember {
                                mapOf(
                                    "recently_played" to R.string.home_recently_played,
                                    "recently_added" to R.string.home_recently_added,
                                    "most_played" to R.string.home_most_played,
                                    "random_songs" to R.string.home_explore_library
                                )
                            }
                            Text(
                                text = stringResource(titleMap[item.key] ?: androidx.media3.session.R.string.error_message_fallback),
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                    },
                                    onDragStopped = {
                                    },
                                    interactionSource = interactionSource,
                                ),
                                onClick = {},
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                                    contentDescription = "Reorder"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                setShowDialog(false)
            }) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        AppearanceSettingsManager(context).setHomeItems(
                            //region Default Values
                            mutableStateListOf(
                                HomeItem(
                                    "recently_played",
                                    true
                                ),
                                HomeItem(
                                    "recently_added",
                                    true
                                ),
                                HomeItem(
                                    "most_played",
                                    true
                                ),
                                HomeItem(
                                    "random_songs",
                                    true
                                )
                            ) //endregion
                        )
                        setShowDialog(false)
                    }
                }
            ) {
                Text(stringResource(R.string.action_reset))
            }
        }
    )
}