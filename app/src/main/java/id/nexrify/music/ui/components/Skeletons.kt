package id.nexrify.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/**
 * Kilau halus yang bergerak melintasi blok placeholder.
 * Warna diambil dari tema yang sudah ada (card -> border), jadi tidak ada warna baru.
 */
fun Modifier.shimmer(): Modifier = composed {
    val colors = NexrifyAppTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val highlight = colors.border.copy(alpha = 0.6f)
    this.drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                startX = w * x,
                endX = w * x + w * 0.6f
            )
        )
    }
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp
) {
    val colors = NexrifyAppTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(colors.card)
            .shimmer()
    )
}

/** Placeholder kartu album 144dp + judul + nama artis (sama ukurannya dengan YtmMusicCard). */
@Composable
fun AlbumCardSkeleton() {
    Column(modifier = Modifier.width(144.dp)) {
        SkeletonBlock(modifier = Modifier.size(144.dp), corner = 12.dp)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonBlock(modifier = Modifier.width(112.dp).height(12.dp), corner = 6.dp)
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonBlock(modifier = Modifier.width(72.dp).height(10.dp), corner = 5.dp)
    }
}

@Composable
fun ShelfSkeleton(cardCount: Int = 4) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            SkeletonBlock(modifier = Modifier.width(150.dp).height(20.dp), corner = 6.dp)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBlock(modifier = Modifier.width(100.dp).height(10.dp), corner = 5.dp)
        }
        LazyRow(
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cardCount) { AlbumCardSkeleton() }
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

/** Skeleton beranda: dua rak berisi kartu album. */
@Composable
fun HomeSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        ShelfSkeleton()
        ShelfSkeleton()
    }
}

@Composable
fun TrackRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBlock(modifier = Modifier.size(50.dp), corner = 8.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp), corner = 6.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(11.dp), corner = 5.dp)
        }
    }
}

@Composable
fun TrackListSkeleton(rows: Int = 8) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(rows) { TrackRowSkeleton() }
    }
}

/** Loading saat mencari lagu: indikator tipis di atas + teks + baris placeholder. */
@Composable
fun SearchLoadingState(message: String = "Mencari lagu…") {
    val colors = NexrifyAppTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = colors.textPrimary,
            trackColor = colors.border
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = colors.textPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = message, style = Typography.bodyMedium, color = colors.textMuted)
        }
        TrackListSkeleton(rows = 7)
    }
}

/** Pesan error + tombol "Coba Lagi". */
@Composable
fun ErrorRetryState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            style = Typography.bodyMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.textPrimary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coba Lagi",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                fontSize = 13.sp,
                color = colors.background
            )
        }
    }
}
