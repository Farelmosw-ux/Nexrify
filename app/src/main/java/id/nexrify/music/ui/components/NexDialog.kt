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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/** Kartu dialog standar Nexrify: sudut bulat, ikon di lingkaran, judul, teks, lalu isi dan tombol. */
@Composable
fun NexDialogCard(
    icon: ImageVector,
    title: String,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    dismissible: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = NexrifyAppTheme.colors
    val accent = if (danger) Color(0xFFFCA5A5) else colors.textPrimary
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.card).border(1.dp, if (danger) accent.copy(alpha = 0.4f) else colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Konfirmasi dua tombol. [danger] untuk tindakan yang menghapus. */
@Composable
fun NexConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = NexIcons.Info,
    danger: Boolean = false
) {
    val colors = NexrifyAppTheme.colors
    NexDialogCard(icon = icon, title = title, onDismiss = onDismiss, danger = danger) {
        Text(text, style = Typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Start)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) { Text(S.cancel, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (danger) Color(0xFFFCA5A5) else colors.accent,
                    contentColor = if (danger) Color.Black else colors.background
                )
            ) { Text(confirmText, fontWeight = FontWeight.Bold) }
        }
    }
}
