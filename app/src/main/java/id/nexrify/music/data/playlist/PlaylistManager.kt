package id.nexrify.music.data.playlist

import android.content.Context
import android.content.SharedPreferences
import id.nexrify.music.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tracks: List<Track> = emptyList(),
    /** Sampul kustom: URL https (hasil import) atau file:// (dipilih dari galeri). Kosong = kolase lagu. */
    val coverUrl: String = ""
)

class PlaylistManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        loadPlaylists()
    }

    fun reload() = loadPlaylists()

    private fun loadPlaylists() {
        val raw = prefs.getString(KEY_PLAYLISTS, "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Playlist>()
            for (i in 0 until arr.length()) {
                val pObj = arr.getJSONObject(i)
                val id = pObj.optString("id", UUID.randomUUID().toString())
                val name = pObj.optString("name", "Playlist")
                val createdAt = pObj.optLong("createdAt", System.currentTimeMillis())

                val tracksArr = pObj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<Track>()
                for (j in 0 until tracksArr.length()) {
                    val tObj = tracksArr.getJSONObject(j)
                    tracks.add(
                        Track(
                            id = tObj.optString("id"),
                            url = tObj.optString("url"),
                            title = tObj.optString("title"),
                            artist = tObj.optString("artist"),
                            thumbnailUrl = tObj.optString("thumbnailUrl"),
                            durationSeconds = tObj.optLong("durationSeconds", 0L)
                        )
                    )
                }
                list.add(Playlist(id = id, name = name, createdAt = createdAt, tracks = tracks, coverUrl = pObj.optString("coverUrl", "")))
            }
            _playlists.value = list
        } catch (_: Exception) {
            _playlists.value = emptyList()
        }
    }

    private fun savePlaylists(list: List<Playlist>) {
        _playlists.value = list
        val arr = JSONArray()
        for (p in list) {
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("createdAt", p.createdAt)
                put("coverUrl", p.coverUrl)
                val tArr = JSONArray()
                for (t in p.tracks) {
                    val tObj = JSONObject().apply {
                        put("id", t.id)
                        put("url", t.url)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("thumbnailUrl", t.thumbnailUrl)
                        put("durationSeconds", t.durationSeconds)
                    }
                    tArr.put(tObj)
                }
                put("tracks", tArr)
            }
            arr.put(pObj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    @Synchronized
    fun createPlaylist(name: String): Playlist {
        val trimmed = name.trim().ifBlank { "Playlist Baru" }
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            tracks = emptyList()
        )
        val current = _playlists.value.toMutableList()
        current.add(0, newPlaylist)
        savePlaylists(current)
        return newPlaylist
    }

    @Synchronized
    fun createPlaylistWithTracks(name: String, tracks: List<Track>, coverUrl: String = ""): Playlist {
        val trimmed = name.trim().ifBlank { "Playlist Baru" }
        val pl = Playlist(name = trimmed, tracks = tracks.distinctBy { it.id }, coverUrl = coverUrl.trim())
        val current = _playlists.value.toMutableList()
        current.add(0, pl)
        savePlaylists(current)
        return pl
    }

    @Synchronized
    fun deletePlaylist(playlistId: String) {
        _playlists.value.firstOrNull { it.id == playlistId }?.let { deleteLocalCover(it.coverUrl) }
        val current = _playlists.value.filter { it.id != playlistId }
        savePlaylists(current)
    }

    @Synchronized
    fun renamePlaylist(playlistId: String, newName: String) {
        val trimmed = newName.trim().ifBlank { "Playlist" }
        val current = _playlists.value.map {
            if (it.id == playlistId) it.copy(name = trimmed) else it
        }
        savePlaylists(current)
    }

    @Synchronized
    fun addTrackToPlaylist(playlistId: String, track: Track): Boolean {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val p = current[index]
        // Prevent duplicate track
        if (p.tracks.any { it.id == track.id }) return false

        val newTracks = p.tracks.toMutableList().apply { add(track) }
        current[index] = p.copy(tracks = newTracks)
        savePlaylists(current)
        return true
    }

    @Synchronized
    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return

        val p = current[index]
        val newTracks = p.tracks.filter { it.id != trackId }
        current[index] = p.copy(tracks = newTracks)
        savePlaylists(current)
    }

    /** Ganti sampul (URL atau file://). Kosong = kembali ke kolase lagu. Sampul lokal lama dihapus. */
    @Synchronized
    fun setCover(playlistId: String, coverUrl: String) {
        val current = _playlists.value
        val old = current.firstOrNull { it.id == playlistId } ?: return
        if (old.coverUrl != coverUrl) deleteLocalCover(old.coverUrl)
        savePlaylists(current.map { if (it.id == playlistId) it.copy(coverUrl = coverUrl) else it })
    }

    /**
     * Simpan gambar pilihan pengguna sebagai sampul: dipotong persegi, maksimal 720 px, JPEG.
     * Mengembalikan alamat file:// atau null kalau gambar gagal dibaca.
     */
    fun saveCoverFromUri(playlistId: String, uri: android.net.Uri): String? {
        return try {
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (opts.outWidth / sample > 1440 || opts.outHeight / sample > 1440) sample *= 2
            val bmp = appContext.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null
            val side = minOf(bmp.width, bmp.height)
            val square = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val out = if (side > 720) android.graphics.Bitmap.createScaledBitmap(square, 720, 720, true) else square
            val dir = java.io.File(appContext.filesDir, "playlist_covers").apply { mkdirs() }
            val file = java.io.File(dir, "cover_${playlistId}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            "file://" + file.absolutePath
        } catch (_: Exception) { null }
    }

    private fun deleteLocalCover(url: String) {
        if (!url.startsWith("file://")) return
        try { java.io.File(url.removePrefix("file://")).delete() } catch (_: Exception) {}
    }

    companion object {
        private const val PREF_NAME = "nexrify_playlists"
        private const val KEY_PLAYLISTS = "playlist_data"

        @Volatile
        private var INSTANCE: PlaylistManager? = null

        fun getInstance(context: Context): PlaylistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaylistManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
