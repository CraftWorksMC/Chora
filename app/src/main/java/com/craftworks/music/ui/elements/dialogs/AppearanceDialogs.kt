package com.craftworks.music.ui.elements.dialogs

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.playing.NowPlayingBackground
import com.craftworks.music.ui.playing.NowPlayingTitleAlignment
import com.craftworks.music.ui.screens.HomeItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewBackgroundDialog(){
    BackgroundDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewNavbarItemsDialog(){
    NavbarItemsDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewHomeItemsDialog(){
    HomeItemsDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewThemeDialog(){
    ThemeDialog(setShowDialog = { })
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun NameDialog(setShowDialog: (Boolean) -> Unit = {} ) {
    val context = LocalContext.current
    val username by AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")
    var usernameTextField by remember(username) { mutableStateOf(username) }

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.Setting_Username)) },
        text = {
            OutlinedTextField(
                value = usernameTextField,
                onValueChange = {
                    runBlocking {
                        AppearanceSettingsManager(context).setUsername(it)
                    }
                },
                label = { stringResource(R.string.Setting_Username) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                runBlocking {
                    AppearanceSettingsManager(context).setUsername(username)
                }
            }) {
                Text(stringResource(R.string.Action_Done))
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun BackgroundDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(NowPlayingBackground.ANIMATED_BLUR)

    val backgroundTypeLabels = mapOf(
        NowPlayingBackground.PLAIN to R.string.Background_Plain,
        NowPlayingBackground.STATIC_BLUR to R.string.Background_Blur,
        NowPlayingBackground.ANIMATED_BLUR to R.string.Background_Anim,
        NowPlayingBackground.SIMPLE_ANIMATED_BLUR to R.string.Background_Anim_Simple
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.Setting_Background)) },
        text = {
            Column{
                NowPlayingBackground.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (option == backgroundType),
                                onClick = {
                                    runBlocking {
                                        AppearanceSettingsManager(context).setBackgroundType(option)
                                    }
                                    setShowDialog(false)
                                },
                                role = Role.RadioButton,
                                enabled = !((option == NowPlayingBackground.ANIMATED_BLUR || option == NowPlayingBackground.SIMPLE_ANIMATED_BLUR) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == backgroundType,
                            onClick = {
                                runBlocking {
                                    AppearanceSettingsManager(context).setBackgroundType(option)
                                }
                                setShowDialog(false)
                            },
                            modifier = Modifier.bounceClick(),
                            enabled = !((option == NowPlayingBackground.ANIMATED_BLUR || option == NowPlayingBackground.SIMPLE_ANIMATED_BLUR) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                        )
                        Text(
                            text = stringResource(id = backgroundTypeLabels[option] ?: androidx.media3.session.R.string.error_message_invalid_state) +
                                    if ((option == NowPlayingBackground.ANIMATED_BLUR || option == NowPlayingBackground.SIMPLE_ANIMATED_BLUR) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                                        " (Android 13+)"
                                    else "",
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { }
    )
}


@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ThemeDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
        AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name)

    val themes = listOf(
       AppearanceSettingsManager.Companion.AppTheme.DARK,
       AppearanceSettingsManager.Companion.AppTheme.LIGHT,
       AppearanceSettingsManager.Companion.AppTheme.SYSTEM
    )

    val themeStrings = listOf(
        R.string.Theme_Dark, R.string.Theme_Light, R.string.Theme_System
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.Dialog_Theme)) },
        text = {
            Column{
                for ((index, option) in themes.withIndex()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (option.name == selectedTheme),
                                onClick = {
                                    runBlocking {
                                        AppearanceSettingsManager(context).setAppTheme(option)
                                        val uiModeManager =
                                            context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                                        when (option) {
                                            AppearanceSettingsManager.Companion.AppTheme.DARK -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                    uiModeManager.setApplicationNightMode(
                                                        UiModeManager.MODE_NIGHT_YES
                                                    )
                                                else
                                                    AppCompatDelegate.setDefaultNightMode(
                                                        AppCompatDelegate.MODE_NIGHT_YES
                                                    )
                                            }

                                            AppearanceSettingsManager.Companion.AppTheme.LIGHT -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                    uiModeManager.setApplicationNightMode(
                                                        UiModeManager.MODE_NIGHT_NO
                                                    )
                                                else
                                                    AppCompatDelegate.setDefaultNightMode(
                                                        AppCompatDelegate.MODE_NIGHT_NO
                                                    )
                                            }

                                            AppearanceSettingsManager.Companion.AppTheme.SYSTEM -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                    uiModeManager.setApplicationNightMode(
                                                        UiModeManager.MODE_NIGHT_AUTO
                                                    )
                                                else
                                                    AppCompatDelegate.setDefaultNightMode(
                                                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                                    )
                                            }
                                        }
                                    }
                                    setShowDialog(false)
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.name == selectedTheme,
                            onClick = {
                                runBlocking {
                                    AppearanceSettingsManager(context).setAppTheme(option)
                                    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                                    when (option) {
                                       AppearanceSettingsManager.Companion.AppTheme.DARK -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        }
                                       AppearanceSettingsManager.Companion.AppTheme.LIGHT -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        }
                                       AppearanceSettingsManager.Companion.AppTheme.SYSTEM -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                        }
                                    }
                                }
                                setShowDialog(false)
                            },
                            modifier = Modifier.bounceClick()
                        )
                        Text(
                            text = stringResource(id = themeStrings[index]),
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { }
    )
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
        title = { Text(stringResource(R.string.Setting_Navbar_Items)) },
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
                Text(stringResource(R.string.Action_Done))
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
                                    "Home", R.drawable.rounded_home_24, "home_screen"
                                ), BottomNavItem(
                                    "Albums",
                                    R.drawable.rounded_library_music_24,
                                    "album_screen"
                                ), BottomNavItem(
                                    "Songs",
                                    R.drawable.round_music_note_24,
                                    "songs_screen"
                                ), BottomNavItem(
                                    "Artists",
                                    R.drawable.rounded_artist_24,
                                    "artists_screen"
                                ), BottomNavItem(
                                    "Radios", R.drawable.rounded_radio, "radio_screen"
                                ), BottomNavItem(
                                    "Playlists",
                                    R.drawable.placeholder,
                                    "playlist_screen"
                                )
                            ) //endregion
                        )
                        setShowDialog(false)
                    }
                }
            ) {
                Text(stringResource(R.string.Action_Reset))
            }
        }
    )
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
        title = { Text(stringResource(R.string.Setting_Home_Items)) },
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
                                    "recently_played" to R.string.recently_played,
                                    "recently_added" to R.string.recently_added,
                                    "most_played" to R.string.most_played,
                                    "random_songs" to R.string.random_songs
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
                Text(stringResource(R.string.Action_Done))
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
                Text(stringResource(R.string.Action_Reset))
            }
        }
    )
}

@Composable
@Preview
fun NowPlayingTitleAlignmentDialog(setShowDialog: (Boolean) -> Unit = { }) {
    val context = LocalContext.current

    val nowPlayingTitleAlignment by AppearanceSettingsManager(context).nowPlayingTitleAlignment.collectAsState(
        NowPlayingTitleAlignment.LEFT
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.Setting_NowPlayingTitleAlignment)) },
        text = {
            Column {
                NowPlayingTitleAlignment.entries.forEach { alignment ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (alignment == nowPlayingTitleAlignment),
                                onClick = {
                                    runBlocking {
                                        AppearanceSettingsManager(context).setNowPlayingTitleAlignment(
                                            alignment
                                        )
                                    }
                                    setShowDialog(false)
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alignment == nowPlayingTitleAlignment,
                            onClick = {
                                runBlocking {
                                    runBlocking {
                                        AppearanceSettingsManager(context).setNowPlayingTitleAlignment(
                                            alignment
                                        )
                                    }
                                    setShowDialog(false)
                                }
                            },
                            modifier = Modifier.bounceClick()
                        )
                        val alignmentStringRes = when (alignment) {
                            NowPlayingTitleAlignment.LEFT -> R.string.NowPlayingTitleAlignment_Left
                            NowPlayingTitleAlignment.CENTER -> R.string.NowPlayingTitleAlignment_Center
                            NowPlayingTitleAlignment.RIGHT -> R.string.NowPlayingTitleAlignment_Right
                        }

                        Text(
                            text = stringResource(id = alignmentStringRes),
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { }
    )
}