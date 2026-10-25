package id.nexrify.music.data.playback

import android.content.Context
import android.content.SharedPreferences
import id.nexrify.music.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * Snapshot sesi pemutaran terakhir.
 * Disimpan hanya di SharedPreferences perangkat ini (tidak ada login, tidak ada server).
 */
data class LastPlayback(
    val queue: List<Track>,
    val index: Int,
    val positionMs: Long
) {
    val track: Track? get() = queue.getOrNull(index)
}

class LastPlaybackStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Simpan antrean + indeks lagu aktif. Dipanggil saat antrean berubah. */
    fun saveQueue(queue: List<Track>, index: Int) {
        if (queue.isEmpty() || index !in queue.indices) return
        // Batasi ukuran: ambil jendela 60 lagu di sekitar lagu aktif.
        val start = (index - 10).coerceAtLeast(0)
        val window = queue.drop(start).take(MAX_QUEUE)
        val arr = JSONArray()
        window.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("url", t.url)
                put("title", t.title)
                put("artist", t.artist)
                put("thumbnailUrl", t.thumbnailUrl)
                put("durationSeconds", t.durationSeconds)
            })
        }
        prefs.edit()
            .putString(KEY_QUEUE, arr.toString())
            .putInt(KEY_INDEX, index - start)
            .apply()
    }

    fun savePosition(positionMs: Long) {
        prefs.edit().putLong(KEY_POSITION, positionMs.coerceAtLeast(0L)).apply()
    }

    fun load(): LastPlayback? {
        return try {
            val raw = prefs.getString(KEY_QUEUE, null) ?: return null
            val arr = JSONArray(raw)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Track(
                        id = o.optString("id"),
                        url = o.optString("url"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        thumbnailUrl = o.optString("thumbnailUrl"),
                        durationSeconds = o.optLong("durationSeconds", 0L)
                    )
                )
            }
            val index = prefs.getInt(KEY_INDEX, 0)
            if (list.isEmpty() || index !in list.indices) null
            else LastPlayback(list, index, prefs.getLong(KEY_POSITION, 0L))
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREF_NAME = "nexrify_last_playback"
        private const val KEY_QUEUE = "queue"
        private const val KEY_INDEX = "index"
        private const val KEY_POSITION = "position"
        private const val MAX_QUEUE = 60
    }
}
