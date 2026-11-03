package id.nexrify.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.data.chat.ChatApi
import id.nexrify.music.data.chat.ChatMessage
import id.nexrify.music.data.chat.SendStatus
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.Strings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ---------------- format ---------------- */

fun chatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

fun chatDayKey(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun chatDayLabel(dayKey: Long, s: Strings): String {
    val today = chatDayKey(System.currentTimeMillis())
    return when (dayKey) {
        today -> s.chatToday
        today - 86_400_000L -> s.chatYesterday
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(dayKey))
    }
}

fun chatJoinedLabel(ts: Long): String =
    if (ts <= 0) "-" else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))

fun badgeShort(code: String): String = when (code) {
    "staff" -> "STAF"
    "early" -> "OG"
    "active" -> "AKTIF"
    "loved" -> "♥"
    else -> code.uppercase()
}

fun badgeFull(code: String, s: Strings): String = when (code) {
    "staff" -> s.chatBadgeStaff
    "early" -> s.chatBadgeEarly
    "active" -> s.chatBadgeActive
    "loved" -> s.chatBadgeLoved
    else -> code
}

private val MENTION = Regex("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,20})")

/** Teks pesan dengan @mention ditebalkan dan diwarnai. Mention untuk pengguna sendiri diberi latar. */
fun chatAnnotated(text: String, me: String, mentionColor: Color, selfBg: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in MENTION.findAll(text)) {
        append(text.substring(last, m.range.first))
        val self = m.groupValues[1].equals(me, ignoreCase = true)
        withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold, background = if (self) selfBg else Color.Unspecified)) {
            append(m.value)
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

/* ---------------- avatar dan lencana ---------------- */

@Composable
fun ChatAvatar(uid: String, av: String, name: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val colors = NexrifyAppTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
        if (av.isNotEmpty() && uid.isNotEmpty() && uid != "sys") {
            AsyncImage(
                model = ChatApi.avatarUrl(uid, av),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun BadgeChip(code: String) {
    val colors = NexrifyAppTheme.colors
    val filled = code == "staff"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) colors.textPrimary else colors.border)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = badgeShort(code),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) colors.background else colors.textSecondary
        )
    }
}

/* ---------------- gelembung pesan ---------------- */

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatBubble(
    msg: ChatMessage,
    mine: Boolean,
    myId: String,
    myUsername: String,
    showHeader: Boolean,
    highlighted: Boolean,
    onLongPress: () -> Unit,
    onReplyClick: (Long) -> Unit,
    onAvatarClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val haptic = LocalHapticFeedback.current
    val flash by animateColorAsState(
        targetValue = if (highlighted) colors.textPrimary.copy(alpha = 0.16f) else Color.Transparent,
        label = "flash"
    )

    if (msg.announce) {
        AnnouncementCard(msg, flash, onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() })
        return
    }

    val bubbleBg = when {
        msg.deleted -> Color.Transparent
        mine -> colors.textPrimary
        else -> colors.card
    }
    val textColor = if (mine) colors.background else colors.textPrimary
    val subColor = if (mine) colors.background.copy(alpha = 0.6f) else colors.textMuted
    val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    else RoundedCornerShape(if (showHeader) 4.dp else 18.dp, 18.dp, 18.dp, 18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(flash)
            .padding(horizontal = 12.dp, vertical = if (showHeader) 5.dp else 1.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!mine) {
            if (showHeader) {
                ChatAvatar(msg.uid, msg.av, msg.display, 34.dp, Modifier.clickable(onClick = onAvatarClick))
            } else {
                Spacer(Modifier.width(34.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            if (showHeader && !mine) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp).clickable(onClick = onAvatarClick)
                ) {
                    Text(
                        msg.display,
                        style = Typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                    Text("  @${msg.username}", style = Typography.labelSmall, color = colors.textMuted, maxLines = 1)
                    msg.badges.forEach { Spacer(Modifier.width(4.dp)); BadgeChip(it) }
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(shape)
                    .background(bubbleBg)
                    .then(if (msg.deleted) Modifier.border(1.dp, colors.border, shape) else Modifier)
                    .combinedClickable(
                        onClick = { if (msg.status == SendStatus.FAILED) onRetry() },
                        onLongClick = {
                            if (msg.status != SendStatus.SENDING) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                msg.reply?.let { r ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mine) colors.background.copy(alpha = 0.10f) else colors.border.copy(alpha = 0.55f))
                            .clickable { onReplyClick(r.seq) }
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(if (mine) colors.background.copy(alpha = 0.55f) else colors.textSecondary))
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text("@${r.username}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                            Text(
                                r.text.ifEmpty { S.chatDeletedMsg },
                                fontSize = 12.sp,
                                color = subColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (msg.deleted) {
                    Text(S.chatDeletedMsg, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = colors.textMuted)
                } else {
                    Text(
                        text = chatAnnotated(msg.text, myUsername, if (mine) textColor else colors.textPrimary, colors.textPrimary.copy(alpha = 0.18f)),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = textColor
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.edited > 0 && !msg.deleted) {
                        Text(S.chatEdited, fontSize = 10.sp, color = subColor)
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(chatTime(msg.ts), fontSize = 10.sp, color = subColor)
                    if (mine && !msg.deleted) {
                        Spacer(Modifier.width(4.dp))
                        when (msg.status) {
                            SendStatus.SENDING -> CircularProgressIndicator(
                                color = subColor, strokeWidth = 1.5.dp, modifier = Modifier.size(10.dp)
                            )
                            SendStatus.SENT -> Icon(NexIcons.Check, null, tint = subColor, modifier = Modifier.size(12.dp))
                            SendStatus.FAILED -> Icon(NexIcons.Info, null, tint = Color(0xFFE5484D), modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
            if (msg.status == SendStatus.FAILED) {
                Text(S.chatFailed, fontSize = 11.sp, color = Color(0xFFE5484D), modifier = Modifier.padding(top = 2.dp, end = 4.dp))
            }
            if (msg.reactions.isNotEmpty() && !msg.deleted) {
                FlowRow(
                    modifier = Modifier.padding(top = 3.dp).widthIn(max = 290.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, if (mine) Alignment.End else Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    msg.reactions.forEach { (emoji, users) ->
                        val iReacted = users.contains(myId)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (iReacted) colors.textPrimary else colors.card)
                                .border(1.dp, if (iReacted) colors.textPrimary else colors.border, RoundedCornerShape(12.dp))
                                .clickable { onReactionClick(emoji) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 12.sp)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                users.size.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (iReacted) colors.background else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnnouncementCard(msg: ChatMessage, flash: Color, onLongPress: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().background(flash).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.textPrimary.copy(alpha = 0.35f), shape)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NexIcons.Mail, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.chatAnnouncement, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.width(6.dp))
                BadgeChip("staff")
                Spacer(Modifier.weight(1f))
                Text(chatTime(msg.ts), fontSize = 10.sp, color = colors.textMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(msg.text, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textPrimary)
        }
    }
}

@Composable
fun ChatDateHeader(label: String) {
    val colors = NexrifyAppTheme.colors
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
