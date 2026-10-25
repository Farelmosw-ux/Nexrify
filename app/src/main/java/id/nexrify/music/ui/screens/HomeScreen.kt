package id.nexrify.music.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.R
import id.nexrify.music.data.auth.UserAccount
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.model.HomeSection
import id.nexrify.music.data.playlist.Playlist
import id.nexrify.music.ui.components.AddToPlaylistDialog
import id.nexrify.music.ui.components.ErrorRetryState
import id.nexrify.music.ui.components.HomeSkeleton
import id.nexrify.music.ui.components.SearchLoadingState
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
import id.nexrify.music.ui.components.SettingsDialog
import id.nexrify.music.ui.theme.NexrifyAppTheme

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier,
    onOpenPlaylist: (String) -> Unit = {}
) {
    val homeSections by viewModel.homeSections.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentAccount by viewModel.authState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val lastPlayback by viewModel.lastPlayback.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val homeError by viewModel.homeError.collectAsState()
    val favoriteIds by remember(favorites) { androidx.compose.runtime.derivedStateOf { favorites.mapTo(HashSet()) { it.id } } }

    var showProfileMenu by remember { mutableStateOf(false) }
    var showLoginWebView by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }

    var selectedGenre by remember { mutableStateOf("Semua") }
    val genres = listOf("Semua", "Pop", "Lo-Fi", "Rock", "Acoustic", "Anime", "Hip-Hop", "R&B", "Top Global")

    val colors = NexrifyAppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 16.dp)
    ) {
        // Spotify-Style Top Header: Avatar on the LEFT, and Filter Chips right beside it!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountTopButton(
                account = currentAccount,
                onClick = { showProfileMenu = true }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Scrollable Spotify-Style Filter Chips (Pills)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre == genre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colors.textPrimary else colors.card)
                            .border(1.dp, if (isSelected) colors.textPrimary else colors.border, RoundedCornerShape(20.dp))
                            .clickable {
                                selectedGenre = genre
                                if (genre == "Semua") {
                                    viewModel.loadTrending()
                                } else {
                                    viewModel.search("$genre music hits")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genre,
                            style = Typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            fontSize = 13.sp,
                            color = if (isSelected) colors.background else colors.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedGenre != "Semua") {
            if (isSearching) {
                SearchLoadingState()
            } else if (searchResults.isEmpty()) {
                ErrorRetryState(
                    message = "Tidak ada lagu ditemukan untuk $selectedGenre, atau koneksi sedang bermasalah.",
                    onRetry = { viewModel.search("$selectedGenre music hits") }
                )
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // ---- 1. Lanjutkan pemutaran terakhir (hanya jika belum ada lagu aktif) ----
                val resumeTrack = lastPlayback?.track
                if (currentTrack == null && resumeTrack != null) {
                    item(key = "personal_resume") {
                        ResumeCard(
                            track = resumeTrack,
                            positionMs = lastPlayback?.positionMs ?: 0L,
                            onClick = { viewModel.resumeLastPlayback() }
                        )
                    }
                }

                // ---- 2. Terakhir diputar ----
                if (recentlyPlayed.isNotEmpty()) {
                    item(key = "personal_recent_header") {
                        ShelfHeader(title = "Terakhir Diputar")
                    }
                    item(key = "personal_recent_shelf") {
                        TrackShelf(
                            prefix = "recent",
                            tracks = recentlyPlayed.take(20),
                            currentTrackId = currentTrack?.id,
                            onPlay = { viewModel.playTrack(it, recentlyPlayed) },
                            onMore = { trackForPlaylist = it }
                        )
                    }
                }

                // ---- 3. Lagu favorit ----
                if (favorites.isNotEmpty()) {
                    item(key = "personal_fav_header") {
                        ShelfHeader(title = "Lagu Favorit", subtitle = "${favorites.size} lagu")
                    }
                    item(key = "personal_fav_shelf") {
                        val favList = favorites.asReversed()
                        TrackShelf(
                            prefix = "fav",
                            tracks = favList.take(20),
                            currentTrackId = currentTrack?.id,
                            onPlay = { viewModel.playTrack(it, favList) },
                            onMore = { trackForPlaylist = it }
                        )
                    }
                }

                // ---- 4. Playlist buatan pengguna ----
                if (playlists.isNotEmpty()) {
                    item(key = "personal_playlist_header") {
                        ShelfHeader(title = "Playlist Kamu")
                    }
                    item(key = "personal_playlist_shelf") {
                        PlaylistShelf(playlists = playlists, onOpen = onOpenPlaylist)
                    }
                }

                // ---- 5. Rekomendasi dari kebiasaan mendengarkan (hanya jika data tersedia) ----
                if (recommendations.isNotEmpty()) {
                    item(key = "personal_rec_header") {
                        ShelfHeader(
                            title = "Rekomendasi Untukmu",
                            subtitle = "Berdasarkan kebiasaan mendengarkanmu"
                        )
                    }
                    item(key = "personal_rec_shelf") {
                        TrackShelf(
                            prefix = "rec",
                            tracks = recommendations,
                            currentTrackId = currentTrack?.id,
                            onPlay = { viewModel.playTrack(it, recommendations) },
                            onMore = { trackForPlaylist = it }
                        )
                    }
                }

                // ---- 6. Katalog musik: skeleton / error / rak ----
                if (isLoadingTrending && homeSections.isEmpty() && trendingTracks.isEmpty()) {
                    item(key = "catalog_skeleton") {
                        HomeSkeleton()
                    }
                } else if (homeError && homeSections.isEmpty() && trendingTracks.isEmpty()) {
                    item(key = "catalog_error") {
                        ErrorRetryState(
                            message = "Gagal memuat katalog musik. Periksa koneksi internetmu lalu coba lagi.",
                            onRetry = { viewModel.loadTrending() },
                            modifier = Modifier.height(320.dp)
                        )
                    }
                } else if (homeSections.isNotEmpty()) {
                    // YouTube Music Style Shelves (Carousel Rows!)
                    homeSections.forEachIndexed { sIndex, section ->
                        item(key = "header_${sIndex}_${section.title}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = section.title,
                                    style = Typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = colors.textPrimary
                                )
                                if (section.subtitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = section.subtitle,
                                        style = Typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
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
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }
                } else {
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
                }

                item(key = "bottom_spacer") {
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
        SettingsDialog(
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
    val colors = NexrifyAppTheme.colors

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
                contentDescription = "Account Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else if (account != null) {
            Text(
                text = account.name.take(1).uppercase(),
                style = Typography.titleMedium,
                color = colors.textPrimary
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Login YouTube",
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
    val colors = NexrifyAppTheme.colors

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
                        contentDescription = "Playing",
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
private fun ShelfHeader(title: String, subtitle: String = "") {
    val colors = NexrifyAppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = Typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.textPrimary
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = Typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackShelf(
    prefix: String,
    tracks: List<Track>,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onMore: (Track) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // key = id lagu -> saat favorit/riwayat berubah, kartu bergeser dengan animasi
        items(tracks, key = { "${prefix}_${it.id}" }) { track ->
            YtmMusicCard(
                track = track,
                isPlaying = currentTrackId == track.id,
                onClick = { onPlay(track) },
                onLongClick = { onMore(track) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
    Spacer(modifier = Modifier.height(22.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistShelf(
    playlists: List<Playlist>,
    onOpen: (String) -> Unit
) {
    val colors = NexrifyAppTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(playlists, key = { "playlist_${it.id}" }) { playlist ->
            Column(
                modifier = Modifier
                    .width(144.dp)
                    .animateItemPlacement()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(playlist.id) }
            ) {
                PlaylistCollageIcon(playlist = playlist, size = 144)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playlist.name,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedContent(targetState = playlist.tracks.size, label = "playlistCount") { count ->
                    Text(
                        text = "$count lagu",
                        style = Typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(22.dp))
}

@Composable
private fun ResumeCard(
    track: Track,
    positionMs: Long,
    onClick: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val seconds = positionMs / 1000L
    val position = if (seconds >= 5L) String.format("Lanjut dari %d:%02d", seconds / 60, seconds % 60) else "Putar lagi"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.highResThumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Lanjutkan pemutaran",
                style = Typography.labelSmall,
                fontSize = 11.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.title,
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • $position",
                style = Typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.textPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nexrify_play),
                contentDescription = "Lanjutkan",
                tint = colors.background,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
