package id.nexrify.music.data.mailbox

import android.content.Context
import id.nexrify.music.data.remote.MailMessage
import id.nexrify.music.data.remote.NexrifyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kotak surat Nexrify: pesan resmi dari pengembang (info, update, promo, penting) yang diatur lewat panel admin website.
 * Status dibaca dan pesan yang dihapus disimpan di perangkat, pesannya sendiri di-cache supaya tetap terlihat saat offline.
 */
class MailboxManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("nexrify_mailbox", Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow(loadCache())
    private val _readIds = MutableStateFlow(loadSet(KEY_READ))
    private val _hiddenIds = MutableStateFlow(loadSet(KEY_HIDDEN))

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    /** Pesan yang belum dihapus pengguna, yang disematkan di atas lalu yang terbaru. */
    val messages: StateFlow<List<MailMessage>> get() = _visible
    private val _visible = MutableStateFlow(compute())

    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    private val _unread = MutableStateFlow(computeUnread())
    val unreadCount: StateFlow<Int> = _unread.asStateFlow()

    suspend fun refresh() {
        _loading.value = true
        try {
            val fresh = NexrifyApi.fetchMailbox()
            _messages.value = fresh
            prefs.edit().putString(KEY_CACHE, toJson(fresh)).apply()
            _failed.value = false
            publish()
        } catch (_: Exception) {
            _failed.value = true
        } finally {
            _loading.value = false
        }
    }

    fun markRead(id: String) {
        if (id in _readIds.value) return
        _readIds.update { it + id }
        saveSet(KEY_READ, _readIds.value)
        publish()
    }

    fun markAllRead() {
        _readIds.value = _readIds.value + _messages.value.map { it.id }
        saveSet(KEY_READ, _readIds.value)
        publish()
    }

    fun delete(id: String) {
        _hiddenIds.update { it + id }
        saveSet(KEY_HIDDEN, _hiddenIds.value)
        publish()
    }

    private fun publish() {
        _visible.value = compute()
        _unread.value = computeUnread()
    }

    private fun compute(): List<MailMessage> =
        _messages.value.filter { it.id !in _hiddenIds.value }
            .sortedWith(compareByDescending<MailMessage> { it.pinned }.thenByDescending { it.createdAt })

    private fun computeUnread(): Int = compute().count { it.id !in _readIds.value }

    private fun loadSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()

    private fun saveSet(key: String, value: Set<String>) {
        // Simpan paling banyak 500 id supaya penyimpanan tidak membengkak.
        prefs.edit().putStringSet(key, value.toList().takeLast(500).toSet()).apply()
    }

    private fun toJson(list: List<MailMessage>): String {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("type", m.type)
                put("titleId", m.titleId); put("titleEn", m.titleEn)
                put("bodyId", m.bodyId); put("bodyEn", m.bodyEn)
                put("link", m.link); put("pinned", m.pinned); put("createdAt", m.createdAt)
            })
        }
        return arr.toString()
    }

    private fun loadCache(): List<MailMessage> {
        val raw = prefs.getString(KEY_CACHE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MailMessage(
                    id = o.getString("id"), type = o.optString("type", "info"),
                    titleId = o.optString("titleId"), titleEn = o.optString("titleEn"),
                    bodyId = o.optString("bodyId"), bodyEn = o.optString("bodyEn"),
                    link = o.optString("link"), pinned = o.optBoolean("pinned", false),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_CACHE = "cache"
        private const val KEY_READ = "read"
        private const val KEY_HIDDEN = "hidden"

        @Volatile
        private var instance: MailboxManager? = null

        fun init(context: Context): MailboxManager =
            instance ?: synchronized(this) {
                instance ?: MailboxManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): MailboxManager =
            instance ?: throw IllegalStateException("MailboxManager belum di inisialisasi")
    }
}
