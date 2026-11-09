package id.nexrify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.chat.ChatMessage
import id.nexrify.music.data.chat.ChatSeen
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.Strings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme

/** Data tambahan untuk satu gelembung pesan, supaya parameter ChatBubble tidak membengkak. */
class BubbleExtras(
    /** "Tersampaikan" atau "Dibaca N" untuk pesan sendiri. */
    val receipt: String? = null,
    val receiptRead: Boolean = false,
    val translated: String? = null,
    val onHideTranslation: () -> Unit = {},
    val threadCount: Int = 0,
    val onThread: () -> Unit = {},
    val onReactionsLong: () -> Unit = {},
    val myVote: Int = -1,
    val onVote: (Int) -> Unit = {}
)

/** Jumlah pengguna lain yang sudah melihat pesan kita (lewat catatan "terakhir dilihat" tiap pengguna). */
fun chatReadCount(msg: ChatMessage, seen: List<ChatSeen>): Int =
    seen.count { it.uid != msg.uid && it.seq >= msg.seq }

fun chatReceiptText(msg: ChatMessage, seen: List<ChatSeen>, s: Strings): Pair<String, Boolean> {
    val n = chatReadCount(msg, seen)
    return if (n > 0) s.chatReceiptRead(n) to true else s.chatReceiptDelivered to false
}

/** "5 mnt lalu", "2 jam lalu", dan seterusnya. */
fun chatAgo(ts: Long, s: Strings): String {
    val diff = (System.currentTimeMillis() - ts).coerceAtLeast(0)
    val min = diff / 60_000
    return when {
        min < 1 -> s.chatJustNow
        min < 60 -> s.chatAgoMin(min.toInt())
        min < 60 * 24 -> s.chatAgoHour((min / 60).toInt())
        else -> s.chatAgoDay((min / (60 * 24)).toInt())
    }
}

@Composable
fun TranslationBlock(text: String, ink: Color, sub: Color, onHide: () -> Unit) {
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ink.copy(alpha = 0.08f))
            .clickable(onClick = onHide)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(NexIcons.Translate, null, tint = sub, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(S.chatTranslated, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sub)
        }
        Spacer(Modifier.height(2.dp))
        Text(text, fontSize = 14.sp, lineHeight = 19.sp, color = ink, fontStyle = FontStyle.Italic)
    }
}

/** Kartu polling di dalam gelembung: pilihan dengan batang persentase, ketuk untuk memberi suara. */
@Composable
fun PollCard(msg: ChatMessage, myVote: Int, onVote: (Int) -> Unit, mine: Boolean) {
    val colors = NexrifyAppTheme.colors
    val poll = msg.poll ?: return
    val ink = if (mine) colors.background else colors.textPrimary
    val sub = if (mine) colors.background.copy(alpha = 0.65f) else colors.textMuted
    val fill = if (mine) colors.background.copy(alpha = 0.14f) else colors.textPrimary.copy(alpha = 0.14f)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(NexIcons.BarChart, null, tint = sub, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(S.chatPollLabel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = sub)
            if (poll.closed) {
                Spacer(Modifier.width(8.dp))
                Text(
                    S.chatPollClosed, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sub,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(fill).padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(poll.question, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(10.dp))
        poll.options.forEachIndexed { i, opt ->
            val pct = if (poll.total > 0) opt.votes * 100f / poll.total else 0f
            val chosen = myVote == i
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .border(if (chosen) 1.5.dp else 1.dp, if (chosen) ink else sub.copy(alpha = 0.35f), shape)
                    .clickable(enabled = !poll.closed) { onVote(i) }
            ) {
                Box(Modifier.matchParentSize()) {
                    Box(Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(fill))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chosen) {
                        Icon(NexIcons.Check, null, tint = ink, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(opt.text, fontSize = 14.sp, color = ink, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text("${pct.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ink)
                }
            }
        }
        Text(
            S.chatPollVotes(poll.total) + if (!poll.closed && myVote < 0) "  ·  " + S.chatPollTapHint else "",
            fontSize = 11.sp, lineHeight = 15.sp, color = sub
        )
    }
}

/** Pita pesan yang disematkan di bawah header. Ketuk untuk melompat, ketuk lencana nomor untuk ganti ke sematan berikutnya. */
@Composable
fun PinnedBar(pins: List<ChatMessage>, index: Int, onJump: (ChatMessage) -> Unit, onNext: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    if (pins.isEmpty()) return
    val cur = pins[index.coerceIn(0, pins.lastIndex)]
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.card)
            .clickable { onJump(cur) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(30.dp).clip(CircleShape).background(colors.textPrimary))
        Spacer(Modifier.width(10.dp))
        Icon(NexIcons.Pin, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(S.chatPinnedTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(
                if (cur.poll != null) cur.poll.question else cur.text,
                fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (pins.size > 1) {
            Text(
                S.chatPinnedOf(index.coerceIn(0, pins.lastIndex) + 1, pins.size), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colors.border).clickable(onClick = onNext).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
}
