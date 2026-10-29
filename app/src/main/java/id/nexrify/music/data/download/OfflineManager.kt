package id.nexrify.music.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import id.nexrify.music.data.auth.AuthManager
import id.nexrify.music.data.auth.UserAccount
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.remote.HttpClientProvider
import id.nexrify.music.data.remote.MusicRepository
import id.nexrify.music.data.settings.AudioQuality
import id.nexrify.music.service.OfflineDownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.ArrayDeque

/** Lagu yang sudah diunduh dan siap diputar tanpa internet. */
data class OfflineTrack(
    val track: Track,
    val sizeBytes: Long,
    val mimeType: String?,
    val downloadedAt: Long
)

sealed class DownloadState {
    object Queued : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

data class DownloadItem(val track: Track, val state: DownloadState)

/**
 * Unduhan lagu offline.
 *
 *  - Disimpan di penyimpanan privat aplikasi (filesDir), bukan galeri, Downloads, atau MediaStore,
 *    jadi tidak muncul di pengelola file dan hanya bisa diputar dari dalam Nexrify.
 *  - Terikat ke akun yang sedang masuk: setiap akun punya folder dan daftar sendiri, dan akun lain tidak
 *    melihat unduhan akun ini. Tanpa login dipakai folder tamu.
 */
class OfflineManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = MusicRepository()

    private var accountKey: String = keyFor(AuthManager.getInstance().authState.value)

    private val _tracks = MutableStateFlow<List<OfflineTrack>>(emptyList())
    val offlineTracks: StateFlow<List<OfflineTrack>> = _tracks.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    /** Unduhan yang sedang antre, berjalan, atau gagal. Yang selesai pindah ke [offlineTracks]. */
    val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

    private val queue = ArrayDeque<Pair<Track, AudioQuality>>()
    private var worker: Job? = null
    @Volatile private var activeId: String? = null
    @Volatile private var activeJob: Job? = null

    init {
        reload()
        scope.launch {
            AuthManager.getInstance().authState.collect { account ->
                val key = keyFor(account)
                if (key != accountKey) {
                    cancelEverything()
                    accountKey = key
                    reload()
                }
            }
        }
    }

    // ------------------------------------------------------------------ query

    fun localFile(trackId: String): File? {
        if (_tracks.value.none { it.track.id == trackId }) return null
        return File(dirFor(accountKey), safeName(trackId) + AUDIO_EXT).takeIf { it.exists() }
    }

    fun localThumb(trackId: String): File? {
        if (_tracks.value.none { it.track.id == trackId }) return null
        return File(dirFor(accountKey), safeName(trackId) + THUMB_EXT).takeIf { it.exists() }
    }

    fun mimeOf(trackId: String): String? = _tracks.value.firstOrNull { it.track.id == trackId }?.mimeType

    fun isDownloaded(trackId: String): Boolean = _tracks.value.any { it.track.id == trackId }

    // ------------------------------------------------------------------ actions

    fun enqueue(track: Track, quality: AudioQuality) {
        if (isDownloaded(track.id)) return
        val existing = _downloads.value[track.id]
        if (existing != null && existing.state !is DownloadState.Failed) return
        setState(track, DownloadState.Queued)
        synchronized(queue) {
            queue.removeAll { it.first.id == track.id }
            queue.addLast(track to quality)
        }
        startService()
        ensureWorker()
    }

    fun retry(trackId: String, quality: AudioQuality) {
        val item = _downloads.value[trackId] ?: return
        enqueue(item.track, quality)
    }

    /** Batalkan unduhan yang antre atau berjalan, atau buang yang gagal dari daftar. */
    fun cancel(trackId: String) {
        synchronized(queue) { queue.removeAll { it.first.id == trackId } }
        if (activeId == trackId) activeJob?.cancel()
        _downloads.update { it - trackId }
    }

    fun delete(trackId: String) {
        val dir = dirFor(accountKey)
        val name = safeName(trackId)
        File(dir, name + AUDIO_EXT).delete()
        File(dir, name + THUMB_EXT).delete()
        val updated = _tracks.value.filterNot { it.track.id == trackId }
        _tracks.value = updated
        writeIndex(accountKey, updated)
    }

    fun deleteAll() {
        val dir = dirFor(accountKey)
        dir.listFiles()?.forEach { it.delete() }
        _tracks.value = emptyList()
    }

    // ------------------------------------------------------------------ worker

    private fun ensureWorker() {
        synchronized(queue) {
            if (worker != null) return
            val job = scope.launch(start = CoroutineStart.LAZY) {
                while (true) {
                    val next = synchronized(queue) {
                        val n = queue.pollFirst()
                        if (n == null) worker = null
                        n
                    } ?: break
                    val (track, quality) = next
                    val child = launch { runDownload(track, quality) }
                    activeId = track.id
                    activeJob = child
                    child.join()
                    activeId = null
                    activeJob = null
                }
            }
            worker = job
            job.start()
        }
    }

    private suspend fun runDownload(track: Track, quality: AudioQuality) {
        val key = accountKey
        val dir = dirFor(key).also { it.mkdirs() }
        val name = safeName(track.id)
        val part = File(dir, "$name.part")
        val dest = File(dir, name + AUDIO_EXT)
        try {
            setState(track, DownloadState.Downloading(0f))
            val stream = repository.getAudioStream(track.url, quality)
                ?: throw IOException("Alamat audio tidak ditemukan")

            var lastPercent = -1
            val size = fetchToFile(stream.url, part) { progress ->
                val pct = (progress * 100f).toInt()
                if (pct != lastPercent) {
                    lastPercent = pct
                    setState(track, DownloadState.Downloading(progress))
                }
            }
            if (size <= 0L) throw IOException("File kosong")
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) throw IOException("Gagal menyimpan file")

            // Sampul disimpan juga supaya tampil saat offline. Kalau gagal, lagu tetap berhasil diunduh.
            runCatching { fetchToFile(track.highResThumbnailUrl, File(dir, name + THUMB_EXT)) {} }

            if (key != accountKey) {
                dest.delete()
                File(dir, name + THUMB_EXT).delete()
                return
            }
            val entry = OfflineTrack(
                track = track.copy(streamUrl = null, isFavorite = false),
                sizeBytes = size,
                mimeType = stream.mimeType,
                downloadedAt = System.currentTimeMillis()
            )
            val updated = _tracks.value.filterNot { it.track.id == track.id } + entry
            _tracks.value = updated
            writeIndex(key, updated)
            _downloads.update { it - track.id }
        } catch (e: CancellationException) {
            part.delete()
            _downloads.update { it - track.id }
            throw e
        } catch (e: Exception) {
            part.delete()
            setState(track, DownloadState.Failed(e.message ?: "Gagal mengunduh"))
        }
    }

    /** Unduh bertahap pakai header Range, sama seperti pemutar, supaya tidak dibatasi server. */
    private suspend fun fetchToFile(url: String, target: File, onProgress: (Float) -> Unit): Long {
        var offset = 0L
        var total = -1L
        val buffer = ByteArray(32 * 1024)
        val client = HttpClientProvider.client
        FileOutputStream(target).use { out ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Range", "bytes=$offset-${offset + CHUNK - 1}")
                    .build()
                var chunkBytes = 0L
                var whole = false
                client.newCall(request).execute().use { response ->
                    if (response.code != 206 && response.code != 200) throw IOException("HTTP ${response.code}")
                    val range = response.header("Content-Range")
                    if (range != null) {
                        total = range.substringAfter('/').toLongOrNull() ?: total
                    } else if (response.code == 200) {
                        total = response.body?.contentLength() ?: -1L
                        whole = true
                    }
                    val input = response.body?.byteStream() ?: throw IOException("Respons kosong")
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        chunkBytes += n
                        offset += n
                        if (total > 0) onProgress((offset.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
                if (whole || chunkBytes == 0L || (total in 1..offset)) break
            }
        }
        return target.length()
    }

    // ------------------------------------------------------------------ helpers

    private fun setState(track: Track, state: DownloadState) {
        _downloads.update { it + (track.id to DownloadItem(track, state)) }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, OfflineDownloadService::class.java))
        } catch (_: Exception) {
            // Tidak boleh memulai layanan dari latar belakang: unduhan tetap jalan selama proses hidup.
        }
    }

    private fun cancelEverything() {
        synchronized(queue) { queue.clear() }
        activeJob?.cancel()
        _downloads.value = emptyMap()
        _tracks.value = emptyList()
    }

    private fun reload() {
        _tracks.value = readIndex(accountKey)
    }

    private fun dirFor(key: String) = File(context.filesDir, "offline/$key")

    private fun safeName(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun readIndex(key: String): List<OfflineTrack> {
        val file = File(dirFor(key), INDEX_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val list = ArrayList<OfflineTrack>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.getString("id")
                if (!File(dirFor(key), safeName(id) + AUDIO_EXT).exists()) continue
                list.add(
                    OfflineTrack(
                        track = Track(
                            id = id,
                            url = o.getString("url"),
                            title = o.getString("title"),
                            artist = o.getString("artist"),
                            thumbnailUrl = o.optString("thumbnailUrl", ""),
                            durationSeconds = o.optLong("durationSeconds", 0L)
                        ),
                        sizeBytes = o.optLong("sizeBytes", 0L),
                        mimeType = o.optString("mimeType", "").ifBlank { null },
                        downloadedAt = o.optLong("downloadedAt", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeIndex(key: String, list: List<OfflineTrack>) {
        try {
            val dir = dirFor(key).also { it.mkdirs() }
            val arr = JSONArray()
            list.forEach { e ->
                arr.put(JSONObject().apply {
                    put("id", e.track.id)
                    put("url", e.track.url)
                    put("title", e.track.title)
                    put("artist", e.track.artist)
                    put("thumbnailUrl", e.track.thumbnailUrl)
                    put("durationSeconds", e.track.durationSeconds)
                    put("sizeBytes", e.sizeBytes)
                    put("mimeType", e.mimeType ?: "")
                    put("downloadedAt", e.downloadedAt)
                })
            }
            File(dir, INDEX_FILE).writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val AUDIO_EXT = ".nxa"
        private const val THUMB_EXT = ".jpg"
        private const val INDEX_FILE = "index.json"
        private const val CHUNK = 1024L * 1024L
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

        @Volatile
        private var instance: OfflineManager? = null

        fun init(context: Context): OfflineManager =
            instance ?: synchronized(this) {
                instance ?: OfflineManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): OfflineManager =
            instance ?: throw IllegalStateException("OfflineManager belum di inisialisasi")

        /** Kunci folder per akun. Tidak memakai email mentah sebagai nama folder. */
        fun keyFor(account: UserAccount?): String {
            if (account == null) return "guest"
            val raw = account.email.lowercase().ifBlank { account.name.lowercase() }
            if (raw.isBlank()) return "account"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.take(8).joinToString("") { "%02x".format(it) }
        }
    }
}
