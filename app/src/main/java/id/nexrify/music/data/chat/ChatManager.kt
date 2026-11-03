package id.nexrify.music.data.chat

import android.content.Context
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.util.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

enum class ChatConnection { CONNECTING, ONLINE, OFFLINE }

/** Permintaan membuka Global Chat dari luar (notifikasi HP). seq > 0 berarti sorot pesan itu. */
object ChatNav {
    val request = MutableStateFlow<Long?>(null)
    /** Buka DM dari notifikasi: username lawan bicara, atau string kosong untuk daftar pesan. */
    val dmRequest = MutableStateFlow<String?>(null)
}

/**
 * Mesin Global Chat. Real-time lewat polling pendek ke /api/chat (hemat kuota Upstash gratis):
 * 3 detik saat ramai, melambat otomatis saat sepi, berhenti total ketika layar chat ditutup.
 */
class ChatManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CrashReporter.handler)
    val settings = ChatSettings.getInstance()

    private val server = MutableStateFlow<Map<Long, ChatMessage>>(emptyMap())
    private val pending = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val pendingData = HashMap<String, Pair<String, Long>>() // localId → (teks asli, seq balasan)
    private var localCounter = 0L

    /** Daftar pesan siap tampil: urut lama ke baru, pesan dari pengguna yang di-mute disembunyikan. */
    val messages: StateFlow<List<ChatMessage>> = combine(server, pending, settings.muted) { s, p, muted ->
        s.values.filter { it.announce || it.username.lowercase() !in muted }.sortedBy { it.seq } + p
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _typing = MutableStateFlow<List<String>>(emptyList())
    val typing: StateFlow<List<String>> = _typing.asStateFlow()
    private val _connection = MutableStateFlow(ChatConnection.CONNECTING)
    val connection: StateFlow<ChatConnection> = _connection.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    /** Sudah pernah berhasil memuat riwayat (membedakan "kosong" dari "belum dimuat"). */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _me = MutableStateFlow<ChatProfile?>(null)
    val me: StateFlow<ChatProfile?> = _me.asStateFlow()
    private val _mentionUnread = MutableStateFlow(0)
    /** Jumlah mention/balasan yang belum dilihat (badge di tombol chat). */
    val mentionUnread: StateFlow<Int> = _mentionUnread.asStateFlow()
    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Kode kesalahan dari server untuk ditampilkan UI (banned, rate_limited, duplicate, ...). */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    @Volatile private var ev = 0L
    @Volatile private var maxSeen = 0L
    @Volatile private var firstLoad = true
    @Volatile var isActive = false
        private set
    private var pollJob: Job? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var lastTypingSent = 0L

    private fun token(): String? = AccountManager.getInstance().token()
    private fun myName(): String = AccountManager.getInstance().user.value?.username.orEmpty()
    private fun myId(): String = AccountManager.getInstance().user.value?.id.orEmpty()

    /* ---------------- polling ---------------- */

    fun setActive(active: Boolean) {
        if (active == isActive) return
        isActive = active
        if (active) {
            _mentionUnread.value = 0
            scope.launch { runCatching { join() } }
            pollJob?.cancel()
            pollJob = scope.launch { pollLoop() }
        } else {
            pollJob?.cancel()
            pollJob = null
            _typing.value = emptyList()
        }
    }

    private fun kick() { wake.trySend(Unit) }

    private suspend fun pollLoop() {
        var idle = 0
        var failures = 0
        while (isActive) {
            val t = token() ?: break
            var changed = false
            try {
                val feed = ChatApi.feed(t, if (firstLoad) 0L else ev)
                changed = apply(feed)
                _connection.value = ChatConnection.ONLINE
                _loaded.value = true
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountError) {
                failures++
                _connection.value = ChatConnection.OFFLINE
                if (e.code == "auth") break
            } catch (e: Throwable) {
                failures++
                _connection.value = ChatConnection.OFFLINE
                CrashReporter.logNonFatal(e)
            }
            idle = if (changed) 0 else idle + 1
            val wait = when {
                failures > 0 -> minOf(30_000L, 3_000L * failures)
                idle < 8 -> 3_000L
                idle < 20 -> 6_000L
                else -> 10_000L
            }
            if (withTimeoutOrNull(wait) { wake.receive() } != null) idle = 0
        }
    }

    /** Terapkan hasil polling. Mengembalikan true kalau ada pesan atau pengetik yang berubah. */
    private fun apply(feed: ChatFeed): Boolean {
        var changed = feed.messages.isNotEmpty() || feed.typing != _typing.value
        val me = myName()
        val newOnes = ArrayList<ChatMessage>()
        server.update { old ->
            val map = if (feed.reset) LinkedHashMap<Long, ChatMessage>() else LinkedHashMap(old)
            for (m in feed.messages) {
                if (m.seq > maxSeen && !firstLoad) newOnes.add(m)
                map[m.seq] = m
            }
            map
        }
        if (feed.messages.isNotEmpty()) maxSeen = maxOf(maxSeen, feed.messages.maxOf { it.seq })
        ev = feed.ev
        firstLoad = false
        _typing.value = feed.typing.filter { it.lowercase() !in settings.muted.value }
        for (m in newOnes) {
            if (m.username == me || m.deleted || m.username.lowercase() in settings.muted.value) continue
            val kind = when {
                m.announce -> "announce"
                me.isNotEmpty() && m.mentions.any { it.equals(me, true) } -> "mention"
                m.reply?.username.equals(me, true) -> "reply"
                else -> null
            }
            if (kind != null) _events.tryEmit(ChatEvent(kind, m))
        }
        if (newOnes.isNotEmpty()) changed = true
        return changed
    }

    /* ---------------- kirim, edit, hapus, reaksi ---------------- */

    fun send(text: String, replyTo: ChatMessage?) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val user = AccountManager.getInstance().user.value ?: return
        localCounter++
        val localId = "L$localCounter"
        val shown = ChatFilter.censor(clean.take(CHAT_MAX_LENGTH))
        val msg = ChatMessage(
            seq = -localCounter, uid = user.id, username = user.username, display = user.display, badges = _me.value?.badges.orEmpty(),
            av = _me.value?.av.orEmpty(), text = shown, ts = System.currentTimeMillis(), edited = 0, deleted = false, announce = false,
            mentions = emptyList(), reply = replyTo?.let { ChatReply(it.seq, it.username, it.text.take(100)) }, reactions = emptyMap(),
            status = SendStatus.SENDING, localId = localId
        )
        pendingData[localId] = clean to (replyTo?.seq ?: 0L)
        pending.update { it + msg }
        scope.launch { deliver(localId) }
    }

    fun retry(localId: String) {
        pending.update { list -> list.map { if (it.localId == localId) it.copy(status = SendStatus.SENDING) else it } }
        scope.launch { deliver(localId) }
    }

    fun discardPending(localId: String) {
        pendingData.remove(localId)
        pending.update { list -> list.filterNot { it.localId == localId } }
    }

    private suspend fun deliver(localId: String) {
        val (text, replySeq) = pendingData[localId] ?: return
        val t = token() ?: return
        try {
            val body = JSONObject().put("action", "send").put("text", text)
            if (replySeq > 0) body.put("replyTo", replySeq)
            val r = ChatApi.post(t, body)
            val m = ChatApi.parseMessage(r.getJSONObject("message"))
            pendingData.remove(localId)
            server.update { it + (m.seq to m) }
            maxSeen = maxOf(maxSeen, m.seq)
            pending.update { list -> list.filterNot { it.localId == localId } }
            kick()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountError) {
            // Kesalahan permanen dari server (dibatasi, duplikat, kena batas kecepatan): buang, bukan kirim ulang.
            if (e.code == "chat_banned" || e.code == "duplicate" || e.code == "rate_limited" || e.code == "empty") {
                discardPending(localId)
                _errors.tryEmit(e.code)
            } else {
                markFailed(localId)
            }
        } catch (e: Throwable) {
            markFailed(localId)
        }
    }

    private fun markFailed(localId: String) {
        pending.update { list -> list.map { if (it.localId == localId) it.copy(status = SendStatus.FAILED) else it } }
    }

    suspend fun edit(seq: Long, text: String): Boolean = simple(JSONObject().put("action", "edit").put("seq", seq).put("text", text.trim()), true)

    suspend fun delete(seq: Long): Boolean {
        val ok = simple(JSONObject().put("action", "delete").put("seq", seq), false)
        if (ok) server.update { map ->
            val m = map[seq] ?: return@update map
            map + (seq to m.copy(deleted = true, text = "", reactions = emptyMap()))
        }
        return ok
    }

    suspend fun report(seq: Long, reason: String): Boolean = simple(JSONObject().put("action", "report").put("seq", seq).put("reason", reason), false)

    /** Reaksi: tampil langsung (optimistis), lalu disamakan dengan jawaban server. */
    fun react(seq: Long, emoji: String) {
        val uid = myId()
        server.update { map ->
            val m = map[seq] ?: return@update map
            val had = m.reactions[emoji].orEmpty().contains(uid)
            val rx = LinkedHashMap<String, List<String>>()
            for ((e, list) in m.reactions) { val l = list.filter { it != uid }; if (l.isNotEmpty()) rx[e] = l }
            if (!had) rx[emoji] = rx[emoji].orEmpty() + uid
            map + (seq to m.copy(reactions = rx))
        }
        scope.launch {
            val t = token() ?: return@launch
            try {
                val r = ChatApi.post(t, JSONObject().put("action", "react").put("seq", seq).put("emoji", emoji))
                val m = ChatApi.parseMessage(r.getJSONObject("message"))
                server.update { it + (m.seq to m) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                kick()
            }
        }
    }

    private suspend fun simple(body: JSONObject, merge: Boolean): Boolean {
        val t = token() ?: return false
        return try {
            val r = ChatApi.post(t, body)
            if (merge) r.optJSONObject("message")?.let { val m = ChatApi.parseMessage(it); server.update { map -> map + (m.seq to m) } }
            kick()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountError) {
            _errors.tryEmit(e.code)
            false
        } catch (e: Throwable) {
            _errors.tryEmit("network")
            false
        }
    }

    /** Dipanggil saat pengguna mengetik. Dibatasi sekali tiap 3 detik. */
    fun notifyTyping() {
        val now = System.currentTimeMillis()
        if (now - lastTypingSent < 3000) return
        lastTypingSent = now
        scope.launch { runCatching { token()?.let { ChatApi.post(it, JSONObject().put("action", "typing")) } } }
    }

    /* ---------------- profil dan pencarian ---------------- */

    suspend fun join(): ChatProfile? {
        val t = token() ?: return null
        val r = ChatApi.post(t, JSONObject().put("action", "join"))
        val p = ChatApi.parseProfile(r.getJSONObject("profile"))
        _me.value = p
        return p
    }

    suspend fun profile(username: String): ChatProfile? {
        val t = token() ?: return null
        return try { ChatApi.profile(t, username) } catch (e: CancellationException) { throw e } catch (e: Throwable) { null }
    }

    suspend fun searchUsers(prefix: String): List<ChatUserHit> {
        val t = token() ?: return emptyList()
        return try { ChatApi.users(t, prefix) } catch (e: CancellationException) { throw e } catch (e: Throwable) { emptyList() }
    }

    suspend fun updateProfile(bio: String, status: String): Boolean {
        val t = token() ?: return false
        return try {
            val r = ChatApi.post(t, JSONObject().put("action", "profile_update").put("bio", bio).put("status", status))
            _me.value = ChatApi.parseProfile(r.getJSONObject("profile"))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            false
        }
    }

    /** Kirim statistik lokal (lagu disukai, playlist, lagu sering diputar) supaya tampil di profil. */
    fun pushStats(liked: Int, playlists: Int, top: List<ChatTopTrack>) {
        scope.launch {
            val t = token() ?: return@launch
            runCatching {
                val arr = JSONArray()
                top.take(5).forEach { arr.put(JSONObject().put("t", it.title).put("a", it.artist)) }
                ChatApi.post(t, JSONObject().put("action", "stats").put("liked", liked).put("playlists", playlists).put("top", arr))
            }
        }
    }

    /** Dipakai untuk menyarankan @mention: pengguna yang baru bicara di chat, terbaru dulu. */
    fun recentParticipants(limit: Int = 30): List<ChatUserHit> =
        server.value.values.sortedByDescending { it.seq }
            .filter { !it.announce }
            .distinctBy { it.username }
            .take(limit)
            .map { ChatUserHit(it.username, it.uid, it.display) }

    fun findMessage(seq: Long): ChatMessage? = server.value[seq]

    /* ---------------- notifikasi HP ---------------- */

    /**
     * Dipanggil berkala (loop di Application). Mengambil mention, balasan, reaksi, dan pengumuman baru lalu menampilkannya
     * sebagai notifikasi HP, satu notifikasi per jenis tiap putaran supaya tidak spam. Tidak menampilkan apa pun saat Global Chat
     * sedang dibuka (di sana sudah ada indikator langsung).
     */
    suspend fun checkNotifications() {
        val t = token() ?: return
        val since = settings.lastNotifTs
        val (now, items) = ChatApi.notifs(t, since)
        settings.lastNotifTs = now
        if (since == 0L) return // pertama kali: jangan banjiri dengan riwayat lama
        if (items.isEmpty()) return
        val me = myName()
        val muted = settings.muted.value
        val dms = DmManager.getInstance()
        val openDm = dms.openUsername()
        val relevant = items.filter { (it.from.lowercase() !in muted && !it.from.equals(me, true)) || it.kind == "a" }
        _mentionUnread.update { it + relevant.count { n -> n.kind == "m" || n.kind == "r" } }
        dms.refreshInbox()
        if (!settings.notifAll.value) return
        val allowed = relevant.filter {
            when (it.kind) {
                "m" -> settings.notifMention.value && !isActive
                "r" -> settings.notifReply.value && !isActive
                "x" -> settings.notifReaction.value && !isActive
                "a" -> settings.notifAnnounce.value && !isActive
                "d" -> settings.notifDm.value && !it.from.equals(openDm, true)
                else -> false
            }
        }
        ChatNotifier.show(context, allowed)
    }

    companion object {
        @Volatile private var instance: ChatManager? = null
        fun init(context: Context): ChatManager {
            ChatSettings.init(context)
            return instance ?: synchronized(this) { instance ?: ChatManager(context.applicationContext).also { instance = it } }
        }
        fun getInstance(): ChatManager = instance ?: throw IllegalStateException("ChatManager belum diinisialisasi")
    }
}
