package id.nexrify.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Perkiraan progres untuk proses yang durasinya tidak diketahui (jaringan).
 * Naik pelan mendekati 94% selama [active], lalu lompat ke 100% begitu selesai.
 */
@Composable
fun rememberEstimatedProgress(active: Boolean): Float {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (active) {
            target = 0.03f
            while (isActive && target < 0.94f) {
                delay(110)
                target += (0.94f - target) * 0.07f
            }
        } else {
            target = 1f
        }
    }
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(220), label = "nexEstimatedProgress")
    return animated
}

/** Loading standar Nexrify: lingkaran berputar dengan angka persen di tengah, progress bar, dan label. */
@Composable
fun NexLoading(
    progress: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = NexrifyAppTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    val percent = (clamped * 100f).toInt()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(if (compact) 64.dp else 92.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = colors.textPrimary,
                trackColor = colors.border,
                strokeWidth = 3.dp
            )
            Text(
                text = "$percent%",
                color = colors.textPrimary,
                style = Typography.titleMedium,
                fontSize = if (compact) 14.sp else 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier
                .width(if (compact) 140.dp else 190.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = colors.textPrimary,
            trackColor = colors.border
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = S.loading,
            color = colors.textMuted,
            style = Typography.bodySmall
        )
    }
}

/** Loading yang mengisi ruang kosong sebuah layar, progresnya diperkirakan otomatis. */
@Composable
fun NexLoadingFill(active: Boolean = true, modifier: Modifier = Modifier.fillMaxSize()) {
    val progress = rememberEstimatedProgress(active)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        NexLoading(progress = progress, compact = true)
    }
}
