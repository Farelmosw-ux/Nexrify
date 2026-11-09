package id.nexrify.music.ui.components

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.download.DownloadState
import id.nexrify.music.data.model.Track
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

/**
 * Deretan aksi cepat di pemutar penuh: playlist, unduh offline, timer tidur, kecepatan putar, bagikan.
 */
@Composable
fun PlayerQuickActions(
    track: Track,
    viewModel: MusicViewModel,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val offlineTracks by viewModel.offlineTracks.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val sleepSec by viewModel.sleepRemainingSec.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    var showSleep by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }

    val isDownloaded = offlineTracks.any { it.track.id == track.id }
    val dlState = downloads[track.id]?.state
    val dlBusy = dlState is DownloadState.Downloading || dlState == DownloadState.Queued

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionTile(
            icon = NexIcons.QueueMusic,
            label = S.actionPlaylist,
            modifier = Modifier.weight(1f),
            onClick = onAddToPlaylist
        )
        QuickActionTile(
            icon = if (isDownloaded) NexIcons.Check else NexIcons.Download,
            label = when {
                isDownloaded -> S.actionDownloaded
                dlState is DownloadState.Downloading -> "${(dlState.progress * 100f).toInt()}%"
                else -> S.actionDownload
            },
            active = isDownloaded || dlBusy,
            busy = dlBusy,
            modifier = Modifier.weight(1f),
            onClick = {
                val st = currentStrings()
                when {
                    isDownloaded -> {
                        viewModel.deleteOffline(track.id)
                        NexNotifier.info(st.toastDownloadRemoved, st.noteDownloadTitle)
                    }
                    dlBusy -> viewModel.cancelDownload(track.id)
                    else -> {
                        viewModel.downloadTrack(track)
                        NexNotifier.success(st.toastDownloadStarted, st.noteDownloadTitle)
                    }
                }
            }
        )
        QuickActionTile(
            icon = NexIcons.Timer,
            label = when {
                sleepSec == null -> S.actionSleep
                sleepSec == -1 -> S.actionSleep
                else -> S.sleepLeft(((sleepSec ?: 0) + 59) / 60)
            },
            active = sleepSec != null,
            modifier = Modifier.weight(1f),
            onClick = { showSleep = true }
        )
        QuickActionTile(
            icon = NexIcons.Speed,
            label = if (speed == 1f) S.actionSpeed else formatSpeed(speed),
            active = speed != 1f,
            modifier = Modifier.weight(1f),
            onClick = { showSpeed = true }
        )
        QuickActionTile(
            icon = NexIcons.Share,
            label = S.actionShare,
            modifier = Modifier.weight(1f),
            onClick = {
                runCatching {
                    val st = currentStrings()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, st.shareText(track.title, track.artist, track.url))
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(send, st.shareVia))
                }
            }
        )
    }

    if (showSleep) {
        SleepTimerSheet(
            activeSec = sleepSec,
            onPick = { minutes ->
                val st = currentStrings()
                viewModel.startSleepTimer(minutes)
                NexNotifier.success(st.sleepSet(minutes), st.noteSleepTitle)
                showSleep = false
            },
            onEndOfTrack = {
                val st = currentStrings()
                viewModel.startSleepUntilEndOfTrack()
                NexNotifier.success(st.sleepSetEndOfTrack, st.noteSleepTitle)
                showSleep = false
            },
            onOff = {
                val st = currentStrings()
                viewModel.cancelSleepTimer()
                NexNotifier.info(st.sleepCancelled, st.noteSleepTitle)
                showSleep = false
            },
            onDismiss = { showSleep = false }
        )
    }

    if (showSpeed) {
        SpeedSheet(
            current = speed,
            onPick = { x ->
                viewModel.setPlaybackSpeed(x)
                NexNotifier.success(currentStrings().speedSet(formatSpeed(x)), currentStrings().noteSpeedTitle)
                showSpeed = false
            },
            onDismiss = { showSpeed = false }
        )
    }
}

private fun formatSpeed(x: Float): String {
    val s = if (x % 1f == 0f) x.toInt().toString() else x.toString().trimEnd('0')
    return "${s}x"
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    busy: Boolean = false,
    onClick: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (active) colors.textPrimary else colors.card)
            .border(1.dp, if (active) colors.textPrimary else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(
                    color = colors.background,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    icon, null,
                    tint = if (active) colors.background else colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = if (active) colors.background else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    activeSec: Int?,
    onPick: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onOff: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        dragHandle = { SheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(S.sleepTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(S.sleepSub, style = Typography.bodySmall, color = colors.textMuted)
            Spacer(Modifier.height(14.dp))
            listOf(5, 10, 15, 30, 45, 60).forEach { m ->
                SheetOption(S.sleepMinutes(m), selected = false, onClick = { onPick(m) })
            }
            SheetOption(S.sleepEndOfTrack, selected = activeSec == -1, onClick = onEndOfTrack)
            if (activeSec != null) {
                Spacer(Modifier.height(6.dp))
                SheetOption(S.sleepOff, selected = false, danger = true, onClick = onOff)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(current: Float, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        dragHandle = { SheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(S.speedTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(14.dp))
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { x ->
                SheetOption(
                    text = if (x == 1f) "1x  ${S.speedNormal}" else formatSpeed(x),
                    selected = current == x,
                    onClick = { onPick(x) }
                )
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    val colors = NexrifyAppTheme.colors
    Box(
        Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border)
    )
}

@Composable
private fun SheetOption(text: String, selected: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(if (selected) colors.card else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) colors.textPrimary.copy(alpha = 0.5f) else colors.border.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = Typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (danger) androidx.compose.ui.graphics.Color(0xFFF87171) else colors.textPrimary
        )
        if (selected) Icon(NexIcons.Check, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
    }
}
