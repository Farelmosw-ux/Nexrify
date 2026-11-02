package id.nexrify.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NoteKind { SUCCESS, INFO, WARNING, ERROR }

data class NexNote(val id: Long, val kind: NoteKind, val title: String, val message: String)

/**
 * Popup pemberitahuan dalam aplikasi, pengganti Toast polos.
 * Panggil [show] dari mana saja (UI maupun ViewModel). Tampilannya dirender oleh [NexNotificationHost].
 */
object NexNotifier {
    private val _current = MutableStateFlow<NexNote?>(null)
    val current: StateFlow<NexNote?> = _current.asStateFlow()

    @Volatile
    private var counter = 0L

    fun show(kind: NoteKind, message: String, title: String? = null) {
        val st = currentStrings()
        val resolvedTitle = title ?: when (kind) {
            NoteKind.SUCCESS -> st.noteSuccess
            NoteKind.INFO -> st.noteInfo
            NoteKind.WARNING -> st.noteWarning
            NoteKind.ERROR -> st.noteError
        }
        counter += 1
        _current.value = NexNote(counter, kind, resolvedTitle, message)
    }

    fun success(message: String, title: String? = null) = show(NoteKind.SUCCESS, message, title)
    fun info(message: String, title: String? = null) = show(NoteKind.INFO, message, title)
    fun warning(message: String, title: String? = null) = show(NoteKind.WARNING, message, title)
    fun error(message: String, title: String? = null) = show(NoteKind.ERROR, message, title)

    internal fun clear(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}

/**
 * Taruh sekali di akar aplikasi. Popup dirender di jendela Popup tersendiri, jadi tetap terlihat
 * di atas dialog dan bottom sheet yang sedang terbuka. Sentuhan di luar popup diteruskan ke layar di bawahnya.
 */
@Composable
fun NexNotificationHost() {
    val note by NexNotifier.current.collectAsState()
    val colors = NexrifyAppTheme.colors

    // Simpan catatan terakhir supaya animasi keluar masih punya isi.
    var shown by remember { mutableStateOf<NexNote?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(note?.id) {
        val n = note
        if (n != null) {
            shown = n
            visible = true
            delay(2800L)
            visible = false
            delay(320L)
            shown = null
            NexNotifier.clear(n.id)
        } else {
            visible = false
            delay(320L)
            shown = null
        }
    }

    val current = shown ?: return
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val margin = with(density) { 10.dp.roundToPx() }
    val positionProvider = remember(topInset, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset((windowSize.width - popupContentSize.width) / 2, topInset + margin)
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            val tone = when (current.kind) {
                NoteKind.SUCCESS -> Color(0xFF4ADE80)
                NoteKind.INFO -> colors.textPrimary
                NoteKind.WARNING -> Color(0xFFFBBF24)
                NoteKind.ERROR -> Color(0xFFFCA5A5)
            }
            val icon = when (current.kind) {
                NoteKind.SUCCESS -> NexIcons.Check
                NoteKind.INFO, NoteKind.WARNING -> NexIcons.Info
                NoteKind.ERROR -> NexIcons.CloseCircle
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, tone.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .clickable { NexNotifier.clear(current.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(tone.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tone, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        current.title,
                        style = Typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        current.message,
                        style = Typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
