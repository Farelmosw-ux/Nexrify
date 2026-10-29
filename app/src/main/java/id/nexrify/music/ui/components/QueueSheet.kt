package id.nexrify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import id.nexrify.music.data.model.Track
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

/**
 * Antrian lagu: lihat urutan putar, ketuk untuk loncat ke lagu, naikkan atau turunkan urutan,
 * hapus satu lagu, atau bersihkan semua lagu berikutnya. Lagu yang sedang diputar tidak bisa dihapus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.queueIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

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
                .fillMaxHeight(0.78f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(S.queueTitle, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(S.queueCount(queue.size), style = Typography.bodySmall, color = colors.textMuted)
                }
                if (currentIndex in 0 until queue.size - 1) {
                    TextButton(onClick = { viewModel.clearUpcoming() }) {
                        Icon(NexIcons.DeleteOutline, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(S.clearUpcoming, color = colors.textMuted, fontSize = 13.sp)
                    }
                }
            }

            if (queue.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(NexIcons.QueueMusic, null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(S.queueEmpty, style = Typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(S.queueEmptyHint, style = Typography.bodySmall, color = colors.textMuted, textAlign = TextAlign.Center)
                }
            } else {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (currentIndex - 1).coerceIn(0, (queue.size - 1).coerceAtLeast(0))
                )
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(queue) { index, track ->
                        QueueRow(
                            track = track,
                            index = index,
                            lastIndex = queue.lastIndex,
                            isCurrent = index == currentIndex,
                            isPlayed = currentIndex >= 0 && index < currentIndex,
                            isPlaying = isPlaying,
                            label = when {
                                index == currentIndex -> S.queueNowPlaying
                                index == currentIndex + 1 -> S.queueNext
                                else -> null
                            },
                            onPlay = { viewModel.playFromQueue(index) },
                            onUp = { viewModel.moveInQueue(index, index - 1) },
                            onDown = { viewModel.moveInQueue(index, index + 1) },
                            onRemove = { viewModel.removeFromQueue(index) }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: Track,
    index: Int,
    lastIndex: Int,
    isCurrent: Boolean,
    isPlayed: Boolean,
    isPlaying: Boolean,
    label: String?,
    onPlay: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = Typography.labelSmall,
                fontSize = 11.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isCurrent) Modifier.background(colors.card).border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .clickable(onClick = onPlay)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = track.highResThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.background.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NexIcons.GraphicEq,
                            contentDescription = S.cd("Playing"),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = Typography.titleMedium,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPlayed) colors.textMuted else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = Typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.size(34.dp)) {
                Icon(
                    NexIcons.KeyboardArrowUp, S.moveUp,
                    tint = if (index > 0) colors.textSecondary else colors.border,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDown, enabled = index < lastIndex, modifier = Modifier.size(34.dp)) {
                Icon(
                    NexIcons.KeyboardArrowDown, S.moveDown,
                    tint = if (index < lastIndex) colors.textSecondary else colors.border,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isCurrent) {
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(NexIcons.Close, S.removeFromQueue, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            } else {
                Spacer(Modifier.width(34.dp))
            }
        }
    }
}
