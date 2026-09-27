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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Preview(showBackground = true)
@Composable
fun PreviewNavbarItemsDialog(){
    NavbarItemsDialog(setShowDialog = { })
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavbarItemsDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomNavigationItems =
        (AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(null).value ?: emptyList()).toMutableList()

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.appearance_navbar_items)) },
        text = {
            val lazyListState = rememberLazyListState()
            val reorderableLazyColumnState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    AppearanceSettingsManager(context).setBottomNavItems(bottomNavigationItems.toMutableList()
                        .apply {
                            add(to.index, removeAt(from.index))
                        })
                }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState
            ) {
                items(bottomNavigationItems, key = { it.title }) { navItem ->
                    ReorderableItem(reorderableLazyColumnState, navItem.title) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val index = bottomNavigationItems.indexOf(navItem)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(48.dp)
                        ) {
                            Checkbox(
                                enabled = navItem.title != "Home",
                                checked = bottomNavigationItems[index].enabled,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        bottomNavigationItems[index] = bottomNavigationItems[index].copy(enabled = it)
                                        AppearanceSettingsManager(context).setBottomNavItems(bottomNavigationItems)
                                    }
                                },
                                modifier = Modifier
                                    .semantics { contentDescription = navItem.title }
                                    .bounceClick()
                            )
                            Text(
                                text = navItem.title,
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
                        AppearanceSettingsManager(context).setBottomNavItems(
                            //region Default Values
                            mutableStateListOf(
                                BottomNavItem(
                                    "Home", R.drawable.rounded_home_24, Screen.Home
                                ), BottomNavItem(
                                    "Albums",
                                    R.drawable.rounded_library_music_24,
                                    Screen.Albums
                                ), BottomNavItem(
                                    "Songs",
                                    R.drawable.round_music_note_24,
                                    Screen.Songs
                                ), BottomNavItem(
                                    "Artists",
                                    R.drawable.rounded_artist_24,
                                    Screen.Artists
                                ), BottomNavItem(
                                    "Radios", R.drawable.rounded_radio, Screen.Radios
                                ), BottomNavItem(
                                    "Playlists",
                                    R.drawable.placeholder,
                                    Screen.Playlists
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