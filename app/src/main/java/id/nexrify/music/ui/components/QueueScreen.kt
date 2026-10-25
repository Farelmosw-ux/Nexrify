package id.nexrify.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

private const val QUEUE_HEADER_KEY = "__queue_header__"

/**
 * Smart Queue: daftar antrean lengkap.
 * - Ketuk lagu -> putar langsung
 * - Tahan & seret ikon ≡ -> ubah urutan (drag-and-drop)
 * - Ikon ✕ -> hapus dari antrean
 * - Ikon playlist di kanan atas -> simpan antrean sebagai playlist
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.queueIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("Antrean Saya") }

    // State drag-and-drop
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var dragStartOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (currentIndex > 0) listState.scrollToItem(currentIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures { } } // cegah tap tembus ke layar di bawah
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tutup antrean",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Antrean",
                    style = Typography.headlineMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "${queue.size} lagu",
                    style = Typography.bodySmall,
                    color = colors.textMuted
                )
            }
            IconButton(
                onClick = { showSaveDialog = true },
                enabled = queue.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = "Simpan antrean sebagai playlist",
                    tint = if (queue.isNotEmpty()) colors.textPrimary else colors.textMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Antrean kosong",
                    style = Typography.bodyMedium,
                    color = colors.textMuted
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = QUEUE_HEADER_KEY) {
                    Text(
                        text = "Tahan dan seret ≡ untuk mengatur urutan",
                        style = Typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                itemsIndexed(queue, key = { _, t -> t.id }) { index, track ->
                    val isDragging = draggedKey == track.id
                    val isCurrent = index == currentIndex
                    val isPast = index < currentIndex

                    QueueRow(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        isPast = isPast,
                        isDragging = isDragging,
                        modifier = Modifier
                            .then(if (isDragging) Modifier else Modifier.animateItemPlacement())
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    val layoutOffset = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == track.id }?.offset?.toFloat()
                                        ?: dragStartOffset
                                    translationY = dragStartOffset + dragTotal - layoutOffset
                                }
                            },
                        onClick = { viewModel.playQueueItem(track.id) },
                        onRemove = { viewModel.removeFromQueue(track.id) },
                        dragHandleModifier = Modifier.pointerInput(track.id) {
                            detectDragGestures(
                                onDragStart = {
                                    val info = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == track.id }
                                    dragStartOffset = info?.offset?.toFloat() ?: 0f
                                    dragTotal = 0f
                                    draggedKey = track.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragTotal += amount.y
                                    handleQueueDrag(
                                        listState = listState,
                                        draggedId = track.id,
                                        dragStartOffset = dragStartOffset,
                                        dragTotal = dragTotal,
                                        viewModel = viewModel
                                    )
                                },
                                onDragEnd = {
                                    draggedKey = null
                                    dragTotal = 0f
                                },
                                onDragCancel = {
                                    draggedKey = null
                                    dragTotal = 0f
                                }
                            )
                        }
                    )
                }

                item(key = "__queue_footer__") {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = colors.card,
            title = {
                Text(
                    text = "Simpan antrean sebagai playlist",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    label = { Text("Nama playlist") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveQueueAsPlaylist(playlistName)
                    showSaveDialog = false
                }) {
                    Text("Simpan", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Batal", color = colors.textMuted)
                }
            }
        )
    }
}

/**
 * Tukar posisi lagu yang sedang diseret dengan lagu yang tertimpa titik tengahnya.
 * Penjaga arah mencegah tukar-balik ganda saat layout belum sempat diperbarui.
 */
private fun handleQueueDrag(
    listState: LazyListState,
    draggedId: String,
    dragStartOffset: Float,
    dragTotal: Float,
    viewModel: MusicViewModel
) {
    val visible = listState.layoutInfo.visibleItemsInfo
    val dragged = visible.firstOrNull { it.key == draggedId } ?: return

    // Auto-scroll ketika mendekati tepi layar
    val top = dragStartOffset + dragTotal
    val bottom = top + dragged.size
    val viewportStart = listState.layoutInfo.viewportStartOffset
    val viewportEnd = listState.layoutInfo.viewportEndOffset
    val edge = 56f
    when {
        bottom > viewportEnd - edge -> listState.dispatchRawDelta(18f)
        top < viewportStart + edge -> listState.dispatchRawDelta(-18f)
    }

    val center = top + dragged.size / 2f
    val target = visible.firstOrNull {
        val key = it.key
        key is String && key != draggedId && key != QUEUE_HEADER_KEY && key != "__queue_footer__" &&
            center >= it.offset && center <= it.offset + it.size
    } ?: return

    val ids = viewModel.queue.value.map { it.id }
    val vmFrom = ids.indexOf(draggedId)
    val vmTo = ids.indexOf(target.key as String)
    if (vmFrom == -1 || vmTo == -1) return

    val targetIsBelowInLayout = target.offset > dragged.offset
    if (targetIsBelowInLayout == (vmTo > vmFrom)) {
        viewModel.moveQueueItem(draggedId, target.key as String)
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isPast: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val rowBackground = when {
        isDragging -> colors.card
        isCurrent -> colors.card
        else -> colors.background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(12.dp)) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .alpha(if (isPast && !isDragging) 0.55f else 1f)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = track.highResThumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) EqualizerAnimation()
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = Typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isCurrent) "Sedang diputar • ${track.artist}" else track.artist,
                style = Typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isCurrent) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus dari antrean",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .then(dragHandleModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Seret untuk mengubah urutan",
                tint = colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
