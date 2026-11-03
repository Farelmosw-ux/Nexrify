package id.nexrify.music.data.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.nexrify.music.MainActivity
import id.nexrify.music.R
import id.nexrify.music.i18n.currentStrings

/** Notifikasi HP untuk Global Chat. Mengetuknya membuka Global Chat dan menyorot pesan terkait. */
object ChatNotifier {
    const val EXTRA_OPEN_CHAT = "nexrify.open_chat"
    const val EXTRA_SEQ = "nexrify.chat_seq"

    private data class Kind(val code: String, val channel: String, val id: Int)

    private val KINDS = listOf(
        Kind("m", "chat_mention", 4101),
        Kind("r", "chat_reply", 4102),
        Kind("x", "chat_reaction", 4103),
        Kind("a", "chat_announce", 4104)
    )

    fun show(context: Context, items: List<ChatNotif>) {
        if (items.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val s = currentStrings()
        val system = context.getSystemService(NotificationManager::class.java)
        for (kind in KINDS) {
            val group = items.filter { it.kind == kind.code }
            if (group.isEmpty()) continue
            val last = group.last()
            val channelName = when (kind.code) {
                "m" -> s.chatNotifChannelMention
                "r" -> s.chatNotifChannelReply
                "x" -> s.chatNotifChannelReaction
                else -> s.chatNotifChannelAnnounce
            }
            val importance = if (kind.code == "x") NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            system.createNotificationChannel(NotificationChannel(kind.channel, channelName, importance))

            val title = when (kind.code) {
                "m" -> s.chatNotifMentionTitle(last.from)
                "r" -> s.chatNotifReplyTitle(last.from)
                "x" -> s.chatNotifReactionTitle(last.from)
                else -> s.chatNotifAnnounceTitle
            }
            val body = when (kind.code) {
                "x" -> s.chatNotifReactionBody(last.text)
                else -> last.text
            }
            val more = if (group.size > 1) "\n" + s.chatNotifMore(group.size - 1) else ""
            val open = PendingIntent.getActivity(
                context, kind.id,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_OPEN_CHAT, true)
                    .putExtra(EXTRA_SEQ, last.seq),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(context, kind.channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body + more))
                .setAutoCancel(true)
                .setContentIntent(open)
                .setPriority(if (kind.code == "x") NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
                .build()
            try { manager.notify(kind.id, n) } catch (_: SecurityException) {}
        }
    }
}
