package id.nexrify.music.data.importer

import id.nexrify.music.data.model.Track
import id.nexrify.music.data.remote.HttpClientProvider
import id.nexrify.music.data.remote.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Import playlist Spotify lewat link, lalu cocokkan tiap lagu ke YouTube Music (MusicRepository).
 * Tidak butuh API key / login Spotify: data diambil dari halaman embed publik.
 * Hanya playlist publik. Halaman embed Spotify hanya menampilkan sekitar 100 lagu pertama.
 */
object SpotifyImporter {

    data class SpotifyTrack(val title: String, val artist: String, val durationSec: Long)
    data class SpotifyPlaylist(val id: String, val name: String, val tracks: List<SpotifyTrack>)
    data class ImportResult(
        val playlistName: String,
        val matched: List<Track>,
        val notFound: List<SpotifyTrack>
    )

    class ImportException(message: String) : Exception(message)

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val ID_REGEX = Regex("playlist[/:]([A-Za-z0-9]{22})")

    /** Terima https://open.spotify.com/playlist/ID?si=..., /intl-xx/playlist/ID, /embed/playlist/ID, atau spotify:playlist:ID. */
    fun parsePlaylistId(input: String): String? {
        val s = input.trim()
        if (s.isEmpty()) return null
        return ID_REGEX.find(s)?.groupValues?.get(1)
    }

    suspend fun fetchPlaylist(id: String): SpotifyPlaylist = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://open.spotify.com/embed/playlist/$id")
            .header("User-Agent", UA)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        val html = HttpClientProvider.client.newCall(req).execute().use { res ->
            if (res.code == 404) throw ImportException("Playlist tidak ditemukan atau bersifat privat")
            if (!res.isSuccessful) throw ImportException("Spotify menolak permintaan (${res.code})")
            res.body?.string().orEmpty()
        }
        val json = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
            ?: throw ImportException("Format halaman Spotify berubah, tidak bisa membaca playlist")

        val root = try { JSONObject(json) } catch (_: Exception) {
            throw ImportException("Data playlist Spotify tidak valid")
        }
        val entity = findEntity(root) ?: throw ImportException("Playlist kosong atau tidak bisa dibaca")
        val list = entity.optJSONArray("trackList") ?: JSONArray()
        val tracks = ArrayList<SpotifyTrack>(list.length())
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            if (title.isEmpty()) continue
            tracks.add(
                SpotifyTrack(
                    title = title,
                    artist = o.optString("subtitle").replace('\u00a0', ' ').trim(),
                    durationSec = o.optLong("duration", 0L) / 1000L
                )
            )
        }
        if (tracks.isEmpty()) throw ImportException("Tidak ada lagu yang bisa dibaca dari playlist ini")
        val name = entity.optString("name").ifBlank { entity.optString("title") }.ifBlank { "Spotify Import" }
        SpotifyPlaylist(id, name, tracks)
    }

    /** Cari objek yang punya "trackList" di mana pun posisinya (struktur __NEXT_DATA__ sering bergeser). */
    private fun findEntity(node: Any?): JSONObject? {
        when (node) {
            is JSONObject -> {
                if (node.has("trackList")) return node
                val keys = node.keys()
                while (keys.hasNext()) findEntity(node.opt(keys.next()))?.let { return it }
            }
            is JSONArray -> for (i in 0 until node.length()) findEntity(node.opt(i))?.let { return it }
        }
        return null
    }

    // ---------------- Pencocokan ke YouTube Music ----------------

    private val NOISE = Regex("\\(.*?\\)|\\[.*?]|- .*(remaster|version|mix|edit|live).*", RegexOption.IGNORE_CASE)
    private val BAD_WORDS = listOf("cover", "karaoke", "instrumental", "slowed", "sped up", "nightcore", "8d", "reverb", "tribute", "remix", "live")

    private fun norm(s: String) = s.lowercase().replace(NOISE, " ").filter { it.isLetterOrDigit() || it == ' ' }
        .replace(Regex("\\s+"), " ").trim()

    private fun tokens(s: String) = norm(s).split(' ').filter { it.length > 1 }.toSet()

    private fun overlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.size.toDouble()
    }

    private fun score(sp: SpotifyTrack, c: Track): Double {
        val titleScore = overlap(tokens(sp.title), tokens(c.title))
        val spArtists = tokens(sp.artist)
        val artistScore = if (spArtists.isEmpty()) 0.5
        else maxOf(overlap(spArtists, tokens(c.artist)), overlap(spArtists, tokens(c.title)))
        var s = titleScore * 0.55 + artistScore * 0.30
        if (sp.durationSec > 0 && c.durationSeconds > 0) {
            val d = abs(sp.durationSec - c.durationSeconds)
            s += when {
                d <= 3 -> 0.15
                d <= 8 -> 0.08
                d <= 20 -> 0.0
                else -> -0.20
            }
        } else s += 0.05
        val spLow = sp.title.lowercase()
        val cLow = c.title.lowercase()
        for (w in BAD_WORDS) if (cLow.contains(w) && !spLow.contains(w)) s -= 0.20
        return s
    }

    private const val MIN_SCORE = 0.62

    suspend fun matchTrack(repo: MusicRepository, sp: SpotifyTrack): Track? {
        val firstArtist = sp.artist.split(',', '&').firstOrNull()?.trim().orEmpty()
        val queries = listOf("$firstArtist ${sp.title}".trim(), "${sp.title} ${sp.artist}".trim()).distinct()
        var best: Track? = null
        var bestScore = 0.0
        for (q in queries) {
            val results = try { repo.searchTracks(q).take(8) } catch (_: Exception) { emptyList() }
            for (c in results) {
                val sc = score(sp, c)
                if (sc > bestScore) { bestScore = sc; best = c }
            }
            if (bestScore >= 0.85) break
        }
        return if (bestScore >= MIN_SCORE) best else null
    }

    /** Ambil playlist dari link dan cocokkan semua lagu. onProgress(selesai, total) dipanggil dari thread worker. */
    suspend fun import(
        repo: MusicRepository,
        link: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        val id = parsePlaylistId(link) ?: throw ImportException("Link playlist Spotify tidak valid")
        val pl = fetchPlaylist(id)
        val total = pl.tracks.size
        val done = AtomicInteger(0)
        val gate = Semaphore(4)
        val results: List<Track?> = coroutineScope {
            pl.tracks.map { sp ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        val m = matchTrack(repo, sp)
                        onProgress(done.incrementAndGet(), total)
                        m
                    }
                }
            }.awaitAll()
        }
        val matched = ArrayList<Track>()
        val notFound = ArrayList<SpotifyTrack>()
        val seen = HashSet<String>()
        pl.tracks.forEachIndexed { i, sp ->
            val m = results[i]
            if (m == null) notFound.add(sp) else if (seen.add(m.id)) matched.add(m)
        }
        return ImportResult(pl.name, matched, notFound)
    }
}
