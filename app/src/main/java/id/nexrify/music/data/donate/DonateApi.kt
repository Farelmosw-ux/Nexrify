package id.nexrify.music.data.donate

import android.util.Base64
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.remote.HttpClientProvider
import id.nexrify.music.data.remote.NexrifyLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Klien /api/donate di nexrify.biz.id. */
object DonateApi {

    private val client by lazy { HttpClientProvider.client.newBuilder().callTimeout(45, TimeUnit.SECONDS).build() }
    /** Untuk kirim bukti: server perlu waktu menyusun potongan lalu mengunggah ke Telegram. */
    private val slowClient by lazy { HttpClientProvider.client.newBuilder().callTimeout(120, TimeUnit.SECONDS).build() }
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val PATH = "/api/donate"

    fun qrisUrl(): String = "${NexrifyLinks.SITE}$PATH?qris=1"

    private fun execute(request: Request, http: okhttp3.OkHttpClient = client): JSONObject {
        try {
            http.newCall(request).execute().use { response ->
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

    private suspend fun get(token: String, params: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val url = (NexrifyLinks.SITE + PATH).toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        execute(
            Request.Builder().url(url).get()
                .header("Accept", "application/json").header("User-Agent", "Nexrify-Android")
                .header("Authorization", "Bearer $token").build()
        )
    }

    private suspend fun post(token: String, body: JSONObject, http: okhttp3.OkHttpClient = client): JSONObject = withContext(Dispatchers.IO) {
        execute(
            Request.Builder().url(NexrifyLinks.SITE + PATH).post(body.toString().toRequestBody(JSON_TYPE))
                .header("Accept", "application/json").header("User-Agent", "Nexrify-Android")
                .header("Authorization", "Bearer $token").build(),
            http
        )
    }

    suspend fun home(token: String): DonateHome {
        val j = get(token, mapOf("config" to "1"))
        return DonateHome(parseConfig(j.getJSONObject("config")), parseTop(j.optJSONArray("top")), j.optInt("myRank"), j.optBoolean("titleOn", true))
    }

    suspend fun top(token: String): List<DonorTop> = parseTop(get(token, mapOf("top" to "1")).optJSONArray("top"))

    suspend fun history(token: String): List<Donation> {
        val arr = get(token, mapOf("history" to "1")).optJSONArray("orders") ?: return emptyList()
        return buildList { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(parseDonation(it)) } }
    }

    suspend fun create(token: String, method: String, amount: Long, sender: String, note: String): Donation {
        val body = JSONObject().put("action", "create").put("method", method).put("amount", amount).put("sender", sender).put("note", note)
        return parseDonation(post(token, body).getJSONObject("order"))
    }

    /**
     * Kirim bukti apa saja (foto, video, PDF, dll). File dipotong 384 KB per bagian supaya muat di batas
     * ukuran permintaan Vercel, lalu server menyusunnya dan meneruskan ke Telegram owner.
     */
    suspend fun submit(token: String, id: String, file: NexProofFile, onProgress: (Int) -> Unit = {}): Donation {
        val size = file.bytes.size
        val total = (size + ProofReader.CHUNK - 1) / ProofReader.CHUNK
        onProgress(0)
        for (i in 0 until total) {
            val from = i * ProofReader.CHUNK
            val len = minOf(ProofReader.CHUNK, size - from)
            val body = JSONObject().put("action", "proof_chunk").put("id", id).put("index", i).put("total", total)
                .put("data", Base64.encodeToString(file.bytes, from, len, Base64.NO_WRAP))
            try {
                post(token, body, slowClient)
            } catch (e: AccountError) {
                if (e.code != "network") throw e
                post(token, body, slowClient)
            }
            onProgress(((i + 1) * 90) / total)
        }
        val body = JSONObject().put("action", "submit").put("id", id).put("chunks", total).put("name", file.name).put("mime", file.mime)
        val order = parseDonation(post(token, body, slowClient).getJSONObject("order"))
        onProgress(100)
        return order
    }

    suspend fun cancel(token: String, id: String): Donation =
        parseDonation(post(token, JSONObject().put("action", "cancel").put("id", id)).getJSONObject("order"))

    suspend fun delete(token: String, id: String) { post(token, JSONObject().put("action", "delete").put("id", id)) }

    suspend fun deleteFinished(token: String) { post(token, JSONObject().put("action", "delete_finished")) }

    suspend fun setTitle(token: String, on: Boolean) { post(token, JSONObject().put("action", "title").put("on", on)) }

    private fun parseTop(arr: JSONArray?): List<DonorTop> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    DonorTop(
                        rank = o.optInt("rank"), uid = o.optString("uid"), username = o.optString("username"),
                        display = o.optString("display"), av = o.optString("av"), total = o.optLong("total"),
                        titleOn = o.optBoolean("titleOn", true)
                    )
                )
            }
        }
    }

    private fun parseConfig(o: JSONObject): DonateConfig {
        val presets = o.optJSONArray("presets")
        val dana = o.optJSONObject("dana")
        return DonateConfig(
            enabled = o.optBoolean("enabled", true), min = o.optLong("min", 1000), max = o.optLong("max", 10_000_000),
            presets = buildList { if (presets != null) for (i in 0 until presets.length()) add(presets.optLong(i)) },
            thanks = o.optString("thanks"), danaName = dana?.optString("name").orEmpty(), danaNumber = dana?.optString("number").orEmpty(),
            danaReady = dana?.optBoolean("ready", false) ?: false,
            qrisPage = o.optJSONObject("qris")?.optString("page").orEmpty()
        )
    }

    private fun parseDonation(o: JSONObject) = Donation(
        id = o.optString("id"), method = o.optString("method"), amount = o.optLong("amount"), status = o.optString("status"),
        sender = o.optString("sender"), note = o.optString("note"), createdAt = o.optLong("createdAt"),
        submittedAt = o.optLong("submittedAt"), reviewedAt = o.optLong("reviewedAt")
    )
}
