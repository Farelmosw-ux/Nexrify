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
import kotlinx.coroutines.delay
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
import org.json.JSONObject

/**
 * Pesan langsung (DM) satu lawan satu. Kotak masuk dicek tiap 8 detik hanya saat layar chat atau pesan terlihat,
 * percakapan yang sedang dibuka dicek tiap 2,5 detik dan melambat saat sepi.
 */
class DmManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CrashReporter.handler)
    private val settings = ChatSettings.getInstance()

    /* ---------------- kotak masuk ---------------- */

    private val _inbox = MutableStateFlow<List<DmConversation>>(emptyList())
    val inbox: StateFlow<List<DmConversation>> = _inbox.asStateFlow()
    private val _unread = MutableStateFlow(0)
    /** Total pesan langsung yang belum dibaca (badge). */
    val unread: StateFlow<Int> = _unread.asStateFlow()
    private val _inboxLoaded = MutableStateFlow(false)
    val inboxLoaded: StateFlow<Boolean> = _inboxLoaded.asStateFlow()

    private var inboxRefs = 0
    private var inboxJob: Job? = null

    private fun token(): String? = AccountManager.getInstance().token()
    private fun myId(): String = AccountManager.getInstance().user.value?.id.orEmpty()

    @Synchronized
    fun watchInbox(on: Boolean) {
        if (on) inboxRefs++ else inboxRefs = maxOf(0, inboxRefs - 1)
        if (inboxRefs > 0 && inboxJob == null) {
            inboxJob = scope.launch {
                while (true) {
                    runCatching { refreshInbox() }
                    delay(8_000L)
                }
            }
        } else if (inboxRefs == 0) {
            inboxJob?.cancel()
            inboxJob = null
        }
    }

    suspend fun refreshInbox() {
        val t = token() ?: return
        try {
            val (unread, list) = ChatApi.dmInbox(t)
            _inbox.value = list
            _unread.value = unread
            _inboxLoaded.value = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // diam saja: dicoba lagi pada putaran berikutnya
        }
    }

    /* ---------------- percakapan ---------------- */

    private val server = MutableStateFlow<Map<Long, DmMessage>>(emptyMap())
    private val pending = MutableStateFlow<List<DmMessage>>(emptyList())
    private val pendingData = HashMap<String, Pair<String, Long>>()
    private var localCounter = 0L

    val messages: StateFlow<List<DmMessage>> = combine(server, pending) { s, p ->
        s.values.sortedBy { it.seq } + p
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _peer = MutableStateFlow<DmPeer?>(null)
    val peer: StateFlow<DmPeer?> = _peer.asStateFlow()
    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()
    private val _peerRead = MutableStateFlow(0L)
    val peerRead: StateFlow<Long> = _peerRead.asStateFlow()
    private val _blocked = MutableStateFlow(false)
    val blocked: StateFlow<Boolean> = _blocked.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    @Volatile private var openUid: String? = null
    private var threadJob: Job? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var lastTypingSent = 0L
    private var lastReadMarked = 0L

    fun openUsername(): String? = _peer.value?.takeIf { openUid != null }?.username

    suspend fun peerFromUsername(username: String): DmPeer? {
        val p = ChatManager.getInstance().profile(username) ?: return null
        return DmPeer(p.uid, p.username, p.display, p.av)
    }

    fun openThread(peer: DmPeer) {
        if (openUid == peer.uid && threadJob?.isActive == true) return
        threadJob?.cancel()
        server.value = emptyMap()
        pending.value = emptyList()
        pendingData.clear()
        _typing.value = false
        _peerRead.value = 0L
        _blocked.value = false
        _loaded.value = false
        _offline.value = false
        _peer.value = peer
        openUid = peer.uid
        threadJob = scope.launch { threadLoop(peer.uid) }
    }

    fun closeThread() {
        threadJob?.cancel()
        threadJob = null
        openUid = null
        _typing.value = false
    }

    private fun kick() { wake.trySend(Unit) }

    private suspend fun threadLoop(uid: String) {
        var ev = 0L
        var idle = 0
        var failures = 0
        var first = true
        while (openUid == uid) {
            val t = token() ?: break
            var changed = false
            try {
                val data = ChatApi.dmThread(t, uid, ev)
                _peer.value = data.peer
                var incoming = false
                if (data.messages.isNotEmpty()) {
                    val me = myId()
                    server.update { old -> old + data.messages.associateBy { it.seq } }
                    incoming = data.messages.any { it.uid != me && !it.deleted }
                    changed = true
                }
                ev = maxOf(ev, data.ev)
                if (data.typing != _typing.value) changed = true
                _typing.value = data.typing
                _peerRead.value = data.peerRead
                _blocked.value = data.blocked
                _loaded.value = true
                _offline.value = false
                failures = 0
                if (incoming || first) markRead()
                first = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountError) {
                failures++
                _offline.value = true
                if (e.code == "auth") break
            } catch (e: Throwable) {
                failures++
                _offline.value = true
                CrashReporter.logNonFatal(e)
            }
            idle = if (changed) 0 else idle + 1
            val wait = when {
                failures > 0 -> minOf(30_000L, 3_000L * failures)
                idle < 6 -> 2_500L
                idle < 20 -> 5_000L
                else -> 8_000L
            }
            if (withTimeoutOrNull(wait) { wake.receive() } != null) idle = 0
        }
    }

    /** Tandai percakapan terbuka sudah dibaca (dibatasi tiap 2 detik) dan kosongkan badge-nya. */
    fun markRead() {
        val uid = openUid ?: return
        val now = System.currentTimeMillis()
        if (now - lastReadMarked < 2_000L) return
        lastReadMarked = now
        _inbox.update { list -> list.map { if (it.uid == uid) it.copy(unread = 0) else it } }
        _unread.value = _inbox.value.sumOf { it.unread }
        scope.launch { runCatching { token()?.let { ChatApi.post(it, JSONObject().put("action", "dm_read").put("to", uid)) } } }
    }

    fun send(text: String, replyTo: DmMessage?) {
        val clean = text.trim()
        val uid = openUid ?: return
        if (clean.isEmpty()) return
        localCounter++
        val localId = "D$localCounter"
        val msg = DmMessage(
            seq = -localCounter, uid = myId(), text = ChatFilter.censor(clean.take(CHAT_MAX_LENGTH)), ts = System.currentTimeMillis(),
            edited = 0, deleted = false, ev = 0, reply = replyTo?.let { DmReply(it.seq, it.uid, it.text.take(100)) },
            status = SendStatus.SENDING, localId = localId
        )
        pendingData[localId] = clean to (replyTo?.seq ?: 0L)
        pending.update { it + msg }
        scope.launch { deliver(localId, uid) }
    }

    fun retry(localId: String) {
        val uid = openUid ?: return
        pending.update { list -> list.map { if (it.localId == localId) it.copy(status = SendStatus.SENDING) else it } }
        scope.launch { deliver(localId, uid) }
    }

    fun discardPending(localId: String) {
        pendingData.remove(localId)
        pending.update { list -> list.filterNot { it.localId == localId } }
    }

    private suspend fun deliver(localId: String, uid: String) {
        val (text, replySeq) = pendingData[localId] ?: return
        val t = token() ?: return
        try {
            val body = JSONObject().put("action", "dm_send").put("to", uid).put("text", text)
            if (replySeq > 0) body.put("replyTo", replySeq)
            val r = ChatApi.post(t, body)
            val m = ChatApi.parseDm(r.getJSONObject("message"))
            pendingData.remove(localId)
            server.update { it + (m.seq to m) }
            pending.update { list -> list.filterNot { it.localId == localId } }
            kick()
            scope.launch { refreshInbox() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountError) {
            val permanent = e.code in setOf("dm_blocked", "dm_you_blocked", "chat_banned", "rate_limited", "empty", "not_found", "invalid")
            if (permanent) { discardPending(localId); _errors.tryEmit(e.code) } else markFailed(localId)
        } catch (e: Throwable) {
            markFailed(localId)
        }
    }

    private fun markFailed(localId: String) {
        pending.update { list -> list.map { if (it.localId == localId) it.copy(status = SendStatus.FAILED) else it } }
    }

    suspend fun edit(seq: Long, text: String): Boolean {
        val uid = openUid ?: return false
        val t = token() ?: return false
        return try {
            val r = ChatApi.post(t, JSONObject().put("action", "dm_edit").put("to", uid).put("seq", seq).put("text", text.trim()))
            val m = ChatApi.parseDm(r.getJSONObject("message"))
            server.update { it + (m.seq to m) }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountError) {
            _errors.tryEmit(e.code); false
        } catch (e: Throwable) {
            _errors.tryEmit("network"); false
        }
    }

    suspend fun delete(seq: Long): Boolean {
        val uid = openUid ?: return false
        val t = token() ?: return false
        return try {
            ChatApi.post(t, JSONObject().put("action", "dm_delete").put("to", uid).put("seq", seq))
            server.update { map ->
                val m = map[seq] ?: return@update map
                map + (seq to m.copy(deleted = true, text = ""))
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _errors.tryEmit("network"); false
        }
    }

    fun notifyTyping() {
        val uid = openUid ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingSent < 3_000L) return
        lastTypingSent = now
        scope.launch { runCatching { token()?.let { ChatApi.post(it, JSONObject().put("action", "dm_typing").put("to", uid)) } } }
    }

    suspend fun setBlock(uid: String, on: Boolean): Boolean {
        val t = token() ?: return false
        return try {
            ChatApi.post(t, JSONObject().put("action", "dm_block").put("to", uid).put("on", on))
            if (openUid == uid) _blocked.value = on
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            false
        }
    }

    companion object {
        @Volatile private var instance: DmManager? = null
        fun init(context: Context): DmManager =
            instance ?: synchronized(this) { instance ?: DmManager(context.applicationContext).also { instance = it } }
        fun getInstance(): DmManager = instance ?: throw IllegalStateException("DmManager belum diinisialisasi")
    }
}
