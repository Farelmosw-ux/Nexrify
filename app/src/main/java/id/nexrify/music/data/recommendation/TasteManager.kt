package id.nexrify.music.data.recommendation

import android.content.Context
import android.content.SharedPreferences
import id.nexrify.music.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class TasteManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    private val _onRepeatTracks = MutableStateFlow<List<Track>>(emptyList())
    val onRepeatTracks: StateFlow<List<Track>> = _onRepeatTracks.asStateFlow()

    // Observable statistics let the UI update while the statistics dialog is open.
    private val _totalPlays = MutableStateFlow(0)
    val totalPlays: StateFlow<Int> = _totalPlays.asStateFlow()
    private val _uniqueArtistsCount = MutableStateFlow(0)
    val uniqueArtistsCount: StateFlow<Int> = _uniqueArtistsCount.asStateFlow()
    private val _topArtists = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val topArtists: StateFlow<List<Pair<String, Int>>> = _topArtists.asStateFlow()

    // In-memory caches to eliminate repeated JSON serialization/deserialization
    private val scoresMap = ConcurrentHashMap<String, Int>()
    private val playsMap = ConcurrentHashMap<String, Int>()
    private val trackDataMap = ConcurrentHashMap<String, Track>()

    init {
        loadDataFromPrefs()
    }

    @Synchronized
    fun recordTrackPlayed(track: Track) {
        updateTrackScore(track, deltaScore = PLAY_DELTA, isPlay = true)
        addToRecents(track)
        refreshOnRepeat()
        refreshStatistics()
    }

    @Synchronized
    fun recordTrackLiked(track: Track) {
        updateTrackScore(track, deltaScore = LIKE_DELTA, isPlay = false)
        refreshOnRepeat()
        refreshStatistics()
    }

    @Synchronized
    fun recordTrackRepeated(track: Track) {
        updateTrackScore(track, deltaScore = 4, isPlay = true)
        refreshOnRepeat()
        refreshStatistics()
    }

    /** Batal suka: mencabut bonus suka supaya skor tidak naik terus. */
    @Synchronized
    fun recordTrackUnliked(track: Track) {
        updateTrackScore(track, deltaScore = -LIKE_DELTA, isPlay = false)
        refreshOnRepeat()
        refreshStatistics()
    }

    /**
     * Lagu dilewati sebelum sempat didengar. Putaran sudah dihitung (+PLAY_DELTA) saat pemutaran
     * dimulai, jadi penalti harus lebih besar dari bonus itu; kalau tidak, skip malah menambah skor.
     * Skor boleh negatif: lagu yang berulang kali dilewati dianggap tidak disukai.
     */
    @Synchronized
    fun recordTrackSkipped(track: Track) {
        updateTrackScore(track, deltaScore = SKIP_DELTA, isPlay = false)
        // Lagu yang dilewati bukan putaran sungguhan: jangan menggelembungkan statistik.
        val plays = playsMap[track.id] ?: 0
        if (plays > 0) playsMap[track.id] = plays - 1
        removeFromRecents(track.id)
        refreshOnRepeat()
        refreshStatistics()
    }

    /** Skor lagu (0 kalau belum pernah dinilai). Negatif berarti cenderung tidak disukai. */
    fun scoreOf(id: String): Int = scoresMap[id] ?: 0

    fun isDisliked(id: String): Boolean = scoreOf(id) <= DISLIKE_THRESHOLD

    /** Buang lagu yang tidak disukai dari daftar rekomendasi, urutan aslinya dijaga. */
    fun withoutDisliked(tracks: List<Track>): List<Track> = tracks.filter { !isDisliked(it.id) }

    /** Buang yang tidak disukai lalu naikkan lagu berskor tinggi (urutan sama tetap stabil). */
    fun personalize(tracks: List<Track>): List<Track> =
        withoutDisliked(tracks).sortedByDescending { scoreOf(it.id) }

    private fun updateTrackScore(track: Track, deltaScore: Int, isPlay: Boolean) {
        val currentScore = scoresMap[track.id] ?: 0
        scoresMap[track.id] = (currentScore + deltaScore).coerceIn(MIN_SCORE, MAX_SCORE)

        if (isPlay) {
            val currentPlays = playsMap[track.id] ?: 0
            playsMap[track.id] = currentPlays + 1
        }

        trackDataMap[track.id] = track

        // Prune entries if exceeding max limit to prevent unbounded memory/disk growth
        if (trackDataMap.size > MAX_TRACK_ENTRIES) {
            // Buang yang paling "netral" (skor mendekati 0). Skor sangat negatif ikut dijaga
            // supaya lagu yang tidak disukai tidak lupa dan muncul lagi.
            val sortedByScore = scoresMap.entries.filter { it.key != track.id }
                .sortedBy { kotlin.math.abs(it.value) }
            val toRemoveCount = trackDataMap.size - MAX_TRACK_ENTRIES
            for (i in 0 until toRemoveCount) {
                val entry = sortedByScore.getOrNull(i) ?: break
                scoresMap.remove(entry.key)
                playsMap.remove(entry.key)
                trackDataMap.remove(entry.key)
            }
        }

        persistTasteDataAsync()
    }

    private fun removeFromRecents(id: String) {
        val current = _recentlyPlayed.value
        if (current.none { it.id == id }) return
        val trimmed = current.filter { it.id != id }
        _recentlyPlayed.value = trimmed
        persistRecents(trimmed)
    }

    private fun persistRecents(list: List<Track>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("url", t.url)
                put("title", t.title)
                put("artist", t.artist)
                put("thumbnailUrl", t.thumbnailUrl)
                put("durationSeconds", t.durationSeconds)
            })
        }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    private fun addToRecents(track: Track) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)

        val trimmed = if (current.size > MAX_RECENTS) current.take(MAX_RECENTS) else current
        _recentlyPlayed.value = trimmed

        // Persist recents
        val arr = JSONArray()
        for (t in trimmed) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("url", t.url)
                put("title", t.title)
                put("artist", t.artist)
                put("thumbnailUrl", t.thumbnailUrl)
                put("durationSeconds", t.durationSeconds)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    private fun refreshOnRepeat() {
        val sorted = scoresMap.entries
            .filter { it.value > 3 }
            .sortedByDescending { it.value }
            .take(15)

        val list = sorted.mapNotNull { trackDataMap[it.key] }
        _onRepeatTracks.value = list
    }

    private fun persistTasteDataAsync() {
        val scoresJson = JSONObject()
        scoresMap.forEach { (k, v) -> scoresJson.put(k, v) }

        val playsJson = JSONObject()
        playsMap.forEach { (k, v) -> playsJson.put(k, v) }

        val trackDataJson = JSONObject()
        trackDataMap.forEach { (k, t) ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("url", t.url)
                put("title", t.title)
                put("artist", t.artist)
                put("thumbnailUrl", t.thumbnailUrl)
                put("durationSeconds", t.durationSeconds)
            }
            trackDataJson.put(k, obj)
        }

        prefs.edit()
            .putString(KEY_SCORES, scoresJson.toString())
            .putString(KEY_PLAYS, playsJson.toString())
            .putString(KEY_TRACK_DATA, trackDataJson.toString())
            .apply()
    }

    /** Muat ulang dari penyimpanan setelah data akun diterapkan atau dihapus. */
    @Synchronized
    fun reload() {
        scoresMap.clear(); playsMap.clear(); trackDataMap.clear()
        loadDataFromPrefs()
    }

    private fun loadDataFromPrefs() {
        // Tiap bagian dimuat terpisah: satu bagian rusak tidak boleh menggagalkan yang lain.
        try {
            // 1. Load recents
            val recentsRaw = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
            val recentsArr = JSONArray(recentsRaw)
            val rList = mutableListOf<Track>()
            for (i in 0 until recentsArr.length()) {
                val obj = recentsArr.getJSONObject(i)
                rList.add(
                    Track(
                        id = obj.optString("id"),
                        url = obj.optString("url"),
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        durationSeconds = obj.optLong("durationSeconds", 0L)
                    )
                )
            }
            _recentlyPlayed.value = rList
        } catch (_: Exception) { _recentlyPlayed.value = emptyList() }

        try {
            // 2. Load taste maps
            val scoresJson = JSONObject(prefs.getString(KEY_SCORES, "{}") ?: "{}")
            val playsJson = JSONObject(prefs.getString(KEY_PLAYS, "{}") ?: "{}")
            val trackDataJson = JSONObject(prefs.getString(KEY_TRACK_DATA, "{}") ?: "{}")

            val sKeys = scoresJson.keys()
            while (sKeys.hasNext()) {
                val k = sKeys.next()
                scoresMap[k] = scoresJson.optInt(k, 0).coerceIn(MIN_SCORE, MAX_SCORE)
            }

            val pKeys = playsJson.keys()
            while (pKeys.hasNext()) {
                val k = pKeys.next()
                playsMap[k] = playsJson.optInt(k, 0)
            }

            val tKeys = trackDataJson.keys()
            while (tKeys.hasNext()) {
                val k = tKeys.next()
                val obj = trackDataJson.optJSONObject(k)
                if (obj != null) {
                    trackDataMap[k] = Track(
                        id = obj.optString("id"),
                        url = obj.optString("url"),
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        durationSeconds = obj.optLong("durationSeconds", 0L)
                    )
                }
            }

        } catch (_: Exception) {
            scoresMap.clear(); playsMap.clear(); trackDataMap.clear()
        }
        refreshOnRepeat()
        refreshStatistics()
    }

    @Synchronized
    private fun refreshStatistics() {
        _totalPlays.value = playsMap.values.sum()
        val artists = trackDataMap.entries.mapNotNull { (id, track) ->
            track.artist.trim().takeIf { it.isNotBlank() && (playsMap[id] ?: 0) > 0 }
        }.toSet()
        _uniqueArtistsCount.value = artists.size
        _topArtists.value = trackDataMap.entries.mapNotNull { (id, track) ->
            val count = playsMap[id] ?: 0
            val artist = track.artist.trim()
            if (count > 0 && artist.isNotBlank()) artist to count else null
        }.groupBy({ it.first }, { it.second })
            .map { it.key to it.value.sum() }
            .sortedByDescending { it.second }
            .take(5)
    }

    fun getTotalPlays(): Int {
        var total = 0
        playsMap.values.forEach { total += it }
        return total
    }

    fun getTopArtists(limit: Int = 5): List<Pair<String, Int>> {
        val artistCounts = mutableMapOf<String, Int>()
        trackDataMap.forEach { (id, track) ->
            val count = playsMap[id] ?: 0
            val artist = track.artist.trim()
            if (artist.isNotBlank() && count > 0) {
                artistCounts[artist] = (artistCounts[artist] ?: 0) + count
            }
        }
        return artistCounts.toList().sortedByDescending { it.second }.take(limit)
    }

    fun getUniqueArtistsCount(): Int {
        val artists = mutableSetOf<String>()
        trackDataMap.forEach { (id, track) ->
            val a = track.artist.trim()
            if ((playsMap[id] ?: 0) > 0 && a.isNotBlank()) artists.add(a)
        }
        return artists.size
    }

    fun getTopTracks(limit: Int = 5): List<Pair<Track, Int>> {
        val list = mutableListOf<Pair<Track, Int>>()
        trackDataMap.forEach { (id, track) ->
            val count = playsMap[id] ?: 0
            if (count > 0) {
                list.add(Pair(track, count))
            }
        }
        return list.sortedByDescending { it.second }.take(limit)
    }

    companion object {
        private const val PREF_NAME = "nexrify_taste_profile"
        private const val KEY_SCORES = "scores"
        private const val KEY_PLAYS = "plays"
        private const val KEY_TRACK_DATA = "track_data"
        private const val KEY_RECENTS = "recents"
        private const val MAX_RECENTS = 30
        private const val MAX_TRACK_ENTRIES = 100
        private const val PLAY_DELTA = 3
        private const val LIKE_DELTA = 5
        private const val SKIP_DELTA = -7
        private const val MIN_SCORE = -30
        private const val MAX_SCORE = 200
        /** Skor di bawah/sama dengan ini = tidak disukai (sekitar dua kali dilewati). */
        private const val DISLIKE_THRESHOLD = -6

        @Volatile
        private var INSTANCE: TasteManager? = null

        fun getInstance(context: Context): TasteManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TasteManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
