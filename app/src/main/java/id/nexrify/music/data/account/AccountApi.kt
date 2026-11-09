package id.nexrify.music.data.account

import id.nexrify.music.data.remote.HttpClientProvider
import id.nexrify.music.data.remote.NexrifyLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Kesalahan dari server akun. [code] dipetakan ke teks sesuai bahasa di UI. */
class AccountError(val code: String, message: String = code) : Exception(message)

/** Klien untuk akun Nexrify di nexrify.biz.id: /api/auth dan /api/user. */
object AccountApi {

    private val client by lazy { HttpClientProvider.client.newBuilder().callTimeout(25, TimeUnit.SECONDS).build() }
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun execute(request: Request): JSONObject {
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                    ?: throw AccountError("server", "HTTP ${response.code}")
                if (!json.optBoolean("ok", false)) {
                    throw AccountError(json.optString("code", "server"), json.optString("error", "HTTP ${response.code}"))
                }
                return json
            }
        } catch (e: AccountError) {
            throw e
        } catch (e: IOException) {
            throw AccountError("network", e.message ?: "network")
        }
    }

    private fun builder(path: String, token: String?): Request.Builder =
        Request.Builder().url(NexrifyLinks.SITE + path)
            .header("Accept", "application/json")
            .header("User-Agent", "Nexrify-Android")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }

    suspend fun auth(body: JSONObject, token: String? = null): JSONObject = withContext(Dispatchers.IO) {
        execute(builder("/api/auth", token).post(body.toString().toRequestBody(JSON_TYPE)).build())
    }

    /** Data akun di server. updatedAt 0 dan data null kalau belum pernah disimpan. */
    suspend fun getData(token: String): Pair<Long, JSONObject?> = withContext(Dispatchers.IO) {
        val json = execute(builder("/api/user", token).get().build())
        json.optLong("updatedAt", 0L) to json.optJSONObject("data")
    }

    suspend fun putData(token: String, data: JSONObject): Long = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("data", data)
        execute(builder("/api/user", token).put(payload.toString().toRequestBody(JSON_TYPE)).build()).optLong("updatedAt", 0L)
    }
}
