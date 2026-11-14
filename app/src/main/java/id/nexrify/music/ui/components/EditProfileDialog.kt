package id.nexrify.music.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.data.account.NexUser
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Edit nama tampilan, foto profil, dan password akun Nexrify. */
@Composable
fun EditProfileDialog(onDismiss: () -> Unit) {
    val colors = NexrifyAppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts = remember { AccountManager.getInstance() }
    val user by accounts.user.collectAsState()

    var display by remember { mutableStateOf(user?.display.orEmpty()) }
    var newAvatar by remember { mutableStateOf<String?>(null) } // null = tidak berubah, "" = hapus
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val data = withContext(Dispatchers.IO) { runCatching { toAvatarDataUrl(context, uri) }.getOrNull() }
            if (data == null) error = "invalid_avatar" else { newAvatar = data; error = null }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
        focusedBorderColor = colors.textPrimary, unfocusedBorderColor = colors.border,
        focusedLabelColor = colors.textSecondary, unfocusedLabelColor = colors.textMuted,
        cursorColor = colors.textPrimary, focusedContainerColor = colors.card, unfocusedContainerColor = colors.card
    )

    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: AccountError) { error = e.code }
            catch (e: Exception) { error = "server" }
            finally { busy = false }
        }
    }

    NexDialogCard(icon = NexIcons.AccountCircle, title = S.editProfile, onDismiss = onDismiss) {
        Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            val preview = NexUser(
                id = user?.id.orEmpty(), username = user?.username.orEmpty(), display = display.ifBlank { user?.username.orEmpty() },
                avatar = newAvatar ?: user?.avatar.orEmpty(), createdAt = 0L
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                NexAvatar(preview, 72.dp)
                Spacer(Modifier.size(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(S.changePhoto, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.clickable { picker.launch("image/*") })
                    if (preview.avatar.isNotBlank()) {
                        Text(S.removePhoto, color = colors.textMuted, fontSize = 13.sp, modifier = Modifier.clickable { newAvatar = "" })
                    }
                    Text("@${user?.username.orEmpty()}", color = colors.textMuted, fontSize = 12.sp)
                }
            }
            OutlinedTextField(
                value = display, onValueChange = { display = it.take(30) }, label = { Text(S.fieldDisplay) },
                singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    run {
                        accounts.updateProfile(display.trim().ifBlank { null }, newAvatar)
                        newAvatar = null
                        NexNotifier.success(currentStrings().profileSaved, currentStrings().noteProfileTitle)
                    }
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
            ) { Text(S.save, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(16.dp))
            Text(S.changePassword, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = oldPw, onValueChange = { oldPw = it.take(72) }, label = { Text(S.fieldOldPassword) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newPw, onValueChange = { newPw = it.take(72) }, label = { Text(S.fieldNewPassword) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    run {
                        accounts.changePassword(oldPw, newPw)
                        oldPw = ""; newPw = ""
                        NexNotifier.success(currentStrings().passwordChanged, currentStrings().noteProfileTitle)
                    }
                },
                enabled = !busy && oldPw.isNotBlank() && newPw.length >= 6,
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) { Text(S.changePassword, color = colors.textPrimary, fontWeight = FontWeight.SemiBold) }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(S.accountError(it), color = Color(0xFFFCA5A5), style = Typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) { Text(S.close, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
    }
}

/** Sisi foto profil (px). 1080 px supaya tetap tajam saat dibuka layar penuh. */
private const val AVATAR_SIDE_PX = 1080
/** Batas panjang data URL; harus sama dengan batas di server (api/auth.js). */
private const val AVATAR_MAX_CHARS = 450_000

/** Ambil foto dari galeri, potong jadi persegi (maks. 1080 px, tidak diperbesar), simpan sebagai JPEG HD dalam bentuk data URL. */
internal fun toAvatarDataUrl(
    context: android.content.Context,
    uri: Uri,
    sidePx: Int = AVATAR_SIDE_PX,
    maxChars: Int = AVATAR_MAX_CHARS
): String {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= sidePx) sample *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("decode")
    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
    val target = minOf(side, sidePx)
    val scaled = if (target == side) square else Bitmap.createScaledBitmap(square, target, target, true)
    // Mulai dari kualitas tinggi, turunkan bertahap kalau hasilnya melewati batas server.
    var result = ""
    for (quality in intArrayOf(92, 86, 80, 72, 64, 55)) {
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        result = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        if (result.length <= maxChars) break
    }
    return result
}
