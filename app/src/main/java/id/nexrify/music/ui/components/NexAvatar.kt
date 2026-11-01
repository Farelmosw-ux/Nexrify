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
fun NexAvatar(user: NexUser?, size: Dp, modifier: Modifier = Modifier) {
    val colors = NexrifyAppTheme.colors
    val bitmap = remember(user?.avatar) { decodeAvatar(user?.avatar) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
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
}

private fun decodeAvatar(dataUrl: String?): ImageBitmap? {
    if (dataUrl.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,", ""), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
