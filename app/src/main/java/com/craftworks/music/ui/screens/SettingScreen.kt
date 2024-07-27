package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.bounceClick

var username = mutableStateOf("Username")
var showMoreInfo = mutableStateOf(true)
var showNavidromeLogo = mutableStateOf(true)

@Preview(showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingScreen(navHostController: NavHostController = rememberNavController()) {
    //val context = LocalContext.current.applicationContext
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val requester = FocusRequester()

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(
            start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )) {

        /* HEADER */
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { navHostController.navigate(Screen.Home.route) {
                launchSingleTop = true
            } },
                modifier = Modifier
                    .size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back To Home",
                    modifier = Modifier.size(32.dp))
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        /* Settings */
        Box(Modifier.padding(12.dp,12.dp,12.dp,12.dp)){
            Column {
                //region Appearance
                Row (modifier = Modifier
                    .height(76.dp)
                    .padding(vertical = 6.dp)
                    .bounceClick()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button) {
                        navHostController.navigate(Screen.S_Appearance.route) {
                            launchSingleTop = true
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                        contentDescription = stringResource(R.string.Settings_Header_Appearance),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.Settings_Header_Appearance),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.Settings_Header_Appearance),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 12.dp)
                            .rotate(-90f))
                }
                //endregion

                //region Media Providers
                Row (modifier = Modifier
                    .height(76.dp)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .bounceClick()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        navHostController.navigate(Screen.S_Providers.route) {
                            launchSingleTop = true
                        }
                    }
                    .focusable(true),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.s_m_media_providers),
                        contentDescription = stringResource(R.string.Settings_Header_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.Settings_Header_Media),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.Settings_Header_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 12.dp)
                            .rotate(-90f))
                }
                //endregion

                //region Playback
                Row (modifier = Modifier
                    .height(76.dp)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .bounceClick()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        navHostController.navigate(Screen.S_Playback.route) {
                            launchSingleTop = true
                        }
                    }
                    .focusable(true),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.s_m_playback),
                        contentDescription = stringResource(R.string.Settings_Header_Playback),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.Settings_Header_Playback),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.Settings_Header_Playback),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 12.dp)
                            .rotate(-90f))
                }
                //endregion
            }
        }

        BottomSpacer()
    }
}