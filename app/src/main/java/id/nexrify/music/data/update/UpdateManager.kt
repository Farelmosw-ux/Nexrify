package id.nexrify.music.data.update

import android.content.Context
import id.nexrify.music.data.remote.NexrifyApi
import id.nexrify.music.data.remote.ReleaseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deteksi rilis baru lewat website Nexrify. Kalau ada versi lebih baru dan pengguna belum menekan "Nanti"
 * dalam 12 jam terakhir, [available] terisi dan dialog update tampil.
 */
class UpdateManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("nexrify_update", Context.MODE_PRIVATE)

    private val _available = MutableStateFlow<ReleaseInfo?>(null)
    val available: StateFlow<ReleaseInfo?> = _available.asStateFlow()

    fun installedVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "0.0.0" }

    @Suppress("DEPRECATION")
    private fun installedCode(): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }.getOrDefault(0L)

    fun isNewer(release: ReleaseInfo): Boolean {
        if (release.versionCode > 0L && release.versionCode != installedCode()) {
            return release.versionCode > installedCode()
        }
        return compareVersions(installedVersion(), release.version) < 0
    }

    /** Ambil rilis terbaru dari website. */
    suspend fun fetchLatest(): Result<ReleaseInfo?> = runCatching { NexrifyApi.fetchLatestRelease() }

    /** Dipanggil saat aplikasi dibuka. Diam saja kalau gagal, tidak mengganggu pengguna. */
    suspend fun checkOnLaunch() {
        val release = fetchLatest().getOrNull() ?: return
        if (!isNewer(release)) {
            _available.value = null
            return
        }
        if (!release.mandatory && snoozed(release)) return
        _available.value = release
    }

    /** Pengguna menekan "Nanti": dialog tidak muncul lagi untuk versi ini selama 12 jam. */
    fun later(release: ReleaseInfo) {
        prefs.edit()
            .putString(KEY_VERSION, release.version)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
        _available.value = null
    }

    fun dismissWithoutSnooze() {
        _available.value = null
    }

    private fun snoozed(release: ReleaseInfo): Boolean {
        val v = prefs.getString(KEY_VERSION, null) ?: return false
        if (v != release.version) return false
        return System.currentTimeMillis() - prefs.getLong(KEY_TIME, 0L) < SNOOZE_MS
    }

    companion object {
        private const val KEY_VERSION = "snoozed_version"
        private const val KEY_TIME = "snoozed_at"
        private const val SNOOZE_MS = 12L * 60L * 60L * 1000L

        @Volatile
        private var instance: UpdateManager? = null

        fun init(context: Context): UpdateManager =
            instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): UpdateManager =
            instance ?: throw IllegalStateException("UpdateManager belum di inisialisasi")

        fun compareVersions(current: String, latest: String): Int {
            val a = current.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val b = latest.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val left = a.getOrElse(i) { 0 }
                val right = b.getOrElse(i) { 0 }
                if (left != right) return left.compareTo(right)
            }
            return 0
        }
    }
}
