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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.model.Track
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.components.AddToPlaylistDialog
import id.nexrify.music.ui.components.NexLoadingFill
import id.nexrify.music.ui.components.TrackItem
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel

/**
 * Halaman pencarian tersendiri: kotak cari di atas, riwayat pencarian (bisa dihapus satu per satu atau semuanya)
 * saat kotak kosong, saran saat mengetik, dan hasil pencarian di bawahnya.
 *
 * [initialQuery] diisi kalau halaman dibuka dari kategori jelajah. Pencarian dari kategori tidak dicatat ke riwayat,
 * hanya yang diketik atau dipilih sendiri oleh pengguna.
 */
@Composable
fun SearchPageScreen(
    viewModel: MusicViewModel,
    initialQuery: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf(initialQuery) }
    var submittedQuery by remember { mutableStateOf(initialQuery) }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val history by viewModel.searchHistory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingTrack by viewModel.isLoadingTrack.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds by remember(favorites) { derivedStateOf { favorites.mapTo(HashSet()) { it.id } } }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) {
            viewModel.search(initialQuery)
        } else {
            focusRequester.requestFocus()
        }
    }

    fun runSearch(text: String, record: Boolean = true) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        query = clean
        submittedQuery = clean
        focusManager.clearFocus()
        if (record) viewModel.searchAndRemember(clean) else viewModel.search(clean)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(NexIcons.ArrowBack, contentDescription = S.back, tint = colors.textPrimary)
            }
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isBlank()) submittedQuery = ""
                    viewModel.loadSuggestions(it)
                },
                placeholder = { Text(S.searchHint, color = colors.textMuted, style = Typography.bodyLarge) },
                leadingIcon = {
                    Icon(NexIcons.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            submittedQuery = ""
                            viewModel.loadSuggestions("")
                            focusRequester.requestFocus()
                        }) {
                            Icon(NexIcons.CloseCircle, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.textPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .focusRequester(focusRequester)
            )
            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        val typing = query.isNotBlank() && query.trim() != submittedQuery
        when {
            // 1. Lagi mengetik: tampilkan saran
            typing && suggestions.isNotEmpty() -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(suggestions) { suggestion ->
                        HistoryLikeRow(
                            icon = { Icon(NexIcons.Search, null, tint = colors.textMuted, modifier = Modifier.size(20.dp)) },
                            text = suggestion,
                            onClick = { runSearch(suggestion) },
                            trailing = {
                                IconButton(onClick = {
                                    query = suggestion
                                    viewModel.loadSuggestions(suggestion)
                                }) {
                                    Icon(NexIcons.NorthWest, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }

            // 2. Kotak kosong: riwayat pencarian
            query.isBlank() && submittedQuery.isBlank() -> {
                if (history.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.card)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(NexIcons.History, null, tint = colors.textMuted, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(S.noHistory, style = Typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(S.noHistoryHint, style = Typography.bodySmall, color = colors.textMuted, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(S.searchHistory, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                TextButton(onClick = { confirmClear = true }) {
                                    Icon(NexIcons.DeleteOutline, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(S.clearAll, color = colors.textMuted, fontSize = 13.sp)
                                }
                            }
                        }
                        items(history, key = { it }) { item ->
                            HistoryLikeRow(
                                icon = { Icon(NexIcons.History, null, tint = colors.textMuted, modifier = Modifier.size(20.dp)) },
                                text = item,
                                onClick = { runSearch(item) },
                                trailing = {
                                    IconButton(onClick = { viewModel.removeSearchHistory(item) }) {
                                        Icon(NexIcons.Close, S.deleteHistoryItem, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(110.dp)) }
                    }
                }
            }

            // 3. Mencari
            isSearching -> NexLoadingFill(active = true)

            // 4. Hasil
            submittedQuery.isNotBlank() && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(S.noTracks, color = colors.textMuted, style = Typography.bodyLarge)
                }
            }

            submittedQuery.isNotBlank() -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }, contentType = { "track" }) { track ->
                        TrackItem(
                            track = track.copy(isFavorite = track.id in favoriteIds),
                            isPlaying = currentTrack?.id == track.id,
                            isLoading = isLoadingTrack && currentTrack?.id == track.id,
                            onClick = { viewModel.playTrackFromSearch(track) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) },
                            onMoreClick = { trackForPlaylist = track }
                        )
                    }
                    item { Spacer(Modifier.height(110.dp)) }
                }
            }
        }
    }

    trackForPlaylist?.let { track ->
        AddToPlaylistDialog(track = track, viewModel = viewModel, onDismiss = { trackForPlaylist = null })
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.surface,
            title = { Text(S.clearHistoryTitle, color = colors.textPrimary, style = Typography.titleLarge) },
            text = { Text(S.clearHistoryBody, color = colors.textMuted, style = Typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    confirmClear = false
                }) { Text(S.delete, color = colors.textPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(S.cancel, color = colors.textMuted) }
            }
        )
    }
}

@Composable
private fun HistoryLikeRow(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = Typography.bodyLarge,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp)
        )
        trailing()
    }
}
