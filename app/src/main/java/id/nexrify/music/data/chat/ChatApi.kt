package id.nexrify.music.data.chat

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

/** Klien /api/chat di nexrify.biz.id. */
object ChatApi {

    private val client by lazy { HttpClientProvider.client.newBuilder().callTimeout(15, TimeUnit.SECONDS).build() }
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val PATH = "/api/chat"

    fun avatarUrl(uid: String, av: String): String = "${NexrifyLinks.SITE}$PATH?avatar=$uid&v=$av"

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

    private suspend fun get(token: String, params: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val url = (NexrifyLinks.SITE + PATH).toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        execute(
            Request.Builder().url(url).get()
                .header("Accept", "application/json").header("User-Agent", "Nexrify-Android")
                .header("Authorization", "Bearer $token").build()
        )
    }

    suspend fun post(token: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        execute(
            Request.Builder().url(NexrifyLinks.SITE + PATH).post(body.toString().toRequestBody(JSON_TYPE))
                .header("Accept", "application/json").header("User-Agent", "Nexrify-Android")
                .header("Authorization", "Bearer $token").build()
        )
    }

    suspend fun feed(token: String, ev: Long): ChatFeed {
        val j = get(token, mapOf("feed" to "1", "ev" to ev.toString()))
        val typing = j.optJSONArray("typing").toStringList()
        val arr = j.optJSONArray("messages") ?: JSONArray()
        val msgs = buildList { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(parseMessage(it)) } }
        return ChatFeed(j.optLong("ev", ev), j.optBoolean("reset", false), typing, msgs)
    }

    suspend fun notifs(token: String, since: Long): Pair<Long, List<ChatNotif>> {
        val j = get(token, mapOf("notifs" to "1", "since" to since.toString()))
        val arr = j.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ChatNotif(o.optString("k"), o.optLong("seq"), o.optString("from"), o.optString("text"), o.optLong("ts")))
            }
        }
        return j.optLong("now", System.currentTimeMillis()) to items
    }

    suspend fun profile(token: String, username: String): ChatProfile =
        parseProfile(get(token, mapOf("profile" to username)).getJSONObject("profile"))

    suspend fun users(token: String, prefix: String): List<ChatUserHit> {
        val arr = get(token, mapOf("users" to prefix)).optJSONArray("users") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ChatUserHit(o.optString("username"), o.optString("uid"), o.optString("display")))
            }
        }
    }

    fun parseProfile(o: JSONObject): ChatProfile {
        val top = o.optJSONArray("top")
        return ChatProfile(
            uid = o.optString("uid"), username = o.optString("username"), display = o.optString("display"),
            av = o.optString("av"), joined = o.optLong("joined"), bio = o.optString("bio"), status = o.optString("status"),
            messages = o.optInt("messages"), reactionsReceived = o.optInt("reactionsReceived"),
            liked = o.optInt("liked"), playlists = o.optInt("playlists"),
            top = buildList {
                if (top != null) for (i in 0 until top.length()) top.optJSONObject(i)?.let { add(ChatTopTrack(it.optString("t"), it.optString("a"))) }
            },
            badges = o.optJSONArray("badges").toStringList()
        )
    }

    fun parseMessage(o: JSONObject): ChatMessage {
        val rx = o.optJSONObject("reactions")
        val reactions = buildMap<String, List<String>> {
            if (rx != null) rx.keys().forEach { k -> put(k, rx.optJSONArray(k).toStringList()) }
        }
        val r = o.optJSONObject("reply")
        return ChatMessage(
            seq = o.optLong("seq"), uid = o.optString("uid"), username = o.optString("username"), display = o.optString("display"),
            badges = o.optJSONArray("badges").toStringList(), av = o.optString("av"), text = o.optString("text"),
            ts = o.optLong("ts"), edited = o.optLong("edited"), deleted = o.optBoolean("deleted"),
            announce = o.optBoolean("announce"), mentions = o.optJSONArray("mentions").toStringList(),
            reply = r?.let { ChatReply(it.optLong("seq"), it.optString("username"), it.optString("text")) },
            reactions = reactions
        )
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else buildList { for (i in 0 until length()) add(optString(i)) }
}
