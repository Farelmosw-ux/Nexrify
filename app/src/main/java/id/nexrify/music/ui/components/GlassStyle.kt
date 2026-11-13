package id.nexrify.music.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import id.nexrify.music.ui.theme.NexrifyAppTheme

/**
 * Shared frosted-glass treatment. Android 13+ gets soft blurred lens highlights;
 * older Android versions retain a lightweight translucent surface and fine border.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.card.copy(alpha = if (Build.VERSION.SDK_INT >= 33) 0.68f else 0.88f),
                        colors.surface.copy(alpha = if (Build.VERSION.SDK_INT >= 33) 0.62f else 0.82f),
                        colors.textPrimary.copy(alpha = if (colors.isDark) 0.06f else 0.12f)
                    )
                )
            )
            .border(1.dp, colors.border.copy(alpha = 0.9f), shape)
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(colors.textPrimary.copy(alpha = if (colors.isDark) 0.20f else 0.28f), colors.accent.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
                    .blur(if (Build.VERSION.SDK_INT >= 33) 30.dp else 12.dp)
            )
        }
        content()
    }
}
