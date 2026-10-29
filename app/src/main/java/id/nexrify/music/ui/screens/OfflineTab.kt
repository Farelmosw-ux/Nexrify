package id.nexrify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.data.auth.AuthManager
import id.nexrify.music.data.download.DownloadItem
import id.nexrify.music.data.download.DownloadState
import id.nexrify.music.data.download.OfflineTrack
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel
import java.util.Locale

/** Tab Offline di Koleksi: unduhan berjalan, lalu daftar lagu offline dengan putar, acak, dan hapus. */
@Composable
fun OfflineTab(viewModel: MusicViewModel) {
    val colors = NexrifyAppTheme.colors
    val offlineTracks by viewModel.offlineTracks.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val account by AuthManager.getInstance().authState.collectAsState()
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val tracks = offlineTracks.sortedByDescending { it.downloadedAt }
    val queueTracks = tracks.map { it.track }
    val totalBytes = offlineTracks.sumOf { it.sizeBytes }

    if (tracks.isEmpty() && downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(NexIcons.Download, null, tint = colors.textSecondary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(16.dp))
                Text(S.offlineEmpty, style = Typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(S.offlineEmptyHint, style = Typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (account == null) S.offlineGuestNote else S.offlineAccountNote,
                    style = Typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (tracks.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = S.offlineSummary(tracks.size, formatSize(totalBytes)),
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PillButton(text = S.offlinePlayAll, filled = true) {
                            viewModel.playTrack(queueTracks.first(), queueTracks)
                        }
                        PillButton(text = S.offlineShuffle, filled = false) {
                            val shuffled = queueTracks.shuffled()
                            viewModel.playTrack(shuffled.first(), shuffled)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { confirmDeleteAll = true }) {
                            Icon(NexIcons.DeleteOutline, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(S.offlineDeleteAll, color = colors.textMuted, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Unduhan yang sedang berjalan, antre, atau gagal
        val active = downloads.values.toList()
        if (active.isNotEmpty()) {
            items(active, key = { "dl_" + it.track.id }) { item ->
                DownloadRow(
                    item = item,
                    onCancel = { viewModel.cancelDownload(item.track.id) },
                    onRetry = { viewModel.retryDownload(item.track.id) }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        items(tracks, key = { it.track.id }, contentType = { "offline" }) { entry ->
            OfflineRow(
                entry = entry,
                thumb = viewModel.offlineThumb(entry.track.id),
                isCurrent = currentTrack?.id == entry.track.id,
                isLoading = isLoadingTrack && currentTrack?.id == entry.track.id,
                onPlay = { viewModel.playTrack(entry.track, queueTracks) },
                onDelete = { viewModel.deleteOffline(entry.track.id) }
            )
        }

        item {
            Text(
                text = if (account == null) S.offlineGuestNote else S.offlineAccountNote,
                style = Typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(90.dp))
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = colors.surface,
            title = { Text(S.offlineDeleteAllTitle, color = colors.textPrimary, style = Typography.titleLarge) },
            text = { Text(S.offlineDeleteAllBody, color = colors.textMuted, style = Typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllOffline()
                    confirmDeleteAll = false
                }) { Text(S.delete, color = colors.textPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text(S.cancel, color = colors.textMuted) }
            }
        )
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (filled) colors.accent else colors.card)
            .border(1.dp, if (filled) colors.accent else colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (filled) colors.background else colors.textPrimary
        )
    }
}

@Composable
private fun OfflineRow(
    entry: OfflineTrack,
    thumb: java.io.File?,
    isCurrent: Boolean,
    isLoading: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isCurrent) Modifier.background(colors.card) else Modifier)
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = thumb ?: entry.track.highResThumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.track.title,
                style = Typography.titleMedium,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.track.artist + "  " + formatSize(entry.sizeBytes),
                style = Typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.width(28.dp).height(3.dp),
                color = colors.textPrimary,
                trackColor = colors.border
            )
            Spacer(Modifier.width(6.dp))
        } else if (isCurrent) {
            Icon(NexIcons.GraphicEq, S.cd("Playing"), tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
            Icon(NexIcons.DeleteOutline, S.offlineDeleteSong, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onCancel: () -> Unit, onRetry: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val state = item.state
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.track.title,
                    style = Typography.titleMedium,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (state) {
                        DownloadState.Queued -> S.offlineQueued
                        is DownloadState.Downloading -> S.downloadingTile((state.progress * 100f).toInt())
                        is DownloadState.Failed -> S.offlineFailed
                    },
                    style = Typography.bodySmall,
                    color = colors.textMuted
                )
            }
            if (state is DownloadState.Failed) {
                IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                    Icon(NexIcons.Refresh, S.offlineRetry, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                Icon(NexIcons.Close, S.offlineCancel, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        if (state is DownloadState.Downloading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = colors.textPrimary,
                trackColor = colors.border
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.2f GB", mb / 1024.0) else String.format(Locale.US, "%.1f MB", mb)
}
