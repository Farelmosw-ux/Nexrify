package id.nexrify.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

/** Dialog import playlist Spotify: tempel link, progres pencocokan, hasil. */
@Composable
fun SpotifyImportDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val state by viewModel.spotifyImport.collectAsState()
    var link by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val close = {
        if (state.running) viewModel.cancelSpotifyImport()
        viewModel.resetSpotifyImport()
        onDismiss()
    }
    val finished = state.playlistId != null

    AlertDialog(
        onDismissRequest = { if (!state.running) close() },
        title = { Text(S.importSpotify, style = Typography.titleLarge, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    finished -> {
                        Text(
                            S.importDone(state.matchedCount, state.notFound.size),
                            style = Typography.bodyMedium, color = colors.textPrimary
                        )
                        if (state.notFound.isNotEmpty()) {
                            Text(S.importNotFoundTitle, style = Typography.labelLarge, color = colors.textMuted)
                            Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                                state.notFound.forEach {
                                    Text("• $it", style = Typography.bodySmall, color = colors.textMuted)
                                }
                            }
                        }
                    }
                    state.running -> {
                        val p = if (state.total > 0) state.done.toFloat() / state.total else 0f
                        if (state.total > 0) LinearProgressIndicator(
                            progress = { p }, modifier = Modifier.fillMaxWidth(), color = colors.accent
                        ) else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.accent)
                        Text(
                            if (state.total > 0) S.importProgress(state.done, state.total) else "…",
                            style = Typography.bodySmall, color = colors.textMuted
                        )
                    }
                    else -> {
                        Text(S.importSpotifyHint, style = Typography.bodySmall, color = colors.textMuted)
                        OutlinedTextField(
                            value = link,
                            onValueChange = { link = it },
                            placeholder = { Text(S.importSpotifyPlaceholder, color = colors.textMuted) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colors.card,
                                unfocusedContainerColor = colors.card,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.textPrimary,
                                focusedIndicatorColor = colors.accent,
                                unfocusedIndicatorColor = colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row {
                            TextButton(onClick = {
                                clipboard.getText()?.text?.let { link = it.trim() }
                            }) { Text(S.importPaste, color = colors.accent) }
                        }
                        state.error?.let {
                            Text(it, style = Typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFFE5484D))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            if (finished) {
                Button(
                    onClick = { val id = state.playlistId!!; close(); onOpenPlaylist(id) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) { Text(S.importOpen) }
            } else if (!state.running) {
                Button(
                    onClick = { viewModel.importSpotifyPlaylist(link) },
                    enabled = link.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) { Text(S.importStart) }
            }
        },
        dismissButton = {
            TextButton(onClick = { close() }) { Text(if (finished) S.close else S.cancel, color = colors.textMuted) }
        }
    )
}
