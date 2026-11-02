package id.nexrify.music.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.nexrify.music.capsule.AppVisibility
import id.nexrify.music.data.prayer.MaghribManager
import id.nexrify.music.data.prayer.MaghribReminder
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import id.nexrify.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Memantau waktu Maghrib secara real-time. Saat memasuki waktu sebelum Maghrib (dan selama jendela Maghrib),
 * musik dijeda dan popup muncul sekali per hari.
 *
 * TUTUP: popup ditutup dan musik dilanjutkan kalau tadi sedang diputar.
 * KELUAR: keluar dari aplikasi, musik tetap berhenti sampai pengguna menyalakannya sendiri.
 */
@Composable
fun MaghribReminderHost(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val manager = remember { MaghribManager.getInstance() }
    val reminder = remember { MaghribReminder.getInstance() }
    val place by manager.place.collectAsState()
    val enabled by reminder.enabled.collectAsState()
    val visible by reminder.visible.collectAsState()

    // Perbarui lokasi berkala supaya jadwal ikut berubah kalau pengguna pindah kota.
    LaunchedEffect(Unit) {
        while (true) {
            manager.refresh()
            delay(15 * 60_000L)
        }
    }

    // Cek waktu tiap 10 detik, jadi popup muncul tepat waktu tanpa menunggu pengguna membuka layar tertentu.
    LaunchedEffect(place, enabled) {
        while (true) {
            val now = ZonedDateTime.now()
            val maghrib = manager.maghribFor(now.toLocalDate(), now.zone)
            if (AppVisibility.inForeground && reminder.shouldShow(now, maghrib)) {
                val wasPlaying = viewModel.pauseForPrayer()
                reminder.show(maghrib, wasPlaying)
            }
            delay(10_000L)
        }
    }

    if (!visible) return

    val colors = NexrifyAppTheme.colors
    val maghrib = reminder.shownMaghrib
    val now = ZonedDateTime.now().toLocalTime()
    val alreadyStarted = maghrib != null && now >= maghrib
    val timeText = maghrib?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""

    NexDialogCard(
        icon = NexIcons.Moon,
        title = if (alreadyStarted) S.reminderTitleNow else S.reminderTitleBefore,
        onDismiss = {},
        dismissible = false
    ) {
        Text(
            S.reminderBody(timeText, alreadyStarted),
            style = Typography.bodyMedium,
            color = colors.textMuted
        )
        if (reminder.musicWasPlaying) {
            Spacer(Modifier.height(10.dp))
            Text(S.reminderMusicPaused, style = Typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    reminder.acknowledge()
                    if (reminder.musicWasPlaying) viewModel.resumeAfterPrayer()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
            ) { Text(S.reminderClose, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = {
                    reminder.acknowledge()
                    viewModel.stopForExit()
                    context.findActivity()?.finishAndRemoveTask()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border)
            ) { Text(S.reminderExit, color = colors.textSecondary, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(12.dp))
        Text(S.reminderCloseHint, style = Typography.bodySmall, color = colors.textMuted)
    }
}

/**
 * Meminta izin lokasi untuk fitur Maghrib. Muncul tiap aplikasi dibuka selama izin belum diberikan.
 * Kalau izin ditolak permanen, tombolnya berubah jadi pembuka pengaturan aplikasi.
 */
@Composable
fun LocationPermissionPrompt() {
    val context = LocalContext.current
    val manager = remember { MaghribManager.getInstance() }
    var hasPermission by remember { mutableStateOf(manager.hasPermission()) }
    var dismissed by remember { mutableStateOf(false) }
    var needSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted || manager.hasPermission()
        if (hasPermission) {
            manager.refresh()
            NexNotifier.success(currentStrings().locGranted, title = currentStrings().noteLocationTitle)
        } else {
            val activity = context.findActivity()
            needSettings = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Pengguna bisa kembali dari pengaturan sistem setelah mengaktifkan izin.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = manager.hasPermission()
                if (granted && !hasPermission) {
                    manager.refresh()
                    NexNotifier.success(currentStrings().locGranted, title = currentStrings().noteLocationTitle)
                }
                hasPermission = granted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasPermission || dismissed) return
    val colors = NexrifyAppTheme.colors

    NexDialogCard(
        icon = NexIcons.Location,
        title = S.locPromptTitle,
        onDismiss = { dismissed = true },
        dismissible = false
    ) {
        Text(S.locPromptBody, style = Typography.bodyMedium, color = colors.textMuted)
        if (needSettings) {
            Spacer(Modifier.height(8.dp))
            Text(S.locPromptSettingsHint, style = Typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (needSettings) {
                    runCatching {
                        context.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                } else {
                    launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
        ) { Text(if (needSettings) S.locPromptSettings else S.locPromptAllow, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { dismissed = true }, modifier = Modifier.fillMaxWidth()) {
            Text(S.locPromptLater, color = colors.textSecondary)
        }
    }
}
