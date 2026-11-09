package id.nexrify.music.data.chat

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pengaturan notifikasi dan daftar pengguna yang di-mute, disimpan di perangkat. */
class ChatSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("nexrify_chat", Context.MODE_PRIVATE)

    private val _all = MutableStateFlow(prefs.getBoolean(K_ALL, true))
    val notifAll: StateFlow<Boolean> = _all.asStateFlow()
    private val _mention = MutableStateFlow(prefs.getBoolean(K_MENTION, true))
    val notifMention: StateFlow<Boolean> = _mention.asStateFlow()
    private val _reply = MutableStateFlow(prefs.getBoolean(K_REPLY, true))
    val notifReply: StateFlow<Boolean> = _reply.asStateFlow()
    private val _reaction = MutableStateFlow(prefs.getBoolean(K_REACTION, false))
    val notifReaction: StateFlow<Boolean> = _reaction.asStateFlow()
    private val _announce = MutableStateFlow(prefs.getBoolean(K_ANNOUNCE, true))
    val notifAnnounce: StateFlow<Boolean> = _announce.asStateFlow()
    private val _dm = MutableStateFlow(prefs.getBoolean(K_DM, true))
    val notifDm: StateFlow<Boolean> = _dm.asStateFlow()
    private val _muted = MutableStateFlow(prefs.getStringSet(K_MUTED, emptySet()).orEmpty().toSet())
    val muted: StateFlow<Set<String>> = _muted.asStateFlow()
    private val _blocked = MutableStateFlow(prefs.getStringSet(K_BLOCKED, emptySet()).orEmpty().toSet())
    /** Username (huruf kecil) yang diblokir; pesannya disembunyikan. Disinkronkan dari server. */
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()
    fun setBlocked(set: Set<String>) { _blocked.value = set; prefs.edit().putStringSet(K_BLOCKED, set).apply() }

    private val _rulesSeen = MutableStateFlow(prefs.getBoolean(K_RULES, false))
    val rulesSeen: StateFlow<Boolean> = _rulesSeen.asStateFlow()

    fun setAll(v: Boolean) { _all.value = v; prefs.edit().putBoolean(K_ALL, v).apply() }
    fun setMention(v: Boolean) { _mention.value = v; prefs.edit().putBoolean(K_MENTION, v).apply() }
    fun setReply(v: Boolean) { _reply.value = v; prefs.edit().putBoolean(K_REPLY, v).apply() }
    fun setReaction(v: Boolean) { _reaction.value = v; prefs.edit().putBoolean(K_REACTION, v).apply() }
    fun setAnnounce(v: Boolean) { _announce.value = v; prefs.edit().putBoolean(K_ANNOUNCE, v).apply() }
    fun setDm(v: Boolean) { _dm.value = v; prefs.edit().putBoolean(K_DM, v).apply() }
    fun dismissRules() { _rulesSeen.value = true; prefs.edit().putBoolean(K_RULES, true).apply() }

    fun mute(username: String) = saveMuted(_muted.value + username.lowercase())
    fun unmute(username: String) = saveMuted(_muted.value - username.lowercase())
    fun isMuted(username: String) = username.lowercase() in _muted.value
    private fun saveMuted(set: Set<String>) { _muted.value = set; prefs.edit().putStringSet(K_MUTED, set).apply() }

    var lastNotifTs: Long
        get() = prefs.getLong(K_LAST_TS, 0L)
        set(v) { prefs.edit().putLong(K_LAST_TS, v).apply() }

    companion object {
        private const val K_ALL = "notif_all"
        private const val K_BLOCKED = "blocked_users"
        private const val K_MENTION = "notif_mention"
        private const val K_REPLY = "notif_reply"
        private const val K_REACTION = "notif_reaction"
        private const val K_ANNOUNCE = "notif_announce"
        private const val K_DM = "notif_dm"
        private const val K_MUTED = "muted"
        private const val K_RULES = "rules_seen"
        private const val K_LAST_TS = "last_notif_ts"

        @Volatile private var instance: ChatSettings? = null
        fun init(context: Context): ChatSettings =
            instance ?: synchronized(this) { instance ?: ChatSettings(context.applicationContext).also { instance = it } }
        fun getInstance(): ChatSettings = instance ?: throw IllegalStateException("ChatSettings belum diinisialisasi")
    }
}
