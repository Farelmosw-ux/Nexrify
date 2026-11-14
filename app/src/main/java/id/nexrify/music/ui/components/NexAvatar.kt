package id.nexrify.music.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.account.NexUser
import id.nexrify.music.ui.theme.FredokaFontFamily
import id.nexrify.music.ui.theme.NexrifyAppTheme

/** Foto profil akun Nexrify, atau huruf pertama nama kalau belum punya foto. */
@Composable
fun NexAvatar(user: NexUser?, size: Dp, modifier: Modifier = Modifier, showRole: Boolean = true) {
    val colors = NexrifyAppTheme.colors
    val bitmap = remember(user?.avatar) { decodeAvatar(user?.avatar) }
    val role = user?.role.orEmpty()
    if (showRole && isGlintRole(role)) {
        // Developer, admin, dan staff: cincin berkilau, cahaya berdenyut, dan kilatan melintas di foto.
        val phase by rememberGlintPhase(role)
        val glow = roleGlintColors(role)[(phase * (roleGlintColors(role).size - 1)).toInt().coerceIn(0, roleGlintColors(role).size - 1)]
        Box(
            modifier = modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        color = glow.copy(alpha = 0.22f + 0.18f * kotlin.math.abs(kotlin.math.sin(phase * Math.PI.toFloat()))),
                        radius = this.size.minDimension / 2f + 5.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(colors.card)
                .border(2.5.dp, glintBrush(role, phase, size.value * 3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AvatarContent(user, bitmap, size)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                            start = Offset(size.value * (phase * 4f - 1.5f), 0f),
                            end = Offset(size.value * (phase * 4f - 0.5f), size.value)
                        )
                    )
            )
        }
        return
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AvatarContent(user, bitmap, size)
    }
}

@Composable
private fun AvatarContent(user: NexUser?, bitmap: ImageBitmap?, size: Dp) {
    val colors = NexrifyAppTheme.colors
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
        Text(
            text = (user?.display ?: user?.username ?: "N").trim().take(1).uppercase().ifBlank { "N" },
            color = colors.textPrimary,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

internal fun decodeAvatar(dataUrl: String?): ImageBitmap? {
    if (dataUrl.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,", ""), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
