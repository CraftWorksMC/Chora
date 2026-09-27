package com.craftworks.music.ui.elements.dialogs.appearance

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppTheme
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.runBlocking

@Preview(showBackground = true)
@Composable
fun PreviewThemeDialog(){
    ThemeDialog(setShowDialog = { })
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ThemeDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
        AppTheme.SYSTEM.name)

    val themes = listOf(
        AppTheme.DARK,
        AppTheme.LIGHT,
        AppTheme.SYSTEM
    )

    val themeStrings = listOf(
        R.string.theme_dark, R.string.theme_light, R.string.theme_system
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.appearance_theme)) },
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
                                            AppTheme.DARK -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                    uiModeManager.setApplicationNightMode(
                                                        UiModeManager.MODE_NIGHT_YES
                                                    )
                                                else
                                                    AppCompatDelegate.setDefaultNightMode(
                                                        AppCompatDelegate.MODE_NIGHT_YES
                                                    )
                                            }

                                            AppTheme.LIGHT -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                    uiModeManager.setApplicationNightMode(
                                                        UiModeManager.MODE_NIGHT_NO
                                                    )
                                                else
                                                    AppCompatDelegate.setDefaultNightMode(
                                                        AppCompatDelegate.MODE_NIGHT_NO
                                                    )
                                            }

                                            AppTheme.SYSTEM -> {
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
                                        AppTheme.DARK -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        }
                                        AppTheme.LIGHT -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                                            else
                                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        }
                                        AppTheme.SYSTEM -> {
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
