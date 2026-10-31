package id.nexrify.music.ui.screens

import androidx.compose.ui.graphics.Color

import id.nexrify.music.ui.components.NexFooter

import id.nexrify.music.ui.components.NexLoadingFill

import id.nexrify.music.i18n.S

import id.nexrify.music.ui.icons.NexIcons

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import id.nexrify.music.R
import id.nexrify.music.data.auth.UserAccount
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.model.HomeSection
import id.nexrify.music.ui.components.AddToPlaylistDialog
import id.nexrify.music.ui.components.LoginWebViewDialog
import id.nexrify.music.ui.components.ProfileMenuSheet
import id.nexrify.music.ui.components.TrackItem
import id.nexrify.music.ui.theme.DarkBorder
import id.nexrify.music.ui.theme.DarkCard
import id.nexrify.music.ui.theme.MutedText
import id.nexrify.music.ui.theme.PureBlack
import id.nexrify.music.ui.theme.PureWhite
import id.nexrify.music.ui.theme.SubtleGray
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onOpenMailbox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val homeSections by viewModel.homeSections.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentAccount by viewModel.authState.collectAsState()
    val currentLanguage by viewModel.settingsManager.language.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favoriteIds by remember(favorites) { androidx.compose.runtime.derivedStateOf { favorites.mapTo(HashSet()) { it.id } } }

    var showProfileMenu by remember { mutableStateOf(false) }
    var showLoginWebView by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }

    var selectedGenre by remember { mutableStateOf("Semua") }
    val genres = listOf("Semua", "Pop", "Lo-Fi", "Rock", "Acoustic", "Anime", "Hip-Hop", "R&B", "Top Global")

    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    var localNow by remember { mutableStateOf(java.time.ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            localNow = java.time.ZonedDateTime.now()
            delay(30_000L)
        }
    }

    // Maghrib dihitung dari lokasi pengguna dan zona waktu perangkat, tidak pernah dari jam tetap.
    val maghribManager = remember { id.nexrify.music.data.prayer.MaghribManager.getInstance() }
    val maghribPlace by maghribManager.place.collectAsState()
    val maghribStatus by maghribManager.status.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            maghribManager.refresh()
            delay(15 * 60_000L)
        }
    }
    val maghribTime = remember(maghribPlace, localNow.toLocalDate(), localNow.zone) {
        maghribManager.maghribFor(localNow.toLocalDate(), localNow.zone)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 16.dp)
    ) {
        // Greeting mengikuti jam lokal perangkat (zona waktu wilayah otomatis).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeContext = id.nexrify.music.data.settings.TimeContext.resolve(localNow, maghribTime)
            val greeting = S.period(timeContext.period)
            val description = if (S.english) timeContext.descriptionEn else timeContext.descriptionId
            Column(modifier = Modifier.weight(1f)) {
                Text(greeting, style = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = Typography.bodyLarge, color = colors.textMuted)
                if (maghribStatus == id.nexrify.music.data.prayer.MaghribManager.Status.NO_PERMISSION &&
                    localNow.hour in 16..18
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(S.maghribHomeHint, style = Typography.bodySmall, color = colors.textSecondary)
                }
            }
            val unreadMail by id.nexrify.music.data.mailbox.MailboxManager.getInstance().unreadCount.collectAsState()
            MailboxTopButton(unread = unreadMail, onClick = onOpenMailbox)
            Spacer(modifier = Modifier.width(10.dp))
            AccountTopButton(account = currentAccount, onClick = { showProfileMenu = true })
        }

        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) colors.textPrimary else colors.card)
                        .border(1.dp, if (isSelected) colors.textPrimary else colors.border, RoundedCornerShape(20.dp))
                        .clickable {
                            selectedGenre = genre
                            if (genre == "Semua") viewModel.loadTrending() else viewModel.search("$genre music hits")
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = S.genreLabel(genre), style = Typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                        fontSize = 13.sp, color = if (isSelected) colors.background else colors.textPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (selectedGenre != "Semua") {
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NexLoadingFill(active = true)
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = S.noSongsFor(S.genreLabel(selectedGenre)),
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults, key = { it.id }, contentType = { "track" }) { track ->
                        val isFav = track.id in favoriteIds
                        TrackItem(
                            track = track.copy(isFavorite = isFav),
                            isPlaying = currentTrack?.id == track.id,
                            isLoading = isLoadingTrack && currentTrack?.id == track.id,
                            onClick = { viewModel.playTrackFromSearch(track) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) },
                            onMoreClick = { trackForPlaylist = track }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        } else if (isLoadingTrending) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(5) { SkeletonMusicRow() }
            }
        } else if (homeSections.isNotEmpty()) {
            // Putar lagi: dari riwayat, atau dari rak pertama kalau riwayat masih kosong.
            val playAgainSource = remember(recentlyPlayed, homeSections) {
                (if (recentlyPlayed.isNotEmpty()) recentlyPlayed else homeSections.firstOrNull()?.tracks.orEmpty())
                    .distinctBy { it.id }
            }
            var playAgainExpanded by remember { mutableStateOf(false) }
            val pickSection = remember(homeSections) { homeSections.firstOrNull { it.tracks.isNotEmpty() } }
            val pickTrack = pickSection?.tracks?.firstOrNull()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (playAgainSource.isNotEmpty()) {
                    item(key = "play_again_header") {
                        HomeSectionHeader(
                            title = S.playAgain,
                            action = if (playAgainSource.size > 4) (if (playAgainExpanded) S.showLess else S.viewAll) else null,
                            onAction = { playAgainExpanded = !playAgainExpanded }
                        )
                    }
                    item(key = "play_again_grid") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playAgainSource.take(if (playAgainExpanded) 12 else 4).chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    pair.forEach { track ->
                                        PlayAgainTile(
                                            track = track,
                                            isPlaying = currentTrack?.id == track.id,
                                            modifier = Modifier.weight(1f),
                                            onClick = { viewModel.playTrack(track, playAgainSource) },
                                            onLongClick = { trackForPlaylist = track }
                                        )
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }

                if (pickTrack != null && pickSection != null) {
                    item(key = "picks_header") { HomeSectionHeader(title = S.picksForYou) }
                    item(key = "picks_banner") {
                        PickBanner(
                            track = pickTrack,
                            onPlay = { viewModel.playTrack(pickTrack, pickSection.tracks) },
                            onLongClick = { trackForPlaylist = pickTrack }
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }

                if (favorites.isNotEmpty()) {
                    item(key = "favorites_header") { HomeSectionHeader(title = S.favoriteSongs) }
                    item(key = "favorites_tracks") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(favorites.take(12), key = { index, track -> "favorite_${track.id}_$index" }) { _, track ->
                                YtmMusicCard(
                                    track = track,
                                    isPlaying = currentTrack?.id == track.id,
                                    onClick = { viewModel.playTrack(track, favorites) },
                                    onLongClick = { trackForPlaylist = track }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }

                homeSections.forEachIndexed { sIndex, section ->
                    item(key = "header_${sIndex}_${section.title}") {
                        HomeSectionHeader(
                            title = S.sectionTitle(section.title),
                            subtitle = section.subtitle.takeIf { it.isNotBlank() }?.let { S.sectionSubtitle(it) }
                        )
                    }
                    item(key = "shelf_${sIndex}_${section.title}") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(section.tracks, key = { tIndex, track -> "shelf_${sIndex}_${track.id}_$tIndex" }) { _, track ->
                                YtmMusicCard(
                                    track = track,
                                    isPlaying = currentTrack?.id == track.id,
                                    onClick = { viewModel.playTrack(track, section.tracks) },
                                    onLongClick = { trackForPlaylist = track }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }

                item {
                    NexFooter()
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(trendingTracks, key = { it.id }, contentType = { "track" }) { track ->
                    val isFav = track.id in favoriteIds
                    TrackItem(
                        track = track.copy(isFavorite = isFav),
                        isPlaying = currentTrack?.id == track.id,
                        isLoading = isLoadingTrack && currentTrack?.id == track.id,
                        onClick = { viewModel.playTrack(track, trendingTracks) },
                        onFavoriteClick = { viewModel.toggleFavorite(track) },
                        onMoreClick = { trackForPlaylist = track }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showProfileMenu) {
        ProfileMenuSheet(
            account = currentAccount,
            viewModel = viewModel,
            onLoginClick = { showLoginWebView = true },
            onLogoutClick = { viewModel.logout() },
            onOpenSettingsClick = { showSettingsDialog = true },
            onDismiss = { showProfileMenu = false }
        )
    }

    if (showLoginWebView) {
        LoginWebViewDialog(
            onDismiss = { showLoginWebView = false },
            onLoginSuccess = { showProfileMenu = true }
        )
    }

    if (showSettingsDialog) {
        id.nexrify.music.ui.components.SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (trackForPlaylist != null) {
        AddToPlaylistDialog(
            track = trackForPlaylist!!,
            viewModel = viewModel,
            onDismiss = { trackForPlaylist = null }
        )
    }
}

@Composable
fun AccountTopButton(
    account: UserAccount?,
    onClick: () -> Unit
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, if (account != null) colors.textPrimary else colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (account != null && account.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = S.cd("Account Profile"),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else if (account != null) {
            Text(
                text = account.name.take(1).uppercase().ifBlank { "G" },
                style = Typography.titleMedium,
                color = colors.textPrimary
            )
        } else {
            Icon(
                imageVector = NexIcons.AccountCircle,
                contentDescription = S.cd("Login YouTube"),
                tint = colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YtmMusicCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    var localNow by remember { mutableStateOf(java.time.ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            localNow = java.time.ZonedDateTime.now()
            delay(30_000L)
        }
    }

    Column(
        modifier = modifier
            .width(144.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
        ) {
            AsyncImage(
                model = track.highResThumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nexrify_play),
                        contentDescription = S.cd("Playing"),
                        tint = PureWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (track.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PureBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.formattedDuration,
                        style = Typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            style = Typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            style = Typography.bodySmall,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun SkeletonMusicRow() {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    val transition = rememberInfiniteTransition(label = "catalogSkeleton")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card.copy(alpha = shimmerAlpha))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                Modifier.fillMaxWidth(0.72f).height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.card.copy(alpha = shimmerAlpha))
            )
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier.fillMaxWidth(0.46f).height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.card.copy(alpha = shimmerAlpha * 0.8f))
            )
        }
    }
}


@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = Typography.bodySmall, color = colors.textMuted)
            }
        }
        if (action != null) {
            Text(
                text = action,
                style = Typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.clickable(onClick = onAction).padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayAgainTile(
    track: Track,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, if (isPlaying) colors.textSecondary else colors.border, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(58.dp)) {
            AsyncImage(
                model = track.highResThumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier.fillMaxSize().background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NexIcons.GraphicEq, S.cd("Playing"), tint = PureWhite, modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            text = track.title,
            style = Typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickBanner(
    track: Track,
    onPlay: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(176.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = track.highResThumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.35f), PureBlack.copy(alpha = 0.88f))
                    )
                )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp, end = 86.dp)
        ) {
            Text(
                text = S.personalMix,
                style = Typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = PureWhite.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.title,
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = PureWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = Typography.bodySmall,
                color = PureWhite.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nexrify_play),
                contentDescription = S.cd("Play"),
                tint = PureBlack,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun MailboxTopButton(unread: Int, onClick: () -> Unit) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(NexIcons.Mail, S.mailboxTitle, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        }
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .border(2.dp, colors.background, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unread > 9) "9+" else unread.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.background
                )
            }
        }
    }
}
