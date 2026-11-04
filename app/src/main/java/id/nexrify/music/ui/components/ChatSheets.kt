package id.nexrify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.chat.CHAT_EMOJIS
import id.nexrify.music.data.chat.ChatManager
import id.nexrify.music.data.chat.ChatMessage
import id.nexrify.music.data.chat.ChatProfile
import id.nexrify.music.data.chat.ChatUserHit
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RED = Color(0xFFE5484D)

@Composable
private fun SheetHandleBar() {
    val colors = NexrifyAppTheme.colors
    Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) RED else colors.textPrimary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = if (danger) RED else colors.textPrimary)
    }
}

/* ---------------- menu pesan (tekan lama) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageMenu(
    msg: ChatMessage,
    mine: Boolean,
    canEdit: Boolean,
    isMuted: Boolean,
    myId: String,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onProfile: () -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background, dragHandle = { SheetHandleBar() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            if (!msg.deleted) {
                // Bar reaksi: kalau tidak muat, digeser ke samping.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CHAT_EMOJIS.forEach { e ->
                        val selected = msg.reactions[e].orEmpty().contains(myId)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.textPrimary.copy(alpha = 0.22f) else Color.Transparent)
                                .then(if (selected) Modifier.border(1.dp, colors.textPrimary.copy(alpha = 0.5f), CircleShape) else Modifier)
                                .clickable { onReact(e) },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 24.sp) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (!msg.deleted) {
                ActionRow(NexIcons.Reply, S.chatReplyAction, onClick = onReply)
                ActionRow(NexIcons.Copy, S.chatCopyAction, onClick = onCopy)
            }
            if (mine && canEdit && !msg.deleted) ActionRow(NexIcons.Edit, S.chatEditAction, onClick = onEdit)
            ActionRow(NexIcons.Person, S.chatProfileAction, onClick = onProfile)
            if (mine) {
                if (!msg.deleted) ActionRow(NexIcons.DeleteOutline, S.chatDeleteAction, danger = true, onClick = onDelete)
            } else if (!msg.announce) {
                ActionRow(if (isMuted) NexIcons.Bell else NexIcons.BellOff, if (isMuted) S.chatUnmuteAction else S.chatMuteAction, onClick = onToggleMute)
                if (!msg.deleted) ActionRow(NexIcons.Flag, S.chatReportAction, danger = true, onClick = onReport)
            }
        }
    }
}

@Composable
fun ChatDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(S.chatDeleteTitle, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(S.chatDeleteBody, color = colors.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(S.chatDeleteAction, color = RED, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel, color = colors.textSecondary) } }
    )
}

@Composable
fun ChatReportDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(S.chatReportTitle, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf("spam" to S.chatReportSpam, "kata kasar atau pelecehan" to S.chatReportRude, "lainnya" to S.chatReportMisc).forEach { (key, label) ->
                    Text(
                        label,
                        color = colors.textPrimary,
                        style = Typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(key) }.padding(vertical = 13.dp, horizontal = 6.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel, color = colors.textSecondary) } }
    )
}

/* ---------------- profil ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileSheet(
    username: String,
    isMe: Boolean,
    isMuted: Boolean,
    onMention: (String) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ChatProfile?>(null) }
    var failed by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(username) {
        val p = manager.profile(username)
        if (p == null) failed = true else { profile = p; bio = p.bio; status = p.status }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { SheetHandleBar() }
    ) {
        val p = profile
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            when {
                p != null -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ChatAvatar(p.uid, p.av, p.display, 84.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(p.display, style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Text("@${p.username}", style = Typography.bodyMedium, color = colors.textMuted)
                        Spacer(Modifier.height(6.dp))
                        PresenceTag(p.online)
                        if (p.badges.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                p.badges.forEach { b ->
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colors.card)
                                            .border(1.dp, colors.border, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BadgeChip(b)
                                        Spacer(Modifier.width(5.dp))
                                        Text(badgeFull(b, S), fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (editing) {
                        ProfileField(S.chatStatusLabel, status, 40) { status = it }
                        Spacer(Modifier.height(10.dp))
                        ProfileField(S.chatBioLabel, bio, 120) { bio = it }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(S.chatSave, filled = true, busy = saving, modifier = Modifier.weight(1f)) {
                                scope.launch {
                                    saving = true
                                    val ok = manager.updateProfile(bio, status)
                                    saving = false
                                    val st = currentStrings()
                                    if (ok) {
                                        NexNotifier.success(st.chatProfileSaved, st.chatProfileTitle)
                                        profile = manager.profile(username) ?: profile
                                        editing = false
                                    } else NexNotifier.error(st.chatGenericError, st.chatProfileTitle)
                                }
                            }
                            PillButton(S.cancel, filled = false, modifier = Modifier.weight(1f)) { editing = false }
                        }
                    } else {
                        InfoBlock(S.chatStatusLabel, p.status.ifBlank { S.chatNoStatus }, dim = p.status.isBlank())
                        Spacer(Modifier.height(8.dp))
                        InfoBlock(S.chatBioLabel, p.bio.ifBlank { S.chatNoBio }, dim = p.bio.isBlank())
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox(p.liked.toString(), S.chatLikedSongs, Modifier.weight(1f))
                        StatBox(p.playlists.toString(), S.chatPlaylistsStat, Modifier.weight(1f))
                        StatBox(p.messages.toString(), S.chatMessagesStat, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox(p.reactionsReceived.toString(), S.chatReactionsStat, Modifier.weight(1f))
                        StatBox(chatJoinedLabel(p.joined), S.chatJoined, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(S.chatTopPlayed, style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    if (p.top.isEmpty()) {
                        Text(S.chatNoTop, style = Typography.bodySmall, color = colors.textMuted)
                    } else {
                        p.top.forEachIndexed { i, t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", color = colors.textMuted, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    if (t.artist.isNotBlank()) Text(t.artist, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    if (isMe) {
                        if (!editing) PillButton(S.chatEditProfile, filled = true, modifier = Modifier.fillMaxWidth()) { editing = true }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(S.chatMention, filled = true, modifier = Modifier.weight(1f)) { onMention(p.username) }
                            PillButton(if (isMuted) S.chatUnmuteAction else S.chatMuteAction, filled = false, modifier = Modifier.weight(1f), onClick = onToggleMute)
                        }
                    }
                }
                failed -> Text(S.chatFindEmpty, color = colors.textMuted, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), textAlign = TextAlign.Center)
                else -> Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, dim: Boolean) {
    val colors = NexrifyAppTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = if (dim) colors.textMuted else colors.textPrimary, style = Typography.bodyMedium)
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    val colors = NexrifyAppTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.5.sp, color = colors.textMuted, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun ProfileField(label: String, value: String, max: Int, onChange: (String) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column {
        Text("$label  ${value.length}/$max", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(max).replace("\n", " ")) },
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, modifier: Modifier = Modifier, busy: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (filled) colors.textPrimary else Color.Transparent)
            .border(1.dp, if (filled) colors.textPrimary else colors.border, shape)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (busy) CircularProgressIndicator(color = colors.background, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Text(text, color = if (filled) colors.background else colors.textPrimary, fontWeight = FontWeight.Bold)
    }
}

/* ---------------- cari pengguna ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchSheet(onOpenProfile: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ChatUserHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim().removePrefix("@")
        if (q.isEmpty()) { results = emptyList(); searched = false; loading = false; return@LaunchedEffect }
        loading = true
        delay(300)
        results = manager.searchUsers(q)
        searched = true
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { SheetHandleBar() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(S.chatFindPeople, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(NexIcons.Search, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(S.chatFindHint, color = colors.textMuted)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.take(20).lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '@' } },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (loading) CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            when {
                query.isBlank() -> Text(S.chatFindStart, color = colors.textMuted, style = Typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                searched && results.isEmpty() && !loading -> Text(S.chatFindEmpty, color = colors.textMuted, style = Typography.bodySmall, modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                else -> LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                    items(results, key = { it.uid }) { hit ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenProfile(hit.username) }.padding(vertical = 9.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatAvatar(hit.uid, "1", hit.display, 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(hit.display, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("@${hit.username}", color = colors.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- pengaturan chat ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsSheet(onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val st = remember { ChatManager.getInstance().settings }
    val all by st.notifAll.collectAsState()
    val mention by st.notifMention.collectAsState()
    val reply by st.notifReply.collectAsState()
    val reaction by st.notifReaction.collectAsState()
    val announce by st.notifAnnounce.collectAsState()
    val muted by st.muted.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { SheetHandleBar() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(colors.card).border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(NexIcons.Settings, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(S.chatSettings, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text(S.chatTitle, fontSize = 12.sp, color = colors.textMuted)
                }
            }

            SettingsLabel(S.chatNotifTitle)
            SettingsCard {
                ToggleRow(NexIcons.Bell, S.chatNotifAll, S.chatNotifAllSub, all) { st.setAll(it) }
                CardDivider()
                ToggleRow(NexIcons.Person, S.chatNotifMention, S.chatNotifMentionSub, mention && all, enabled = all) { st.setMention(it) }
                CardDivider()
                ToggleRow(NexIcons.Reply, S.chatNotifReply, S.chatNotifReplySub, reply && all, enabled = all) { st.setReply(it) }
                CardDivider()
                ToggleRow(NexIcons.Favorite, S.chatNotifReaction, S.chatNotifReactionSub, reaction && all, enabled = all) { st.setReaction(it) }
                CardDivider()
                ToggleRow(NexIcons.Mail, S.chatNotifAnnounce, S.chatNotifAnnounceSub, announce && all, enabled = all) { st.setAnnounce(it) }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(colors.card.copy(alpha = 0.6f)).padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(NexIcons.Info, null, tint = colors.textMuted, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Spacer(Modifier.width(10.dp))
                Text(S.chatNotifLimitNote, fontSize = 12.sp, lineHeight = 17.sp, color = colors.textMuted)
            }

            SettingsLabel("${S.chatMutedUsers}  ·  ${muted.size}")
            if (muted.isEmpty()) {
                SettingsCard {
                    Text(S.chatNoMuted, color = colors.textMuted, style = Typography.bodySmall, modifier = Modifier.padding(16.dp))
                }
            } else {
                SettingsCard {
                    muted.sorted().forEachIndexed { i, name ->
                        if (i > 0) CardDivider()
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(colors.border),
                                contentAlignment = Alignment.Center
                            ) { Text(name.take(1).uppercase(), color = colors.textPrimary, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(12.dp))
                            Text("@$name", color = colors.textPrimary, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { st.unmute(name) }) { Text(S.chatUnmuteAction, color = colors.textPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    val colors = NexrifyAppTheme.colors
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textMuted,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(18.dp))
    ) { content() }
}

@Composable
private fun CardDivider() {
    val colors = NexrifyAppTheme.colors
    Box(Modifier.fillMaxWidth().padding(start = 58.dp).height(1.dp).background(colors.border.copy(alpha = 0.7f)))
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (enabled) colors.border else colors.border.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = if (enabled) colors.textPrimary else colors.textMuted, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) colors.textPrimary else colors.textMuted, fontWeight = FontWeight.SemiBold)
            Text(sub, color = colors.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background, checkedTrackColor = colors.textPrimary,
                uncheckedThumbColor = colors.textMuted, uncheckedTrackColor = colors.card, uncheckedBorderColor = colors.border
            )
        )
    }
}
