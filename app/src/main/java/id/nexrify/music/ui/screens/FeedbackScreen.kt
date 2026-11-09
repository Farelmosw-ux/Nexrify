package id.nexrify.music.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.data.chat.ChatXApi
import id.nexrify.music.data.chat.FeedbackItem
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.components.NexNotifier
import id.nexrify.music.ui.components.chatErrText
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val FB_GREEN = Color(0xFF3DDC84)
private val FB_RED = Color(0xFFE5484D)
private val FB_AMBER = Color(0xFFF5B83D)
private val FB_BLUE = Color(0xFF60A5FA)

@Composable
private fun statusInfo(status: String): Pair<String, Color> = when (status) {
    "planned" -> S.feedbackStatusPlanned to FB_BLUE
    "progress" -> S.feedbackStatusProgress to FB_AMBER
    "done" -> S.feedbackStatusDone to FB_GREEN
    "rejected" -> S.feedbackStatusRejected to FB_RED
    else -> S.feedbackStatusOpen to NexrifyAppTheme.colors.textMuted
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<FeedbackItem>?>(null) }
    var tab by remember { mutableStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<FeedbackItem?>(null) }
    val fmt = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    fun token(): String = AccountManager.getInstance().token().orEmpty()

    suspend fun load() {
        try { items = ChatXApi.feedbackList(token()) }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { if (items == null) items = emptyList(); NexNotifier.warning(chatErrText(e), currentStrings().feedbackTitle) }
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(NexIcons.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
            }
            Text(S.feedbackTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { scope.launch { load() } }, contentAlignment = Alignment.Center) {
                Icon(NexIcons.Refresh, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip(S.feedbackTabFeatures, tab == 0) { tab = 0 }
            TabChip(S.feedbackTabMine, tab == 1) { tab = 1 }
        }

        val all = items
        if (all == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
        } else {
            val shown = if (tab == 0) all.filter { it.kind == "feature" } else all.filter { it.mine }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
                if (tab == 1) {
                    item {
                        Text(S.feedbackBugPrivacy, fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                if (shown.isEmpty()) {
                    item {
                        Text(
                            if (tab == 0) S.feedbackEmpty else S.feedbackMineEmpty,
                            color = colors.textMuted, textAlign = TextAlign.Center, fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp)
                        )
                    }
                }
                items(shown, key = { it.id }) { it ->
                    val (label, tone) = statusInfo(it.status)
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).background(colors.card).border(1.dp, colors.border, shape).padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (it.kind == "bug") NexIcons.Bug else NexIcons.Lightbulb, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it.title, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(
                                label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tone,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tone.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(it.body, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textSecondary, maxLines = 6)
                        if (it.note.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.border.copy(alpha = 0.6f)).padding(10.dp)) {
                                Text(S.feedbackOwnerNote, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                                Text(it.note, fontSize = 13.sp, color = colors.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (it.kind == "feature" && it.author.isNotBlank()) "@${it.author} · " else "") + fmt.format(Date(it.createdAt)),
                                fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.weight(1f)
                            )
                            if (it.mine && it.status == "open") {
                                Text(
                                    S.feedbackDelete, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FB_RED,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { confirmDelete = it }.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (it.kind == "feature") {
                                val canVote = it.status != "done" && it.status != "rejected"
                                Row(
                                    Modifier.clip(RoundedCornerShape(16.dp))
                                        .background(if (it.voted) colors.textPrimary else Color.Transparent)
                                        .border(1.dp, if (it.voted) colors.textPrimary else colors.border, RoundedCornerShape(16.dp))
                                        .clickable(enabled = canVote) {
                                            scope.launch {
                                                try {
                                                    val upd = ChatXApi.feedbackVote(token(), it.id)
                                                    items = items?.map { x -> if (x.id == upd.id) upd else x }
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (e: Throwable) {
                                                    NexNotifier.warning(chatErrText(e), currentStrings().feedbackTitle)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(NexIcons.Check, null, tint = if (it.voted) colors.background else colors.textPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        S.feedbackVotes(it.votes), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = if (it.voted) colors.background else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(26.dp)).background(colors.textPrimary).clickable { showNew = true },
                contentAlignment = Alignment.Center
            ) { Text(S.feedbackNew, color = colors.background, fontWeight = FontWeight.Bold) }
        }
    }

    if (showNew) {
        NewFeedbackSheet(
            onSent = { item ->
                showNew = false
                items = listOf(item) + (items ?: emptyList())
                tab = if (item.kind == "bug") 1 else 0
                NexNotifier.success(currentStrings().feedbackSent, currentStrings().feedbackTitle)
            },
            onDismiss = { showNew = false }
        )
    }
    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = colors.card,
            title = { Text(S.feedbackDelete, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(item.title, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch {
                        try {
                            ChatXApi.feedbackDelete(token(), item.id)
                            items = items?.filter { it.id != item.id }
                            NexNotifier.info(currentStrings().feedbackDeleted, currentStrings().feedbackTitle)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            NexNotifier.warning(chatErrText(e), currentStrings().feedbackTitle)
                        }
                    }
                }) { Text(S.feedbackDelete, color = FB_RED, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(S.cancel, color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = if (selected) colors.background else colors.textPrimary,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.textPrimary else colors.card)
            .border(1.dp, if (selected) colors.textPrimary else colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFeedbackSheet(onSent: (FeedbackItem) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("feature") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(S.feedbackNew, style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip(S.feedbackKindFeature, kind == "feature") { kind = "feature" }
                TabChip(S.feedbackKindBug, kind == "bug") { kind = "bug" }
            }
            Spacer(Modifier.height(14.dp))
            Field(S.feedbackTitleLabel, title, 80, singleLine = true) { title = it }
            Spacer(Modifier.height(10.dp))
            Field(if (kind == "bug") S.feedbackBodyBug else S.feedbackBodyFeature, body, 800, singleLine = false) { body = it }
            Spacer(Modifier.height(8.dp))
            Text(if (kind == "bug") S.feedbackDeviceNote + " " + S.feedbackBugPrivacy else "", fontSize = 12.sp, lineHeight = 16.sp, color = colors.textMuted)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(26.dp)).background(colors.textPrimary).clickable(enabled = !busy) {
                    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                    val device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), Nexrify $version"
                    scope.launch {
                        busy = true
                        try {
                            val token = AccountManager.getInstance().token().orEmpty()
                            onSent(ChatXApi.feedbackSubmit(token, kind, title.trim(), body.trim(), if (kind == "bug") device else ""))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            NexNotifier.warning(chatErrText(e), currentStrings().feedbackTitle)
                        }
                        busy = false
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                if (busy) CircularProgressIndicator(color = colors.background, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Text(S.feedbackSubmit, color = colors.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, max: Int, singleLine: Boolean, onChange: (String) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text("$label  ${value.length}/$max", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(max).let { t -> if (singleLine) t.replace("\n", " ") else t }) },
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 8,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.height(130.dp))
            )
        }
    }
}
