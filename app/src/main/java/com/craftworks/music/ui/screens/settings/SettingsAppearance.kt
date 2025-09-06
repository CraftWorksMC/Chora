package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
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
import com.craftworks.music.ui.elements.dialogs.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.ThemeDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_AppearanceScreen(navHostController: NavHostController = rememberNavController()) {
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNavbarItemsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .clickable(
                onClick = { focusManager.clearFocus() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
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
                        .size(48.dp)
                        .focusRequester(focusRequester)
                        .focusProperties { left = FocusRequester.Cancel }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        Column(Modifier.padding(12.dp,12.dp,24.dp,12.dp)){

            //Username
            val username by SettingsManager(context).usernameFlow.collectAsState("Username")
            var usernameTextField by remember(username) { mutableStateOf(username) }

            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .focusRequester(focusRequester)
                    .focusProperties { left = FocusRequester.Cancel }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_username),
                    contentDescription = "Settings Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                OutlinedTextField(
                    value = usernameTextField,
                    onValueChange = {
                        coroutineScope.launch {
                            usernameTextField = it
                            SettingsManager(context).setUsername(it)
                        }
                    },
                    label = { Text("Username:") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }


            //Background Style
            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        showBackgroundDialog = true
                    }
                    .focusRequester(focusRequester)
                    .focusProperties { left = FocusRequester.Cancel }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                    contentDescription = "Background Style Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Background),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    val backgroundType by SettingsManager(context).npBackgroundFlow.collectAsState("Animated Blur")
                    val backgroundTypes = listOf(
                        "Plain", "Static Blur", "Animated Blur"
                    )
                    val backgroundTypeStrings = listOf(
                        R.string.Background_Plain,
                        R.string.Background_Blur,
                        R.string.Background_Anim
                    )
                    Text(
                        text = stringResource(
                            id = backgroundTypeStrings[backgroundTypes.indexOf(backgroundType)]
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            //Theme
            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        showThemesDialog = true
                    }
                    .focusRequester(focusRequester)
                    .focusProperties { left = FocusRequester.Cancel }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                    contentDescription = "Background Style Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Dialog_Theme),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    val selectedTheme by SettingsManager(context).appTheme.collectAsState(SettingsManager.Companion.AppTheme.SYSTEM.name)

                    val themes = listOf(
                        SettingsManager.Companion.AppTheme.DARK.name,
                        SettingsManager.Companion.AppTheme.LIGHT.name,
                        SettingsManager.Companion.AppTheme.SYSTEM.name
                    )

                    val themeStrings = listOf(
                        R.string.Theme_Dark, R.string.Theme_Light, R.string.Theme_System
                    )
                    Text(
                        text = stringResource(
                            id = themeStrings[themes.indexOf(selectedTheme)]
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            //Navbar Items
            val navBarItemsEnabled = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION

            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (navBarItemsEnabled)
                            showNavbarItemsDialog = true
                    }
                    .focusable(navBarItemsEnabled)
                    .focusRequester(focusRequester)
                    .focusProperties { left = FocusRequester.Cancel }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_navbar_items),
                    contentDescription = "Navbar Menus Icon",
                    tint = if (navBarItemsEnabled)
                        MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(0.5f),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Navbar_Items),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = if (navBarItemsEnabled)
                        MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(0.5f),
                        modifier = Modifier.fillMaxSize(),
                        textAlign = TextAlign.Start
                    )
                    val enabledNavbarItems =
                        SettingsManager(context).bottomNavItemsFlow.collectAsState(emptyList()).value
                            .filter { it.enabled }
                            .joinToString(", ") { it.title }
                    Text(
                        text = if (navBarItemsEnabled) enabledNavbarItems else "Not Available for Android TV",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color =if (navBarItemsEnabled)
                            MaterialTheme.colorScheme.onBackground.copy(0.75f)
                        else MaterialTheme.colorScheme.onBackground.copy(0.4f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            //More Song Info
            val showMoreInfo = SettingsManager(context).showMoreInfoFlow.collectAsState(true)

            SettingsSwitch(
                showMoreInfo.value,
                stringResource(R.string.Setting_MoreInfo),
                toggleEvent = {
                    coroutineScope.launch {
                        SettingsManager(context).setShowMoreInfo(!showMoreInfo.value)
                    }
                }
            )

            //Lyrics blur Info
            val nowPlayingLyricsBlur = SettingsManager(context).nowPlayingLyricsBlurFlow.collectAsState(true)

            SettingsSwitch(
                nowPlayingLyricsBlur.value,
                stringResource(R.string.Setting_NowPlayingLyricsBlur),
                toggleEvent = {
                    coroutineScope.launch {
                        SettingsManager(context).setNowPlayingLyricsBlur(!nowPlayingLyricsBlur.value)
                    }
                },
                enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
            )

            //Show Navidrome Logo
            val showNavidromeLogo = SettingsManager(context).showNavidromeLogoFlow.collectAsState(true)

            SettingsSwitch(
                showNavidromeLogo.value,
                stringResource(R.string.Setting_NavidromeLogo),
                toggleEvent = {
                    coroutineScope.launch {
                        SettingsManager(context).setShowNavidromeLogo(!showNavidromeLogo.value)
                    }
                }
            )

            //Show Provider Dividers
            val showProviderDividers = SettingsManager(context).showProviderDividersFlow.collectAsState(true)

            SettingsSwitch(
                showProviderDividers.value,
                stringResource(R.string.Setting_ProviderDividers),
                toggleEvent = {
                    coroutineScope.launch {
                        SettingsManager(context).setShowProviderDividers(!showProviderDividers.value)
                    }
                }
            )

            //Refresh Ripple
            val refreshRipple = SettingsManager(context).refreshAnimationFlow.collectAsState(true)

            SettingsSwitch(
                refreshRipple.value,
                stringResource(R.string.Setting_RefreshAnimation),
                toggleEvent = {
                    coroutineScope.launch {
                        SettingsManager(context).setUseRefreshAnimation(!refreshRipple.value)
                    }
                },
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            )

            // Lyrics Animation Speed
            val lyricsAnimationSpeed = SettingsManager(context).lyricsAnimationSpeedFlow.collectAsState(1200)
            val interactionSource = remember { MutableInteractionSource() }
            Column {
                Text(
                    text = stringResource(R.string.Setting_LyricsAnimationSpeed),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
                Slider(
                    modifier = Modifier
                        //.semantics { contentDescription = "Lyrics Animation Speed" }
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                                    runBlocking {
                                        SettingsManager(context).setLyricsAnimationSpeed((lyricsAnimationSpeed.value + 300).coerceAtMost(2400))
                                    }
                                    true
                                }
                                keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
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

        if(showBackgroundDialog)
            BackgroundDialog(setShowDialog = { showBackgroundDialog = it })

        if(showThemesDialog)
            ThemeDialog(setShowDialog = { showThemesDialog = it })

        if(showNavbarItemsDialog)
            NavbarItemsDialog(setShowDialog = { showNavbarItemsDialog = it })
    }
}

@Composable
private fun SettingsSwitch(
    selected: Boolean,
    settingsName: String,
    toggleEvent: () -> Unit = {},
    enabled: Boolean = true
){
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                onClick = toggleEvent,
                role = Role.RadioButton,
                enabled = enabled
            )
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.s_a_moreinfo),
            contentDescription = "Settings Icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(32.dp)
        )
        Text(
            text = settingsName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Switch(checked = selected, onCheckedChange = { toggleEvent() }, enabled = enabled)
    }
}