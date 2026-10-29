package id.nexrify.music.ui.components

import id.nexrify.music.i18n.S

import id.nexrify.music.ui.icons.NexIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    track: Track,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val colors = NexrifyAppTheme.colors
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Track Header Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.highResThumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = Typography.bodyMedium,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Aksi antrian
            val queueContext = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QueueActionTile(
                    icon = NexIcons.PlayArrow,
                    text = S.playNextAction,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.playNext(track)
                        android.widget.Toast.makeText(queueContext, id.nexrify.music.i18n.currentStrings().toastPlayNext, android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
                QueueActionTile(
                    icon = NexIcons.QueueMusic,
                    text = S.addToQueueAction,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.addToQueue(track)
                        android.widget.Toast.makeText(queueContext, id.nexrify.music.i18n.currentStrings().toastQueued, android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Create New Playlist Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NexIcons.Add,
                        contentDescription = S.cd("New"),
                        tint = colors.background,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = S.newPlaylist,
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = S.choosePlaylist,
                style = Typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = S.noPlaylistCreateFirst,
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val alreadyInPlaylist = playlist.tracks.any { it.id == track.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (alreadyInPlaylist) colors.card.copy(alpha = 0.5f) else colors.card)
                                .clickable {
                                    if (!alreadyInPlaylist) {
                                        viewModel.addTrackToPlaylist(playlist.id, track)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.border),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NexIcons.QueueMusic,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = Typography.titleMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.tracks.size} lagu",
                                    style = Typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            if (alreadyInPlaylist) {
                                Icon(
                                    imageVector = NexIcons.Check,
                                    contentDescription = S.cd("Added"),
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = S.namePlaylist,
                    style = Typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text(S.playlistNameHint, color = colors.textMuted) },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val created = viewModel.createPlaylist(newPlaylistName.trim())
                            viewModel.addTrackToPlaylist(created.id, track)
                            showCreateDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    )
                ) {
                    Text(S.createAndAdd)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(S.cancel, color = colors.textMuted)
                }
            },
            containerColor = colors.background
        )
    }
}

@Composable
private fun QueueActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
