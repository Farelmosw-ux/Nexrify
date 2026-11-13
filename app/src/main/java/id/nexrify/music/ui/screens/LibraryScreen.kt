package id.nexrify.music.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.playlist.Playlist
import id.nexrify.music.ui.components.AddToPlaylistDialog
import id.nexrify.music.ui.components.LibrarySearchField
import id.nexrify.music.ui.components.SearchNoResult
import id.nexrify.music.ui.components.matchesQuery
import id.nexrify.music.ui.components.matchesText
import id.nexrify.music.ui.components.TrackItem
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentAccount by viewModel.authState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Liked Songs, 1: Playlists, 2: Offline
    val openOffline by viewModel.openOfflineTab.collectAsState()
    androidx.compose.runtime.LaunchedEffect(openOffline) {
        if (openOffline) {
            selectedTab = 2
            viewModel.consumeOfflineTab()
        }
    }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(selectedTab) { query = "" }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val colors = NexrifyAppTheme.colors
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 16.dp)
    ) {
        if (selectedPlaylist != null) {
            // Playlist Detail View
            PlaylistDetailView(
                playlist = selectedPlaylist,
                viewModel = viewModel,
                currentTrack = currentTrack,
                isLoadingTrack = isLoadingTrack,
                onBack = { selectedPlaylistId = null },
                onAddTrackClick = { trackForPlaylist = it }
            )
        } else {
            val isV2 = NexrifyAppTheme.isV2
            val tabLabels = listOf(S.likedSongs, S.playlistsTab(playlists.size), S.offlineTab)
            val countLine = when (selectedTab) {
                0 -> S.likedCount(favorites.size)
                1 -> S.playlistCount(playlists.size)
                else -> S.offlineTitle
            }
            val searchHint = when (selectedTab) {
                0 -> S.searchLikedHint
                1 -> S.searchPlaylistsHint
                else -> S.searchOfflineHint
            }

            val headerBlock: @Composable () -> Unit = {
                if (isV2) {
                    // V2: judul tab aktif dan jumlahnya, sebagai kepala daftar tepat di atas lagu.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tabLabels[selectedTab.coerceIn(0, 2)],
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = countLine, style = Typography.labelMedium, color = colors.accentAlt)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = S.library,
                                style = Typography.headlineLarge,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = countLine,
                                style = Typography.bodyMedium,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            val pillsBlock: @Composable () -> Unit = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val sel = selectedTab == index
                        val pillShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .then(
                                    if (isV2 && sel) Modifier.background(Brush.horizontalGradient(listOf(colors.accent, colors.accentAlt)))
                                    else Modifier.background(if (sel) colors.accent else colors.card)
                                )
                                .border(1.dp, if (sel) colors.accent else colors.border, pillShape)
                                .clickable { selectedTab = index }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelMedium.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium),
                                color = if (sel) colors.background else colors.textPrimary
                            )
                        }
                    }
                }
            }

            val searchBlock: @Composable () -> Unit = {
                LibrarySearchField(
                    value = query,
                    onValueChange = { query = it },
                    hint = searchHint,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (isV2) {
                // V2: pencarian paling atas, lalu tab, lalu kepala daftar (urutan kebalikan dari tema lain).
                searchBlock()
                Spacer(modifier = Modifier.height(12.dp))
                pillsBlock()
                Spacer(modifier = Modifier.height(16.dp))
                headerBlock()
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                headerBlock()
                Spacer(modifier = Modifier.height(14.dp))
                pillsBlock()
                Spacer(modifier = Modifier.height(12.dp))
                searchBlock()
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (selectedTab == 0) {
                // Tab 0: Liked Songs
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = NexIcons.LibraryMusic,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = S.libraryEmpty,
                                style = Typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = S.libraryEmptyHint,
                                style = Typography.bodyMedium,
                                color = colors.textMuted
                            )
                        }
                    }
                } else {
                    val shownFavorites = remember(favorites, query) { favorites.filter { it.matchesQuery(query) } }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (shownFavorites.isEmpty()) {
                            item { SearchNoResult(query = query) }
                        }
                        items(shownFavorites, key = { it.id }, contentType = { "track" }) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id,
                                isLoading = isLoadingTrack && currentTrack?.id == track.id,
                                onClick = { viewModel.playTrack(track, shownFavorites) },
                                onFavoriteClick = { viewModel.toggleFavorite(track) },
                                onMoreClick = { trackForPlaylist = track }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            } else if (selectedTab == 2) {
                OfflineTab(viewModel = viewModel, query = query)
            } else {
                // Tab 1: Playlists
                val shownPlaylists = remember(playlists, query) {
                    if (query.isBlank()) playlists
                    else playlists.filter { pl ->
                        matchesText(query, pl.name) || pl.tracks.any { it.matchesQuery(query) }
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (query.isBlank()) item {
                        // Button to create new playlist
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable { showCreateDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NexIcons.Add,
                                    contentDescription = S.cd("New Playlist"),
                                    tint = colors.background,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = S.newPlaylist,
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = S.newPlaylistHint,
                                    style = Typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (query.isNotBlank() && shownPlaylists.isEmpty()) {
                        item { SearchNoResult(query = query) }
                    } else if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = NexIcons.QueueMusic,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = S.noPlaylistTitle,
                                        style = Typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = S.noPlaylistHint,
                                        style = Typography.bodyMedium,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    } else {
                        items(shownPlaylists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.card)
                                    .clickable { selectedPlaylistId = playlist.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistCollageIcon(playlist = playlist, size = 52)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${playlist.tracks.size} lagu",
                                        style = Typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.deletePlaylist(playlist.id)
                                    id.nexrify.music.i18n.currentStrings().let {
                                        id.nexrify.music.ui.components.NexNotifier.info(it.notePlaylistDeleted(playlist.name), it.notePlaylistDeletedTitle)
                                    }
                                }) {
                                    Icon(
                                        imageVector = NexIcons.DeleteOutline,
                                        contentDescription = S.cd("Delete"),
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
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
                    placeholder = { Text(S.playlistNameExample, color = colors.textMuted) },
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
                            selectedPlaylistId = created.id
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    )
                ) {
                    Text(S.create)
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

    if (trackForPlaylist != null) {
        AddToPlaylistDialog(
            track = trackForPlaylist!!,
            viewModel = viewModel,
            onDismiss = { trackForPlaylist = null }
        )
    }
}

@Composable
fun PlaylistCollageIcon(playlist: Playlist, size: Int = 50) {
    val colors = NexrifyAppTheme.colors
    val tracks = playlist.tracks

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.border),
        contentAlignment = Alignment.Center
    ) {
        if (tracks.size >= 4) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = tracks[0].highResThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = tracks[1].highResThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = tracks[2].highResThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = tracks[3].highResThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else if (tracks.isNotEmpty()) {
            AsyncImage(
                model = tracks[0].highResThumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = NexIcons.QueueMusic,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size((size * 0.5).dp)
            )
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    viewModel: MusicViewModel,
    currentTrack: Track?,
    isLoadingTrack: Boolean,
    onBack: () -> Unit,
    onAddTrackClick: (Track) -> Unit
) {
    val colors = NexrifyAppTheme.colors
    var trackQuery by remember { mutableStateOf("") }
    val shownTracks = remember(playlist.tracks, trackQuery) { playlist.tracks.filter { it.matchesQuery(trackQuery) } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = NexIcons.ArrowBack,
                    contentDescription = S.cd("Back"),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlist.name,
                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistCollageIcon(playlist = playlist, size = 180)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = playlist.name,
                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = S.songCount(playlist.tracks.size),
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (playlist.tracks.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.playPlaylist(playlist, shuffle = false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.background
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(NexIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(S.playAll, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.playPlaylist(playlist, shuffle = true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.card,
                                    contentColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(NexIcons.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(S.shuffle, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (playlist.tracks.isNotEmpty()) {
                    LibrarySearchField(
                        value = trackQuery,
                        onValueChange = { trackQuery = it },
                        hint = S.searchPlaylistSongsHint,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (playlist.tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = S.playlistEmpty,
                            style = Typography.bodyMedium,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                if (shownTracks.isEmpty()) {
                    item { SearchNoResult(query = trackQuery) }
                }
                items(shownTracks, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        isPlaying = currentTrack?.id == track.id,
                        isLoading = isLoadingTrack && currentTrack?.id == track.id,
                        onClick = { viewModel.playPlaylist(playlist, startTrack = track) },
                        onFavoriteClick = { viewModel.toggleFavorite(track) },
                        onMoreClick = { onAddTrackClick(track) },
                        onRemoveClick = {
                            viewModel.removeTrackFromPlaylist(playlist.id, track.id)
                            id.nexrify.music.i18n.currentStrings().let {
                                id.nexrify.music.ui.components.NexNotifier.info(
                                    it.notePlaylistRemoved(track.title, playlist.name), it.notePlaylistRemovedTitle
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
