package id.nexrify.music.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/** Tampil setelah aplikasi sempat berhenti tiba tiba, berisi laporan yang bisa disalin atau dibagikan. */
@Composable
fun CrashReportDialog(report: String, onClose: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedText = S.crashCopied
    val shareText = S.crashShare

    NexDialogCard(icon = NexIcons.Info, title = S.crashTitle, onDismiss = onClose, danger = true) {
        Text(S.crashBody, style = Typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
        ) {
            Text(
                text = report,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(report))
                    Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) { Text(S.crashCopy, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
                    runCatching { context.startActivity(Intent.createChooser(send, shareText).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
            ) { Text(shareText, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) { Text(S.close, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
    }
}
