package id.nexrify.music.data.remote

import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException

data class SongCommentNotif(val type: String, val song: String, val commentId: String, val from: String, val text: String, val ts: Long)
data class SongComment(val id: String, val uid: String, val username: String, val display: String, val text: String, val ts: Long, val parentId: String, val likes: Int, val liked: Boolean, val edited: Boolean)
object SongCommentsApi {
    private val client by lazy { HttpClientProvider.client }
    private val media = "application/json; charset=utf-8".toMediaType()
    private fun token() = AccountManager.getInstance().token() ?: throw AccountError("auth", "Silakan masuk untuk berkomentar")
    private suspend fun call(request: Request): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { r ->
            val j = runCatching { JSONObject(r.body?.string().orEmpty()) }.getOrNull() ?: throw AccountError("server", "Respons server tidak valid")
            if (!r.isSuccessful || !j.optBoolean("ok")) throw AccountError(j.optString("code", "server"), j.optString("error", "Permintaan gagal"))
            j
        }
    }
    suspend fun notifications(since: Long = 0L): Pair<Long, List<SongCommentNotif>> {
        val url = (NexrifyLinks.SITE + "/api/song-comments").toHttpUrl().newBuilder().addQueryParameter("notifications", "1").addQueryParameter("since", since.toString()).build()
        val j = call(Request.Builder().url(url).get().header("Authorization", "Bearer ${token()}").build())
        val a = j.optJSONArray("items")
        val items = buildList { if (a != null) for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(SongCommentNotif(it.optString("type"), it.optString("song"), it.optString("commentId"), it.optString("from"), it.optString("text"), it.optLong("ts"))) } }
        return j.optLong("now", System.currentTimeMillis()) to items
    }
    suspend fun list(song: String): List<SongComment> {
        val url = (NexrifyLinks.SITE + "/api/song-comments").toHttpUrl().newBuilder().addQueryParameter("song", song).build()
        val j = call(Request.Builder().url(url).get().header("Authorization", "Bearer ${token()}").build())
        val a = j.optJSONArray("comments") ?: return emptyList()
        return buildList { for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(parse(it)) } }
    }
    suspend fun send(song: String, text: String, parentId: String = ""): SongComment {
        val b = JSONObject().put("action", "send").put("song", song).put("text", text).put("parentId", parentId)
        return parse(call(post(b)).getJSONObject("comment"))
    }
    suspend fun like(song: String, id: String): Pair<Boolean, Int> {
        val j = call(post(JSONObject().put("action", "like").put("song", song).put("id", id)))
        return j.optBoolean("liked") to j.optInt("likes")
    }
    suspend fun delete(song: String, id: String) { call(post(JSONObject().put("action", "delete").put("song", song).put("id", id))) }
    private fun post(b: JSONObject) = Request.Builder().url(NexrifyLinks.SITE + "/api/song-comments").post(b.toString().toRequestBody(media)).header("Authorization", "Bearer ${token()}").build()
    private fun parse(o: JSONObject) = SongComment(o.optString("id"), o.optString("uid"), o.optString("username"), o.optString("display"), o.optString("text"), o.optLong("ts"), o.optString("parentId"), o.optInt("likes"), o.optBoolean("liked"), o.optBoolean("edited"))
}
