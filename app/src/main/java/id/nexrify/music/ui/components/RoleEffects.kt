package id.nexrify.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Peran yang punya efek kilau: developer (RGB), admin (emas), staff (perak). */
val GlintRoles = setOf("developer", "admin", "staff")

fun isGlintRole(role: String?) = role != null && role in GlintRoles

private val GoldGlint = listOf(
    Color(0xFF8A5A00), Color(0xFFF5B83D), Color(0xFFFFE680), Color(0xFFFFFFFF),
    Color(0xFFFFE680), Color(0xFFF5B83D), Color(0xFF8A5A00)
)
private val SilverGlint = listOf(
    Color(0xFF7D838D), Color(0xFFC9CED6), Color(0xFFFFFFFF), Color(0xFFC9CED6), Color(0xFF7D838D)
)
private val RgbGlint = listOf(
    Color(0xFFFFD700), Color(0xFFFF2D55), Color(0xFFFF9500), Color(0xFF34C759),
    Color(0xFF00C7FF), Color(0xFF5E5CE6), Color(0xFFBF5AF2), Color(0xFFFFD700)
)

fun roleGlintColors(role: String): List<Color> = when (role) {
    "developer" -> RgbGlint
    "admin" -> GoldGlint
    else -> SilverGlint
}

fun roleLabel(role: String): String = when (role) {
    "developer" -> "DEVELOPER"
    "admin" -> "ADMIN"
    "staff" -> "STAFF"
    else -> role.uppercase()
}

/** Fase 0..1 yang berulang terus untuk menggerakkan kilatan. Developer lebih cepat. */
@Composable
fun rememberGlintPhase(role: String): State<Float> {
    val t = rememberInfiniteTransition(label = "role-glint")
    return t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (role == "developer") 2000 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "role-glint-phase"
    )
}

/** Kuas gradasi yang bergeser mengikuti fase, bikin efek kilatan berjalan. */
fun glintBrush(role: String, phase: Float, span: Float): Brush {
    val shift = phase * span
    return Brush.linearGradient(
        colors = roleGlintColors(role),
        start = Offset(-span + shift, 0f),
        end = Offset(shift, span * 0.35f),
        tileMode = TileMode.Repeated
    )
}

/** Lencana peran dengan kilatan. [compact] untuk chat, versi penuh untuk profil. */
@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (!isGlintRole(role)) return
    val phase by rememberGlintPhase(role)
    val shape = RoundedCornerShape(if (compact) 6.dp else 9.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(glintBrush(role, phase, if (compact) 120f else 260f))
            .padding(horizontal = if (compact) 5.dp else 11.dp, vertical = if (compact) 1.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                compact && role == "developer" -> "DEV"
                compact && role == "staff" -> "STAF"
                compact -> roleLabel(role)
                else -> "✦ " + roleLabel(role)
            },
            fontSize = if (compact) 8.5.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (compact) 0.sp else 1.sp,
            color = Color(0xFF14110A)
        )
    }
}
