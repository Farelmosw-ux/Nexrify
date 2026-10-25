package id.nexrify.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.R
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.launch

/** Nama yang tampil di loading screen. Ubah di sini jika ingin memakai nama lain. */
const val SPLASH_BRAND_NAME = "Nexrify"

/**
 * Loading screen: logo fade-in, progress bar horizontal yang mengalir.
 *
 * [progress] = null berarti progres tidak bisa diukur -> bar indeterminate dan TIDAK ada persentase.
 * Persentase hanya muncul bila [progress] berisi nilai nyata 0f..1f.
 */
@Composable
fun NexrifySplashScreen(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.9f) }
    val barAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch {
            kotlinx.coroutines.delay(500)
            barAlpha.animateTo(1f, tween(500))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures { } }, // blokir sentuhan selama loading
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = SPLASH_BRAND_NAME,
                    colorFilter = ColorFilter.tint(colors.textPrimary),
                    modifier = Modifier.size(120.dp)
                )
                Text(
                    text = SPLASH_BRAND_NAME,
                    style = Typography.headlineLarge,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = barAlpha.value }
            ) {
                FlowingProgressBar(
                    progress = progress,
                    modifier = Modifier.width(220.dp)
                )
                if (progress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        style = Typography.labelMedium,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

/** Bar horizontal dengan highlight yang mengalir dari kiri ke kanan. */
@Composable
fun FlowingProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val transition = rememberInfiniteTransition(label = "flow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "flowPhase"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(300),
        label = "progress"
    )
    val primary = colors.textPrimary
    val background = colors.background
    val determinate = progress != null

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.border)
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                if (!determinate) {
                    val segment = w * 0.4f
                    val x = -segment + (w + segment) * phase
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, primary, Color.Transparent),
                            startX = x,
                            endX = x + segment
                        )
                    )
                } else {
                    val fill = w * animatedProgress
                    drawRect(color = primary, size = Size(fill, h))
                    clipRect(left = 0f, top = 0f, right = fill, bottom = h) {
                        val segment = w * 0.3f
                        val x = -segment + (fill + segment) * phase
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, background.copy(alpha = 0.35f), Color.Transparent),
                                startX = x,
                                endX = x + segment
                            )
                        )
                    }
                }
            }
    )
}
