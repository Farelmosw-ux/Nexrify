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
    val badges: List<String>
)

data class ChatUserHit(val username: String, val uid: String, val display: String)

/** Kejadian yang relevan untuk pengguna saat Global Chat sedang dibuka. */
data class ChatEvent(val kind: String, val message: ChatMessage)

/** Notifikasi dari server untuk notifikasi HP: k = m (mention), r (balasan), x (reaksi), a (pengumuman). */
data class ChatNotif(val kind: String, val seq: Long, val from: String, val text: String, val ts: Long)

data class ChatFeed(val ev: Long, val reset: Boolean, val typing: List<String>, val messages: List<ChatMessage>)

val CHAT_EMOJIS = listOf("❤️", "😂", "🔥", "🎵", "👍")
const val CHAT_MAX_LENGTH = 500
