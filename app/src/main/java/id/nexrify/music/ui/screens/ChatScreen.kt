package id.nexrify.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.data.chat.CHAT_MAX_LENGTH
import id.nexrify.music.data.chat.ChatConnection
import id.nexrify.music.data.chat.ChatManager
import id.nexrify.music.data.chat.ChatMessage
import id.nexrify.music.data.chat.ChatTopTrack
import id.nexrify.music.data.chat.ChatUserHit
import id.nexrify.music.data.chat.SendStatus
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.components.ChatAvatar
import id.nexrify.music.ui.components.ChatBubble
import id.nexrify.music.ui.components.ChatDateHeader
import id.nexrify.music.ui.components.ChatDeleteDialog
import id.nexrify.music.ui.components.ChatMessageMenu
import id.nexrify.music.ui.components.ChatProfileSheet
import id.nexrify.music.ui.components.ChatReportDialog
import id.nexrify.music.ui.components.ChatSearchSheet
import id.nexrify.music.ui.components.ChatSettingsSheet
import id.nexrify.music.ui.components.NexNotifier
import id.nexrify.music.ui.components.chatDayKey
import id.nexrify.music.ui.components.chatDayLabel
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class ChatRow {
    data class Header(val dayKey: Long) : ChatRow()
    data class Msg(val msg: ChatMessage, val showHeader: Boolean) : ChatRow()
}

private const val EDIT_WINDOW_MS = 3_600_000L

@Composable
fun ChatScreen(viewModel: MusicViewModel, highlightSeq: Long, onBack: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ChatManager.getInstance() }
    val settings = manager.settings

    val messages by manager.messages.collectAsState()
    val typing by manager.typing.collectAsState()
    val connection by manager.connection.collectAsState()
    val loaded by manager.loaded.collectAsState()
    val muted by settings.muted.collectAsState()
    val rulesSeen by settings.rulesSeen.collectAsState()
    val account by AccountManager.getInstance().user.collectAsState()
    val myId = account?.id.orEmpty()
    val myName = account?.username.orEmpty()

    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    // Polling hanya jalan selama layar ini terlihat.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) manager.setActive(true)
            if (event == Lifecycle.Event.ON_PAUSE) manager.setActive(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) manager.setActive(true)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.setActive(false)
        }
    }

    // Statistik untuk profil: lagu disukai, jumlah playlist, dan lagu yang sering diputar.
    LaunchedEffect(favorites.size, playlists.size) {
        val top = (viewModel.onRepeatTracks.value.ifEmpty { viewModel.recentlyPlayed.value })
            .take(3).map { ChatTopTrack(it.title, it.artist) }
        manager.pushStats(favorites.size, playlists.size, top)
    }

    var input by remember { mutableStateOf(TextFieldValue("")) }
    var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editing by remember { mutableStateOf<ChatMessage?>(null) }
    var menuMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var reportMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var profileName by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var highlighted by remember { mutableStateOf<Long?>(null) }
    var unseen by remember { mutableStateOf(0) }
    var lastToast by remember { mutableStateOf(0L) }

    // ---- baris daftar: terbaru di bawah (reverseLayout), dengan pemisah tanggal ----
    val rows = remember(messages) {
        val asc = ArrayList<ChatRow>()
        var lastDay = -1L
        var prev: ChatMessage? = null
        for (m in messages) {
            val day = chatDayKey(m.ts)
            if (day != lastDay) { asc.add(ChatRow.Header(day)); lastDay = day; prev = null }
            val sameGroup = prev != null && !m.announce && prev.uid == m.uid && !prev.announce && m.ts - prev.ts < 180_000L
            asc.add(ChatRow.Msg(m, showHeader = !sameGroup))
            prev = m
        }
        asc.reversed()
    }
    val listState = rememberLazyListState()
    val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex <= 1 } }

    fun rowKey(r: ChatRow): String = when (r) {
        is ChatRow.Header -> "d${r.dayKey}"
        is ChatRow.Msg -> if (r.msg.localId != null) "p${r.msg.localId}" else "m${r.msg.seq}"
    }

    // Pesan baru: ikut turun kalau sudah di bawah atau pesan kita sendiri, kalau tidak hitung sebagai belum dibaca.
    var lastCount by remember { mutableStateOf(0) }
    LaunchedEffect(messages.size, messages.lastOrNull()?.seq, messages.lastOrNull()?.status) {
        val last = messages.lastOrNull()
        val added = messages.size - lastCount
        lastCount = messages.size
        if (last == null || added <= 0) return@LaunchedEffect
        if (atBottom || last.uid == myId) {
            listState.animateScrollToItem(0)
            unseen = 0
        } else {
            unseen += added
        }
    }
    LaunchedEffect(atBottom) { if (atBottom) unseen = 0 }

    fun jumpTo(seq: Long) {
        val idx = rows.indexOfFirst { it is ChatRow.Msg && it.msg.seq == seq }
        if (idx < 0) {
            NexNotifier.info(currentStrings().chatJumpToOriginal, currentStrings().chatTitle)
            return
        }
        scope.launch {
            listState.animateScrollToItem(idx)
            highlighted = seq
            delay(1800)
            if (highlighted == seq) highlighted = null
        }
    }

    // Dibuka dari notifikasi: sorot pesan terkait begitu riwayat termuat.
    var jumped by remember { mutableStateOf(false) }
    LaunchedEffect(loaded, rows.size) {
        if (!jumped && highlightSeq > 0 && loaded && rows.isNotEmpty()) {
            jumped = true
            delay(250)
            jumpTo(highlightSeq)
        }
    }

    // Mention, balasan, dan pengumuman yang masuk saat kita sedang menggulir ke atas.
    LaunchedEffect(Unit) {
        manager.events.collect { e ->
            val now = System.currentTimeMillis()
            if (!atBottom && now - lastToast > 8_000L) {
                lastToast = now
                val s = currentStrings()
                when (e.kind) {
                    "mention" -> NexNotifier.info(e.message.text.take(80), s.chatNotifMentionTitle(e.message.username))
                    "reply" -> NexNotifier.info(e.message.text.take(80), s.chatNotifReplyTitle(e.message.username))
                    else -> NexNotifier.info(e.message.text.take(80), s.chatNotifAnnounceTitle)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        manager.errors.collect { code ->
            val s = currentStrings()
            val text = when (code) {
                "chat_banned" -> s.chatBannedMsg
                "rate_limited" -> s.chatSlowDown
                "duplicate" -> s.chatDuplicate
                "edit_expired" -> s.chatEditExpired
                "network" -> s.chatNoNetwork
                else -> s.chatGenericError
            }
            NexNotifier.warning(text, s.chatTitle)
        }
    }

    // ---- autocomplete @mention ----
    val cursor = input.selection.start.coerceIn(0, input.text.length)
    val mentionToken = remember(input.text, cursor) { Regex("(?:^|[^A-Za-z0-9_])@([A-Za-z0-9_]{0,20})$").find(input.text.substring(0, cursor)) }
    val mentionPrefix = mentionToken?.groupValues?.get(1)?.lowercase()
    var remoteHits by remember { mutableStateOf<List<ChatUserHit>>(emptyList()) }
    LaunchedEffect(mentionPrefix) {
        remoteHits = emptyList()
        if (mentionPrefix == null || mentionPrefix.isEmpty()) return@LaunchedEffect
        delay(250)
        remoteHits = manager.searchUsers(mentionPrefix)
    }
    val suggestions = remember(mentionPrefix, remoteHits, messages) {
        if (mentionPrefix == null) emptyList<ChatUserHit>()
        else {
            val local = manager.recentParticipants().filter { it.username.startsWith(mentionPrefix) }
            (local + remoteHits.filter { it.username.startsWith(mentionPrefix) }).distinctBy { it.username }
                .filter { !it.username.equals(myName, true) }.take(6)
        }
    }

    fun insertMention(username: String) {
        val start = cursor - (mentionPrefix?.length ?: 0)
        val before = input.text.substring(0, start)
        val after = input.text.substring(cursor)
        val text = "$before$username $after"
        input = TextFieldValue(text, TextRange(before.length + username.length + 1))
    }

    fun submit() {
        val text = input.text.trim()
        if (text.isEmpty()) return
        val edit = editing
        if (edit != null) {
            scope.launch { manager.edit(edit.seq, text) }
            editing = null
        } else {
            manager.send(text, replyTo)
            replyTo = null
        }
        input = TextFieldValue("")
    }

    fun copy(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Global Chat", text))
        NexNotifier.success(currentStrings().chatCopied, currentStrings().chatTitle)
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        // ---- header ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderIcon(NexIcons.ArrowBack, onClick = onBack)
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(S.chatTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                val sub = when {
                    typing.isNotEmpty() -> when (typing.size) {
                        1 -> S.chatTypingOne(typing[0])
                        2 -> S.chatTypingTwo(typing[0], typing[1])
                        else -> S.chatTypingMany(typing.size)
                    }
                    connection == ChatConnection.OFFLINE -> S.chatOffline
                    connection == ChatConnection.CONNECTING && !loaded -> S.chatConnecting
                    else -> S.chatSubtitle
                }
                Text(
                    sub,
                    fontSize = 12.sp,
                    color = if (typing.isNotEmpty()) colors.textPrimary else colors.textMuted,
                    fontWeight = if (typing.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HeaderIcon(NexIcons.Search) { showSearch = true }
            HeaderIcon(NexIcons.Settings) { showSettings = true }
            Spacer(Modifier.width(4.dp))
            ChatAvatar(myId, account?.let { if (it.avatar.isNotEmpty()) "1" else "" }.orEmpty(), account?.display ?: "?", 34.dp, Modifier.clickable { profileName = myName })
            Spacer(Modifier.width(4.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        if (!rulesSeen) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(14.dp)).background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(NexIcons.Info, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(S.chatRulesBanner, fontSize = 12.sp, lineHeight = 17.sp, color = colors.textSecondary, modifier = Modifier.weight(1f))
                HeaderIcon(NexIcons.Close, small = true) { settings.dismissRules() }
            }
        }

        // ---- daftar pesan ----
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !loaded && messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                messages.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(NexIcons.ChatBubbleOutline, null, tint = colors.textMuted, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(S.chatEmptyTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(S.chatEmptySub, color = colors.textMuted, textAlign = TextAlign.Center, style = Typography.bodySmall)
                }
                else -> LazyColumn(state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                    items(rows, key = { rowKey(it) }) { row ->
                        when (row) {
                            is ChatRow.Header -> ChatDateHeader(chatDayLabel(row.dayKey, S))
                            is ChatRow.Msg -> {
                                val m = row.msg
                                ChatBubble(
                                    msg = m,
                                    mine = m.uid == myId,
                                    myId = myId,
                                    myUsername = myName,
                                    showHeader = row.showHeader,
                                    highlighted = highlighted == m.seq,
                                    onLongPress = { menuMsg = m },
                                    onReplyClick = { jumpTo(it) },
                                    onAvatarClick = { if (!m.announce) profileName = m.username },
                                    onReactionClick = { manager.react(m.seq, it) },
                                    onRetry = { m.localId?.let { id -> manager.retry(id) } }
                                )
                            }
                        }
                    }
                }
            }

            if (connection == ChatConnection.OFFLINE && messages.isNotEmpty()) {
                Text(
                    S.chatOffline,
                    fontSize = 11.sp,
                    color = colors.background,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(colors.textPrimary).padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            if (!atBottom) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(16.dp).size(44.dp).clip(CircleShape).background(colors.card)
                        .border(1.dp, colors.border, CircleShape).clickable { scope.launch { listState.animateScrollToItem(0) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NexIcons.KeyboardArrowDown, S.chatNewMessages, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                }
                if (unseen > 0) {
                    Text(
                        if (unseen > 99) "99+" else unseen.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.background,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 52.dp)
                            .clip(CircleShape).background(colors.textPrimary).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // ---- saran mention ----
        if (suggestions.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                suggestions.forEach { hit ->
                    Row(
                        Modifier.fillMaxWidth().clickable { insertMention(hit.username) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatAvatar(hit.uid, "1", hit.display, 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(hit.display, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text("  @${hit.username}", color = colors.textMuted, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // ---- banner balas / edit ----
        val banner = editing ?: replyTo
        if (banner != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (editing != null) NexIcons.Edit else NexIcons.Reply, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (editing != null) S.chatEditing else S.chatReplyingTo("@${banner.username}"),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary
                    )
                    Text(banner.text, fontSize = 12.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HeaderIcon(NexIcons.Close, small = true) {
                    if (editing != null) { editing = null; input = TextFieldValue("") }
                    replyTo = null
                }
            }
        }

        // ---- kolom input ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                if (input.text.isEmpty()) Text(S.chatInputHint, color = colors.textMuted, fontSize = 15.sp)
                BasicTextField(
                    value = input,
                    onValueChange = { v ->
                        val clipped = if (v.text.length > CHAT_MAX_LENGTH) v.copy(text = v.text.take(CHAT_MAX_LENGTH)) else v
                        if (clipped.text.length > input.text.length && clipped.text.isNotBlank()) manager.notifyTyping()
                        input = clipped
                    },
                    maxLines = 5,
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(colors.textPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            val canSend = input.text.isNotBlank()
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(if (canSend) colors.textPrimary else colors.card)
                    .border(1.dp, if (canSend) colors.textPrimary else colors.border, CircleShape)
                    .clickable(enabled = canSend) { submit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (editing != null) NexIcons.Check else NexIcons.Send, null,
                    tint = if (canSend) colors.background else colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (input.text.length > 400) {
            Text(
                "${input.text.length}/$CHAT_MAX_LENGTH",
                fontSize = 11.sp,
                color = if (input.text.length >= CHAT_MAX_LENGTH) Color(0xFFE5484D) else colors.textMuted,
                modifier = Modifier.padding(start = 24.dp, bottom = 6.dp)
            )
        }
    }

    // ---- menu dan dialog ----
    menuMsg?.let { m ->
        val mine = m.uid == myId
        ChatMessageMenu(
            msg = m,
            mine = mine,
            canEdit = System.currentTimeMillis() - m.ts < EDIT_WINDOW_MS && m.seq > 0,
            isMuted = m.username.lowercase() in muted,
            myId = myId,
            onReact = { e -> if (m.seq > 0) manager.react(m.seq, e); menuMsg = null },
            onReply = { replyTo = m; editing = null; menuMsg = null },
            onCopy = { copy(m.text); menuMsg = null },
            onEdit = { editing = m; replyTo = null; input = TextFieldValue(m.text, TextRange(m.text.length)); menuMsg = null },
            onDelete = { deleteMsg = m; menuMsg = null },
            onReport = { reportMsg = m; menuMsg = null },
            onProfile = { if (!m.announce) profileName = m.username; menuMsg = null },
            onToggleMute = {
                val s = currentStrings()
                if (m.username.lowercase() in muted) { settings.unmute(m.username); NexNotifier.info(s.chatUnmuted("@${m.username}"), s.chatTitle) }
                else { settings.mute(m.username); NexNotifier.info(s.chatMuted("@${m.username}"), s.chatTitle) }
                menuMsg = null
            },
            onDismiss = { menuMsg = null }
        )
    }
    deleteMsg?.let { m ->
        ChatDeleteDialog(
            onConfirm = { scope.launch { manager.delete(m.seq) }; deleteMsg = null },
            onDismiss = { deleteMsg = null }
        )
    }
    reportMsg?.let { m ->
        ChatReportDialog(
            onPick = { reason ->
                scope.launch {
                    val ok = manager.report(m.seq, reason)
                    if (ok) NexNotifier.success(currentStrings().chatReportDone, currentStrings().chatTitle)
                }
                reportMsg = null
            },
            onDismiss = { reportMsg = null }
        )
    }
    profileName?.let { name ->
        val isMe = name.equals(myName, true)
        ChatProfileSheet(
            username = name,
            isMe = isMe,
            isMuted = name.lowercase() in muted,
            onMention = { u ->
                val base = input.text
                val text = (if (base.isEmpty() || base.endsWith(" ")) base else "$base ") + "@$u "
                input = TextFieldValue(text, TextRange(text.length))
                profileName = null
            },
            onToggleMute = {
                val s = currentStrings()
                if (name.lowercase() in muted) { settings.unmute(name); NexNotifier.info(s.chatUnmuted("@$name"), s.chatTitle) }
                else { settings.mute(name); NexNotifier.info(s.chatMuted("@$name"), s.chatTitle) }
            },
            onDismiss = { profileName = null }
        )
    }
    if (showSearch) ChatSearchSheet(onOpenProfile = { showSearch = false; profileName = it }, onDismiss = { showSearch = false })
    if (showSettings) ChatSettingsSheet(onDismiss = { showSettings = false })
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, small: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Box(
        Modifier.size(if (small) 32.dp else 40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = colors.textPrimary, modifier = Modifier.size(if (small) 18.dp else 22.dp))
    }
}
