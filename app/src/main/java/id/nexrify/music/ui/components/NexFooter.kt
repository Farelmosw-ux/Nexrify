package id.nexrify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.theme.FredokaFontFamily
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/** Footer aplikasi: wordmark, tagline, versi asli dari paket, kredit pengembang, dan catatan kode sumber. */
@Composable
fun NexFooter(
    modifier: Modifier = Modifier,
    showPrivateNote: Boolean = true
) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "1.0.0" }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(28.dp).height(1.dp).background(colors.border))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(5.dp).background(colors.textMuted, CircleShape))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(28.dp).height(1.dp).background(colors.border))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Nexrify",
            color = colors.textPrimary,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = S.footerTagline,
            color = colors.textMuted,
            style = Typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .background(colors.card, RoundedCornerShape(50))
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = S.footerVersion(version),
                color = colors.textSecondary,
                style = Typography.labelSmall,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = S.footerMadeBy,
            color = colors.textSecondary,
            style = Typography.labelSmall,
            fontSize = 11.sp
        )
        if (showPrivateNote) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = S.footerPrivate,
                color = colors.textMuted,
                style = Typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
