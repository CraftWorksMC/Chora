package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.dialogs.BackgroundDialog
import com.craftworks.music.ui.elements.dialogs.NameDialog
import com.craftworks.music.ui.elements.dialogs.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.ThemeDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_AppearanceScreen(navHostController: NavHostController = rememberNavController()) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNavbarItemsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .dialogFocusable()
            //.background(MaterialTheme.colorScheme.background)
    ) {
        /* HEADER */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.Settings_Header_Appearance),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { navHostController.navigate(Screen.Setting.route) {
                    launchSingleTop = true
                } },
                    modifier = Modifier
                        .size(56.dp, 70.dp)
                        .focusRequester(focusRequester)
                        .focusProperties { left = FocusRequester.Cancel }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        Column(
            Modifier
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                //Username
                val username by SettingsManager(context).usernameFlow.collectAsState("Username")
                //var usernameTextField by remember(username) { mutableStateOf(username) }

                SettingsDialogButton(
                    stringResource(R.string.Setting_Username),
                    username,
                    ImageVector.vectorResource(R.drawable.s_a_username),
                    toggleEvent = {
                        showNameDialog = true
                    }
                )
            }

            Column (
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                //Theme
                val selectedTheme by SettingsManager(context).appTheme.collectAsState(
                    SettingsManager.Companion.AppTheme.SYSTEM.name
                )
                val themes = listOf(
                    SettingsManager.Companion.AppTheme.DARK.name,
                    SettingsManager.Companion.AppTheme.LIGHT.name,
                    SettingsManager.Companion.AppTheme.SYSTEM.name
                )
                val themeStrings = listOf(
                    R.string.Theme_Dark, R.string.Theme_Light, R.string.Theme_System
                )
                SettingsDialogButton(
                    stringResource(R.string.Dialog_Theme),
                    stringResource(
                        id = themeStrings[themes.indexOf(selectedTheme)]
                    ),
                    ImageVector.vectorResource(R.drawable.s_a_palette),
                    toggleEvent = {
                        showThemesDialog = true
                    }
                )

                //Background Style
                val backgroundType by SettingsManager(context).npBackgroundFlow.collectAsState("Animated Blur")
                val backgroundTypes = listOf(
                    "Plain", "Static Blur", "Animated Blur"
                )
                val backgroundTypeStrings = listOf(
                    R.string.Background_Plain,
                    R.string.Background_Blur,
                    R.string.Background_Anim
                )
                SettingsDialogButton(
                    stringResource(R.string.Setting_Background),
                    stringResource(
                        id = backgroundTypeStrings[backgroundTypes.indexOf(backgroundType)]
                    ),
                    ImageVector.vectorResource(R.drawable.s_a_palette),
                    toggleEvent = {
                        showBackgroundDialog = true
                    }
                )

                //Navbar Items
                val navBarItemsEnabled =
                    LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION
                val enabledNavbarItems =
                    SettingsManager(context).bottomNavItemsFlow.collectAsState(emptyList()).value
                        .filter { it.enabled }
                        .joinToString(", ") { it.title }
                SettingsDialogButton(
                    stringResource(R.string.Setting_Navbar_Items),
                    enabledNavbarItems,
                    ImageVector.vectorResource(R.drawable.s_a_navbar_items),
                    toggleEvent = {
                        if (navBarItemsEnabled)
                            showNavbarItemsDialog = true
                    }
                )
            }

            Column (
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                //Lyrics blur Info
                val nowPlayingLyricsBlur = SettingsManager(context).nowPlayingLyricsBlurFlow.collectAsState(true)
                SettingsSwitch(
                    nowPlayingLyricsBlur.value,
                    stringResource(R.string.Setting_NowPlayingLyricsBlur),
                    Icons.Rounded.Menu,
                    toggleEvent = {
                        coroutineScope.launch {
                            SettingsManager(context).setNowPlayingLyricsBlur(!nowPlayingLyricsBlur.value)
                        }
                    },
                    enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                )

                // Lyrics Animation Speed
                val lyricsAnimationSpeed = SettingsManager(context).lyricsAnimationSpeedFlow.collectAsState(1200)
                val interactionSource = remember { MutableInteractionSource() }
                Column (
                    Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Text(
                        text = stringResource(R.string.Setting_LyricsAnimationSpeed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp).padding(top = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Slider(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 10.dp)
                            //.semantics { contentDescription = "Lyrics Animation Speed" }
                            .onKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.DirectionRight if keyEvent.type == KeyEventType.KeyDown -> {
                                        runBlocking {
                                            SettingsManager(context).setLyricsAnimationSpeed((lyricsAnimationSpeed.value + 300).coerceAtMost(2400))
                                        }
                                        true
                                    }
                                    Key.DirectionLeft if keyEvent.type == KeyEventType.KeyDown -> {
                                        runBlocking {
                                            SettingsManager(context).setLyricsAnimationSpeed((lyricsAnimationSpeed.value - 300).coerceAtLeast(600))
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
                        interactionSource = interactionSource,
                        value = lyricsAnimationSpeed.value.toFloat(),
                        steps = 5,
                        onValueChange = {
                            runBlocking {
                                SettingsManager(context).setLyricsAnimationSpeed(it.toInt())
                            }
                        },
                        valueRange = 600f..2400f
                    )
                }
            }

            Column (
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                //More Song Info
                val showMoreInfo = SettingsManager(context).showMoreInfoFlow.collectAsState(true)
                SettingsSwitch(
                    showMoreInfo.value,
                    stringResource(R.string.Setting_MoreInfo),
                    ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                    toggleEvent = {
                        coroutineScope.launch {
                            SettingsManager(context).setShowMoreInfo(!showMoreInfo.value)
                        }
                    }
                )

                //Show Navidrome Logo
                val showNavidromeLogo =
                    SettingsManager(context).showNavidromeLogoFlow.collectAsState(true)
                SettingsSwitch(
                    showNavidromeLogo.value,
                    stringResource(R.string.Setting_NavidromeLogo),
                    ImageVector.vectorResource(R.drawable.s_m_navidrome),
                    toggleEvent = {
                        coroutineScope.launch {
                            SettingsManager(context).setShowNavidromeLogo(!showNavidromeLogo.value)
                        }
                    }
                )

                //Show Provider Dividers
                val showProviderDividers =
                    SettingsManager(context).showProviderDividersFlow.collectAsState(true)
                SettingsSwitch(
                    showProviderDividers.value,
                    stringResource(R.string.Setting_ProviderDividers),
                    ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                    toggleEvent = {
                        coroutineScope.launch {
                            SettingsManager(context).setShowProviderDividers(!showProviderDividers.value)
                        }
                    }
                )

                //Refresh Ripple
                val refreshRipple =
                    SettingsManager(context).refreshAnimationFlow.collectAsState(true)
                SettingsSwitch(
                    refreshRipple.value,
                    stringResource(R.string.Setting_RefreshAnimation),
                    Icons.Rounded.Refresh,
                    toggleEvent = {
                        coroutineScope.launch {
                            SettingsManager(context).setUseRefreshAnimation(!refreshRipple.value)
                        }
                    },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                )
            }
        }



        if(showNameDialog)
            NameDialog(setShowDialog = { showNameDialog = it })

        if(showBackgroundDialog)
            BackgroundDialog(setShowDialog = { showBackgroundDialog = it })

        if(showThemesDialog)
            ThemeDialog(setShowDialog = { showThemesDialog = it })

        if(showNavbarItemsDialog)
            NavbarItemsDialog(setShowDialog = { showNavbarItemsDialog = it })
    }
}