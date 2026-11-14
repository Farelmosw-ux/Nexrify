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
    data class SpotifyPlaylist(val id: String, val name: String, val coverUrl: String, val tracks: List<SpotifyTrack>)
    data class ImportResult(
        val playlistName: String,
        val matched: List<Track>,
        val notFound: List<SpotifyTrack>,
        val coverUrl: String = ""
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
        SpotifyPlaylist(id, name, findCover(entity), tracks)
    }

    /** Sampul playlist: coverArt.sources / visualIdentity.image / images, ambil yang terbesar. Kosong kalau tidak ada. */
    private fun findCover(entity: JSONObject): String {
        fun best(arr: JSONArray?): String {
            if (arr == null) return ""
            var url = ""; var w = -1
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val u = o.optString("url").trim()
                if (!u.startsWith("http")) continue
                val width = maxOf(o.optInt("width", 0), o.optInt("maxWidth", 0), o.optInt("maxHeight", 0))
                if (width > w || url.isEmpty()) { url = u; w = width }
            }
            return url
        }
        best(entity.optJSONObject("coverArt")?.optJSONArray("sources")).let { if (it.isNotEmpty()) return it }
        best(entity.optJSONObject("visualIdentity")?.optJSONArray("image")).let { if (it.isNotEmpty()) return it }
        best(entity.optJSONArray("images")).let { if (it.isNotEmpty()) return it }
        best(entity.optJSONObject("images")?.optJSONArray("items")).let { if (it.isNotEmpty()) return it }
        entity.optString("image").trim().let { if (it.startsWith("http")) return it }
        // Struktur bergeser: cari "coverArt" di mana pun di dalam entity.
        fun deep(node: Any?): String {
            when (node) {
                is JSONObject -> {
                    node.optJSONObject("coverArt")?.optJSONArray("sources")?.let { a -> best(a).let { if (it.isNotEmpty()) return it } }
                    val k = node.keys(); while (k.hasNext()) deep(node.opt(k.next())).let { if (it.isNotEmpty()) return it }
                }
                is JSONArray -> for (i in 0 until node.length()) deep(node.opt(i)).let { if (it.isNotEmpty()) return it }
            }
            return ""
        }
        return deep(entity)
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

    private val PARENS = Regex("\\(.*?\\)|\\[.*?]")
    private val ACCENTS = Regex("\\p{M}+")
    private val SPACES = Regex("\\s+")
    // Kata penanda versi lain yang tidak diinginkan; dicek sebagai kata utuh ("alive" tidak boleh kena "live").
    private val BAD_WORDS = listOf("cover", "karaoke", "instrumental", "slowed", "sped up", "nightcore", "8d", "reverb", "tribute", "remix", "live", "lofi", "lo-fi")

    private fun fold(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(ACCENTS, "")

    private fun norm(s: String): String =
        fold(s).lowercase().replace("&", " and ")
            .map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
            .replace(SPACES, " ").trim()

    /** Judul inti: buang (..) [..] dan keterangan setelah " - " seperti - From "Film", - Remastered, - Radio Edit. */
    private fun cleanTitle(t: String): String {
        var x = t.replace('\u00a0', ' ').replace(PARENS, " ").trim()
        val cut = x.indexOf(" - ")
        if (cut > 0) x = x.substring(0, cut)
        x = x.replace(SPACES, " ").trim()
        return x.ifEmpty { t.trim() }
    }

    private fun tokens(s: String): Set<String> = norm(s).split(' ').filter { it.isNotEmpty() }.toSet()

    private fun coverage(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.size.toDouble()
    }

    private fun artistsOf(sp: SpotifyTrack): List<String> =
        sp.artist.split(',', '&', ';', '/').map { it.trim() }.filter { it.isNotEmpty() }

    private class Scored(val track: Track, val total: Double, val title: Double, val artist: Double, val durClose: Boolean)

    private fun score(sp: SpotifyTrack, c: Track): Scored {
        val spT = tokens(cleanTitle(sp.title))
        val cT = tokens(cleanTitle(c.title))
        // "K." -> {"k"}: token satu huruf tetap dihitung, jadi judul pendek tetap bisa cocok.
        val titleScore = coverage(spT, cT) * 0.7 + coverage(cT, spT) * 0.3
        val cAll = tokens(c.artist) + tokens(c.title)
        val artists = artistsOf(sp)
        val artistScore = if (artists.isEmpty()) 0.5 else artists.maxOf { coverage(tokens(it), cAll) }
        var s = titleScore * 0.55 + artistScore * 0.30
        var durClose = false
        if (sp.durationSec > 0 && c.durationSeconds > 0) {
            val d = abs(sp.durationSec - c.durationSeconds)
            durClose = d <= 8
            s += when {
                d <= 3 -> 0.15
                d <= 8 -> 0.08
                d <= 20 -> 0.0
                else -> -0.20
            }
        } else s += 0.05
        val spNorm = " " + norm(sp.title) + " "
        val cNorm = " " + norm(c.title) + " "
        for (w in BAD_WORDS) {
            val key = " " + norm(w) + " "
            if (cNorm.contains(key) && !spNorm.contains(key)) s -= 0.20
        }
        return Scored(c, s, titleScore, artistScore, durClose)
    }

    private const val MIN_SCORE = 0.62

    private fun accepted(x: Scored): Boolean =
        x.total >= MIN_SCORE ||
            (x.title >= 0.95 && (x.artist >= 0.5 || x.durClose)) ||
            (x.title >= 0.8 && x.artist >= 0.5 && x.durClose)

    private suspend fun searchRetry(repo: MusicRepository, q: String, limit: Int): List<Track> {
        repeat(2) { attempt ->
            try { return repo.searchTracks(q).take(limit) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            if (attempt == 0) kotlinx.coroutines.delay(400)
        }
        return emptyList()
    }

    private suspend fun runQueries(repo: MusicRepository, sp: SpotifyTrack, queries: List<String>, limit: Int): Scored? {
        var best: Scored? = null
        for (q in queries) {
            for (c in searchRetry(repo, q, limit)) {
                val sc = score(sp, c)
                if (best == null || sc.total > best.total) best = sc
            }
            if (best != null && best.total >= 0.85 && accepted(best)) break
        }
        return best?.takeIf { accepted(it) }
    }

    /** Putaran pertama: beberapa bentuk query (artis+judul, judul+artis, judul saja, judul asli). */
    suspend fun matchTrack(repo: MusicRepository, sp: SpotifyTrack): Track? {
        val title = cleanTitle(sp.title)
        val first = artistsOf(sp).firstOrNull().orEmpty()
        val all = artistsOf(sp).joinToString(" ")
        val queries = listOf(
            "$first $title", "$title $all", title, "$first ${sp.title}"
        ).map { it.replace(SPACES, " ").trim() }.filter { it.isNotEmpty() }.distinct()
        return runQueries(repo, sp, queries, 10)?.track
    }

    /** Putaran kedua untuk yang belum ketemu: lebih banyak hasil dan kata kunci "audio"/"lyrics". */
    private suspend fun matchTrackRelaxed(repo: MusicRepository, sp: SpotifyTrack): Track? {
        val title = cleanTitle(sp.title)
        val first = artistsOf(sp).firstOrNull().orEmpty()
        val queries = listOf(
            "$title $first official audio", "$title $first lyrics", "$title $first topic", "$title song"
        ).map { it.replace(SPACES, " ").trim() }.distinct()
        return runQueries(repo, sp, queries, 15)?.track
    }

    /** Info playlist yang sudah terbaca, sebelum pencocokan dimulai. */
    data class Meta(val name: String, val coverUrl: String, val total: Int)

    /**
     * Ambil playlist dari link dan cocokkan semua lagu.
     * onMeta dipanggil sekali setelah playlist terbaca; onProgress(selesai, total, lagu sekarang, cocok sejauh ini)
     * dipanggil dari thread worker.
     */
    suspend fun import(
        repo: MusicRepository,
        link: String,
        onMeta: (Meta) -> Unit = {},
        onProgress: (done: Int, total: Int, current: String, matched: Int) -> Unit = { _, _, _, _ -> }
    ): ImportResult {
        val id = parsePlaylistId(link) ?: throw ImportException("Link playlist Spotify tidak valid")
        val pl = fetchPlaylist(id)
        val total = pl.tracks.size
        onMeta(Meta(pl.name, pl.coverUrl, total))
        val done = AtomicInteger(0)
        val hit = AtomicInteger(0)
        val gate = Semaphore(4)
        val results: MutableList<Track?> = coroutineScope {
            pl.tracks.map { sp ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        val m = matchTrack(repo, sp)
                        if (m != null) hit.incrementAndGet()
                        onProgress(done.incrementAndGet(), total, (sp.artist + " - " + sp.title).trim(' ', '-'), hit.get())
                        m
                    }
                }
            }.awaitAll()
        }.toMutableList()

        // Putaran kedua: hanya yang belum ketemu, lebih longgar.
        val missingIdx = results.indices.filter { results[it] == null }
        if (missingIdx.isNotEmpty()) {
            val second = coroutineScope {
                missingIdx.map { i ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            val sp = pl.tracks[i]
                            val m = matchTrackRelaxed(repo, sp)
                            if (m != null) hit.incrementAndGet()
                            onProgress(total, total, "↻ " + (sp.artist + " - " + sp.title).trim(' ', '-'), hit.get())
                            i to m
                        }
                    }
                }.awaitAll()
            }
            second.forEach { (i, m) -> if (m != null) results[i] = m }
        }

        val matched = ArrayList<Track>()
        val notFound = ArrayList<SpotifyTrack>()
        val seen = HashSet<String>()
        pl.tracks.forEachIndexed { i, sp ->
            val m = results[i]
            if (m == null) notFound.add(sp) else if (seen.add(m.id)) matched.add(m)
        }
        return ImportResult(pl.name, matched, notFound, pl.coverUrl)
    }
}
