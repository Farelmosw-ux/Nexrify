package id.nexrify.music.data.account

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Menyimpan semua data pengguna ke akun: favorit, playlist, riwayat putar, selera, riwayat pencarian,
 * pengaturan, dan status kotak surat. Cara kerjanya memantau berkas SharedPreferences yang dipakai
 * masing masing fitur, jadi fitur lama tidak perlu diubah. Perubahan dikirim ke server setelah jeda singkat.
 */
class AccountSync private constructor(private val context: Context) {

    private class Spec(val file: String, val strings: List<String> = emptyList(), val bools: List<String> = emptyList(), val sets: List<String> = emptyList())

    private val specs = listOf(
        Spec("nexrify_favorites", strings = listOf("fav_list")),
        Spec("nexrify_playlists", strings = listOf("playlist_data")),
        Spec("nexrify_taste_profile", strings = listOf("scores", "plays", "track_data", "recents")),
        Spec("nexrify_search_history", strings = listOf("items")),
        Spec(
            "nexrify_settings",
            strings = listOf("theme_mode", "audio_quality", "app_language"),
            bools = listOf("lyrics_auto_translate", "music_capsule_enabled", "music_capsule_locked")
        ),
        Spec("nexrify_mailbox", sets = listOf("read", "hidden"))
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val meta = context.getSharedPreferences("nexrify_sync", Context.MODE_PRIVATE)
    private val reloadHooks = mutableListOf<() -> Unit>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    @Volatile private var applying = false
    @Volatile private var ready = false
    private var pushJob: Job? = null
    private var lastPush = 0L

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** Dipanggil fitur yang menyimpan datanya di memori, supaya memuat ulang setelah data akun diterapkan. */
    fun addReloadHook(hook: () -> Unit) {
        synchronized(reloadHooks) { reloadHooks.add(hook) }
    }

    /** Mulai memantau perubahan. Aman dipanggil berulang. */
    fun startWatching() {
        if (listeners.isNotEmpty()) return
        specs.forEach { spec ->
            val prefs = context.getSharedPreferences(spec.file, Context.MODE_PRIVATE)
            val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (!applying && key != null && isTracked(spec, key)) markDirty()
            }
            listeners.add(l)
            prefs.registerOnSharedPreferenceChangeListener(l)
        }
    }

    private fun isTracked(spec: Spec, key: String) = key in spec.strings || key in spec.bools || key in spec.sets

    // ------------------------------------------------------------------ sinkron awal

    /**
     * Setelah masuk atau saat aplikasi dibuka. [preferLocal] true untuk akun yang baru dibuat: data di
     * perangkat (misalnya favorit lama) dipindahkan ke akun. Selain itu, data akun di server yang dipakai.
     */
    suspend fun initialSync(preferLocal: Boolean) {
        val token = AccountManager.getInstance().token() ?: return
        _syncing.value = true
        try {
            val (updatedAt, data) = AccountApi.getData(token)
            val dirty = meta.getBoolean(KEY_DIRTY, false)
            when {
                data == null || preferLocal || dirty -> push(force = true)
                updatedAt > meta.getLong(KEY_UPDATED, 0L) || meta.getString(KEY_OWNER, null) != AccountManager.getInstance().userId() -> {
                    apply(data); meta.edit().putLong(KEY_UPDATED, updatedAt).apply()
                }
            }
            meta.edit().putString(KEY_OWNER, AccountManager.getInstance().userId()).apply()
            ready = true
        } catch (_: Exception) {
            // Tanpa internet: pakai data lokal, sinkron lagi di kesempatan berikutnya.
            ready = true
        } finally {
            _syncing.value = false
        }
    }

    // ------------------------------------------------------------------ kirim ke server

    private fun markDirty() {
        meta.edit().putBoolean(KEY_DIRTY, true).apply()
        if (!ready) return
        pushJob?.cancel()
        pushJob = scope.launch {
            val wait = maxOf(6_000L, MIN_INTERVAL_MS - (System.currentTimeMillis() - lastPush))
            delay(wait)
            push()
        }
    }

    private suspend fun push(force: Boolean = false) {
        if (applying && !force) return
        val token = AccountManager.getInstance().token() ?: return
        try {
            val updatedAt = AccountApi.putData(token, collect())
            lastPush = System.currentTimeMillis()
            meta.edit().putBoolean(KEY_DIRTY, false).putLong(KEY_UPDATED, updatedAt).apply()
        } catch (_: Exception) {
            // Dicoba lagi pada perubahan berikutnya atau saat aplikasi dibuka lagi.
        }
    }

    /** Kirim perubahan yang tersisa sekarang juga (dipakai sebelum keluar akun). */
    suspend fun flush() {
        if (meta.getBoolean(KEY_DIRTY, false)) push(force = true)
    }

    private fun collect(): JSONObject {
        fun build(dropKeys: Set<String>): JSONObject {
            val root = JSONObject().put("v", 1)
            val all = JSONObject()
            specs.forEach { spec ->
                val prefs = context.getSharedPreferences(spec.file, Context.MODE_PRIVATE)
                val o = JSONObject()
                spec.strings.forEach { k -> if (k !in dropKeys) prefs.getString(k, null)?.let { o.put(k, it) } }
                spec.bools.forEach { k -> if (prefs.contains(k)) o.put(k, prefs.getBoolean(k, false)) }
                spec.sets.forEach { k -> prefs.getStringSet(k, null)?.let { s -> o.put(k, JSONArray(s.toList())) } }
                all.put(spec.file, o)
            }
            return root.put("prefs", all)
        }
        // Batas server sekitar 900 KB: buang data selera yang paling besar dulu kalau kebanyakan.
        var drop = emptySet<String>()
        var json = build(drop)
        for (step in listOf(setOf("track_data"), setOf("track_data", "plays"), setOf("track_data", "plays", "recents", "scores"))) {
            if (json.toString().length <= MAX_CHARS) break
            drop = step
            json = build(drop)
        }
        return json
    }

    // ------------------------------------------------------------------ terapkan dari server

    private fun apply(data: JSONObject) {
        val all = data.optJSONObject("prefs") ?: return
        applying = true
        try {
            specs.forEach { spec ->
                val o = all.optJSONObject(spec.file) ?: return@forEach
                val edit = context.getSharedPreferences(spec.file, Context.MODE_PRIVATE).edit()
                spec.strings.forEach { k -> if (o.has(k)) edit.putString(k, o.getString(k)) else edit.remove(k) }
                spec.bools.forEach { k -> if (o.has(k)) edit.putBoolean(k, o.getBoolean(k)) else edit.remove(k) }
                spec.sets.forEach { k ->
                    if (o.has(k)) {
                        val a = o.getJSONArray(k)
                        edit.putStringSet(k, HashSet<String>().apply { for (i in 0 until a.length()) add(a.getString(i)) })
                    } else edit.remove(k)
                }
                edit.commit()
            }
        } finally {
            applying = false
        }
        reloadAll()
    }

    /** Hapus semua data lokal milik akun (saat keluar), supaya akun berikutnya tidak tercampur. */
    fun wipeLocal() {
        applying = true
        try {
            specs.forEach { spec ->
                val edit = context.getSharedPreferences(spec.file, Context.MODE_PRIVATE).edit()
                (spec.strings + spec.bools + spec.sets).forEach { edit.remove(it) }
                edit.commit()
            }
            meta.edit().clear().apply()
        } finally {
            applying = false
        }
        ready = false
        reloadAll()
    }

    private fun reloadAll() {
        val hooks = synchronized(reloadHooks) { reloadHooks.toList() }
        main.post { hooks.forEach { runCatching(it) } }
    }

    companion object {
        private const val KEY_DIRTY = "dirty"
        private const val KEY_UPDATED = "updated_at"
        private const val KEY_OWNER = "owner"
        private const val MIN_INTERVAL_MS = 20_000L
        private const val MAX_CHARS = 850_000

        @Volatile private var instance: AccountSync? = null

        fun init(context: Context): AccountSync =
            instance ?: synchronized(this) {
                instance ?: AccountSync(context.applicationContext).also { instance = it }
            }

        fun getInstance(): AccountSync =
            instance ?: throw IllegalStateException("AccountSync belum di inisialisasi")
    }
}
