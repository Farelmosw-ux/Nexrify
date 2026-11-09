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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.chat.ChatManager
import id.nexrify.music.data.chat.ChatMessage
import id.nexrify.music.data.chat.ChatPrivacy
import id.nexrify.music.data.chat.ChatThread
import id.nexrify.music.data.chat.ModReport
import id.nexrify.music.data.chat.ReactionGroup
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

private val XRED = Color(0xFFE5484D)

/** Pesan kesalahan ramah pengguna untuk semua aksi chat. */
fun chatErrText(e: Throwable): String {
    val s = currentStrings()
    if (e !is AccountError) return s.chatGenericError
    return when (e.code) {
        "network" -> s.chatNoNetwork
        "chat_banned" -> s.chatBannedMsg
        "rate_limited" -> s.chatSlowDown
        "duplicate" -> s.chatDuplicate
        "new_account" -> s.chatErrNewAccount
        "mention_spam" -> s.chatErrMentionSpam
        "spam" -> s.chatErrSpam
        "forbidden" -> s.chatErrForbidden
        "edit_expired" -> s.chatEditExpired
        "translate_failed" -> s.chatTranslateFail
        else -> if (!e.message.isNullOrBlank() && e.code != "server") e.message ?: s.chatGenericError else s.chatGenericError
    }
}

@Composable
private fun XHandle() {
    val colors = NexrifyAppTheme.colors
    Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
}

@Composable
private fun XButton(text: String, filled: Boolean, modifier: Modifier = Modifier, busy: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val tone = if (danger) XRED else colors.textPrimary
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(if (filled) tone else Color.Transparent)
            .border(1.dp, if (filled) tone else if (danger) XRED.copy(alpha = 0.5f) else colors.border, shape)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (busy) CircularProgressIndicator(color = if (filled) colors.background else tone, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Text(text, color = if (filled) (if (danger) Color.White else colors.background) else tone, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun XField(label: String, value: String, hint: String, max: Int, singleLine: Boolean = true, onChange: (String) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text("$label  ${value.length}/$max", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = colors.textMuted, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(max).let { t -> if (singleLine) t.replace("\n", " ") else t }) },
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 4,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = if (selected) colors.background else colors.textPrimary,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.textPrimary else colors.card)
            .border(1.dp, if (selected) colors.textPrimary else colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/* ======================= pencarian pesan ======================= */

private fun highlight(text: String, kw: String, base: Color, mark: Color): AnnotatedString {
    if (kw.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        val lower = text.lowercase()
        val k = kw.lowercase()
        while (i < text.length) {
            val at = lower.indexOf(k, i)
            if (at < 0) { append(text.substring(i)); break }
            append(text.substring(i, at))
            withStyle(SpanStyle(color = base, fontWeight = FontWeight.ExtraBold, background = mark)) { append(text.substring(at, at + k.length)) }
            i = at + k.length
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageSearchSheet(onJump: (ChatMessage) -> Unit, onSwitchToPeople: () -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    var keyword by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(0L) }
    var to by remember { mutableStateOf(0L) }
    var dateLabel by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    fun dayStart(offsetDays: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_YEAR, offsetDays)
        return c.timeInMillis
    }

    LaunchedEffect(keyword, user, from, to) {
        val empty = keyword.isBlank() && user.isBlank() && from == 0L && to == 0L
        if (empty) { results = emptyList(); searched = false; loading = false; return@LaunchedEffect }
        loading = true
        delay(380)
        try {
            results = manager.searchMessages(keyword.trim(), user.trim().removePrefix("@"), from, to)
            searched = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            NexNotifier.warning(chatErrText(e), currentStrings().chatSearchMessages)
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(S.chatSearchTabMessages, true) {}
                Chip(S.chatSearchTabPeople, false, onSwitchToPeople)
            }
            Spacer(Modifier.height(12.dp))
            XField(S.chatSearchKeyword, keyword, S.chatSearchKeyword, 60) { keyword = it }
            Spacer(Modifier.height(8.dp))
            XField(S.chatSearchUser, user, "@username", 20) { user = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '@' } }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(S.chatSearchAnyDate, dateLabel == null) { from = 0; to = 0; dateLabel = null }
                Chip(S.chatToday, dateLabel == "today") { from = dayStart(0); to = dayStart(1) - 1; dateLabel = "today" }
                Chip(S.chatYesterday, dateLabel == "yesterday") { from = dayStart(-1); to = dayStart(0) - 1; dateLabel = "yesterday" }
                Chip(S.chatPollDays(7), dateLabel == "week") { from = dayStart(-6); to = 0; dateLabel = "week" }
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(if (dateLabel?.startsWith("d:") == true) colors.textPrimary else colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp)).clickable { pickDate = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val on = dateLabel?.startsWith("d:") == true
                    Icon(NexIcons.Calendar, null, tint = if (on) colors.background else colors.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (on) dateLabel!!.removePrefix("d:") else S.chatSearchPickDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) colors.background else colors.textPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                !searched -> Text(S.chatSearchStartHint, color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
                results.isEmpty() -> Text(S.chatSearchNoResult, color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
                else -> {
                    Text(S.chatSearchCount(results.size), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(results, key = { it.seq }) { m ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onJump(m) }.padding(vertical = 9.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ChatAvatar(m.uid, m.av, m.display, 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(m.display, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        Text("  @${m.username}", color = colors.textMuted, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Text(
                                        highlight(if (m.poll != null) m.poll.question else m.text, keyword.trim(), colors.textPrimary, colors.textPrimary.copy(alpha = 0.2f)),
                                        color = colors.textSecondary, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(fmt.format(Date(m.ts)), color = colors.textMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val sel = state.selectedDateMillis
                    if (sel != null) {
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = sel }
                        val local = Calendar.getInstance().apply {
                            clear(); set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                        }
                        from = local.timeInMillis
                        to = from + 86_400_000L - 1
                        dateLabel = "d:" + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(from))
                    }
                    pickDate = false
                }) { Text("OK", color = colors.textPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(S.cancel, color = colors.textSecondary) } }
        ) { DatePicker(state = state) }
    }
}

/* ======================= thread ======================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadSheet(root: ChatMessage, myId: String, myUsername: String, onOpenProfile: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    val scope = rememberCoroutineScope()
    var thread by remember { mutableStateOf<ChatThread?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(reload) {
        try { thread = manager.thread(root.seq); failed = false }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { failed = true }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(NexIcons.ChatBubbleOutline, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.chatThreadTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.weight(1f))
                thread?.let { Text(S.chatRepliesCount(it.replies.size), fontSize = 12.sp, color = colors.textMuted) }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val th = thread
                when {
                    th != null -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                        item(key = "root") {
                            ChatBubble(
                                msg = th.root, mine = th.root.uid == myId, myId = myId, myUsername = myUsername, showHeader = true, highlighted = false,
                                onLongPress = {}, onReplyClick = {}, onAvatarClick = { onOpenProfile(th.root.username) }, onReactionClick = {}, onRetry = {}
                            )
                            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(1.dp).background(colors.border))
                        }
                        if (th.replies.isEmpty()) {
                            item(key = "empty") {
                                Text(S.chatThreadEmpty, color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                            }
                        }
                        items(th.replies, key = { it.seq }) { m ->
                            ChatBubble(
                                msg = m, mine = m.uid == myId, myId = myId, myUsername = myUsername, showHeader = true, highlighted = false,
                                onLongPress = {}, onReplyClick = {}, onAvatarClick = { onOpenProfile(m.username) }, onReactionClick = {}, onRetry = {}
                            )
                        }
                    }
                    failed -> Text(S.chatGenericError, color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 11.dp)) {
                    if (text.isEmpty()) Text(S.chatThreadHint, color = colors.textMuted, fontSize = 15.sp)
                    BasicTextField(
                        value = text, onValueChange = { text = it.take(500) }, maxLines = 4,
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp), cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                val can = text.isNotBlank()
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(if (can) colors.textPrimary else colors.card).border(1.dp, if (can) colors.textPrimary else colors.border, CircleShape)
                        .clickable(enabled = can) {
                            manager.send(text, root)
                            text = ""
                            scope.launch { delay(1600); reload++ }
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(NexIcons.Send, null, tint = if (can) colors.background else colors.textMuted, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

/* ======================= daftar reaksi ======================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatReactorsSheet(msg: ChatMessage, onOpenProfile: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    var groups by remember { mutableStateOf<List<ReactionGroup>?>(null) }
    var selected by remember { mutableStateOf<String?>(null) } // null = semua

    LaunchedEffect(msg.seq) {
        try { groups = manager.reactors(msg.seq) }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { groups = emptyList(); NexNotifier.warning(chatErrText(e), currentStrings().chatReactionsTitle) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(horizontal = 20.dp)) {
            val g = groups
            val total = g?.sumOf { it.users.size } ?: 0
            Text(S.chatReactionsTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            when {
                g == null -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                g.isEmpty() -> Text(S.chatNoReactions, color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                else -> {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(S.chatReactionsTotal(total), selected == null) { selected = null }
                        g.forEach { grp -> Chip("${grp.emoji} ${grp.users.size}", selected == grp.emoji) { selected = grp.emoji } }
                    }
                    Spacer(Modifier.height(10.dp))
                    val rows = if (selected == null) g.flatMap { grp -> grp.users.map { it to grp.emoji } } else g.filter { it.emoji == selected }.flatMap { grp -> grp.users.map { it to grp.emoji } }
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(rows, key = { (p, e) -> p.uid + e }) { (p, e) ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenProfile(p.username) }.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChatAvatar(p.uid, p.av, p.display, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.display, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("@${p.username}", color = colors.textMuted, fontSize = 12.sp)
                                }
                                Text(e, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ======================= buat polling ======================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPollCreateSheet(onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var hours by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NexIcons.BarChart, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.chatPollCreate, style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            }
            Spacer(Modifier.height(14.dp))
            XField(S.chatPollQuestion, question, "", 200, singleLine = false) { question = it }
            Spacer(Modifier.height(12.dp))
            options.forEachIndexed { i, opt ->
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f)) { XField(S.chatPollOption(i + 1), opt, "", 60) { options[i] = it } }
                    if (options.size > 2) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(colors.card).border(1.dp, colors.border, CircleShape).clickable { options.removeAt(i) },
                            contentAlignment = Alignment.Center
                        ) { Icon(NexIcons.Close, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            if (options.size < 6) {
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable { options.add("") }.padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(NexIcons.Add, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(S.chatPollAddOption, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(S.chatPollDuration.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(S.chatPollNoLimit, hours == 0) { hours = 0 }
                Chip(S.chatPollHours(1), hours == 1) { hours = 1 }
                Chip(S.chatPollHours(24), hours == 24) { hours = 24 }
                Chip(S.chatPollDays(3), hours == 72) { hours = 72 }
                Chip(S.chatPollDays(7), hours == 168) { hours = 168 }
            }
            Spacer(Modifier.height(22.dp))
            XButton(S.chatPollPublish, filled = true, busy = busy, modifier = Modifier.fillMaxWidth()) {
                val opts = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                if (question.trim().length < 4 || opts.size < 2) {
                    NexNotifier.warning(currentStrings().chatPollNeed, currentStrings().chatPollCreate)
                    return@XButton
                }
                scope.launch {
                    busy = true
                    try { manager.createPoll(question.trim(), opts, hours); onDismiss() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Throwable) { NexNotifier.warning(chatErrText(e), currentStrings().chatPollCreate) }
                    busy = false
                }
            }
        }
    }
}

/* ======================= privasi dan blokir ======================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPrivacySheet(onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    val scope = rememberCoroutineScope()
    val my by manager.my.collectAsState()
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { manager.refreshMy() } }

    fun update(p: ChatPrivacy) {
        scope.launch {
            busy = true
            try { manager.setPrivacy(p) }
            catch (e: CancellationException) { throw e }
            catch (e: Throwable) { NexNotifier.warning(chatErrText(e), currentStrings().chatPrivacyTitle) }
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        val d = my
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(colors.card).border(1.dp, colors.border, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(NexIcons.Lock, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(S.chatPrivacyTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            }
            if (d == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(18.dp))) {
                    PrivacyRow(NexIcons.EyeOff, S.chatPrivacyPresence, S.chatPrivacyPresenceSub, d.privacy.hidePresence, !busy) { update(d.privacy.copy(hidePresence = it)) }
                    Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(colors.border.copy(alpha = 0.7f)))
                    PrivacyRow(NexIcons.Person, S.chatPrivacyProfile, S.chatPrivacyProfileSub, d.privacy.hideProfile, !busy) { update(d.privacy.copy(hideProfile = it)) }
                    Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(colors.border.copy(alpha = 0.7f)))
                    PrivacyRow(NexIcons.Headphones, S.chatPrivacyMusic, S.chatPrivacyMusicSub, d.privacy.hideMusic, !busy) { update(d.privacy.copy(hideMusic = it)) }
                }
                if (d.warns > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5B83D).copy(alpha = 0.14f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(NexIcons.Shield, null, tint = Color(0xFFF5B83D), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(S.chatYourWarns(d.warns), fontSize = 13.sp, color = colors.textPrimary)
                    }
                }
                Text(
                    "${S.chatBlockedUsers}  ·  ${d.blocked.size}".uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted,
                    modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp)
                )
                if (d.blocked.isEmpty()) {
                    Text(S.chatNoBlocked, color = colors.textMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                } else {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(18.dp))) {
                        d.blocked.forEachIndexed { i, p ->
                            if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(1.dp).background(colors.border.copy(alpha = 0.7f)))
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                ChatAvatar(p.uid, p.av, p.display, 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.display, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("@${p.username}", color = colors.textMuted, fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        try { manager.block(p.username, false); NexNotifier.info(currentStrings().chatUnblockedOk(p.username), currentStrings().chatPrivacyTitle) }
                                        catch (e: CancellationException) { throw e }
                                        catch (e: Throwable) { NexNotifier.warning(chatErrText(e), currentStrings().chatPrivacyTitle) }
                                    }
                                }) { Text(S.chatUnblockAction, color = colors.textPrimary, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(icon: ImageVector, title: String, sub: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.border), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(sub, color = colors.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background, checkedTrackColor = colors.textPrimary,
                uncheckedThumbColor = colors.textMuted, uncheckedTrackColor = colors.card, uncheckedBorderColor = colors.border
            )
        )
    }
}

/* ======================= moderasi ======================= */

@Composable
fun ModWarnDialog(username: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text("${S.chatModWarn} @$username", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                XField(S.chatModWarnReason, reason, "", 120) { reason = it }
                Spacer(Modifier.height(8.dp))
                Text(S.chatModWarnNote, fontSize = 12.sp, color = colors.textMuted)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }) { Text(S.chatModWarn, color = colors.textPrimary, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel, color = colors.textSecondary) } }
    )
}

@Composable
fun ModMuteDialog(username: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    var hours by remember { mutableStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text("${S.chatModMute} @$username", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(S.chatModMuteDuration, fontSize = 12.sp, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 6, 24, 72, 168).forEach { h -> Chip(if (h >= 24) S.chatPollDays(h / 24) else S.chatModHours(h), hours == h) { hours = h } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hours) }) { Text(S.chatModMute, color = XRED, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel, color = colors.textSecondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatReportsSheet(onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val manager = remember { ChatManager.getInstance() }
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<ModReport>?>(null) }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    suspend fun load() {
        try { reports = manager.modReports() }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { reports = emptyList(); NexNotifier.warning(chatErrText(e), currentStrings().chatModTitle) }
    }
    LaunchedEffect(Unit) { load() }

    fun act(r: ModReport, block: suspend () -> Unit) {
        scope.launch {
            try { block(); manager.modReportClear(r.id); load() }
            catch (e: CancellationException) { throw e }
            catch (e: Throwable) { NexNotifier.warning(chatErrText(e), currentStrings().chatModTitle) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { XHandle() }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NexIcons.Shield, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.chatModReports, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.weight(1f))
                reports?.let { Text("${it.size}", fontSize = 12.sp, color = colors.textMuted) }
            }
            Spacer(Modifier.height(10.dp))
            val list = reports
            when {
                list == null -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                list.isEmpty() -> Text(S.chatModReportsEmpty, color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                else -> LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { r ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(18.dp)).padding(14.dp)) {
                            Text("@${r.author}", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text(S.chatModReportBy(r.by, r.reason), fontSize = 11.sp, color = colors.textMuted)
                            Spacer(Modifier.height(8.dp))
                            Text(r.text.ifBlank { "…" }, fontSize = 14.sp, lineHeight = 19.sp, color = colors.textSecondary)
                            Text(fmt.format(Date(r.ts)), fontSize = 10.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                XButton(S.chatDeleteAction, filled = false, danger = true, modifier = Modifier.weight(1f)) { act(r) { manager.modDelete(r.seq) } }
                                XButton(S.chatModWarn, filled = false, modifier = Modifier.weight(1f)) { act(r) { manager.modWarn(r.author, r.reason) } }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                XButton("${S.chatModMute} 24", filled = false, modifier = Modifier.weight(1f)) { act(r) { manager.modMute(r.author, 24) } }
                                XButton(S.chatModDismiss, filled = false, modifier = Modifier.weight(1f)) { act(r) { } }
                            }
                        }
                    }
                }
            }
        }
    }
}
