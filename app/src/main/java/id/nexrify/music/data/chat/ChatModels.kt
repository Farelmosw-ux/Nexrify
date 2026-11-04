package id.nexrify.music.data.chat

enum class SendStatus { SENDING, SENT, FAILED }

data class ChatReply(val seq: Long, val username: String, val text: String)

data class ChatMessage(
    val seq: Long,
    val uid: String,
    val username: String,
    val display: String,
    val badges: List<String>,
    val av: String,
    val text: String,
    val ts: Long,
    val edited: Long,
    val deleted: Boolean,
    val announce: Boolean,
    val mentions: List<String>,
    val reply: ChatReply?,
    /** emoji → daftar uid yang bereaksi */
    val reactions: Map<String, List<String>>,
    val status: SendStatus = SendStatus.SENT,
    /** Hanya untuk pesan yang belum terkirim (seq negatif). */
    val localId: String? = null
)

data class ChatTopTrack(val title: String, val artist: String)

data class ChatProfile(
    val uid: String,
    val username: String,
    val display: String,
    val av: String,
    val joined: Long,
    val bio: String,
    val status: String,
    val messages: Int,
    val reactionsReceived: Int,
    val liked: Int,
    val playlists: Int,
    val top: List<ChatTopTrack>,
    val badges: List<String>,
    val blockedByMe: Boolean = false,
    val online: Boolean = false
)

data class ChatUserHit(val username: String, val uid: String, val display: String)

/** Kejadian yang relevan untuk pengguna saat Global Chat sedang dibuka. */
data class ChatEvent(val kind: String, val message: ChatMessage)

/** Notifikasi dari server untuk notifikasi HP: k = m (mention), r (balasan), x (reaksi), a (pengumuman). */
data class ChatNotif(val kind: String, val seq: Long, val from: String, val text: String, val ts: Long)

data class ChatFeed(val ev: Long, val reset: Boolean, val typing: List<String>, val messages: List<ChatMessage>)

val CHAT_EMOJIS = listOf("👍", "😂", "😮", "😭", "🙏", "😁", "😡", "❌", "✅", "❤️", "🗿")
const val CHAT_MAX_LENGTH = 500

/* ---------------- pesan langsung (DM) ---------------- */

data class DmReply(val seq: Long, val uid: String, val text: String)

data class DmMessage(
    val seq: Long,
    val uid: String,
    val text: String,
    val ts: Long,
    val edited: Long,
    val deleted: Boolean,
    val ev: Long,
    val reply: DmReply?,
    val status: SendStatus = SendStatus.SENT,
    val localId: String? = null
)

data class DmPeer(val uid: String, val username: String, val display: String, val av: String)

data class DmConversation(
    val uid: String,
    val username: String,
    val display: String,
    val av: String,
    val last: String,
    val ts: Long,
    val unread: Int,
    val fromMe: Boolean
)

data class DmThreadData(val peer: DmPeer, val messages: List<DmMessage>, val typing: Boolean, val peerRead: Long, val blocked: Boolean, val ev: Long)

/** Info grup Global Chat. myRole: "admin", "staff", atau kosong. */
data class ChatGroup(
    val name: String,
    val desc: String,
    val photoV: String,
    val hasPhoto: Boolean,
    val members: Int,
    val online: Int,
    val myRole: String,
    val canEdit: Boolean
)

data class ChatMember(val uid: String, val username: String, val display: String, val role: String, val online: Boolean)
