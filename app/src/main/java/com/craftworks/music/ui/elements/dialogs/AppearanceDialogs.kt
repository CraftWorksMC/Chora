package com.craftworks.music.ui.elements.dialogs

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.bounceClick
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
fun PreviewThemeDialog(){
    ThemeDialog(setShowDialog = { })
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun BackgroundDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val backgroundType by SettingsManager(context).npBackgroundFlow.collectAsState("")

    val backgroundTypes = listOf(
        "Plain", "Static Blur", "Animated Blur"
    )

    val backgroundTypeStrings = listOf(
        R.string.Background_Plain, R.string.Background_Blur, R.string.Background_Anim
    )

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup()
        ) {
            Text(
                text = stringResource(R.string.Setting_Background),
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            for ((index, option) in backgroundTypes.withIndex()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = (option == backgroundType),
                            onClick = {
                                runBlocking {
                                    SettingsManager(context).setBackgroundType(option)
                                }
                                setShowDialog(false)
                            },
                            role = Role.RadioButton,
                            enabled = option == "Animated Blur" && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option == backgroundType,
                        onClick = {
                            runBlocking {
                                SettingsManager(context).setBackgroundType(option)
                            }
                            setShowDialog(false)
                        },
                        modifier = Modifier.bounceClick(),
                        enabled = option == "Animated Blur" && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    )
                    Text(
                        text = stringResource(id = backgroundTypeStrings[index]) +
                                if (option == "Animated Blur" && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
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
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ThemeDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val selectedTheme by SettingsManager(context).appTheme.collectAsState(SettingsManager.Companion.AppTheme.SYSTEM.name)

    val themes = listOf(
        SettingsManager.Companion.AppTheme.DARK,
        SettingsManager.Companion.AppTheme.LIGHT,
        SettingsManager.Companion.AppTheme.SYSTEM
    )

    val themeStrings = listOf(
        R.string.Theme_Dark, R.string.Theme_Light, R.string.Theme_System
    )

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup()
        ) {
            Text(
                text = stringResource(R.string.Dialog_Theme),
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            for ((index, option) in themes.withIndex()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = (option.name == selectedTheme),
                            onClick = {
                                runBlocking {
                                    SettingsManager(context).setAppTheme(option)
                                    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                                    when (option) {
                                        SettingsManager.Companion.AppTheme.DARK -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        }
                                        SettingsManager.Companion.AppTheme.LIGHT -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        }
                                        SettingsManager.Companion.AppTheme.SYSTEM -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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
                                SettingsManager(context).setAppTheme(option)
                                val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

                                when (option) {
                                    SettingsManager.Companion.AppTheme.DARK -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                            uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                                        else
                                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                    }
                                    SettingsManager.Companion.AppTheme.LIGHT -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                            uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                                        else
                                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                    }
                                    SettingsManager.Companion.AppTheme.SYSTEM -> {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavbarItemsDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomNavigationItems =
        (SettingsManager(context).bottomNavItemsFlow.collectAsState(null).value ?: emptyList()).toMutableList()

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Setting_Navbar_Items),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    val lazyListState = rememberLazyListState()
                    val reorderableLazyColumnState =
                        rememberReorderableLazyListState(lazyListState) { from, to ->
                            SettingsManager(context).setBottomNavItems(bottomNavigationItems.toMutableList()
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
                                    var checked by remember { mutableStateOf(true) }
                                    Checkbox(
                                        enabled = navItem.title != "Home",
                                        checked = bottomNavigationItems[index].enabled,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                checked = it
                                                bottomNavigationItems[index] = bottomNavigationItems[index].copy(enabled = it)
                                                SettingsManager(context).setBottomNavItems(bottomNavigationItems)
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    SettingsManager(context).setBottomNavItems(
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
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Reset))
                        }
                        Button(
                            onClick = {
                                setShowDialog(false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Done))
                        }
                    }
                }
            }
        }
    }
}