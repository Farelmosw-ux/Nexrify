package id.nexrify.music.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.chat.ChatManager
import id.nexrify.music.data.chat.ChatMember
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Info grup Global Chat: foto, nama, deskripsi, jumlah anggota, dan daftar anggota dengan tag ONLINE / OFFLINE. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupSheet(onOpenProfile: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ChatManager.getInstance() }
    val group by manager.group.collectAsState()
    val members by manager.members.collectAsState()

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var newPhoto by remember { mutableStateOf<String?>(null) } // null = tidak berubah, "" = hapus
    var saving by remember { mutableStateOf(false) }
    var loadedMembers by remember { mutableStateOf(false) }

    // Segarkan info dan daftar anggota selama sheet terbuka (status online berubah-ubah).
    LaunchedEffect(Unit) {
        while (true) {
            manager.refreshGroup()
            manager.refreshMembers()
            loadedMembers = true
            delay(20_000)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val data = withContext(Dispatchers.IO) { runCatching { toAvatarDataUrl(context, uri) }.getOrNull() }
            if (data == null) NexNotifier.warning(currentStrings().chatPhotoInvalid, currentStrings().chatGroupInfo) else newPhoto = data
        }
    }

    val g = group
    val filtered = remember(members, query) {
        val q = query.trim().lowercase().removePrefix("@")
        if (q.isEmpty()) members else members.filter { it.username.contains(q) || it.display.lowercase().contains(q) }
    }
    val staff = filtered.filter { it.role.isNotEmpty() }
    val regular = filtered.filter { it.role.isEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.padding(vertical = 10.dp).width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
        }
    ) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.92f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        GroupAvatar(
                            photoV = g?.photoV.orEmpty(),
                            hasPhoto = (g?.hasPhoto == true && newPhoto != "") || false,
                            size = 96.dp,
                            modifier = if (editing) Modifier.clickable { picker.launch("image/*") } else Modifier
                        )
                        if (editing) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(colors.textPrimary).clickable { picker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) { Icon(NexIcons.Edit, null, tint = colors.background, modifier = Modifier.size(16.dp)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (editing) {
                        if (newPhoto != null && newPhoto != "") {
                            Text("✓ " + S.chatChangePhoto, fontSize = 12.sp, color = colors.textSecondary)
                        } else if (g?.hasPhoto == true && newPhoto == null) {
                            Text(
                                S.chatRemovePhoto, fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { newPhoto = "" }.padding(6.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        GroupField(S.chatGroupNameLabel, name, 40, singleLine = true) { name = it }
                        Spacer(Modifier.height(10.dp))
                        GroupField(S.chatGroupDescLabel, desc, 200, singleLine = false) { desc = it }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GroupButton(S.chatSave, filled = true, busy = saving, modifier = Modifier.weight(1f)) {
                                scope.launch {
                                    val st = currentStrings()
                                    if (name.isBlank()) { NexNotifier.warning(st.chatGroupNameEmpty, st.chatGroupInfo); return@launch }
                                    saving = true
                                    val ok = manager.updateGroup(
                                        name = name.trim().takeIf { it != g?.name },
                                        desc = desc.trim().takeIf { it != g?.desc },
                                        photoDataUrl = newPhoto
                                    )
                                    saving = false
                                    if (ok) {
                                        NexNotifier.success(st.chatGroupSaved, st.chatGroupInfo)
                                        editing = false
                                        newPhoto = null
                                    } else NexNotifier.error(st.chatGenericError, st.chatGroupInfo)
                                }
                            }
                            GroupButton(S.cancel, filled = false, modifier = Modifier.weight(1f)) { editing = false; newPhoto = null }
                        }
                    } else {
                        Text(
                            g?.name ?: S.chatTitle,
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (g != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(S.chatMembersOnline(g.members, g.online), fontSize = 13.sp, color = colors.textMuted)
                            if (g.desc.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(g.desc, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                            }
                            if (g.canEdit) {
                                Spacer(Modifier.height(14.dp))
                                GroupButton(S.chatEditGroup, filled = false, modifier = Modifier.fillMaxWidth()) {
                                    name = g.name; desc = g.desc; newPhoto = null; editing = true
                                }
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(S.chatMembersTitle, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, modifier = Modifier.weight(1f))
                    if (g != null) Text(S.chatOnlineNow(g.online), fontSize = 12.sp, color = Color(0xFF3DDC84), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(NexIcons.Search, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text(S.chatSearchMembers, color = colors.textMuted, fontSize = 14.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it.take(30) },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(colors.textPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (!loadedMembers && members.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(S.chatNoMembers, color = colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                }
            }
            if (staff.isNotEmpty()) {
                item { SectionLabel(S.chatStaffAdminSection, staff.size) }
                items(staff, key = { "s" + it.uid }) { MemberRow(it) { onOpenProfile(it.username) } }
            }
            if (regular.isNotEmpty()) {
                item { SectionLabel(S.chatMemberSection, regular.size) }
                items(regular, key = { "m" + it.uid }) { MemberRow(it) { onOpenProfile(it.username) } }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String, count: Int) {
    val colors = NexrifyAppTheme.colors
    Text(
        "$text · $count",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textMuted,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun MemberRow(m: ChatMember, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(m.uid, "1", m.display, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.display,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (m.role.isNotEmpty()) { Spacer(Modifier.width(6.dp)); BadgeChip(m.role) }
            }
            Text("@${m.username}", color = colors.textMuted, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        PresenceTag(m.online)
    }
}

@Composable
private fun GroupField(label: String, value: String, max: Int, singleLine: Boolean, onChange: (String) -> Unit) {
    val colors = NexrifyAppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text("$label  ${value.length}/$max", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(max).let { t -> if (singleLine) t.replace("\n", " ") else t }) },
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 5,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GroupButton(text: String, filled: Boolean, modifier: Modifier = Modifier, busy: Boolean = false, onClick: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(46.dp)
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
