package com.craftworks.music.ui.elements.dialogs.tv.provider

import android.content.res.Configuration
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaProviderData
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.data.providers.media.local.LocalMediaProvider
import com.craftworks.music.data.providers.media.local.LocalProviderData
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.launch
import java.io.File

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun CreateLocalProviderDialog(
    setShowDialog: (Boolean) -> Unit = {}
) {
    val rootOptions = remember {
        buildList {
            Environment.getExternalStorageDirectory()?.let { add(it) }
            System.getenv("SECONDARY_STORAGE")
                ?.split(":")
                ?.map { File(it) }
                ?.filter { it.exists() }
                ?.let { addAll(it) }
        }
    }

    var currentDir by remember { mutableStateOf(rootOptions.firstOrNull() ?: File("/")) }

    val entries = remember(currentDir) {
        currentDir.listFiles()
            ?.filter { it.isDirectory && !it.isHidden && it.canRead() }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    val backgroundColor = MaterialTheme.colorScheme.surface
    val (content, action) = remember { FocusRequester.createRefs() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        BackHandler {
            val parentDir = currentDir.parentFile?.takeIf { it.canRead() }
            if (parentDir != null) {
                currentDir = parentDir
                println("parentDir: $parentDir")
            }
            else {
                setShowDialog(false)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val parentDir = currentDir.parentFile?.takeIf { it.canRead() }
            if (parentDir != null) {
                ListItem(
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(content),
                    headlineContent = {
                        Text(
                            text = "..",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go up"
                        )
                    },
                    onClick = {
                        currentDir = parentDir
                        content.requestFocus()
                    }
                )
            }

            LazyColumn (
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(content)
                    .focusRestorer(content)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.absolutePath }) { dir ->
                    ListItem(
                        selected = false,
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = {
                            Text(
                                text = dir.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.s_m_local_filled),
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            currentDir = dir
                            content.requestFocus()
                        }
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListItem(
                    selected = false,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .focusRequester(action),
                    headlineContent = {
                        Text(stringResource(R.string.action_add))
                    },
                    supportingContent = {
                        Text(
                            text = currentDir.canonicalPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            val provider = LocalMediaProvider(
                                LocalProviderData("")
                            ).apply {
                                data = MediaProviderData(listOf(Pair(MusicFolder(currentDir.canonicalPath, currentDir.canonicalPath), true)))
                            }

                            provider.init(context)

                            try {
                                MediaProviderManager.addProvider(provider)
                                setShowDialog(false)
                            } catch (ex: Exception) {
                                println(ex.message)
                                println(ex.stackTrace)
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        content.requestFocus()
    }
    LaunchedEffect(entries) {
        if (entries.isEmpty())
            action.requestFocus()
        else
            content.requestFocus()
    }
}