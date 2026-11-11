package id.nexrify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.model.Track
import id.nexrify.music.data.remote.SongComment
import id.nexrify.music.data.remote.SongCommentNotif
import id.nexrify.music.data.remote.SongCommentsApi
import id.nexrify.music.ui.theme.NexrifyAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCommentsSheet(track: Track, onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    var comments by remember(track.id) { mutableStateOf<List<SongComment>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<SongComment?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf<List<SongCommentNotif>>(emptyList()) }
    var showActivity by remember { mutableStateOf(false) }
    suspend fun refresh() { loading = true; try { comments = SongCommentsApi.list(track.id); error = "" } catch (e: Exception) { error = e.message ?: "Gagal memuat komentar" }; loading = false }
    LaunchedEffect(track.id) { refresh(); runCatching { activity = SongCommentsApi.notifications().second } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background, contentColor = colors.textPrimary) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal = 18.dp)) {
            Text("Komentar lagu", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("${track.title} · ${track.artist}", fontSize = 12.sp, color = colors.textSecondary, maxLines = 1)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showActivity, onClick = { showActivity = false }, label = { Text("Komentar") })
                FilterChip(selected = showActivity, onClick = { showActivity = true }, label = { Text("Aktivitas") })
            }
            when {
                showActivity && activity.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Belum ada aktivitas komentar.", color = colors.textSecondary) }
                showActivity -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activity, key = { "${it.ts}-${it.commentId}-${it.type}" }) { n ->
                        Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text(when (n.type) { "like" -> "@${n.from} menyukai komentar kamu"; "reply" -> "@${n.from} membalas komentar kamu"; "mention" -> "@${n.from} menyebut kamu"; else -> "Aktivitas komentar dari @${n.from}" }, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(n.text, color = colors.textSecondary, fontSize = 12.sp)
                            Text(SimpleDateFormat("dd MMM · HH:mm", Locale("id", "ID")).format(Date(n.ts)), color = colors.textMuted, fontSize = 10.sp)
                        }
                    }
                }
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.textPrimary) }
                comments.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(error.ifBlank { "Belum ada komentar. Mulai obrolannya!" }, color = colors.textSecondary) }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(comments, key = { it.id }) { c ->
                        val isReply = c.parentId.isNotBlank()
                        Row(Modifier.fillMaxWidth().padding(start = if (isReply) 28.dp else 0.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("@${c.username}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text(c.text, fontSize = 14.sp, color = colors.textPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(SimpleDateFormat("dd MMM · HH:mm", Locale("id", "ID")).format(Date(c.ts)), fontSize = 10.sp, color = colors.textMuted)
                                    Text("Balas", Modifier.clickable { reply = c }, fontSize = 11.sp, color = colors.textSecondary)
                                    if (c.edited) Text("diedit", fontSize = 10.sp, color = colors.textMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (c.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, Modifier.size(20.dp).clickable { scope.launch { runCatching { SongCommentsApi.like(track.id, c.id); refresh() }.onFailure { error = it.message ?: "Gagal menyukai komentar" } } }, tint = if (c.liked) Color(0xFFE45B78) else colors.textMuted)
                                Text(c.likes.toString(), fontSize = 10.sp, color = colors.textMuted)
                            }
                        }
                    }
                }
            }
            if (!showActivity && reply != null) Row(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(10.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Membalas @${reply!!.username}", Modifier.weight(1f), fontSize = 12.sp, color = colors.textSecondary)
                Text("Batal", Modifier.clickable { reply = null }, fontSize = 12.sp, color = colors.textPrimary)
            }
            if (!showActivity) Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it.take(500) }, modifier = Modifier.weight(1f), placeholder = { Text("Tulis komentar...") }, shape = RoundedCornerShape(18.dp), maxLines = 4)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { val body = text.trim(); if (body.isNotEmpty()) scope.launch { try { val sent = SongCommentsApi.send(track.id, body, reply?.id.orEmpty()); comments = comments + sent; text = ""; reply = null; refresh() } catch (e: Exception) { error = e.message ?: "Komentar gagal dikirim" } } }, enabled = text.isNotBlank()) { Text("Kirim") }
            }
            if (error.isNotBlank() && !loading) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}
