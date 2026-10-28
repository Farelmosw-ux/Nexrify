package id.nexrify.music.ui.components

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.nexrify.music.capsule.MusicCapsuleController
import id.nexrify.music.i18n.S
import id.nexrify.music.i18n.currentStrings
import id.nexrify.music.ui.components.NexFooter

import id.nexrify.music.ui.icons.NexIcons

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

private sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NoRelease : UpdateState()
    object Failed : UpdateState()
    data class Latest(val current: String) : UpdateState()
    data class Available(val current: String, val latest: String) : UpdateState()
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

    var cacheSize by remember { mutableStateOf<String?>(null) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var latestReleaseUrl by remember { mutableStateOf("https://github.com/Farelmosw-ux/Nexrify/releases") }
    var updateBusy by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val capsuleEnabled by settingsManager.capsuleEnabled.collectAsState()
    var overlayGranted by remember { mutableStateOf(MusicCapsuleController.canDrawOverlays(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = MusicCapsuleController.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                        text = S.settings,
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
                            imageVector = NexIcons.Close,
                            contentDescription = S.close,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Language preference (persisted; SYSTEM follows device locale).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = NexIcons.Translate, contentDescription = null,
                        tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bahasa / Language", style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        AppLanguage.SYSTEM to S.langSystem,
                        AppLanguage.INDONESIAN to S.langId,
                        AppLanguage.ENGLISH to S.langEn
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
                Text(S.updateTitle, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(S.updateDesc, style = Typography.bodySmall, color = colors.textMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (val st = updateState) {
                        UpdateState.Idle -> S.updateNotChecked
                        UpdateState.Checking -> S.updateChecking
                        UpdateState.NoRelease -> S.updateNoRelease
                        UpdateState.Failed -> S.updateFailed
                        is UpdateState.Latest -> S.updateLatest(st.current)
                        is UpdateState.Available -> S.updateAvailable(st.current, st.latest)
                    },
                    style = Typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (!updateBusy) scope.launch {
                            updateBusy = true
                            updateState = UpdateState.Checking
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
                                updateState = if (latest.isBlank()) UpdateState.NoRelease else if (compareVersions(current, latest) >= 0) UpdateState.Latest(current) else UpdateState.Available(current, latest)
                            }.onFailure { updateState = UpdateState.Failed }
                        }
                    }, enabled = !updateBusy, colors = ButtonDefaults.buttonColors(containerColor = colors.card, contentColor = colors.textPrimary), modifier = Modifier.weight(1f)) {
                        Text(if (updateBusy) S.checking else S.checkUpdate)
                    }
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestReleaseUrl))) }
                            .onFailure { Toast.makeText(context, currentStrings().cannotOpenUpdate, Toast.LENGTH_SHORT).show() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = colors.card, contentColor = colors.textPrimary), modifier = Modifier.weight(1f)) {
                        Text(S.openReleases)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Panel musik kapsul
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = NexIcons.GraphicEq,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = S.capsuleTitle,
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = S.capsuleDesc,
                                style = Typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = capsuleEnabled,
                        onCheckedChange = { settingsManager.setCapsuleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.card
                        )
                    )
                }
                if (capsuleEnabled && !overlayGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(S.capsulePermission, style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(S.capsulePermissionHint, style = Typography.bodySmall, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    Toast.makeText(context, currentStrings().capsuleNeedPermissionToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                        ) {
                            Text(S.capsuleGrant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Audio Quality Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = NexIcons.GraphicEq,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = S.audioQualityTitle,
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
                                text = S.qualityTitle(quality),
                                style = Typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = S.qualityDesc(quality),
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
                                    imageVector = NexIcons.Check,
                                    contentDescription = S.selected,
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
                        imageVector = NexIcons.Palette,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = S.themeTitle,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val themeOptions = listOf(
                    Triple(ThemeMode.SYSTEM, S.themeSystem, S.themeSystemDesc),
                    Triple(ThemeMode.DARK, S.themeDark, S.themeDarkDesc),
                    Triple(ThemeMode.LIGHT, S.themeLight, S.themeLightDesc)
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
                                    imageVector = NexIcons.Check,
                                    contentDescription = S.selected,
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
                            imageVector = NexIcons.Translate,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = S.lyricsTranslateTitle,
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = S.lyricsTranslateDesc,
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
                            imageVector = NexIcons.DeleteOutline,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = S.storageTitle,
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = S.cacheSize(cacheSize ?: S.calculating),
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
                                        Toast.makeText(context, currentStrings().cacheCleared, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = S.clear,
                            style = Typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Footer
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                NexFooter(showPrivateNote = false)

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
