package id.nexrify.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

private val SpotifyGreen = Color(0xFF1DB954)
private val WarnAmber = Color(0xFFF5B945)
private val ErrorRed = Color(0xFFFCA5A5)

private enum class ImportStage { Idle, Running, Done }

/** Dialog import playlist Spotify: tempel link, progres hidup dengan sampul, lalu ringkasan hasil. */
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
    val stage = when {
        state.playlistId != null -> ImportStage.Done
        state.running -> ImportStage.Running
        else -> ImportStage.Idle
    }

    Dialog(
        onDismissRequest = { if (stage != ImportStage.Running) close() },
        properties = DialogProperties(
            dismissOnBackPress = stage != ImportStage.Running,
            dismissOnClickOutside = stage != ImportStage.Running
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(32.dp))
                .animateContentSize()
        ) {
            // Pita warna di atas kartu: hijau Spotify memudar ke warna kartu.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(SpotifyGreen, SpotifyGreen.copy(alpha = 0.15f))))
            )
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                AnimatedContent(
                    targetState = stage,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 12 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 12 })
                    },
                    label = "import-stage"
                ) { st ->
                    when (st) {
                        ImportStage.Idle -> IdleContent(
                            link = link,
                            onLink = { link = it },
                            error = state.error,
                            onPaste = { clipboard.getText()?.text?.let { link = it.trim() } },
                            onStart = { viewModel.importSpotifyPlaylist(link) },
                            onCancel = close
                        )
                        ImportStage.Running -> RunningContent(state = state, onCancel = close)
                        ImportStage.Done -> DoneContent(
                            state = state,
                            onClose = close,
                            onOpen = { val id = state.playlistId!!; close(); onOpenPlaylist(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyBadge(size: Int = 52) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SpotifyGreen, Color(0xFF0E7A37)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(NexIcons.Download, null, tint = Color.White, modifier = Modifier.size((size * 0.46f).dp))
    }
}

@Composable
private fun IdleContent(
    link: String,
    onLink: (String) -> Unit,
    error: String?,
    onPaste: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpotifyBadge()
            Spacer(Modifier.width(14.dp))
            Column {
                Text(S.importSpotify, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(S.importSub, style = Typography.bodySmall, color = colors.textMuted)
            }
        }
        Spacer(Modifier.height(20.dp))

        // Langkah singkat.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StepRow(1, S.importStep1)
            StepRow(2, S.importStep2)
            StepRow(3, S.importStep3)
        }
        Spacer(Modifier.height(16.dp))

        // Kolom link dengan tombol tempel di dalamnya.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .border(
                    1.dp,
                    if (error != null) ErrorRed.copy(alpha = 0.7f) else colors.border,
                    RoundedCornerShape(18.dp)
                )
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (link.isEmpty()) {
                    Text(S.importSpotifyPlaceholder, style = Typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicTextField(
                    value = link,
                    onValueChange = onLink,
                    singleLine = true,
                    textStyle = Typography.bodyMedium.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.border.copy(alpha = 0.7f))
                    .clickable { onPaste() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(NexIcons.Copy, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Text(S.importPaste, style = Typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ErrorRed.copy(alpha = 0.12f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(NexIcons.Info, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(error, style = Typography.bodySmall, color = ErrorRed)
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(S.importPublicOnly, style = Typography.bodySmall, color = colors.textMuted, modifier = Modifier.padding(start = 4.dp))
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border)
            ) { Text(S.cancel, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = onStart,
                enabled = link.isNotBlank(),
                modifier = Modifier.weight(1.3f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = colors.card,
                    disabledContentColor = colors.textMuted
                )
            ) { Text(S.importStart, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun StepRow(n: Int, text: String) {
    val colors = NexrifyAppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(SpotifyGreen.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Text("$n", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = Typography.bodySmall, color = colors.textSecondary)
    }
}

/** Sampul persegi dengan sudut bulat; kosong -> kotak berikon musik. */
@Composable
private fun CoverBox(url: String, size: Int, radius: Int = 20) {
    val colors = NexrifyAppTheme.colors
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(radius.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(size.dp), contentScale = ContentScale.Crop)
        } else {
            Icon(NexIcons.QueueMusic, null, tint = colors.textMuted, modifier = Modifier.size((size * 0.42f).dp))
        }
    }
}

@Composable
private fun RunningContent(state: MusicViewModel.SpotifyImportState, onCancel: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val target = if (state.total > 0) state.done.toFloat() / state.total else 0f
    val progress by animateFloatAsState(target, tween(350), label = "import-progress")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CoverBox(url = state.coverUrl, size = 132, radius = 26)
        Spacer(Modifier.height(16.dp))
        Text(
            state.sourceName.ifBlank { S.importPreparing },
            style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.total > 0) S.importProgress(state.done, state.total) else S.importPreparing,
            style = Typography.bodySmall, color = colors.textMuted
        )
        Spacer(Modifier.height(18.dp))

        // Batang progres tebal dengan persen di kanan.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.card)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(SpotifyGreen, Color(0xFF7CF0A6))))
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("${(progress * 100).toInt()}%", style = Typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        }
        Spacer(Modifier.height(14.dp))

        // Lagu yang sedang dicocokkan + penghitung hidup.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                if (state.retrying) S.importRetrying else S.importMatched + ": ${state.matchedSoFar}",
                style = Typography.labelMedium, color = if (state.retrying) WarnAmber else SpotifyGreen, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                state.current.ifBlank { "…" },
                style = Typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, colors.border)
        ) { Text(S.cancel, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun StatTile(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    val colors = NexrifyAppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Text("$value", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = tint)
        Text(label, style = Typography.labelMedium, color = colors.textSecondary)
    }
}

@Composable
private fun DoneContent(state: MusicViewModel.SpotifyImportState, onClose: () -> Unit, onOpen: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.BottomEnd) {
            CoverBox(url = state.coverUrl, size = 120, radius = 26)
            Box(
                modifier = Modifier.padding(6.dp).size(32.dp).clip(CircleShape).background(SpotifyGreen),
                contentAlignment = Alignment.Center
            ) { Icon(NexIcons.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            state.playlistName ?: state.sourceName,
            style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(state.matchedCount, S.importMatched, SpotifyGreen, Modifier.weight(1f))
            StatTile(state.notFound.size, S.importMissing, if (state.notFound.isEmpty()) colors.textMuted else WarnAmber, Modifier.weight(1f))
        }
        if (state.notFound.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                S.importMissingHint,
                style = Typography.bodySmall, color = colors.textMuted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                state.notFound.forEach {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(WarnAmber))
                        Spacer(Modifier.width(10.dp))
                        Text(it, style = Typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border)
            ) { Text(S.close, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1.4f).height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black)
            ) { Text(S.importOpen, fontWeight = FontWeight.ExtraBold) }
        }
    }
}
