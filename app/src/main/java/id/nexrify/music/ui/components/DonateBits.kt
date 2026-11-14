package id.nexrify.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.donate.DonorTop
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import java.text.NumberFormat
import java.util.Locale

fun formatRp(n: Long): String = "Rp" + NumberFormat.getNumberInstance(Locale("id", "ID")).format(n)

/** Warna tiap peringkat: 1 emas, 2 perak, 3 perunggu. */
class RankStyle(val brush: Brush, val ink: Color, val ring: Color)

fun rankStyle(rank: Int): RankStyle = when (rank) {
    1 -> RankStyle(
        Brush.linearGradient(listOf(Color(0xFFFFE9A0), Color(0xFFF5C242), Color(0xFFB97E12))),
        Color(0xFF2A1B00), Color(0xFFFFE38A)
    )
    2 -> RankStyle(
        Brush.linearGradient(listOf(Color(0xFFF5F7FA), Color(0xFFC5CDD8), Color(0xFF8995A6))),
        Color(0xFF1B222C), Color(0xFFE3E8EF)
    )
    else -> RankStyle(
        Brush.linearGradient(listOf(Color(0xFFF6CFA5), Color(0xFFD38C50), Color(0xFF8B5222))),
        Color(0xFF2B1608), Color(0xFFE9B685)
    )
}

/** Tag "TOP n DONATE" bergaya premium. plain = latar gelap transparan (dipakai di dalam banner). */
@Composable
fun TopTag(rank: Int, modifier: Modifier = Modifier, plain: Boolean = false) {
    val st = rankStyle(rank)
    // Kilatan berjalan sesuai peringkat: 1 emas (cepat dan paling terang), 2 perak, 3 perunggu.
    val phase by rememberInfiniteTransition(label = "toptag").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(when (rank) { 1 -> 1800; 2 -> 2400; else -> 2800 }, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "toptag-phase"
    )
    val glintColors = when (rank) {
        1 -> listOf(Color(0xFFB97E12), Color(0xFFF5C242), Color(0xFFFFF1B8), Color(0xFFFFFFFF), Color(0xFFFFF1B8), Color(0xFFF5C242), Color(0xFFB97E12))
        2 -> listOf(Color(0xFF8995A6), Color(0xFFC5CDD8), Color(0xFFFFFFFF), Color(0xFFC5CDD8), Color(0xFF8995A6))
        else -> listOf(Color(0xFF8B5222), Color(0xFFD38C50), Color(0xFFFFD9B0), Color(0xFFD38C50), Color(0xFF8B5222))
    }
    val span = 90f
    val glint = Brush.linearGradient(
        colors = glintColors,
        start = Offset(-span + phase * span, 0f),
        end = Offset(phase * span, span * 0.4f),
        tileMode = androidx.compose.ui.graphics.TileMode.Repeated
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .then(if (plain) Modifier.background(Color.Black.copy(alpha = 0.18f)) else Modifier.background(glint))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(NexIcons.Crown, null, tint = st.ink, modifier = Modifier.size(9.dp))
        Spacer(Modifier.width(3.dp))
        Text(S.donateTag(rank), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = st.ink, maxLines = 1)
    }
}

/** Banner donatur di papan peringkat. Juara 1 emas dengan kilau bergerak. */
@Composable
fun DonorBanner(top: DonorTop, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val st = rankStyle(top.rank)
    val gold = top.rank == 1
    val shape = RoundedCornerShape(if (gold) 26.dp else 22.dp)
    val shimmerX by rememberInfiniteTransition(label = "gold").animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (gold) 124.dp else 100.dp)
            .clip(shape)
            .background(st.brush)
            .border(if (gold) 1.5.dp else 1.dp, st.ring.copy(alpha = if (gold) 0.9f else 0.6f), shape)
            .clickable(onClick = onClick)
    ) {
        run {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = if (gold) 0.38f else 0.28f), Color.Transparent),
                        start = Offset(shimmerX - 260f, 0f),
                        end = Offset(shimmerX, 220f)
                    )
                )
            )
        }
        // Angka peringkat besar sebagai hiasan
        Text(
            top.rank.toString(),
            fontSize = if (gold) 96.sp else 80.sp,
            fontWeight = FontWeight.Black,
            color = st.ink.copy(alpha = 0.08f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier.padding(top = if (gold) 10.dp else 6.dp).size(if (gold) 70.dp else 60.dp).clip(CircleShape)
                        .border(2.5.dp, st.ink.copy(alpha = 0.75f), CircleShape)
                ) {
                    ChatAvatar(top.uid, top.av, top.display, if (gold) 70.dp else 60.dp)
                }
                if (gold) Icon(NexIcons.Crown, null, tint = st.ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    top.display,
                    fontSize = if (gold) 18.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = st.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("@${top.username}", fontSize = 12.sp, color = st.ink.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                TopTag(top.rank, plain = true)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(S.donateTotalLabel, fontSize = 10.sp, color = st.ink.copy(alpha = 0.7f))
                Text(
                    formatRp(top.total),
                    fontSize = if (gold) 18.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = st.ink,
                    maxLines = 1
                )
            }
        }
    }
}
