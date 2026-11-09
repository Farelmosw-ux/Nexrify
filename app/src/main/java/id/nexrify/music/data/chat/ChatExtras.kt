package id.nexrify.music.data.chat

import org.json.JSONArray
import org.json.JSONObject

/** Satu orang di daftar (pemberi reaksi, pengguna yang diblokir). */
data class ChatPerson(val uid: String, val username: String, val display: String, val av: String)

data class ReactionGroup(val emoji: String, val users: List<ChatPerson>)

data class ChatThread(val root: ChatMessage, val replies: List<ChatMessage>)

data class ChatPrivacy(val hidePresence: Boolean = false, val hideProfile: Boolean = false, val hideMusic: Boolean = false)

data class ChatMyData(val privacy: ChatPrivacy, val blocked: List<ChatPerson>, val warns: Int)

data class ModReport(val id: String, val seq: Long, val by: String, val author: String, val text: String, val reason: String, val ts: Long)

/** Status: open, planned, progress, done, rejected. kind: feature atau bug. */
data class FeedbackItem(
    val id: String, val kind: String, val title: String, val body: String, val status: String, val note: String,
    val votes: Int, val createdAt: Long, val mine: Boolean, val voted: Boolean, val author: String, val device: String
)

/** Panggilan API untuk fitur chat lanjutan. Semua melempar AccountError kalau gagal. */
object ChatXApi {

    private fun people(arr: JSONArray?): List<ChatPerson> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ChatPerson(o.optString("uid"), o.optString("username"), o.optString("display"), o.optString("av")))
            }
        }
    }

    private fun messages(arr: JSONArray?): List<ChatMessage> {
        if (arr == null) return emptyList()
        return buildList { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ChatApi.parseMessage(it)) } }
    }

    suspend fun search(token: String, q: String, user: String, from: Long, to: Long): List<ChatMessage> {
        val p = HashMap<String, String>()
        p["search"] = "1"
        if (q.isNotBlank()) p["q"] = q
        if (user.isNotBlank()) p["user"] = user
        if (from > 0) p["from"] = from.toString()
        if (to > 0) p["to"] = to.toString()
        return messages(ChatApi.get(token, p).optJSONArray("results"))
    }

    suspend fun thread(token: String, seq: Long): ChatThread {
        val j = ChatApi.get(token, mapOf("thread" to seq.toString()))
        return ChatThread(ChatApi.parseMessage(j.getJSONObject("root")), messages(j.optJSONArray("replies")))
    }

    suspend fun reactors(token: String, seq: Long): List<ReactionGroup> {
        val arr = ChatApi.get(token, mapOf("reactors" to seq.toString())).optJSONArray("reactions") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ReactionGroup(o.optString("emoji"), people(o.optJSONArray("users"))))
            }
        }
    }

    suspend fun translate(token: String, seq: Long, to: String): String =
        ChatApi.post(token, JSONObject().put("action", "translate").put("seq", seq).put("to", to)).optString("text")

    suspend fun myVotes(token: String): Map<Long, Int> {
        val o = ChatApi.get(token, mapOf("myvotes" to "1")).optJSONObject("votes") ?: return emptyMap()
        val out = HashMap<Long, Int>()
        o.keys().forEach { k -> k.toLongOrNull()?.let { out[it] = o.optInt(k, -1) } }
        return out
    }

    suspend fun pollVote(token: String, seq: Long, index: Int): ChatMessage =
        ChatApi.parseMessage(ChatApi.post(token, JSONObject().put("action", "poll_vote").put("seq", seq).put("index", index)).getJSONObject("message"))

    suspend fun pollCreate(token: String, question: String, options: List<String>, hours: Int): ChatMessage {
        val arr = JSONArray(); options.forEach { arr.put(it) }
        val body = JSONObject().put("action", "poll_create").put("question", question).put("options", arr).put("hours", hours)
        return ChatApi.parseMessage(ChatApi.post(token, body).getJSONObject("message"))
    }

    suspend fun pollClose(token: String, seq: Long): ChatMessage =
        ChatApi.parseMessage(ChatApi.post(token, JSONObject().put("action", "poll_close").put("seq", seq)).getJSONObject("message"))

    suspend fun pin(token: String, seq: Long, on: Boolean): ChatMessage =
        ChatApi.parseMessage(ChatApi.post(token, JSONObject().put("action", "pin").put("seq", seq).put("on", on)).getJSONObject("message"))

    suspend fun seen(token: String, seq: Long) { ChatApi.post(token, JSONObject().put("action", "seen").put("seq", seq)) }

    private fun parseMy(j: JSONObject): ChatMyData {
        val p = j.optJSONObject("privacy")
        return ChatMyData(
            ChatPrivacy(p?.optBoolean("hidePresence") ?: false, p?.optBoolean("hideProfile") ?: false, p?.optBoolean("hideMusic") ?: false),
            people(j.optJSONArray("blocked")), j.optInt("warns")
        )
    }

    suspend fun myChat(token: String): ChatMyData = parseMy(ChatApi.get(token, mapOf("mychat" to "1")))

    suspend fun setPrivacy(token: String, p: ChatPrivacy): ChatMyData = parseMy(
        ChatApi.post(token, JSONObject().put("action", "privacy").put("hidePresence", p.hidePresence).put("hideProfile", p.hideProfile).put("hideMusic", p.hideMusic))
    )

    suspend fun block(token: String, username: String, on: Boolean): ChatMyData =
        parseMy(ChatApi.post(token, JSONObject().put("action", "block").put("username", username).put("on", on)))

    suspend fun modDelete(token: String, seq: Long) { ChatApi.post(token, JSONObject().put("action", "mod_delete").put("seq", seq)) }

    /** Mengembalikan jumlah peringatan pengguna itu setelah ditambah. */
    suspend fun modWarn(token: String, username: String, reason: String): Int =
        ChatApi.post(token, JSONObject().put("action", "mod_warn").put("username", username).put("reason", reason)).optInt("warns")

    suspend fun modMute(token: String, username: String, hours: Int) {
        ChatApi.post(token, JSONObject().put("action", "mod_mute").put("username", username).put("hours", hours))
    }

    suspend fun modUnmute(token: String, username: String) { ChatApi.post(token, JSONObject().put("action", "mod_unmute").put("username", username)) }

    suspend fun modReports(token: String): List<ModReport> {
        val arr = ChatApi.get(token, mapOf("modreports" to "1")).optJSONArray("reports") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ModReport(o.optString("id"), o.optLong("seq"), o.optString("by"), o.optString("author"), o.optString("text"), o.optString("reason"), o.optLong("ts")))
            }
        }
    }

    suspend fun modReportClear(token: String, id: String) { ChatApi.post(token, JSONObject().put("action", "mod_report_clear").put("id", id)) }

    /* ---- saran dan bug ---- */
    private fun parseFb(o: JSONObject) = FeedbackItem(
        o.optString("id"), o.optString("kind"), o.optString("title"), o.optString("body"), o.optString("status"), o.optString("note"),
        o.optInt("votes"), o.optLong("createdAt"), o.optBoolean("mine"), o.optBoolean("voted"), o.optString("author"), o.optString("device")
    )

    suspend fun feedbackList(token: String): List<FeedbackItem> {
        val arr = ChatApi.get(token, mapOf("fb" to "1")).optJSONArray("items") ?: return emptyList()
        return buildList { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(parseFb(it)) } }
    }

    suspend fun feedbackSubmit(token: String, kind: String, title: String, body: String, device: String): FeedbackItem =
        parseFb(ChatApi.post(token, JSONObject().put("action", "fb_submit").put("kind", kind).put("title", title).put("body", body).put("device", device)).getJSONObject("item"))

    suspend fun feedbackVote(token: String, id: String): FeedbackItem =
        parseFb(ChatApi.post(token, JSONObject().put("action", "fb_vote").put("id", id)).getJSONObject("item"))

    suspend fun feedbackDelete(token: String, id: String) { ChatApi.post(token, JSONObject().put("action", "fb_delete").put("id", id)) }
}
