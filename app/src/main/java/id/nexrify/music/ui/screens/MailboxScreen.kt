package id.nexrify.music.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.mailbox.MailboxManager
import id.nexrify.music.data.remote.MailMessage
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.components.NexLoadingFill
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import java.text.DateFormat
import java.util.Date

/** Kotak surat: pesan resmi dari pengembang. Ketuk surat untuk membaca, tanda titik hilang setelah dibaca. */
@Composable
fun MailboxScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NexrifyAppTheme.colors
    val mailbox = remember { MailboxManager.getInstance() }
    val messages by mailbox.messages.collectAsState()
    val readIds by mailbox.readIds.collectAsState()
    val loading by mailbox.loading.collectAsState()
    val failed by mailbox.failed.collectAsState()
    var expandedId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { mailbox.refresh() }

    Column(modifier = modifier.fillMaxSize().background(colors.background).padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(NexIcons.ArrowBack, S.back, tint = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(S.mailboxTitle, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(S.mailboxSubtitle, style = Typography.bodySmall, color = colors.textMuted)
            }
            if (messages.any { it.id !in readIds }) {
                TextButton(onClick = { mailbox.markAllRead() }) {
                    Text(S.mailboxMarkAll, color = colors.textSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            messages.isEmpty() && loading -> NexLoadingFill(active = true)
            messages.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(colors.card).border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(NexIcons.Mail, null, tint = colors.textMuted, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (failed) S.mailboxFailed else S.mailboxEmpty,
                        style = Typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (failed) S.mailboxFailedHint else S.mailboxEmptyHint,
                        style = Typography.bodySmall, color = colors.textMuted, textAlign = TextAlign.Center
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MailCard(
                        message = message,
                        unread = message.id !in readIds,
                        expanded = expandedId == message.id,
                        onClick = {
                            expandedId = if (expandedId == message.id) null else message.id
                            mailbox.markRead(message.id)
                        },
                        onDelete = { mailbox.delete(message.id) },
                        onOpenLink = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.link))) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MailCard(
    message: MailMessage,
    unread: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenLink: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val english = S.english
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, if (unread) colors.textSecondary else colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(S.mailType(message.type), style = Typography.labelSmall, fontSize = 10.sp, color = colors.textSecondary)
            }
            if (message.pinned) {
                Spacer(Modifier.width(6.dp))
                Text(S.mailboxPinned, style = Typography.labelSmall, fontSize = 10.sp, color = colors.textMuted)
            }
            Spacer(Modifier.weight(1f))
            if (unread) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.textPrimary))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = message.title(english),
            style = Typography.titleMedium,
            fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = if (expanded) Int.MAX_VALUE else 2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message.body(english),
            style = Typography.bodyMedium,
            color = colors.textMuted,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDate(message.createdAt),
                style = Typography.labelSmall,
                fontSize = 11.sp,
                color = colors.textMuted,
                modifier = Modifier.weight(1f)
            )
            if (expanded && message.link.startsWith("http")) {
                TextButton(onClick = onOpenLink) {
                    Text(S.mailboxOpenLink, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            if (expanded) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(NexIcons.DeleteOutline, S.delete, tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
