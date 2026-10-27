package id.nexrify.music.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.nexrify.music.data.settings.AudioQuality
import id.nexrify.music.data.settings.AppLanguage
import id.nexrify.music.data.settings.SettingsManager
import id.nexrify.music.data.settings.ThemeMode
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private fun compareVersions(current: String, latest: String): Int {
    val a = current.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
    val b = latest.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(i) { 0 }
        val right = b.getOrElse(i) { 0 }
        if (left != right) return left.compareTo(right)
    }
    return 0
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = NexrifyAppTheme.colors
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance() }
    val currentTheme by settingsManager.themeMode.collectAsState()
    val currentQuality by settingsManager.audioQuality.collectAsState()
    val autoTranslate by settingsManager.autoTranslate.collectAsState()
    val currentLanguage by settingsManager.language.collectAsState()

    var cacheSize by remember { mutableStateOf("Menghitung...") }
    var updateStatus by remember { mutableStateOf("Belum diperiksa") }
    var latestReleaseUrl by remember { mutableStateOf("https://github.com/Farelmosw-ux/Nexrify/releases") }
    var updateBusy by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        cacheSize = settingsManager.getFormattedCacheSizeAsync()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengaturan",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Language preference (persisted; SYSTEM follows device locale).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Translate, contentDescription = null,
                        tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bahasa / Language", style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        AppLanguage.SYSTEM to "Ikuti perangkat",
                        AppLanguage.INDONESIAN to "Indonesia",
                        AppLanguage.ENGLISH to "English"
                    ).forEach { (language, label) ->
                        val selected = currentLanguage == language
                        Text(label, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.card else colors.surface)
                            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(10.dp))
                            .clickable { settingsManager.setLanguage(language) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                            color = colors.textPrimary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text("Pembaruan Nexrify", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Periksa versi terbaru dari GitHub Releases.", style = Typography.bodySmall, color = colors.textMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(updateStatus, style = Typography.bodySmall, color = colors.textMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (!updateBusy) scope.launch {
                            updateBusy = true
                            updateStatus = "Memeriksa pembaruan…"
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
                                    val request = Request.Builder()
                                        .url("https://api.github.com/repos/Farelmosw-ux/Nexrify/releases/latest")
                                        .header("Accept", "application/vnd.github+json")
                                        .build()
                                    client.newCall(request).execute().use { response ->
                                        if (!response.isSuccessful) error("Server mengembalikan HTTP ${response.code}")
                                        val json = JSONObject(response.body?.string().orEmpty())
                                        val tag = json.optString("tag_name").removePrefix("v")
                                        val url = json.optString("html_url", "https://github.com/Farelmosw-ux/Nexrify/releases")
                                        tag to url
                                    }
                                }
                            }
                            updateBusy = false
                            result.onSuccess { (latest, url) ->
                                latestReleaseUrl = url
                                val current = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0" }.getOrDefault("0.0.0")
                                updateStatus = if (latest.isBlank()) "Versi terbaru belum dipublikasikan." else if (compareVersions(current, latest) >= 0) "Nexrify sudah versi terbaru ($current)." else "Pembaruan tersedia: $current → $latest"
                            }.onFailure { updateStatus = "Pemeriksaan gagal. Periksa koneksi atau halaman Releases." }
                        }
                    }, enabled = !updateBusy, colors = ButtonDefaults.buttonColors(containerColor = colors.card, contentColor = colors.textPrimary), modifier = Modifier.weight(1f)) {
                        Text(if (updateBusy) "Memeriksa…" else "Check Update")
                    }
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestReleaseUrl))) }
                            .onFailure { Toast.makeText(context, "Tidak dapat membuka halaman pembaruan", Toast.LENGTH_SHORT).show() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = colors.card, contentColor = colors.textPrimary), modifier = Modifier.weight(1f)) {
                        Text("Buka Releases")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // 1. Audio Quality Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kualitas Audio Streaming",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AudioQuality.values().forEach { quality ->
                    val isSelected = currentQuality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.card else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(12.dp))
                            .clickable { settingsManager.setAudioQuality(quality) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quality.title,
                                style = Typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = quality.description,
                                style = Typography.bodySmall,
                                color = colors.textMuted
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Dipilih",
                                    tint = colors.background,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Theme Selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tema Tampilan",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val themeOptions = listOf(
                    Triple(ThemeMode.SYSTEM, "Mengikuti Sistem", "Sesuai tema mode HP kamu"),
                    Triple(ThemeMode.DARK, "Dark Mode", "Hitam OLED hemat baterai"),
                    Triple(ThemeMode.LIGHT, "Light Mode", "Putih monokrom bersih")
                )

                themeOptions.forEach { (mode, title, desc) ->
                    val isSelected = currentTheme == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.card else colors.surface)
                            .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(12.dp))
                            .clickable { settingsManager.setThemeMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = Typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = Typography.bodySmall,
                                color = colors.textMuted
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Dipilih",
                                    tint = colors.background,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Lyrics Translation Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Terjemahan Indonesia Lirik",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Tampilkan arti lirik bahasa Indonesia",
                                style = Typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = autoTranslate,
                        onCheckedChange = { settingsManager.setAutoTranslate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.card
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 4. Storage & Cache
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Penyimpanan & Cache",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Ukuran Cache: $cacheSize",
                                style = Typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch {
                                    val success = settingsManager.clearCacheAsync()
                                    if (success) {
                                        cacheSize = settingsManager.getFormattedCacheSizeAsync()
                                        Toast.makeText(context, "Cache berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Bersihkan",
                            style = Typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. About / Version Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nexrify v3.3.0 • Media3 & Jetpack Compose",
                            style = Typography.labelSmall,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Open source, zero ads & ultra lightweight",
                        style = Typography.labelSmall,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
