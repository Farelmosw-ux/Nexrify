package id.nexrify.music.ui.components

import id.nexrify.music.i18n.S

import id.nexrify.music.ui.icons.NexIcons

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.nexrify.music.R
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun FullPlayerSheet(
    track: Track,
    viewModel: MusicViewModel,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    isShuffle: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val audioDeviceState by viewModel.audioDeviceState.collectAsState()

    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isLyricsExpanded by remember { mutableStateOf(false) }

    val colors = NexrifyAppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
    ) {
        // -------------------------------------------------------------
        // TOP SECTION: Full-Bleed Edge-to-Edge Artwork Header (Design #2)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            // Ultra-HD Master Artwork
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.highResThumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Scrim Gradient Overlay: seamlessly blends into background at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                colors.background.copy(alpha = 0.35f),
                                colors.background.copy(alpha = 0.85f),
                                colors.background
                            )
                        )
                    )
            )

            // Overlaid Top Bar (Collapse Chevron, Context Title, More Options)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = NexIcons.KeyboardArrowDown,
                        contentDescription = S.cd("Collapse Player"),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = S.nowPlaying,
                    style = Typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )

                Box {
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = NexIcons.MoreVert,
                            contentDescription = S.cd("Options"),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(colors.card)
                    ) {
                        DropdownMenuItem(
                            text = { Text(S.addToPlaylist, color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(NexIcons.QueueMusic, contentDescription = null, tint = colors.textPrimary)
                            },
                            onClick = {
                                showOptionsMenu = false
                                showAddToPlaylist = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(S.settingsAudio, color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(NexIcons.Tune, contentDescription = null, tint = colors.textPrimary)
                            },
                            onClick = {
                                showOptionsMenu = false
                                showSettingsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(S.shareSong, color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(NexIcons.Share, contentDescription = null, tint = colors.textPrimary)
                            },
                            onClick = {
                                showOptionsMenu = false
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, id.nexrify.music.i18n.currentStrings().shareText(track.title, track.artist, track.url))
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, id.nexrify.music.i18n.currentStrings().shareVia))
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // MAIN CONTROLS SECTION (Padding 24.dp)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Track Title, Artist, and Vibrant Red Favorite Heart (Design #2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = Typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = Typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Heart: Vibrant Red (#FF3B30) when liked as in Design #2
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) NexIcons.Favorite else NexIcons.FavoriteBorder,
                        contentDescription = S.cd("Favorite"),
                        tint = if (track.isFavorite) Color(0xFFFF3B30) else colors.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scrubber Bar
            PlayerScrubber(
                viewModel = viewModel,
                track = track
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Playback Controls: Shuffle, Rewind, Play/Pause, Fast-Forward, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = NexIcons.Shuffle,
                        contentDescription = S.cd("Shuffle"),
                        tint = if (isShuffle) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onSkipPreviousClick,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nexrify_rewind),
                        contentDescription = S.cd("Previous Track"),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Big Solid Center Play/Pause Circle (72dp)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.textPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = colors.background,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                painter = if (isPlaying) painterResource(R.drawable.ic_nexrify_pause) else painterResource(R.drawable.ic_nexrify_play),
                                contentDescription = if (isPlaying) S.cd("Pause") else S.cd("Play"),
                                tint = colors.background,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSkipNextClick,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nexrify_fast_forward),
                        contentDescription = S.cd("Next Track"),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onRepeatClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    val icon = if (repeatMode == Player.REPEAT_MODE_ONE) NexIcons.RepeatOne else NexIcons.Repeat
                    val tint = if (repeatMode != Player.REPEAT_MODE_OFF) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
                    Icon(
                        imageVector = icon,
                        contentDescription = S.cd("Repeat"),
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -------------------------------------------------------------
            // SECONDARY UTILITIES ROW (Design #2 Component Breakdown):
            // 1. Audio Output Device Pill ("Wired Connection" / BT)
            // 2. Lyrics Quick-Jump / Expand
            // 3. Add to Playlist
            // 4. Equalizer / Settings
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Device Output Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                        .clickable { viewModel.openAudioOutputSettings() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (audioDeviceState.iconType) {
                        id.nexrify.music.data.audio.OutputType.WIRED -> NexIcons.Headphones
                        id.nexrify.music.data.audio.OutputType.BLUETOOTH -> NexIcons.BluetoothAudio
                        id.nexrify.music.data.audio.OutputType.SPEAKER -> NexIcons.VolumeUp
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = S.cd("Audio Output"),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = S.deviceLabel(audioDeviceState.title),
                        style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Lyrics Toggle / Scroll
                    IconButton(
                        onClick = {
                            isLyricsExpanded = !isLyricsExpanded
                            if (isLyricsExpanded) {
                                scope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isLyricsExpanded) colors.accent else colors.card)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = NexIcons.ChatBubbleOutline,
                            contentDescription = S.cd("Lyrics"),
                            tint = if (isLyricsExpanded) colors.background else colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Antrian lagu
                    IconButton(
                        onClick = { showQueue = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = NexIcons.QueueMusic,
                            contentDescription = S.cd("Queue"),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings & Equalizer
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = NexIcons.Tune,
                            contentDescription = S.cd("Settings & Equalizer"),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------------------
            // DESIGN #2 LYRICS CARD / DRAWER (Bottom)
            // -------------------------------------------------------------
            val hasLyrics = currentLyrics != null && currentLyrics!!.lines.isNotEmpty() &&
                    currentLyrics!!.lines.none { it.text.trim().equals("null", ignoreCase = true) }

            if (hasLyrics) {
                val lyricsHeight = if (isLyricsExpanded) 380.dp else 160.dp
                SyncedLyricsView(
                    lyricsData = currentLyrics,
                    isLoading = isLoadingLyrics,
                    playbackPositionFlow = viewModel.playbackPosition,
                    onSeekTo = { viewModel.seekTo(it) },
                    isExpanded = isLyricsExpanded,
                    onExpandToggle = {
                        isLyricsExpanded = !isLyricsExpanded
                        if (isLyricsExpanded) {
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lyricsHeight)
                )
                Spacer(modifier = Modifier.height(36.dp))
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            track = track,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    if (showQueue) {
        QueueSheet(viewModel = viewModel, onDismiss = { showQueue = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun PlayerScrubber(
    viewModel: MusicViewModel,
    track: Track,
    modifier: Modifier = Modifier
) {
    val currentPositionMs by viewModel.playbackPosition.collectAsState()
    val durationMs by viewModel.duration.collectAsState()

    val effectiveDuration = if (durationMs > 0) durationMs else (track.durationSeconds * 1000L).coerceAtLeast(1L)
    val progress = if (effectiveDuration > 0) {
        (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val currentFormatted = formatMs(currentPositionMs)
    val totalFormatted = formatMs(effectiveDuration)

    val colors = NexrifyAppTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                viewModel.seekTo((newProgress * effectiveDuration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.textPrimary,
                activeTrackColor = colors.textPrimary,
                inactiveTrackColor = colors.border
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = currentFormatted, style = Typography.bodyMedium, color = colors.textSecondary)
            Text(text = totalFormatted, style = Typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
