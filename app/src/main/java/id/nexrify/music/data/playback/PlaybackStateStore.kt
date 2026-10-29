package id.nexrify.music.data.playback

import android.content.Context
import id.nexrify.music.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * Menyimpan antrian, lagu yang sedang diputar, posisi, serta mode acak dan ulang,
 * supaya setelah aplikasi ditutup atau HP restart Nexrify bisa lanjut dari terakhir.
 * Alamat stream tidak disimpan karena kedaluwarsa, jadi selalu diambil ulang saat lagu diputar lagi.
 */
class PlaybackStateStore(context: Context) {

    data class Snapshot(
        val queue: List<Track>,
        val index: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeatMode: Int
    )

    private val prefs = context.applicationContext.getSharedPreferences("nexrify_playback_state", Context.MODE_PRIVATE)

    fun save(snapshot: Snapshot) {
        if (snapshot.queue.isEmpty()) return
        // Simpan jendela di sekitar lagu aktif saja agar ringan.
        val from = (snapshot.index - KEEP_BEFORE).coerceAtLeast(0)
        val to = (snapshot.index + KEEP_AFTER + 1).coerceAtMost(snapshot.queue.size)
        val window = snapshot.queue.subList(from, to)
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
            .putInt(KEY_INDEX, (snapshot.index - from).coerceAtLeast(0))
            .putLong(KEY_POSITION, snapshot.positionMs.coerceAtLeast(0L))
            .putBoolean(KEY_SHUFFLE, snapshot.shuffle)
            .putInt(KEY_REPEAT, snapshot.repeatMode)
            .apply()
    }

    fun load(): Snapshot? {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return null
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<Track>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Track(
                        id = o.getString("id"),
                        url = o.getString("url"),
                        title = o.getString("title"),
                        artist = o.getString("artist"),
                        thumbnailUrl = o.optString("thumbnailUrl", ""),
                        durationSeconds = o.optLong("durationSeconds", 0L)
                    )
                )
            }
            if (list.isEmpty()) null
            else Snapshot(
                queue = list,
                index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, list.lastIndex),
                positionMs = prefs.getLong(KEY_POSITION, 0L),
                shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
                repeatMode = prefs.getInt(KEY_REPEAT, 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_QUEUE = "queue"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
        const val KEEP_BEFORE = 10
        const val KEEP_AFTER = 70
    }
}
