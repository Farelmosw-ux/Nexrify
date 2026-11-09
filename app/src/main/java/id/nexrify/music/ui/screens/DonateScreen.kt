package id.nexrify.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.data.donate.DonateApi
import id.nexrify.music.data.donate.DonateConfig
import id.nexrify.music.data.donate.ProofReader
import id.nexrify.music.data.donate.NexProofResult
import id.nexrify.music.data.donate.DonateManager
import id.nexrify.music.data.donate.Donation
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.components.ChatAvatar
import id.nexrify.music.ui.components.ChatProfileSheet
import id.nexrify.music.ui.components.DonorBanner
import id.nexrify.music.ui.components.NexNotifier
import id.nexrify.music.ui.components.TopTag
import id.nexrify.music.ui.components.formatRp
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val GREEN = Color(0xFF3DDC84)
private val RED = Color(0xFFE5484D)
private val AMBER = Color(0xFFF5B83D)

private fun errText(e: Throwable): String {
    val s = currentStrings()
    return when {
        e is AccountError && e.code == "network" -> s.chatNoNetwork
        e is AccountError && !e.message.isNullOrBlank() && e.code != "server" -> e.message ?: s.donateGenericError
        else -> s.donateGenericError
    }
}

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    val home by DonateManager.home.collectAsState()
    val history by DonateManager.history.collectAsState()
    val historyLoaded by DonateManager.historyLoaded.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var showNew by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<String?>(null) }
    var profileName by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    suspend fun loadAll() {
        loading = true
        try {
            DonateManager.refreshHome()
            DonateManager.refreshHistory()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            NexNotifier.warning(errText(e), currentStrings().donateTitle)
        }
        loading = false
    }

    LaunchedEffect(Unit) { loadAll() }

    // Selama ada donasi yang sedang diperiksa owner, cek statusnya berkala dan beri tahu kalau sudah diterima.
    val reviewing = history.any { it.status == "review" }
    LaunchedEffect(reviewing) {
        if (!reviewing) return@LaunchedEffect
        while (true) {
            delay(7000)
            val before = DonateManager.history.value.filter { it.status == "review" }.map { it.id }
            try { DonateManager.refreshHistory() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Throwable) { continue }
            val now = DonateManager.history.value
            if (before.any { id -> now.firstOrNull { it.id == id }?.status == "paid" }) {
                NexNotifier.success(currentStrings().donatePaidToast, currentStrings().donateTitle)
                runCatching { DonateManager.refreshHome() }
            }
            if (now.none { it.status == "review" }) break
        }
    }

    val cfg = home?.config
    val top = home?.top.orEmpty()

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircle(NexIcons.ArrowBack, onBack)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(S.donateTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Text(S.donateSubtitle, fontSize = 12.sp, color = colors.textMuted)
            }
            IconCircle(NexIcons.Refresh) { scope.launch { loadAll() } }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)) {
            // ---- ajakan donasi ----
            item {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(colors.card).border(1.dp, colors.border, shape).padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFE9A0), Color(0xFFD9962B)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(NexIcons.Gift, null, tint = Color(0xFF2A1B00), modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(S.donateHeroTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(S.donateHeroBody, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(16.dp))
                    val enabled = cfg?.enabled != false && cfg != null
                    Box(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(26.dp))
                            .background(if (enabled) colors.textPrimary else colors.border)
                            .clickable(enabled = enabled) { showNew = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cfg == null && loading) CircularProgressIndicator(color = colors.background, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text(
                            if (cfg?.enabled == false) S.donateClosed else S.donateNow,
                            color = if (enabled) colors.background else colors.textMuted, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---- Top Donatur ----
            item {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(NexIcons.Crown, null, tint = AMBER, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(S.donateTopTitle, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                }
                Spacer(Modifier.height(12.dp))
            }
            if (top.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(20.dp)).padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(NexIcons.Crown, null, tint = colors.textMuted, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(S.donateTopEmpty, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(S.donateTopEmptySub, fontSize = 12.sp, color = colors.textMuted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(top, key = { "top" + it.uid }) { t ->
                    DonorBanner(t, onClick = { profileName = t.username }, modifier = Modifier.padding(bottom = 10.dp))
                }
            }

            // ---- title milik sendiri ----
            val myRank = home?.myRank ?: 0
            if (myRank > 0) {
                item {
                    var on by remember(home?.titleOn) { mutableStateOf(home?.titleOn ?: true) }
                    Spacer(Modifier.height(6.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(20.dp)).padding(16.dp)
                    ) {
                        Text(S.donateMyTitle.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(S.donateYouAre(myRank), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Spacer(Modifier.height(6.dp))
                                TopTag(myRank)
                            }
                            Switch(
                                checked = on,
                                onCheckedChange = { v ->
                                    on = v
                                    scope.launch {
                                        try { DonateManager.setTitle(v) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) {
                                            on = !v
                                            NexNotifier.warning(errText(e), currentStrings().donateTitle)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.background, checkedTrackColor = colors.textPrimary,
                                    uncheckedThumbColor = colors.textMuted, uncheckedTrackColor = colors.card, uncheckedBorderColor = colors.border
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(S.donateTitleSwitch, fontSize = 12.sp, color = colors.textMuted)
                    }
                }
            }

            // ---- riwayat ----
            item {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(S.donateHistory, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.weight(1f))
                    if (history.any { !it.isActive }) {
                        Text(
                            S.donateDeleteAll, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RED,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { confirmClear = true }.padding(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (history.isEmpty()) {
                item {
                    Text(
                        if (historyLoaded) S.donateHistoryEmpty else "…",
                        color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                items(history, key = { "h" + it.id }) { d -> HistoryRow(d) { openId = d.id } }
            }
        }
    }

    // ---- sheet dan dialog ----
    if (showNew && cfg != null) {
        NewDonationSheet(
            config = cfg,
            onCreated = { d -> showNew = false; openId = d.id },
            onDismiss = { showNew = false }
        )
    }
    openId?.let { id ->
        DonationDetailSheet(id = id, config = cfg, onDismiss = { openId = null; scope.launch { runCatching { DonateManager.refreshHome() } } })
    }
    profileName?.let { name ->
        ChatProfileSheet(
            username = name,
            isMe = name.equals(AccountManager.getInstance().user.value?.username, true),
            isMuted = false,
            onMention = {},
            onToggleMute = {},
            onDismiss = { profileName = null },
            showActions = false
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.card,
            title = { Text(S.donateDeleteAll, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(S.donateDeleteAllBody, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        try { DonateManager.deleteFinished(); NexNotifier.success(currentStrings().donateDeleted, currentStrings().donateTitle) }
                        catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Throwable) { NexNotifier.warning(errText(e), currentStrings().donateTitle) }
                    }
                }) { Text(S.donateDeleteHistory, color = RED, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(S.cancel, color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun statusLabel(status: String): Pair<String, Color> = when (status) {
    "pending" -> S.donateStatusPending to AMBER
    "review" -> S.donateStatusReview to Color(0xFF60A5FA)
    "paid" -> S.donateStatusPaid to GREEN
    "rejected" -> S.donateStatusRejected to RED
    else -> S.donateStatusCancelled to NexrifyAppTheme.colors.textMuted
}

@Composable
private fun StatusChip(status: String) {
    val (label, tone) = statusLabel(status)
    Text(
        label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tone,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tone.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun dateText(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@Composable
private fun HistoryRow(d: Donation, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape).background(colors.card).border(1.dp, colors.border, shape).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(colors.border), contentAlignment = Alignment.Center) {
            Icon(if (d.method == "dana") NexIcons.Wallet else NexIcons.QrCode, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(formatRp(d.amount), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("${if (d.method == "dana") "DANA" else "QRIS"} · ${dateText(d.createdAt)}", fontSize = 11.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (d.status == "pending") Text(S.donateContinuePay, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AMBER)
        }
        StatusChip(d.status)
    }
}

/* ======================= donasi baru ======================= */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewDonationSheet(config: DonateConfig, onCreated: (Donation) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    val account by AccountManager.getInstance().user.collectAsState()
    var method by remember { mutableStateOf("qris") }
    var amountText by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf(account?.display.orEmpty()) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val amount = amountText.toLongOrNull() ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(S.donateNow, style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)

            SectionLabel(S.donateMethod)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MethodCard(NexIcons.QrCode, S.donateQris, S.donateQrisSub, method == "qris", true, Modifier.weight(1f)) { method = "qris" }
                MethodCard(NexIcons.Wallet, S.donateDana, if (config.danaReady) S.donateDanaSub else S.donateDanaNotReady, method == "dana", config.danaReady, Modifier.weight(1f)) { method = "dana" }
            }

            SectionLabel(S.donateAmount)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                config.presets.forEach { p ->
                    val sel = amount == p
                    Text(
                        formatRp(p), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = if (sel) colors.background else colors.textPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) colors.textPrimary else colors.card)
                            .border(1.dp, if (sel) colors.textPrimary else colors.border, RoundedCornerShape(20.dp))
                            .clickable { amountText = p.toString() }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Field(S.donateCustomAmount, amountText, "Rp", keyboard = KeyboardType.Number) { v -> amountText = v.filter { it.isDigit() }.take(9) }
            Text(S.donateMin(formatRp(config.min)), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            SectionLabel(S.donateDonor)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatAvatar(account?.id.orEmpty(), if (account?.avatar.isNullOrEmpty()) "" else "1", account?.display ?: "?", 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(account?.display.orEmpty(), color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("@${account?.username.orEmpty()}", color = colors.textMuted, fontSize = 12.sp)
                }
            }
            Text(S.donateDonorSub, fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            Spacer(Modifier.height(14.dp))
            Field(S.donateSenderLabel, sender, S.donateSenderHint) { sender = it.take(60).replace("\n", " ") }
            Spacer(Modifier.height(10.dp))
            Field(S.donateNoteLabel, note, "") { note = it.take(120).replace("\n", " ") }

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(colors.textPrimary)
                    .clickable(enabled = !busy) {
                        val s = currentStrings()
                        when {
                            amount < config.min -> NexNotifier.warning(s.donateNeedAmount(formatRp(config.min)), s.donateTitle)
                            sender.trim().length < 2 -> NexNotifier.warning(s.donateNeedSender, s.donateTitle)
                            else -> scope.launch {
                                busy = true
                                try { onCreated(DonateManager.create(method, amount, sender.trim(), note.trim())) }
                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (e: Throwable) { NexNotifier.warning(errText(e), s.donateTitle) }
                                busy = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (busy) CircularProgressIndicator(color = colors.background, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Text(S.donateContinue, color = colors.background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = NexrifyAppTheme.colors
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 2.dp))
}

@Composable
private fun MethodCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) colors.textPrimary else colors.card)
            .border(1.dp, if (selected) colors.textPrimary else colors.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        val ink = if (selected) colors.background else if (enabled) colors.textPrimary else colors.textMuted
        Icon(icon, null, tint = ink, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, color = ink)
        Text(sub, fontSize = 11.sp, lineHeight = 15.sp, color = if (selected) colors.background.copy(alpha = 0.7f) else colors.textMuted)
    }
}

@Composable
private fun Field(label: String, value: String, hint: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = colors.textMuted, fontSize = 14.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ======================= detail dan bukti ======================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationDetailSheet(id: String, config: DonateConfig?, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history by DonateManager.history.collectAsState()
    val d = history.firstOrNull { it.id == id }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var proofInfo by remember { mutableStateOf<ProofReader.Info?>(null) }
    var progress by remember { mutableStateOf(0) }
    var sending by remember { mutableStateOf(false) }
    var qrFailed by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> if (uri != null) photoUri = uri }
    LaunchedEffect(photoUri) {
        proofInfo = photoUri?.let { u -> withContext(Dispatchers.IO) { runCatching { ProofReader.info(context, u) }.getOrNull() } }
    }

    // Menunggu owner: segarkan status tiap beberapa detik selama sheet terbuka.
    LaunchedEffect(d?.status) {
        if (d?.status != "review") return@LaunchedEffect
        while (true) {
            delay(6000)
            try { DonateManager.refreshHistory() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Throwable) {}
            if (DonateManager.history.value.firstOrNull { it.id == id }?.status != "review") break
        }
    }

    if (d == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(S.donateDetailTitle, fontSize = 12.sp, color = colors.textMuted)
                    Text(formatRp(d.amount), style = Typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = colors.textPrimary)
                    Text("${if (d.method == "dana") "DANA" else "QRIS"} · ${dateText(d.createdAt)}", fontSize = 12.sp, color = colors.textMuted)
                }
                StatusChip(d.status)
            }

            when (d.status) {
                "pending" -> {
                    SectionLabel(S.donateStepPay)
                    Text(S.donateTransferExact(formatRp(d.amount)), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    if (d.method == "qris") {
                        Text(S.donateScanQris, fontSize = 12.sp, color = colors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrFailed) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                                    Text(S.donateQrisFail, color = Color(0xFF444444), fontSize = 13.sp, textAlign = TextAlign.Center)
                                }
                            } else {
                                AsyncImage(
                                    model = DonateApi.qrisUrl(),
                                    contentDescription = "QRIS",
                                    contentScale = ContentScale.Fit,
                                    onError = { qrFailed = true },
                                    modifier = Modifier.fillMaxWidth().height(340.dp)
                                )
                            }
                        }
                        val page = config?.qrisPage.orEmpty()
                        if (page.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlineButton(S.donateOpenQris) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                        }
                    } else {
                        val shape = RoundedCornerShape(18.dp)
                        Column(Modifier.fillMaxWidth().clip(shape).background(colors.card).border(1.dp, colors.border, shape).padding(16.dp)) {
                            Text(S.donateDanaName, fontSize = 11.sp, color = colors.textMuted)
                            Text(config?.danaName.orEmpty(), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(Modifier.height(10.dp))
                            Text(S.donateDanaNumber, fontSize = 11.sp, color = colors.textMuted)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(config?.danaNumber.orEmpty(), style = Typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(
                                    Modifier.clip(RoundedCornerShape(12.dp)).background(colors.border).clickable {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("DANA", config?.danaNumber.orEmpty()))
                                        NexNotifier.success(currentStrings().donateCopied, currentStrings().donateTitle)
                                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(NexIcons.Copy, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(S.donateCopy, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    SectionLabel(S.donateStepProof)
                    Text(S.donateProofHint, fontSize = 13.sp, lineHeight = 18.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    val uri = photoUri
                    val info = proofInfo
                    if (uri != null) {
                        if (info == null || info.mime.startsWith("image/")) {
                            AsyncImage(
                                model = uri, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, colors.border, RoundedCornerShape(18.dp))
                            )
                        } else {
                            ProofFileCard(info)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlineButton(if (uri == null) S.donatePickProof else S.donateChangeProof, icon = NexIcons.ImagePick) { picker.launch("*/*") }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                            .background(if (uri != null) colors.textPrimary else colors.border)
                            .clickable(enabled = uri != null && !sending) {
                                scope.launch {
                                    val s = currentStrings()
                                    sending = true
                                    progress = 0
                                    when (val prep = withContext(Dispatchers.IO) { ProofReader.prepare(context, uri!!) }) {
                                        is NexProofResult.TooBig -> NexNotifier.warning(s.donateProofTooBig, s.donateTitle)
                                        is NexProofResult.Unreadable -> NexNotifier.warning(s.donateProofInvalid, s.donateTitle)
                                        is NexProofResult.Ok -> {
                                            try {
                                                DonateManager.submit(id, prep.file) { progress = it }
                                                photoUri = null
                                                NexNotifier.success(s.donateProofSent, s.donateTitle)
                                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                            catch (e: Throwable) { NexNotifier.warning(errText(e), s.donateTitle) }
                                        }
                                    }
                                    sending = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sending) Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = colors.background, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("$progress%", color = colors.background, fontWeight = FontWeight.Bold)
                        }
                        else Text(S.donateSendProof, color = if (uri != null) colors.background else colors.textMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlineButton(S.donateCancel, danger = true) { confirmCancel = true }
                }
                "review" -> {
                    Spacer(Modifier.height(16.dp))
                    InfoBox(S.donateReviewInfo, Color(0xFF60A5FA), loading = true)
                    Spacer(Modifier.height(12.dp))
                    OutlineButton(S.donateCancel, danger = true) { confirmCancel = true }
                }
                "paid" -> {
                    Spacer(Modifier.height(16.dp))
                    InfoBox(S.donatePaidInfo, GREEN)
                    Spacer(Modifier.height(12.dp))
                    OutlineButton(S.donateDeleteHistory, danger = true) { confirmDelete = true }
                }
                "rejected" -> {
                    Spacer(Modifier.height(16.dp))
                    InfoBox(S.donateRejectedInfo, RED)
                    Spacer(Modifier.height(12.dp))
                    OutlineButton(S.donateDeleteHistory, danger = true) { confirmDelete = true }
                }
                else -> {
                    Spacer(Modifier.height(16.dp))
                    InfoBox(S.donateCancelledInfo, colors.textMuted)
                    Spacer(Modifier.height(12.dp))
                    OutlineButton(S.donateDeleteHistory, danger = true) { confirmDelete = true }
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            containerColor = colors.card,
            title = { Text(S.donateCancelTitle, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(S.donateCancelBody, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    scope.launch {
                        val s = currentStrings()
                        try { DonateManager.cancel(id); NexNotifier.info(s.donateCancelDone, s.donateTitle) }
                        catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Throwable) { NexNotifier.warning(errText(e), s.donateTitle) }
                    }
                }) { Text(S.donateCancel, color = RED, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(S.donateKeep, color = colors.textSecondary) } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = colors.card,
            title = { Text(S.donateDeleteTitle, color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(S.donateDeleteBody, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val s = currentStrings()
                        try { DonateManager.delete(id); NexNotifier.info(s.donateDeleted, s.donateTitle); onDismiss() }
                        catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Throwable) { NexNotifier.warning(errText(e), s.donateTitle) }
                    }
                }) { Text(S.donateDeleteHistory, color = RED, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(S.donateKeep, color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun ProofFileCard(info: ProofReader.Info) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val ext = info.name.substringAfterLast('.', "").uppercase().take(4).ifBlank { info.mime.substringAfter('/').uppercase().take(4) }
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.card).border(1.dp, colors.border, shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.border), contentAlignment = Alignment.Center) {
            Text(ext, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(info.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (info.size > 0) Text(sizeText(info.size), fontSize = 12.sp, color = colors.textMuted)
        }
    }
}

private fun sizeText(bytes: Long): String =
    if (bytes >= 1024 * 1024) String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0) else "${maxOf(1L, bytes / 1024)} KB"

@Composable
private fun InfoBox(text: String, tone: Color, loading: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tone.copy(alpha = 0.12f)).border(1.dp, tone.copy(alpha = 0.35f), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) CircularProgressIndicator(color = tone, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Icon(NexIcons.Info, null, tint = tone, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = NexrifyAppTheme.colors.textPrimary)
    }
}

@Composable
private fun OutlineButton(text: String, danger: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val ink = if (danger) RED else colors.textPrimary
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(shape).border(1.dp, if (danger) RED.copy(alpha = 0.5f) else colors.border, shape).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = ink, fontWeight = FontWeight.Bold)
    }
}
