package id.nexrify.music.viewmodel

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.playback.LastPlayback
import id.nexrify.music.data.playback.LastPlaybackStore
import id.nexrify.music.ui.widget.NowPlayingWidgetProvider
import id.nexrify.music.data.remote.MusicRepository
import id.nexrify.music.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import id.nexrify.music.data.audio.AudioOutputManager
import id.nexrify.music.data.auth.AuthManager
import id.nexrify.music.data.lyrics.LyricsData
import id.nexrify.music.data.lyrics.LyricsRepository
import id.nexrify.music.data.model.HomeSection
import id.nexrify.music.data.playlist.Playlist
import id.nexrify.music.data.playlist.PlaylistManager
import id.nexrify.music.data.recommendation.TasteManager
import id.nexrify.music.data.settings.SettingsManager

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoadingTrack = MutableStateFlow(false)
    val isLoadingTrack = _isLoadingTrack.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded = _isPlayerExpanded.asStateFlow()

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections = _homeSections.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks = _trendingTracks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val favPrefs = getApplication<android.app.Application>().getSharedPreferences("nexrify_favorites", android.content.Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow<List<Track>>(loadStoredFavorites())
    val favorites = _favorites.asStateFlow()

    private fun loadStoredFavorites(): List<Track> {
        val jsonStr = favPrefs.getString("fav_list", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Track(
                        id = obj.getString("id"),
                        url = obj.getString("url"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        thumbnailUrl = obj.getString("thumbnailUrl"),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        isFavorite = true
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveStoredFavorites(list: List<Track>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { t ->
                val obj = org.json.JSONObject().apply {
                    put("id", t.id)
                    put("url", t.url)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("thumbnailUrl", t.thumbnailUrl)
                    put("durationSeconds", t.durationSeconds)
                }
                arr.put(obj)
            }
            favPrefs.edit().putString("fav_list", arr.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("NexrifyVM", "Save favorites error: ${e.message}")
        }
    }

    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending = _isLoadingTrending.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    val authState = AuthManager.getInstance().authState

    val settingsManager = SettingsManager.getInstance()
    val themeMode = settingsManager.themeMode
    val audioQuality = settingsManager.audioQuality

    val audioOutputManager = AudioOutputManager.getInstance(getApplication())
    val audioDeviceState = audioOutputManager.deviceState
    fun openAudioOutputSettings() = audioOutputManager.openAudioSettings()

    val tasteManager = TasteManager.getInstance(getApplication())
    val recentlyPlayed = tasteManager.recentlyPlayed
    val onRepeatTracks = tasteManager.onRepeatTracks

    val playlistManager = PlaylistManager.getInstance(getApplication())
    val playlists = playlistManager.playlists

    fun createPlaylist(name: String) = playlistManager.createPlaylist(name)
    fun deletePlaylist(id: String) = playlistManager.deletePlaylist(id)
    fun renamePlaylist(id: String, name: String) = playlistManager.renamePlaylist(id, name)
    fun addTrackToPlaylist(id: String, track: Track) = playlistManager.addTrackToPlaylist(id, track)
    fun removeTrackFromPlaylist(id: String, trackId: String) = playlistManager.removeTrackFromPlaylist(id, trackId)

    fun playPlaylist(playlist: Playlist, startTrack: Track? = null, shuffle: Boolean = false) {
        if (playlist.tracks.isEmpty()) return
        val list = if (shuffle) playlist.tracks.shuffled() else playlist.tracks
        val initialTrack = startTrack ?: list.first()
        playTrack(initialTrack, list)
    }

    val lyricsRepository = LyricsRepository(getApplication())

    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    private val playlistQueue = java.util.Collections.synchronizedList(mutableListOf<Track>())
    private var currentIndex = -1
    private var progressJob: Job? = null
    private var suggestJob: Job? = null
    private var playJob: Job? = null
    private var lyricsJob: Job? = null
    private var searchJob: Job? = null
    private var pendingTrackToPlay: Track? = null
    private var hasRetriedFallback = false
    private var trackStartTime = 0L

    // ------------------------------------------------------------------
    // Smart Queue: state yang diamati UI
    // ------------------------------------------------------------------
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex = _queueIndex.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible = _isQueueVisible.asStateFlow()

    fun showQueue(show: Boolean) {
        _isQueueVisible.value = show
    }

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    private fun postMessage(message: String) {
        _uiMessages.tryEmit(message)
    }

    // ------------------------------------------------------------------
    // Home personal (semua data hanya tersimpan di perangkat ini)
    // ------------------------------------------------------------------
    private val lastPlaybackStore = LastPlaybackStore(getApplication())

    private val _lastPlayback = MutableStateFlow<LastPlayback?>(lastPlaybackStore.load())
    val lastPlayback = _lastPlayback.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Track>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    private val _homeError = MutableStateFlow(false)
    val homeError = _homeError.asStateFlow()

    private val _requestedPlaylistId = MutableStateFlow<String?>(null)
    val requestedPlaylistId = _requestedPlaylistId.asStateFlow()

    fun openPlaylist(playlistId: String) {
        _requestedPlaylistId.value = playlistId
    }

    fun consumeRequestedPlaylist() {
        _requestedPlaylistId.value = null
    }

    private var resumeSeekMs = 0L
    private var lastPositionSaveAt = 0L
    private var recommendationJob: Job? = null

    // Widget layar utama
    private var widgetArtJob: Job? = null
    private var widgetArt: Bitmap? = null
    private var widgetArtTrackId: String? = null

    private val mediaActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                PlaybackService.ACTION_NEXT -> skipNext()
                PlaybackService.ACTION_PREV -> skipPrevious()
            }
        }
    }

    init {
        initializeMediaController()
        loadTrending()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(PlaybackService.ACTION_NEXT)
                addAction(PlaybackService.ACTION_PREV)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                getApplication(),
                mediaActionReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                pendingTrackToPlay?.let { pending ->
                    pendingTrackToPlay = null
                    playTrack(pending)
                }
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        publishWidget()
                        if (!playing) lastPlaybackStore.savePosition(_playbackPosition.value)
                        if (playing) {
                            _isLoadingTrack.value = false
                            startProgressTracking()
                        } else {
                            stopProgressTracking()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                _isLoadingTrack.value = true
                            }
                            Player.STATE_READY -> {
                                _isLoadingTrack.value = false
                                isSeeking = false
                                mediaController?.let { controller ->
                                    val d = controller.duration
                                    if (d > 0) {
                                        _duration.value = d
                                        val currentLrc = _currentLyrics.value
                                        val currentTrk = _currentTrack.value
                                        if (currentTrk != null && (currentLrc == null || !currentLrc.isSynced)) {
                                            val realDurSec = d / 1000L
                                            if (realDurSec > 0 && realDurSec != currentTrk.durationSeconds) {
                                                lyricsJob?.cancel()
                                                lyricsJob = viewModelScope.launch(Dispatchers.IO) {
                                                    val refreshed = lyricsRepository.getLyrics(
                                                        trackId = currentTrk.id,
                                                        title = currentTrk.title,
                                                        artist = currentTrk.artist,
                                                        durationSeconds = realDurSec
                                                    )
                                                    if (refreshed != null && refreshed.isSynced && _currentTrack.value?.id == currentTrk.id) {
                                                        _currentLyrics.value = refreshed
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Player.STATE_ENDED -> {
                                _isLoadingTrack.value = false
                                _currentTrack.value?.let { tasteManager.recordTrackPlayed(it) }
                                skipNext()
                            }
                            Player.STATE_IDLE -> {
                                _isLoadingTrack.value = false
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("NexrifyMusicViewModel", "Player error: ${error.message}", error)
                        val track = _currentTrack.value
                        if (track != null && track.streamUrl != null && !hasRetriedFallback) {
                            hasRetriedFallback = true
                            viewModelScope.launch(Dispatchers.IO) {
                                val fallbackUrl = repository.getFallbackStreamUrl(track.url)
                                if (fallbackUrl != null && fallbackUrl != track.streamUrl) {
                                    val fallbackTrack = track.copy(streamUrl = fallbackUrl, mimeType = "audio/webm")
                                    withContext(Dispatchers.Main) {
                                        _currentTrack.value = fallbackTrack
                                        mediaController?.let { ctrl ->
                                            val meta = MediaMetadata.Builder()
                                                .setTitle(fallbackTrack.title)
                                                .setArtist(fallbackTrack.artist)
                                                .setArtworkUri(Uri.parse(fallbackTrack.highResThumbnailUrl))
                                                .setDisplayTitle(fallbackTrack.title)
                                                .build()

                                            val item = MediaItem.Builder()
                                                .setMediaId(fallbackTrack.id)
                                                .setUri(fallbackUrl)
                                                .setMimeType("audio/webm")
                                                .setMediaMetadata(meta)
                                                .build()

                                            ctrl.setMediaItem(item)
                                            ctrl.prepare()
                                            ctrl.play()
                                        }
                                    }
                                    return@launch
                                }
                                withContext(Dispatchers.Main) {
                                    _isLoadingTrack.value = false
                                    _isPlaying.value = false
                                    skipNext()
                                }
                            }
                        } else {
                            hasRetriedFallback = false
                            _isLoadingTrack.value = false
                            _isPlaying.value = false
                            skipNext()
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    // ------------------------------------------------------------------
    // Smart Queue: operasi
    // ------------------------------------------------------------------
    private fun publishQueue() {
        val snapshot = synchronized(playlistQueue) { playlistQueue.toList() }
        val index = currentIndex
        _queue.value = snapshot
        _queueIndex.value = index
        lastPlaybackStore.saveQueue(snapshot, index)
    }

    private fun hasActiveQueue(): Boolean =
        _currentTrack.value != null && synchronized(playlistQueue) { playlistQueue.isNotEmpty() }

    /** Tambahkan ke akhir antrean. Jika lagu sudah ada di antrean, dipindahkan ke akhir. */
    fun addToQueue(track: Track) {
        if (!hasActiveQueue()) {
            playTrackFromSearch(track)
            postMessage("Memutar \"${track.title}\"")
            return
        }
        var alreadyPlaying = false
        synchronized(playlistQueue) {
            val existing = playlistQueue.indexOfFirst { it.id == track.id }
            if (existing != -1 && existing == currentIndex) {
                alreadyPlaying = true
            } else {
                if (existing != -1) {
                    playlistQueue.removeAt(existing)
                    if (existing < currentIndex) currentIndex--
                }
                playlistQueue.add(track)
            }
        }
        if (alreadyPlaying) {
            postMessage("Lagu ini sedang diputar")
            return
        }
        publishQueue()
        postMessage("Ditambahkan ke antrean")
    }

    /** Putar setelah lagu yang sedang berjalan. */
    fun playNext(track: Track) {
        if (!hasActiveQueue()) {
            playTrackFromSearch(track)
            postMessage("Memutar \"${track.title}\"")
            return
        }
        var alreadyPlaying = false
        synchronized(playlistQueue) {
            val existing = playlistQueue.indexOfFirst { it.id == track.id }
            if (existing != -1 && existing == currentIndex) {
                alreadyPlaying = true
            } else {
                if (existing != -1) {
                    playlistQueue.removeAt(existing)
                    if (existing < currentIndex) currentIndex--
                }
                playlistQueue.add((currentIndex + 1).coerceAtMost(playlistQueue.size), track)
            }
        }
        if (alreadyPlaying) {
            postMessage("Lagu ini sedang diputar")
            return
        }
        publishQueue()
        postMessage("Akan diputar berikutnya")
        // Siapkan URL audio lagu berikutnya di latar belakang
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAudioStreamUrl(track.url, settingsManager.audioQuality.value)
        }
    }

    /** Pindahkan lagu [fromId] ke posisi lagu [toId] (dipakai drag-and-drop). */
    fun moveQueueItem(fromId: String, toId: String) {
        var changed = false
        synchronized(playlistQueue) {
            val from = playlistQueue.indexOfFirst { it.id == fromId }
            val to = playlistQueue.indexOfFirst { it.id == toId }
            if (from != -1 && to != -1 && from != to) {
                val item = playlistQueue.removeAt(from)
                playlistQueue.add(to, item)
                currentIndex = when {
                    currentIndex == from -> to
                    from < currentIndex && to >= currentIndex -> currentIndex - 1
                    from > currentIndex && to <= currentIndex -> currentIndex + 1
                    else -> currentIndex
                }
                changed = true
            }
        }
        if (changed) publishQueue()
    }

    /** Hapus lagu dari antrean (lagu yang sedang diputar tidak bisa dihapus). */
    fun removeFromQueue(trackId: String) {
        var changed = false
        synchronized(playlistQueue) {
            val idx = playlistQueue.indexOfFirst { it.id == trackId }
            if (idx != -1 && idx != currentIndex) {
                playlistQueue.removeAt(idx)
                if (idx < currentIndex) currentIndex--
                changed = true
            }
        }
        if (changed) publishQueue()
    }

    /** Lompat ke lagu tertentu di antrean. */
    fun playQueueItem(trackId: String) {
        val target: Track? = synchronized(playlistQueue) {
            val idx = playlistQueue.indexOfFirst { it.id == trackId }
            if (idx == -1) {
                null
            } else {
                currentIndex = idx
                playlistQueue[idx]
            }
        }
        if (target != null) playTrack(target)
    }

    /** Simpan seluruh antrean saat ini menjadi playlist baru (lokal di perangkat). */
    fun saveQueueAsPlaylist(name: String) {
        val tracks = synchronized(playlistQueue) { playlistQueue.toList() }
        if (tracks.isEmpty()) {
            postMessage("Antrean masih kosong")
            return
        }
        val created = playlistManager.createPlaylistWithTracks(name, tracks)
        postMessage("Antrean disimpan sebagai \"${created.name}\"")
    }

    // ------------------------------------------------------------------
    // Lanjutkan pemutaran terakhir & rekomendasi
    // ------------------------------------------------------------------
    fun resumeLastPlayback() {
        val snapshot = lastPlaybackStore.load() ?: return
        val track = snapshot.track ?: return
        resumeSeekMs = snapshot.positionMs
        playTrack(track, snapshot.queue)
    }

    private fun loadRecommendations() {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            // Hanya tampil jika data kebiasaan mendengarkan sudah cukup.
            if (tasteManager.getTotalPlays() < 5) {
                _recommendations.value = emptyList()
                return@launch
            }
            val topArtists = tasteManager.getTopArtists(2).map { it.first }
            if (topArtists.isEmpty()) {
                _recommendations.value = emptyList()
                return@launch
            }
            val known = (tasteManager.recentlyPlayed.value.map { it.id } + _favorites.value.map { it.id }).toSet()
            val found = mutableListOf<Track>()
            for (artist in topArtists) {
                found += repository.searchTracks("$artist songs")
            }
            _recommendations.value = found
                .distinctBy { it.id }
                .filter { it.id !in known }
                .take(15)
        }
    }

    // ------------------------------------------------------------------
    // Widget layar utama
    // ------------------------------------------------------------------
    private fun publishWidget() {
        val app = getApplication<Application>()
        val track = _currentTrack.value
        val art = if (track != null && widgetArtTrackId == track.id) widgetArt else null
        NowPlayingWidgetProvider.publish(app, track?.title, track?.artist, _isPlaying.value, art)

        if (track != null && widgetArtTrackId != track.id) {
            widgetArtJob?.cancel()
            widgetArtJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(app)
                        .data(track.highResThumbnailUrl)
                        .size(256)
                        .allowHardware(false)
                        .build()
                    val result = Coil.imageLoader(app).execute(request)
                    val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        widgetArt = bitmap
                        widgetArtTrackId = track.id
                        withContext(Dispatchers.Main) {
                            if (_currentTrack.value?.id == track.id) {
                                NowPlayingWidgetProvider.publish(app, track.title, track.artist, _isPlaying.value, bitmap)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Cover widget bersifat opsional.
                }
            }
        }
    }

    private var isSeeking = false
    private var lastSeekTimestamp = 0L

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (isSeeking && (now - lastSeekTimestamp < 1200L)) {
                    // Grace period: allow ExoPlayer to complete async seek over network without bouncing back
                } else {
                    isSeeking = false
                    mediaController?.let { controller ->
                        val pos = controller.currentPosition.coerceAtLeast(0)
                        _playbackPosition.value = pos
                        if (now - lastPositionSaveAt > 10_000L) {
                            lastPositionSaveAt = now
                            lastPlaybackStore.savePosition(pos)
                        }
                        val realDuration = controller.duration
                        if (realDuration > 0) {
                            _duration.value = realDuration
                        }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoadingTrending.value = true
            _homeError.value = false
            val rawShelves = repository.getHomeShelves().toMutableList()
            val onRepeat = tasteManager.onRepeatTracks.value
            if (onRepeat.isNotEmpty()) {
                rawShelves.add(0, HomeSection(
                    title = "Lagi Sering Kamu Putar",
                    subtitle = "Berdasarkan selera musikmu",
                    tracks = onRepeat
                ))
            }
            _homeSections.value = rawShelves
            val tracks = if (rawShelves.isNotEmpty()) {
                rawShelves.flatMap { it.tracks }.distinctBy { it.id }
            } else {
                repository.getTrendingTracks()
            }
            _trendingTracks.value = tracks
            // Repository mengembalikan list kosong saat gagal -> tampilkan error + tombol coba lagi
            _homeError.value = rawShelves.isEmpty() && tracks.isEmpty()
            _isLoadingTrending.value = false
            loadRecommendations()

            // Pre-warm audio URLs for top 2 tracks in background with chosen quality
            launch(Dispatchers.IO) {
                delay(2000)
                tracks.take(2).forEach {
                    repository.getAudioStreamUrl(it.url, settingsManager.audioQuality.value)
                }
            }
        }
    }

    fun loadSuggestions(query: String) {
        suggestJob?.cancel()
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        suggestJob = viewModelScope.launch {
            delay(200)
            val list = repository.getSuggestions(query)
            _suggestions.value = list
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        _suggestions.value = emptyList()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchTracks(query)
            _searchResults.value = results
            _isSearching.value = false

            // Pre-warm audio URLs for top 2 search results in background
            launch(Dispatchers.IO) {
                delay(1500)
                results.take(2).forEach {
                    repository.getAudioStreamUrl(it.url, settingsManager.audioQuality.value)
                }
            }
        }
    }

    fun playTrackFromSearch(track: Track) {
        playlistQueue.clear()
        playlistQueue.add(track)
        currentIndex = 0
        playTrack(track, playlistQueue)
    }

    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        // Posisi awal (dipakai saat "Lanjutkan pemutaran"); selalu direset untuk lagu baru.
        val startPositionMs = resumeSeekMs
        resumeSeekMs = 0L
        lastPlaybackStore.savePosition(startPositionMs)
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _currentTrack.value = track
            _isLoadingTrack.value = true
            publishWidget()
            _playbackPosition.value = 0L
            _duration.value = if (track.durationSeconds > 0) track.durationSeconds * 1000L else 0L

            if (newQueue.isNotEmpty() && newQueue !== playlistQueue) {
                playlistQueue.clear()
                playlistQueue.addAll(newQueue)
                currentIndex = playlistQueue.indexOfFirst { it.id == track.id }
            } else if (currentIndex == -1 || playlistQueue.getOrNull(currentIndex)?.id != track.id) {
                val idx = playlistQueue.indexOfFirst { it.id == track.id }
                if (idx != -1) {
                    currentIndex = idx
                } else if (playlistQueue.isEmpty()) {
                    playlistQueue.add(track)
                    currentIndex = 0
                }
            }

            publishQueue()

            // Automix Radio: If queue is small (<= 2 tracks, e.g. played from search), fetch 50 radio tracks!
            if (playlistQueue.size <= 2) {
                launch(Dispatchers.IO) {
                    try {
                        val radioTracks = repository.getRadioTracks(track.id)
                        if (radioTracks.isNotEmpty()) {
                            val existingIds = playlistQueue.map { it.id }.toSet()
                            val freshTracks = radioTracks.filter { it.id !in existingIds }
                            playlistQueue.addAll(freshTracks)
                            publishQueue()
                            if (playlistQueue.size > 1) {
                                repository.getAudioStreamUrl(playlistQueue[1].url, settingsManager.audioQuality.value)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("NexrifyVM", "Automix radio fetch: ${e.message}")
                    }
                }
            }

                // Fetch Synced Lyrics + Translations in background (Non-blocking Fast Initial Emit!)
                lyricsJob?.cancel()
                lyricsJob = viewModelScope.launch(Dispatchers.IO) {
                    _isLoadingLyrics.value = true
                    _currentLyrics.value = null
                    try {
                        val data = lyricsRepository.getLyrics(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            durationSeconds = track.durationSeconds,
                            autoTranslate = false
                        )
                        _currentLyrics.value = data
                        _isLoadingLyrics.value = false

                        // If lyrics loaded and autoTranslate enabled, fetch translation asynchronously without blocking UI!
                        if (data != null && data.lines.isNotEmpty() && settingsManager.autoTranslate.value) {
                            val translated = lyricsRepository.translateLyrics(data)
                            if (_currentTrack.value?.id == track.id) {
                                _currentLyrics.value = translated
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NexrifyVM", "Lyrics load error: ${e.message}")
                        _isLoadingLyrics.value = false
                    }
                }

                // Resolve direct audio stream URL via NewPipeExtractor with user chosen Audio Quality!
            hasRetriedFallback = false
            trackStartTime = System.currentTimeMillis()
            val resolvedStream = repository.getAudioStream(track.url, settingsManager.audioQuality.value)
            val bestThumbnail = repository.resolveBestThumbnail(track)
            if (resolvedStream != null) {
                val updatedTrack = track.copy(
                    streamUrl = resolvedStream.url,
                    mimeType = resolvedStream.mimeType,
                    thumbnailUrl = bestThumbnail
                )
                _currentTrack.value = updatedTrack

                val controller = mediaController
                if (controller != null) {
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(updatedTrack.title)
                        .setArtist(updatedTrack.artist)
                        .setArtworkUri(Uri.parse(updatedTrack.highResThumbnailUrl))
                        .setDisplayTitle(updatedTrack.title)
                        .build()

                    val mediaItemBuilder = MediaItem.Builder()
                        .setMediaId(updatedTrack.id)
                        .setUri(resolvedStream.url)
                        .setMediaMetadata(mediaMetadata)

                    if (!resolvedStream.mimeType.isNullOrBlank()) {
                        mediaItemBuilder.setMimeType(resolvedStream.mimeType)
                    }

                    if (startPositionMs > 0L) {
                        controller.setMediaItem(mediaItemBuilder.build(), startPositionMs)
                        _playbackPosition.value = startPositionMs
                    } else {
                        controller.setMediaItem(mediaItemBuilder.build())
                    }
                    controller.prepare()
                    controller.play()
                } else {
                    pendingTrackToPlay = updatedTrack
                }

                // Pre-fetch next track in background for instantaneous next play!
                if (playlistQueue.isNotEmpty() && currentIndex + 1 < playlistQueue.size) {
                    val nextTrack = playlistQueue[currentIndex + 1]
                    launch(Dispatchers.IO) {
                        repository.getAudioStreamUrl(nextTrack.url, settingsManager.audioQuality.value)
                    }
                }
            } else {
                _isLoadingTrack.value = false
                Log.e("NexrifyMusicViewModel", "Failed to resolve stream URL for ${track.title}")
                // Auto skip to next available song if this track cannot be resolved!
                if (playlistQueue.isNotEmpty() && currentIndex + 1 < playlistQueue.size) {
                    skipNext()
                }
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun getProgress(): Float {
        val d = if (_duration.value > 0) _duration.value else ((_currentTrack.value?.durationSeconds ?: 0) * 1000L)
        return if (d > 0) (_playbackPosition.value.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        _playbackPosition.value = target
        lastSeekTimestamp = System.currentTimeMillis()
        isSeeking = true
        mediaController?.let { controller ->
            controller.seekTo(target)
            if (!controller.isPlaying) {
                controller.play()
            }
        }
    }

    fun getPositionMs(): Long = _playbackPosition.value

    fun skipNext() {
        if (playlistQueue.isEmpty()) return
        val current = _currentTrack.value
        if (current != null && trackStartTime > 0L) {
            val playedSec = (System.currentTimeMillis() - trackStartTime) / 1000L
            if (playedSec in 1..14) {
                tasteManager.recordTrackSkipped(current)
            } else if (playedSec >= 25) {
                tasteManager.recordTrackPlayed(current)
            }
        }
        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            _currentTrack.value?.let { playTrack(it) }
            return
        }
        val nextIndex = if (_isShuffle.value) {
            (0 until playlistQueue.size).random()
        } else if (currentIndex + 1 < playlistQueue.size) {
            currentIndex + 1
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            0
        } else {
            -1
        }

        if (nextIndex in 0 until playlistQueue.size) {
            currentIndex = nextIndex
            playTrack(playlistQueue[nextIndex])

            // When nearing the end of the queue, auto-replenish with more radio tracks!
            if (playlistQueue.size - nextIndex <= 3 && playlistQueue.isNotEmpty()) {
                val seed = playlistQueue[nextIndex]
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val freshRadio = repository.getRadioTracks(seed.id)
                        val existingIds = playlistQueue.map { it.id }.toSet()
                        val toAdd = freshRadio.filter { it.id !in existingIds }
                        playlistQueue.addAll(toAdd)
                        publishQueue()
                    } catch (_: Exception) {}
                }
            }
        } else {
            _isPlaying.value = false
            mediaController?.pause()
        }
    }

    fun skipPrevious() {
        if (playlistQueue.isEmpty()) return
        val current = _currentTrack.value
        if (current != null && trackStartTime > 0L) {
            val playedSec = (System.currentTimeMillis() - trackStartTime) / 1000L
            if (playedSec >= 25) {
                tasteManager.recordTrackPlayed(current)
            }
        }
        val prevIndex = if (currentIndex - 1 >= 0) {
            currentIndex - 1
        } else {
            playlistQueue.size - 1
        }
        currentIndex = prevIndex
        playTrack(playlistQueue[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        mediaController?.repeatMode = if (next == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (next == Player.REPEAT_MODE_ONE) {
            _currentTrack.value?.let { tasteManager.recordTrackRepeated(it) }
        }
    }

    fun toggleFavorite(track: Track) {
        val currentFavs = _favorites.value.toMutableList()
        val exists = currentFavs.any { it.id == track.id }
        if (exists) {
            currentFavs.removeAll { it.id == track.id }
        } else {
            currentFavs.add(track.copy(isFavorite = true))
            tasteManager.recordTrackLiked(track)
        }
        _favorites.value = currentFavs
        saveStoredFavorites(currentFavs)

        if (_currentTrack.value?.id == track.id) {
            _currentTrack.value = _currentTrack.value?.copy(isFavorite = !exists)
        }
    }

    fun expandPlayer(expand: Boolean) {
        _isPlayerExpanded.value = expand
    }

    fun syncLikedSongs() {
        viewModelScope.launch {
            _isLoadingTrending.value = true
            val liked = repository.getLikedTracks()
            if (liked.isNotEmpty()) {
                val currentFavs = _favorites.value.toMutableList()
                liked.forEach { track ->
                    if (currentFavs.none { it.id == track.id }) {
                        currentFavs.add(track.copy(isFavorite = true))
                    }
                }
                _favorites.value = currentFavs
                saveStoredFavorites(currentFavs)
            }
            _isLoadingTrending.value = false
        }
    }

    fun logout() {
        AuthManager.getInstance().logout()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<android.app.Application>().unregisterReceiver(mediaActionReceiver)
        } catch (_: Exception) {}
        stopProgressTracking()
        suggestJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
