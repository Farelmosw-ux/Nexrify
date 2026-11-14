package id.nexrify.music.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.account.AccountError
import id.nexrify.music.data.account.AccountManager
import id.nexrify.music.data.account.AccountSync
import id.nexrify.music.data.remote.NexrifyLinks
import id.nexrify.music.data.settings.AppLanguage
import id.nexrify.music.data.settings.SettingsManager
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.components.NexConfirmDialog
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.FredokaFontFamily
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Layar wajib sebelum masuk aplikasi: masuk atau daftar akun Nexrify. */
@Composable
fun AuthScreen() {
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val settings = remember { SettingsManager.getInstance() }

    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf<String?>(null) }
    // Id reset dari server (setelah salah password 3x) dan username pemiliknya.
    var resetId by remember { mutableStateOf<String?>(null) }
    var resetUser by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }

    fun openLink(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openReset() {
        val id = resetId ?: return
        openLink("${NexrifyLinks.SITE}/$resetUser/resetpassword:$id")
    }

    fun submit() {
        if (busy) return
        focus.clearFocus()
        val u = username.trim().lowercase()
        errorCode = when {
            !Regex("^[a-z0-9_]{3,20}$").matches(u) -> "invalid_username"
            password.length < 6 -> if (register) "weak_password" else "bad_credentials"
            else -> null
        }
        if (errorCode != null) return
        busy = true
        scope.launch {
            try {
                val accounts = AccountManager.getInstance()
                if (register) accounts.register(u, password, display.trim().ifBlank { u }) else accounts.login(u, password)
                AccountSync.getInstance().initialSync(preferLocal = register)
                resetId = null
                accounts.publish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountError) {
                errorCode = e.code
                if (e.code == "bad_credentials" && e.resetId != null) {
                    resetId = e.resetId
                    resetUser = u
                    showReset = true
                }
            } catch (e: Exception) {
                errorCode = "server"
            } finally {
                busy = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedBorderColor = colors.textPrimary,
        unfocusedBorderColor = colors.border,
        focusedLabelColor = colors.textSecondary,
        unfocusedLabelColor = colors.textMuted,
        cursorColor = colors.textPrimary,
        focusedContainerColor = colors.card,
        unfocusedContainerColor = colors.card
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.background).imePadding()) {
        // Pilihan bahasa langsung di layar masuk
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 18.dp)
                .clip(RoundedCornerShape(12.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            listOf(AppLanguage.INDONESIAN to "ID", AppLanguage.ENGLISH to "EN").forEach { (lang, label) ->
                val on = (lang == AppLanguage.ENGLISH) == S.english
                Box(
                    modifier = Modifier.clickable { settings.setLanguage(lang) }
                        .background(if (on) colors.accent else colors.card).padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(label, color = if (on) colors.background else colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Nexrify", color = colors.textPrimary, fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Spacer(Modifier.height(6.dp))
            Text(S.authSubtitle, color = colors.textMuted, style = Typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card).border(1.dp, colors.border, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to S.authLogin, true to S.authRegister).forEach { (isRegister, label) ->
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(if (register == isRegister) colors.accent else colors.card)
                            .clickable(enabled = !busy) { register = isRegister; errorCode = null }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (register == isRegister) colors.background else colors.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(20); errorCode = null },
                label = { Text(S.fieldUsername) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors,
                supportingText = if (register) { { Text(S.usernameHint, color = colors.textMuted, fontSize = 12.sp) } } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            if (register) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = display, onValueChange = { display = it.take(30) },
                    label = { Text(S.fieldDisplay) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it.take(72); errorCode = null },
                label = { Text(S.fieldPassword) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (showPassword) S.passwordHide else S.passwordShow,
                        color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 12.dp).clickable { showPassword = !showPassword }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            errorCode?.let {
                Spacer(Modifier.height(12.dp))
                Text(S.accountError(it), color = androidx.compose.ui.graphics.Color(0xFFFCA5A5), style = Typography.bodySmall, textAlign = TextAlign.Center)
            }
            if (!register && resetId != null && username.trim().lowercase() == resetUser) {
                Spacer(Modifier.height(8.dp))
                Text(
                    S.resetOpen, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { openReset() }.padding(6.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { submit() }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background, disabledContainerColor = colors.card, disabledContentColor = colors.textMuted)
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.textPrimary)
                    Spacer(Modifier.size(10.dp))
                    Text(S.authSyncing, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(if (register) S.authCreate else S.authLogin, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(S.authAgree, color = colors.textMuted, style = Typography.bodySmall, textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(S.authTerms, color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { openLink(NexrifyLinks.TERMS) })
                Text(S.authPrivacy, color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { openLink(NexrifyLinks.PRIVACY) })
            }
        }
        if (showReset && resetId != null) {
            NexConfirmDialog(
                title = S.resetTitle,
                text = S.resetBody(resetUser),
                confirmText = S.resetOpen,
                onConfirm = { showReset = false; openReset() },
                onDismiss = { showReset = false },
                icon = NexIcons.Lock
            )
        }
    }
}
