package id.nexrify.music.util

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * Pelapor crash sederhana tanpa layanan luar.
 *  - Crash yang mematikan aplikasi disimpan ke berkas, lalu ditampilkan di dialog pada pembukaan berikutnya
 *    supaya pengguna bisa menyalin atau membagikan laporannya ke pengembang.
 *  - Kesalahan di coroutine latar belakang dicatat (tidak mematikan aplikasi) lewat [handler].
 */
object CrashReporter {
    private const val FATAL = "last_crash.txt"
    private const val NON_FATAL = "non_fatal.txt"
    @Volatile private var app: Context? = null

    /** Dipasang pertama kali di Application.onCreate. */
    fun install(context: Context) {
        val appContext = context.applicationContext
        app = appContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { File(appContext.filesDir, FATAL).writeText(report(appContext, thread.name, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Handler untuk scope coroutine tingkat aplikasi: catat saja, jangan matikan aplikasi. */
    val handler = CoroutineExceptionHandler { _, error -> logNonFatal(error) }

    fun logNonFatal(error: Throwable) {
        val context = app ?: return
        runCatching {
            val file = File(context.filesDir, NON_FATAL)
            val old = if (file.exists() && file.length() < 20_000) file.readText() else ""
            file.writeText(old + report(context, Thread.currentThread().name, error) + "\n----\n")
        }
    }

    fun pending(context: Context): String? =
        runCatching { File(context.filesDir, FATAL).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FATAL).delete() }
    }

    private fun report(context: Context, thread: String, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString().take(6000)
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        return buildString {
            appendLine("Nexrify $version")
            appendLine("Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: $thread")
            appendLine("Waktu: ${Date()}")
            appendLine()
            append(trace)
        }
    }
}
