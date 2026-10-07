package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.elements.dialogs.playlistToDelete
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel


@Composable
fun PlaylistDeletionConfirmationDialog(
    setShowDialog: (Boolean) -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Box(
                modifier = Modifier.focusable()
            ) {
                Text(
                    text = stringResource(R.string.delete_playlist_are_you_sure),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deletePlaylist(playlistToDelete.value)
                    setShowDialog(false)
                }
            ) {
                Text(
                    stringResource(R.string.playlist_delete_playlist)
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                setShowDialog(false)
            }) {
                Text(
                    stringResource(R.string.action_close)
                )
            }
        }
    )
}