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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.nexrify.music.data.remote.ReleaseInfo
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import java.util.Locale

/**
 * Dialog update: versi baru, catatan rilis dalam bahasa yang dipilih, tombol Nanti dan Update sekarang.
 * Rilis yang ditandai wajib tidak punya tombol Nanti dan tidak bisa ditutup.
 */
@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onLater: () -> Unit,
    onUpdate: () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    Dialog(
        onDismissRequest = { if (!release.mandatory) onLater() },
        properties = DialogProperties(dismissOnBackPress = !release.mandatory, dismissOnClickOutside = !release.mandatory)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.card).border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NexIcons.Download, null, tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(S.updateDialogTitle, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    val size = if (release.sizeBytes > 0) "  " + formatMb(release.sizeBytes) else ""
                    Text("Nexrify v${release.version}$size", style = Typography.bodySmall, color = colors.textMuted)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(S.updateWhatsNew, style = Typography.labelMedium, fontFamily = FontFamily.Monospace, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))

            val notes = plainNotes(release.notes(S.english)).ifBlank { S.updateNoNotes }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = notes,
                    style = Typography.bodySmall,
                    color = colors.textSecondary,
                    lineHeight = 19.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (!release.mandatory) {
                    OutlinedButton(
                        onClick = onLater,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Text(S.updateLater, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.weight(if (release.mandatory) 1f else 1.3f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) {
                    Icon(NexIcons.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(S.updateNow, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Ubah markdown sederhana dari catatan rilis jadi teks polos yang rapi. */
private fun plainNotes(raw: String): String =
    raw.lines().joinToString("\n") { line ->
        line.trim()
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^[-*]\\s+"), "• ")
            .replace("**", "")
            .replace("`", "")
    }.replace(Regex("\n{3,}"), "\n\n").trim()

private fun formatMb(bytes: Long): String =
    String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
