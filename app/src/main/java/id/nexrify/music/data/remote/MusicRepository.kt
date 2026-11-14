package id.nexrify.music.data.remote

import android.util.Log
import android.util.LruCache
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.model.HomeSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class ResolvedStream(
    val url: String,
    val mimeType: String? = null
)

class MusicRepository {

    private val service = ServiceList.YouTube

    private companion object {
        const val YTM_CLIENT_VERSION = "1.20251001.03.00"
        /** Filter pencarian YouTube Music: hanya Lagu. */
        const val YTM_FILTER_SONGS = "EgWKAQIIAWoKEAkQBRAKEAMQBA=="
        /** Filter pencarian YouTube Music: hanya Video (lagu yang cuma ada sebagai video, cover, remix, dll). */
        const val YTM_FILTER_VIDEOS = "EgWKAQIQAWoKEAkQBRAKEAMQBA=="
        const val MIN_YTM_RESULTS = 5
        val YTM_TYPE_LABELS = setOf("song", "video", "single", "ep", "album", "lagu", "playlist", "artist", "artis")
    }
    private val streamUrlCache = LruCache<String, Pair<String, Long>>(50)
    private val CACHE_EXPIRY_MS = 3 * 60 * 60 * 1000L

    private var homeCache: Pair<List<HomeSection>, Long>? = null
    private val HOME_CACHE_TTL = 10 * 60 * 1000L // 10 minutes

    private val searchCache = LruCache<String, Pair<List<Track>, Long>>(30)
    private val SEARCH_CACHE_TTL = 5 * 60 * 1000L // 5 minutes

    private val radioCache = LruCache<String, Pair<List<Track>, Long>>(20)
    private val RADIO_CACHE_TTL = 30 * 60 * 1000L // 30 minutes

    private fun cleanTrackTitle(raw: String): String {
        return raw
            .replace(Regex("""(?i)\s*[\(\[](?:official\s*(?:music\s*)?video|official\s*audio|music\s*video|lyric\s*video|lyrics|audio|visualizer|remastered|remaster|4k|hd|hq|mv|clip\s*officiel|video\s*oficial)[\)\]]"""), "")
            .replace(Regex("""(?i)\s*\|\s*(?:official\s*(?:music\s*)?video|official\s*audio|visualizer).*"""), "")
            .trim()
    }

    fun upgradeThumbnailUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return rawUrl
        var u = rawUrl.trim()
        if (u.startsWith("//")) {
            u = "https:$u"
        }
        if (u.contains("googleusercontent.com")) {
            return if (u.contains(Regex("=w\\d+-h\\d+"))) {
                u.replace(Regex("=w\\d+-h\\d+[^&?]*"), "=w1080-h1080-l90-rj")
            } else if (u.contains(Regex("=s\\d+"))) {
                u.replace(Regex("=s\\d+[^&?]*"), "=w1080-h1080-l90-rj")
            } else if (u.contains("=")) {
                u.replace(Regex("=[^=]*$"), "=w1080-h1080-l90-rj")
            } else {
                "$u=w1080-h1080-l90-rj"
            }
        }
        if (u.contains("ytimg.com")) {
            return u
                .replace("/default.jpg", "/hqdefault.jpg")
                .replace("/default.webp", "/hqdefault.webp")
                .replace("/mqdefault.jpg", "/hqdefault.jpg")
                .replace("/mqdefault.webp", "/hqdefault.webp")
        }
        return u
    }

    suspend fun resolveBestThumbnail(track: Track): String = withContext(Dispatchers.IO) {
        val upgraded = upgradeThumbnailUrl(track.thumbnailUrl)
        if (upgraded.contains("ytimg.com") && track.id.isNotBlank()) {
            val maxres = "https://i.ytimg.com/vi/${track.id}/maxresdefault.jpg"
            try {
                val req = okhttp3.Request.Builder()
                    .url(maxres)
                    .head()
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val client = HttpClientProvider.client
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return@withContext maxres
                    }
                }
            } catch (_: Exception) {}
        }
        upgraded
    }

    private fun parseDurationToSeconds(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        val clean = durationStr.replace(".", ":").trim()
        val parts = clean.split(":")
        return try {
            when (parts.size) {
                2 -> (parts[0].toLong() * 60) + parts[1].toLong()
                3 -> (parts[0].toLong() * 3600) + (parts[1].toLong() * 60) + parts[2].toLong()
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun searchTracks(query: String, forceRefresh: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = query.trim().lowercase()
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            synchronized(searchCache) {
                searchCache.get(q)?.let { (cached, time) ->
                    if (now - time < SEARCH_CACHE_TTL) {
                        return@withContext cached
                    }
                }
            }
        }

        // 1. YouTube Music: Lagu DAN Video dicari bersamaan. Dulu hanya filter Lagu, jadi lagu yang
        //    di YouTube Music cuma ada sebagai video (atau versi cover/remix/upload) tidak pernah muncul.
        val (songs, videos) = coroutineScope {
            val s = async { searchYouTubeMusic(query, YTM_FILTER_SONGS) }
            val v = async { searchYouTubeMusic(query, YTM_FILTER_VIDEOS) }
            s.await() to v.await()
        }
        val merged = mergeSearchResults(songs, videos)
        if (merged.size >= MIN_YTM_RESULTS) {
            synchronized(searchCache) { searchCache.put(q, Pair(merged, now)) }
            return@withContext merged
        }

        // 2. Hasil YouTube Music sedikit atau kosong: lengkapi dengan NewPipe.
        val fallback = searchWithNewPipe(query)
        val combined = mergeSearchResults(merged, fallback)
        if (combined.isNotEmpty()) {
            synchronized(searchCache) { searchCache.put(q, Pair(combined, now)) }
        }
        combined
    }

    private fun trackKey(t: Track): String =
        (t.title + "|" + t.artist).lowercase().filter { it.isLetterOrDigit() }

    /** Gabung hasil, urutan dipertahankan, tanpa duplikat id maupun judul+artis yang sama. */
    private fun mergeSearchResults(first: List<Track>, second: List<Track>): List<Track> {
        val seenIds = HashSet<String>()
        val seenKeys = HashSet<String>()
        val out = ArrayList<Track>(first.size + second.size)
        for (t in first + second) {
            if (t.id.isBlank() || !seenIds.add(t.id)) continue
            if (!seenKeys.add(trackKey(t))) continue
            out.add(t)
        }
        return out
    }

    private fun searchWithNewPipe(query: String): List<Track> {
        return try {
            val searchExtractor = service.getSearchExtractor(
                query,
                listOf("videos"),
                ""
            )
            searchExtractor.fetchPage()
            val page = searchExtractor.initialPage
            page.items
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { item ->
                    val vid = item.url.substringAfter("v=", "").substringBefore("&")
                    if (vid.isBlank()) return@mapNotNull null
                    val rawThumb = item.thumbnails.maxByOrNull { it.width * it.height }?.url
                        ?: item.thumbnails.lastOrNull()?.url
                        ?: item.thumbnails.firstOrNull()?.url
                        ?: ""
                    Track(
                        id = vid,
                        url = item.url,
                        title = cleanTrackTitle(item.name ?: "Unknown Title"),
                        artist = item.uploaderName?.replace(" - Topic", "") ?: "Unknown Artist",
                        thumbnailUrl = upgradeThumbnailUrl(rawThumb),
                        durationSeconds = item.duration.coerceAtLeast(0L)
                    )
                }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Search failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun searchYouTubeMusic(query: String, params: String): List<Track> {
        return try {
            val auth = id.nexrify.music.data.auth.AuthManager.getInstance()
            val origin = "https://music.youtube.com"
            val cookie = auth.getCookie() ?: ""
            val sapisidHash = auth.getSapisidHash(origin)

            val payload = org.json.JSONObject().apply {
                put("context", org.json.JSONObject().apply {
                    put("client", org.json.JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", YTM_CLIENT_VERSION)
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("query", query)
                put("params", params)
            }

            val client = HttpClientProvider.client
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = okhttp3.Request.Builder()
                .url("$origin/youtubei/v1/search")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", origin)
                .header("Referer", "$origin/")
                .header("X-Origin", origin)
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", YTM_CLIENT_VERSION)
                .post(reqBody)

            if (cookie.isNotBlank()) reqBuilder.header("Cookie", cookie)
            if (!sapisidHash.isNullOrBlank()) {
                reqBuilder.header("Authorization", sapisidHash)
                reqBuilder.header("X-Goog-AuthUser", "0")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("MusicRepository", "YTM search HTTP ${response.code}")
                    return emptyList()
                }
                val jsonStr = response.body?.string() ?: return emptyList()
                parseYtmSearchTracks(org.json.JSONObject(jsonStr))
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "YTM Search exception: ${e.message}")
            emptyList()
        }
    }

    /** Ambil nama artis dari baris subtitle YouTube Music, melewati label jenis ("Song", "Video", ...) dan pemisah. */
    private fun pickArtist(runs: org.json.JSONArray?): String {
        if (runs == null) return "Unknown Artist"
        for (i in 0 until runs.length()) {
            val t = runs.optJSONObject(i)?.optString("text", "")?.trim().orEmpty()
            if (t.isEmpty() || t == "•" || t == "&" || t == ",") continue
            if (t.lowercase() in YTM_TYPE_LABELS) continue
            return t
        }
        return "Unknown Artist"
    }

    private fun parseYtmSearchTracks(root: org.json.JSONObject): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()
            val sections = tabs.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (s in 0 until sections.length()) {
                val shelf = sections.optJSONObject(s)?.optJSONObject("musicShelfRenderer") ?: continue
                val items = shelf.optJSONArray("contents") ?: continue
                for (i in 0 until items.length()) {
                    val r = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val cols = r.optJSONArray("flexColumns") ?: continue
                    if (cols.length() < 2) continue

                    val col0Runs = cols.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")?.optJSONArray("runs") ?: continue
                    if (col0Runs.length() == 0) continue

                    val title = col0Runs.optJSONObject(0)?.optString("text", "") ?: ""
                    val videoId = col0Runs.optJSONObject(0)
                        ?.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("watchEndpoint")
                        ?.optString("videoId", "") ?: ""
                    if (videoId.isBlank()) continue

                    val col1Runs = cols.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")?.optJSONArray("runs")
                    val artist = pickArtist(col1Runs)

                    var durationSec = 0L
                    if (col1Runs != null && col1Runs.length() > 0) {
                        val lastRunText = col1Runs.optJSONObject(col1Runs.length() - 1)?.optString("text", "") ?: ""
                        durationSec = parseDurationToSeconds(lastRunText)
                    }

                    var thumbUrl = ""
                    val thumbs = r.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    if (thumbs != null && thumbs.length() > 0) {
                        thumbUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                    }

                    list.add(
                        Track(
                            id = videoId,
                            url = "https://www.youtube.com/watch?v=$videoId",
                            title = cleanTrackTitle(title),
                            artist = artist.replace(" - Topic", ""),
                            thumbnailUrl = upgradeThumbnailUrl(thumbUrl),
                            durationSeconds = durationSec
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "parseYtmSearchTracks error: ${e.message}")
        }
        return list
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val extractor = service.suggestionExtractor
            extractor.suggestionList(query)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Suggestions failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getHomeShelves(forceRefresh: Boolean = false): List<HomeSection> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (forceRefresh) homeCache = null
            homeCache?.let { (cached, time) ->
                if (now - time < HOME_CACHE_TTL) {
                    return@withContext cached
                }
            }
        }
        val ytmShelves = fetchYouTubeMusicShelves()
        if (ytmShelves.isNotEmpty()) {
            synchronized(this) { homeCache = Pair(ytmShelves, now) }
            return@withContext ytmShelves
        }

        // Fallback: group into categories
        val defaultTracks = searchTracks("Top Global Hits 2026")
        if (defaultTracks.isNotEmpty()) {
            listOf(
                HomeSection(title = "Pilihan Cepat", subtitle = "Dengarkan musik favoritmu", tracks = defaultTracks.take(10)),
                HomeSection(title = "Lagu Trending", subtitle = "Paling banyak diputar", tracks = defaultTracks.drop(10))
            )
        } else {
            emptyList()
        }
    }

    suspend fun getTrendingTracks(): List<Track> = withContext(Dispatchers.IO) {
        searchTracks("Top Global Hits 2026")
    }

    suspend fun getRadioTracks(videoId: String): List<Track> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        synchronized(radioCache) {
            radioCache.get(videoId)?.let { (cached, time) ->
                if (now - time < RADIO_CACHE_TTL) {
                    return@withContext cached
                }
            }
        }
        try {
            val auth = id.nexrify.music.data.auth.AuthManager.getInstance()
            val origin = "https://music.youtube.com"
            val cookie = auth.getCookie() ?: ""
            val sapisidHash = auth.getSapisidHash(origin)

            val payload = org.json.JSONObject().apply {
                put("context", org.json.JSONObject().apply {
                    put("client", org.json.JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", YTM_CLIENT_VERSION)
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("videoId", videoId)
                put("playlistId", "RDAMVM$videoId")
            }

            val client = HttpClientProvider.client
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = okhttp3.Request.Builder()
                .url("$origin/youtubei/v1/next")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", origin)
                .header("Referer", "$origin/")
                .header("X-Origin", origin)
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", YTM_CLIENT_VERSION)
                .post(reqBody)

            if (cookie.isNotBlank()) reqBuilder.header("Cookie", cookie)
            if (!sapisidHash.isNullOrBlank()) {
                reqBuilder.header("Authorization", sapisidHash)
            }

            val bodyStr = client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                resp.body?.string().orEmpty()
            }
            val json = org.json.JSONObject(bodyStr)
            val tabs = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs") ?: return@withContext emptyList()

            var queueTab: org.json.JSONObject? = null
            for (ti in 0 until tabs.length()) {
                val q = tabs.optJSONObject(ti)?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicQueueRenderer")
                if (q != null) {
                    queueTab = q
                    break
                }
            }
            if (queueTab == null) {
                queueTab = tabs.optJSONObject(0)?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicQueueRenderer")
            }

            val items = queueTab?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")?.optJSONArray("contents") ?: return@withContext emptyList()

            val radioList = mutableListOf<Track>()
            for (i in 0 until items.length()) {
                val r = items.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                val vid = r.optString("videoId", "")
                if (vid.isBlank() || vid == videoId) continue

                val t = r.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                val a = r.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "Unknown Artist") ?: "Unknown Artist"
                val durStr = r.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                val durSec = parseDurationToSeconds(durStr)

                val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                } else ""

                radioList.add(
                    Track(
                        id = vid,
                        url = "https://www.youtube.com/watch?v=$vid",
                        title = cleanTrackTitle(t),
                        artist = a.replace(" - Topic", ""),
                        thumbnailUrl = upgradeThumbnailUrl(thumbUrl),
                        durationSeconds = durSec
                    )
                )
            }
            if (radioList.isNotEmpty()) {
                synchronized(radioCache) { radioCache.put(videoId, Pair(radioList, now)) }
            }
            radioList
        } catch (e: Exception) {
            Log.e("MusicRepository", "getRadioTracks error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchYouTubeMusicShelves(): List<HomeSection> = withContext(Dispatchers.IO) {
        val result = mutableListOf<HomeSection>()
        try {
            val auth = id.nexrify.music.data.auth.AuthManager.getInstance()
            val origin = "https://music.youtube.com"
            val cookie = auth.getCookie() ?: ""
            val sapisidHash = auth.getSapisidHash(origin)
            val client = HttpClientProvider.client

            fun fetchBrowse(browseId: String): org.json.JSONObject? {
                val payload = org.json.JSONObject().apply {
                    put("context", org.json.JSONObject().apply {
                        put("client", org.json.JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", YTM_CLIENT_VERSION)
                            put("hl", "id")
                            put("gl", "ID")
                        })
                    })
                    put("browseId", browseId)
                }
                val reqBuilder = okhttp3.Request.Builder()
                    .url("$origin/youtubei/v1/browse")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Origin", origin)
                    .header("Referer", "$origin/")
                    .header("X-Origin", origin)
                    .header("X-YouTube-Client-Name", "67")
                    .header("X-YouTube-Client-Version", YTM_CLIENT_VERSION)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))

                if (cookie.isNotBlank()) reqBuilder.header("Cookie", cookie)
                if (!sapisidHash.isNullOrBlank()) {
                    reqBuilder.header("Authorization", sapisidHash)
                    reqBuilder.header("X-Goog-AuthUser", "0")
                }

                return client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string()?.let { org.json.JSONObject(it) } else null
                }
            }

            coroutineScope {
                val homeDeferred = async { fetchBrowse("FEmusic_home") }
                val exploreDeferred = async { fetchBrowse("FEmusic_explore") }
                val homeJson = homeDeferred.await()
                val exploreJson = exploreDeferred.await()

                if (homeJson != null) {
                    result.addAll(parseYtmSections(homeJson, "Pilihan Cepat"))
                }
                if (exploreJson != null) {
                    result.addAll(parseYtmSections(exploreJson, "Lagu Trending"))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "fetchYouTubeMusicShelves error: ${e.message}")
        }
        result
    }

    private fun parseYtmSections(root: org.json.JSONObject, defaultTitle: String): List<HomeSection> {
        val list = mutableListOf<HomeSection>()
        try {
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()
            val sections = tabs.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (s in 0 until sections.length()) {
                val shelfObj = sections.optJSONObject(s)?.optJSONObject("musicCarouselShelfRenderer") ?: continue
                val hRuns = shelfObj.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")?.optJSONArray("runs")
                var shelfTitle = ""
                if (hRuns != null) {
                    val sb = StringBuilder()
                    for (k in 0 until hRuns.length()) {
                        sb.append(hRuns.optJSONObject(k)?.optString("text", ""))
                    }
                    shelfTitle = sb.toString().trim()
                }
                if (shelfTitle.isBlank()) shelfTitle = defaultTitle

                val sRuns = shelfObj.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("strapline")?.optJSONArray("runs")
                var strapline = ""
                if (sRuns != null) {
                    val sb = StringBuilder()
                    for (k in 0 until sRuns.length()) {
                        sb.append(sRuns.optJSONObject(k)?.optString("text", ""))
                    }
                    strapline = sb.toString().trim()
                }

                val items = shelfObj.optJSONArray("contents") ?: continue
                val tracks = mutableListOf<Track>()
                for (i in 0 until items.length()) {
                    val it = items.optJSONObject(i) ?: continue
                    val track = parseSingleTrackItem(it)
                    if (track != null) tracks.add(track)
                }

                if (tracks.isNotEmpty()) {
                    list.add(HomeSection(title = shelfTitle, subtitle = strapline, tracks = tracks))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "parseYtmSections error: ${e.message}")
        }
        return list
    }

    private fun parseSingleTrackItem(it: org.json.JSONObject): Track? {
        try {
            if (it.has("musicResponsiveListItemRenderer")) {
                val r = it.getJSONObject("musicResponsiveListItemRenderer")
                val cols = r.optJSONArray("flexColumns") ?: return null
                if (cols.length() < 2) return null

                val col0Runs = cols.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs") ?: return null
                if (col0Runs.length() == 0) return null

                val title = col0Runs.optJSONObject(0)?.optString("text", "") ?: ""
                val videoId = col0Runs.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId", "") ?: ""
                if (videoId.isBlank()) return null

                val col1Runs = cols.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")
                val artist = pickArtist(col1Runs)

                var durationSec = 0L
                if (col1Runs != null && col1Runs.length() > 0) {
                    val lastRunText = col1Runs.optJSONObject(col1Runs.length() - 1)?.optString("text", "") ?: ""
                    durationSec = parseDurationToSeconds(lastRunText)
                }

                var thumbUrl = ""
                val thumbs = r.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                if (thumbs != null && thumbs.length() > 0) {
                    thumbUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                }

                return Track(
                    id = videoId,
                    url = "https://www.youtube.com/watch?v=$videoId",
                    title = cleanTrackTitle(title),
                    artist = artist.replace(" - Topic", ""),
                    thumbnailUrl = upgradeThumbnailUrl(thumbUrl),
                    durationSeconds = durationSec
                )
            } else if (it.has("musicTwoRowItemRenderer")) {
                val r = it.getJSONObject("musicTwoRowItemRenderer")
                val titleRuns = r.optJSONObject("title")?.optJSONArray("runs") ?: return null
                if (titleRuns.length() == 0) return null

                val title = titleRuns.optJSONObject(0)?.optString("text", "") ?: ""
                val videoId = r.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId", "") ?: ""
                if (videoId.isBlank()) return null

                val subtitleRuns = r.optJSONObject("subtitle")?.optJSONArray("runs")
                val artist = pickArtist(subtitleRuns)

                var thumbUrl = ""
                val thumbs = r.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                if (thumbs != null && thumbs.length() > 0) {
                    thumbUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                }

                return Track(
                    id = videoId,
                    url = "https://www.youtube.com/watch?v=$videoId",
                    title = cleanTrackTitle(title),
                    artist = artist.replace(" - Topic", ""),
                    thumbnailUrl = upgradeThumbnailUrl(thumbUrl),
                    durationSeconds = 0L
                )
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun getAudioStreamUrl(
        url: String,
        quality: id.nexrify.music.data.settings.AudioQuality = id.nexrify.music.data.settings.AudioQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$url#${quality.name}"
        val now = System.currentTimeMillis()
        synchronized(streamUrlCache) {
            streamUrlCache.get(cacheKey)?.let { (cachedUrl, timestamp) ->
                if (now - timestamp < CACHE_EXPIRY_MS) {
                    return@withContext cachedUrl
                }
            }
        }

        try {
            val streamInfo = StreamInfo.getInfo(service, url)
            val audioStreams = streamInfo.audioStreams
            val httpAudios = audioStreams.filter { it.content != null && it.content.startsWith("http") }

            val chosenAudio = when (quality) {
                id.nexrify.music.data.settings.AudioQuality.LOW -> {
                    httpAudios.filter { it.averageBitrate > 0 }.minByOrNull { it.averageBitrate }
                        ?: httpAudios.firstOrNull()
                }
                id.nexrify.music.data.settings.AudioQuality.MEDIUM -> {
                    httpAudios.filter { it.averageBitrate in 90..180 }
                        .minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
                        ?: httpAudios.sortedBy { it.averageBitrate }.getOrNull(httpAudios.size / 2)
                }
                id.nexrify.music.data.settings.AudioQuality.HIGH -> {
                    val m4aStreams = httpAudios.filter {
                        it.format == org.schabi.newpipe.extractor.MediaFormat.M4A ||
                        it.format?.name.equals("m4a", ignoreCase = true) ||
                        it.format?.suffix.equals("m4a", ignoreCase = true) ||
                        it.format.toString().contains("M4A", ignoreCase = true)
                    }
                    m4aStreams.maxByOrNull { it.averageBitrate }
                        ?: httpAudios.maxByOrNull { it.averageBitrate }
                }
            } ?: httpAudios.maxByOrNull { it.averageBitrate } ?: httpAudios.firstOrNull()

            val resolved = chosenAudio?.content ?: run {
                // Fallback to muxed video/audio stream if separate audio stream is unavailable
                streamInfo.videoStreams
                    .filter { it.content != null && it.content.startsWith("http") }
                    .firstOrNull()?.content
            }
            if (resolved != null) {
                synchronized(streamUrlCache) {
                    streamUrlCache.put(cacheKey, Pair(resolved, now))
                }
            }
            resolved
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to get audio stream for $url: ${e.message}", e)
            null
        }
    }

    suspend fun getAudioStream(
        url: String,
        quality: id.nexrify.music.data.settings.AudioQuality = id.nexrify.music.data.settings.AudioQuality.HIGH
    ): ResolvedStream? = withContext(Dispatchers.IO) {
        val streamUrl = getAudioStreamUrl(url, quality) ?: return@withContext null
        val mime = if (streamUrl.contains("audio%2Fwebm") || streamUrl.contains("audio/webm")) {
            "audio/webm"
        } else if (streamUrl.contains("audio%2Fmp4") || streamUrl.contains("audio/mp4") || streamUrl.contains("itag=140") || streamUrl.contains("itag=141") || streamUrl.contains("itag=139")) {
            "audio/mp4"
        } else if (quality == id.nexrify.music.data.settings.AudioQuality.HIGH) {
            "audio/mp4"
        } else {
            null
        }
        ResolvedStream(streamUrl, mime)
    }

    suspend fun getFallbackStreamUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(service, url)
            val audioStreams = streamInfo.audioStreams
            val httpAudios = audioStreams.filter { it.content != null && it.content.startsWith("http") }
            val opusStream = httpAudios.filter {
                it.format == org.schabi.newpipe.extractor.MediaFormat.WEBMA_OPUS ||
                it.format == org.schabi.newpipe.extractor.MediaFormat.WEBMA ||
                it.format?.name.equals("webm", ignoreCase = true) ||
                it.format?.name.equals("opus", ignoreCase = true)
            }.maxByOrNull { it.averageBitrate }
            opusStream?.content ?: httpAudios.maxByOrNull { it.averageBitrate }?.content
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLikedTracks(): List<Track> = withContext(Dispatchers.IO) {
        val auth = id.nexrify.music.data.auth.AuthManager.getInstance()
        val cookie = auth.getCookie()
        if (cookie.isNullOrBlank()) return@withContext emptyList()

        try {
            val origin = "https://music.youtube.com"
            val sapisidHash = auth.getSapisidHash(origin)

            val payload = org.json.JSONObject().apply {
                put("context", org.json.JSONObject().apply {
                    put("client", org.json.JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", YTM_CLIENT_VERSION)
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("browseId", "FEmusic_liked_videos")
            }

            val client = HttpClientProvider.client
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = okhttp3.Request.Builder()
                .url("$origin/youtubei/v1/browse")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", origin)
                .header("Referer", "$origin/")
                .header("X-Origin", origin)
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", YTM_CLIENT_VERSION)
                .header("Cookie", cookie)
                .post(reqBody)

            if (sapisidHash != null) {
                reqBuilder.header("Authorization", sapisidHash)
            }

            val bodyStr = client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
            if (bodyStr.isNotBlank()) {
                val json = org.json.JSONObject(bodyStr)
                val contents = json.optJSONObject("contents")
                    ?.optJSONObject("singleColumnBrowseResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")

                val trackList = mutableListOf<Track>()
                if (contents != null) {
                    for (si in 0 until contents.length()) {
                        val items = contents.optJSONObject(si)
                            ?.optJSONObject("musicShelfRenderer")
                            ?.optJSONArray("contents") ?: continue
                        for (i in 0 until items.length()) {
                            val row = items.optJSONObject(i) ?: continue
                            val t = parseSingleTrackItem(row) ?: continue
                            if (t.title.isNotBlank()) trackList.add(t.copy(isFavorite = true))
                        }
                    }
                }
                if (trackList.isNotEmpty()) {
                    return@withContext trackList.distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Liked songs InnerTube fetch error: ${e.message}", e)
        }

        // Fallback: NewPipe playlist extractor
        try {
            val playlistExtractor = service.getPlaylistExtractor("https://www.youtube.com/playlist?list=LL")
            playlistExtractor.fetchPage()
            val page = playlistExtractor.initialPage
            page.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val rawThumb = item.thumbnails.maxByOrNull { it.width * it.height }?.url
                        ?: item.thumbnails.lastOrNull()?.url
                        ?: item.thumbnails.firstOrNull()?.url
                        ?: ""
                    Track(
                        id = item.url.substringAfter("v=").substringBefore("&"),
                        url = item.url,
                        title = cleanTrackTitle(item.name ?: "Unknown Title"),
                        artist = item.uploaderName?.replace(" - Topic", "") ?: "Unknown Artist",
                        thumbnailUrl = upgradeThumbnailUrl(rawThumb),
                        durationSeconds = item.duration.coerceAtLeast(0L),
                        isFavorite = true
                    )
                }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to fetch liked tracks via NewPipe: ${e.message}")
            emptyList()
        }
    }
}
