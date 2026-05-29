package com.craftworks.music.ui.elements.dialogs.tv

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.playing.NowPlayingAlignment
import com.craftworks.music.ui.playing.NowPlayingBackground
import com.craftworks.music.ui.screens.HomeItem
import kotlinx.coroutines.launch

@Preview(device = "id:tv_1080p", showSystemUi = false, showBackground = true)
@Composable
fun NameDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val username by AppearanceSettingsManager(context).usernameFlow.collectAsStateWithLifecycle("Username")
    var usernameTextField by remember(username) { mutableStateOf(username) }

    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            BasicTextField(
                value = usernameTextField,
                onValueChange = {
                    coroutineScope.launch {
                        AppearanceSettingsManager(context).setUsername(it)
                    }
                },
                textStyle = textStyle,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setUsername(username)
                            setShowDialog(false)
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(percent = 50),
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(id = R.drawable.s_a_username),
                                contentDescription = stringResource(R.string.Setting_Username),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            innerTextField()
                        }
                    }
                },
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        AppearanceSettingsManager(context).setUsername(username)
                        setShowDialog(false)
                    }
                }
            ) {
                Text(stringResource(R.string.Action_Done))
            }
        }
    )
}


@Composable
fun ThemeDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedThemeName by AppearanceSettingsManager(context).appTheme.collectAsState(
        AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
    )

    val themes = AppearanceSettingsManager.Companion.AppTheme.entries
    val currentTheme = themes.find { it.name == selectedThemeName }
        ?: AppearanceSettingsManager.Companion.AppTheme.SYSTEM

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Dialog_Theme,
        options = themes,
        selectedOption = currentTheme,
        onOptionSelected = { theme ->
            coroutineScope.launch {
                AppearanceSettingsManager(context).setAppTheme(theme)
                val uiModeManager =
                    context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                when (theme) {
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
        },
        label = { theme ->
            stringResource(
            when (theme) {
                    AppearanceSettingsManager.Companion.AppTheme.DARK -> R.string.Theme_Dark
                    AppearanceSettingsManager.Companion.AppTheme.LIGHT -> R.string.Theme_Light
                    AppearanceSettingsManager.Companion.AppTheme.SYSTEM -> R.string.Theme_System
                }
            )
        }
    )
}

@Composable
fun BackgroundDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
        NowPlayingBackground.ANIMATED_BLUR
    )

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Setting_Background,
        options = NowPlayingBackground.entries,
        selectedOption = backgroundType,
        onOptionSelected = { option ->
            coroutineScope.launch { AppearanceSettingsManager(context).setBackgroundType(option) }
        },
        label = { option ->
            stringResource(
            when (option) {
                    NowPlayingBackground.PLAIN -> R.string.Background_Plain
                    NowPlayingBackground.STATIC_BLUR -> R.string.Background_Blur
                    NowPlayingBackground.ANIMATED_BLUR -> R.string.Background_Anim
                }
            )
        },
        helperText = { option ->
            if (option == NowPlayingBackground.ANIMATED_BLUR && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                " (Android 13+)" else ""
        }
    )
}

@Preview
@Composable
fun NowPlayingAlignmentDialog(
    setShowDialog: (Boolean) -> Unit = { },
    selection: NowPlayingAlignment = NowPlayingAlignment.CENTER,
    onSet: (NowPlayingAlignment) -> Unit = { },
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val nowPlayingTitleAlignment by remember { mutableStateOf(selection) }

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Setting_NowPlayingTitleAlignment,
        options = NowPlayingAlignment.entries,
        selectedOption = nowPlayingTitleAlignment,
        onOptionSelected = { option ->
            onSet(option)
        },
        label = { option ->
            stringResource(
            when (option) {
                    NowPlayingAlignment.LEFT -> R.string.NowPlayingTitleAlignment_Left
                    NowPlayingAlignment.CENTER -> R.string.NowPlayingTitleAlignment_Center
                    NowPlayingAlignment.RIGHT -> R.string.NowPlayingTitleAlignment_Right
                }
            )
        }
    )
}

@Composable
fun NavbarItemsDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navItems = (AppearanceSettingsManager(context).bottomNavItemsFlow
        .collectAsState(initial = emptyList()).value).toMutableList()

    GenericCheckDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Setting_Navbar_Items,
        items = navItems,
        label = { it.title },
        isEnabled = { it.enabled },
        onCheckedChange = { index, checked ->
            coroutineScope.launch {
                navItems[index] = navItems[index].copy(enabled = checked)
                AppearanceSettingsManager(context).setBottomNavItems(navItems)
            }
        },
        onReset = {
            coroutineScope.launch {
                AppearanceSettingsManager(context).setBottomNavItems(
                    mutableStateListOf(
                        BottomNavItem(
                            "Home", R.drawable.rounded_home_24, "home_screen"
                        ), BottomNavItem(
                            "Albums", R.drawable.rounded_library_music_24, "album_screen"
                        ), BottomNavItem(
                            "Songs", R.drawable.round_music_note_24, "songs_screen", false
                        ), BottomNavItem(
                            "Artists", R.drawable.rounded_artist_24, "artists_screen"
                        ), BottomNavItem(
                            "Radios", R.drawable.rounded_radio, "radio_screen"
                        ), BottomNavItem(
                            "Playlists", R.drawable.placeholder, "playlist_screen"
                        )
                    )
                )
            }
        }
    )
}

@Composable
fun HomeItemsDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeItems = (AppearanceSettingsManager(context).homeItemsItemsFlow
        .collectAsState(initial = emptyList()).value).toMutableList()

    val titleMap = remember {
        mapOf(
            "recently_played" to R.string.recently_played,
            "recently_added" to R.string.recently_added,
            "most_played" to R.string.most_played,
            "random_songs" to R.string.random_songs
        )
    }

    GenericCheckDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Setting_Home_Items,
        items = homeItems,
        label = { stringResource(titleMap[it.key] ?: R.string.recently_played) },
        isEnabled = { it.enabled },
        onCheckedChange = { index, checked ->
            coroutineScope.launch {
                homeItems[index] = homeItems[index].copy(enabled = checked)
                AppearanceSettingsManager(context).setHomeItems(homeItems)
            }
        },
        onReset = {
            coroutineScope.launch {
                AppearanceSettingsManager(context).setHomeItems(
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
                        )
                    )
                )
            }
        }
    )
}