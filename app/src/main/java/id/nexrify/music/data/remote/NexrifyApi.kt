package id.nexrify.music.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Alamat resmi Nexrify. Semua fitur online milik Nexrify (rilis, unduhan, kotak surat) lewat domain ini. */
object NexrifyLinks {
    const val SITE = "https://nexrify.biz.id"
    const val API_RELEASE = "$SITE/api/release"
    const val API_MAILBOX = "$SITE/api/mailbox"
    const val DOWNLOAD = "$SITE/download"
    const val HELP = "$SITE/help"
    const val PRIVACY = "$SITE/privacy"
    const val TERMS = "$SITE/terms"
}

/** Informasi rilis terbaru dari website (endpoint detect release). */
data class ReleaseInfo(
    val version: String,
    val versionCode: Long,
    val notesId: String,
    val notesEn: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val publishedAt: Long,
    val mandatory: Boolean
) {
    fun notes(english: Boolean): String =
        (if (english) notesEn.ifBlank { notesId } else notesId.ifBlank { notesEn }).trim()
}

/** Satu surat di kotak surat aplikasi. */
data class MailMessage(
    val id: String,
    val type: String,
    val titleId: String,
    val titleEn: String,
    val bodyId: String,
    val bodyEn: String,
    val link: String,
    val pinned: Boolean,
    val createdAt: Long
) {
    fun title(english: Boolean) = (if (english) titleEn.ifBlank { titleId } else titleId.ifBlank { titleEn })
    fun body(english: Boolean) = (if (english) bodyEn.ifBlank { bodyId } else bodyId.ifBlank { bodyEn })
}

object NexrifyApi {

    private val client by lazy {
        HttpClientProvider.client.newBuilder().callTimeout(12, TimeUnit.SECONDS).build()
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Nexrify-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    /** Rilis terbaru, atau null kalau belum ada rilis yang dipublikasikan. */
    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val json = getJson(NexrifyLinks.API_RELEASE)
        val r = json.optJSONObject("release") ?: return@withContext null
        val notes = r.optJSONObject("notes")
        ReleaseInfo(
            version = r.optString("version").removePrefix("v"),
            versionCode = r.optLong("versionCode", 0L),
            notesId = notes?.optString("id").orEmpty(),
            notesEn = notes?.optString("en").orEmpty(),
            downloadUrl = r.optString("downloadUrl").ifBlank { NexrifyLinks.DOWNLOAD },
            sizeBytes = r.optLong("size", 0L),
            publishedAt = r.optLong("publishedAt", 0L),
            mandatory = r.optBoolean("mandatory", false)
        )
    }

    suspend fun fetchMailbox(): List<MailMessage> = withContext(Dispatchers.IO) {
        val json = getJson(NexrifyLinks.API_MAILBOX)
        val arr = json.optJSONArray("messages") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    MailMessage(
                        id = o.optString("id"),
                        type = o.optString("type", "info"),
                        titleId = o.optString("titleId"),
                        titleEn = o.optString("titleEn"),
                        bodyId = o.optString("bodyId"),
                        bodyEn = o.optString("bodyEn"),
                        link = o.optString("link"),
                        pinned = o.optBoolean("pinned", false),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                )
            }
        }.filter { it.id.isNotBlank() }
    }
}
