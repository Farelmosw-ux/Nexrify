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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.graphics.compositeOver
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
    val context = LocalContext.current
    // Activity ini tidak edge to edge, jadi inset Compose bernilai 0 dan popup (yang memakai koordinat layar)
    // menimpa status bar. Ambil tinggi status bar dari resource sistem sebagai pegangan.
    val composeTop = WindowInsets.statusBars.getTop(density)
    val resTop = remember(context) {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }
    val topInset = maxOf(composeTop, resTop)
    val margin = with(density) { 12.dp.roundToPx() }
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
                NoteKind.INFO -> Color(0xFF60A5FA)
                NoteKind.WARNING -> Color(0xFFFBBF24)
                NoteKind.ERROR -> Color(0xFFF87171)
            }
            val icon = when (current.kind) {
                NoteKind.SUCCESS -> NexIcons.Check
                NoteKind.INFO, NoteKind.WARNING -> NexIcons.Info
                NoteKind.ERROR -> NexIcons.CloseCircle
            }
            val shape = RoundedCornerShape(22.dp)
            // Garis hitung mundur: penuh lalu menyusut selama popup tampil.
            val remaining = remember(current.id) { Animatable(1f) }
            LaunchedEffect(current.id) { remaining.animateTo(0f, tween(2800, easing = LinearEasing)) }
            var dragY by remember(current.id) { mutableStateOf(0f) }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, dragY.toInt().coerceAtMost(0)) }
                    .shadow(16.dp, shape, ambientColor = tone, spotColor = tone)
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(tone.copy(alpha = 0.20f).compositeOver(colors.card), colors.card, colors.card)
                        )
                    )
                    .border(1.dp, tone.copy(alpha = 0.35f), shape)
                    .pointerInput(current.id) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, d -> dragY += d },
                            onDragEnd = {
                                if (dragY < -40f) NexNotifier.clear(current.id) else dragY = 0f
                            },
                            onDragCancel = { dragY = 0f }
                        )
                    }
                    .clickable { NexNotifier.clear(current.id) }
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tone.copy(alpha = 0.18f))
                            .border(1.dp, tone.copy(alpha = 0.40f), CircleShape),
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
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            current.message,
                            style = Typography.bodySmall,
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(remaining.value)
                        .height(2.5.dp)
                        .background(tone.copy(alpha = 0.85f))
                )
            }
        }
    }
}
