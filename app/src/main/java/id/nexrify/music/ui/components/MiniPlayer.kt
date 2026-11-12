package id.nexrify.music.ui.components

import id.nexrify.music.i18n.S

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.draw.blur

import androidx.compose.runtime.collectAsState

import android.os.Build

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.nexrify.music.R
import id.nexrify.music.data.model.Track
import id.nexrify.music.ui.theme.DarkBorder
import id.nexrify.music.ui.theme.DarkCard
import id.nexrify.music.ui.theme.MutedText
import id.nexrify.music.ui.theme.PureWhite
import id.nexrify.music.ui.theme.Typography

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = id.nexrify.music.ui.theme.NexrifyAppTheme.colors
    val glassStyle by id.nexrify.music.data.settings.SettingsManager.getInstance().themeStyle.collectAsState()
    val isGlass = glassStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS ||
        glassStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS_V2
    val isGlassV2 = glassStyle == id.nexrify.music.data.settings.ThemeStyle.LIQUID_GLASS_V2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(if (isGlassV2) 24.dp else 17.dp))
            .background(
                if (isGlassV2) Brush.linearGradient(listOf(Color(0xE62C3A52), Color(0xC51A263B), Color(0xE63D4B68)))
                else if (isGlass) Brush.linearGradient(listOf(Color(0xD52C3A4B), Color(0xC51B2938), Color(0xD53B4B5C)))
                else Brush.linearGradient(listOf(colors.card, colors.card))
            )
            .border(1.dp, if (isGlass) colors.accent.copy(alpha = if (isGlassV2) 0.72f else 0.48f) else colors.border, RoundedCornerShape(if (isGlassV2) 24.dp else 17.dp))
            .clickable { onClick() }
    ) {
        if (isGlass && Build.VERSION.SDK_INT >= 33) {
            Box(
                Modifier.fillMaxWidth().height(if (isGlassV2) 18.dp else 10.dp)
                    .background(Brush.radialGradient(listOf(colors.accent.copy(alpha = if (isGlassV2) 0.34f else 0.22f), Color.Transparent)))
                    .blur(if (isGlassV2) 26.dp else 16.dp)
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = colors.textPrimary,
            trackColor = colors.border
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.highResThumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.textPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        painter = if (isPlaying) painterResource(R.drawable.ic_nexrify_pause) else painterResource(R.drawable.ic_nexrify_play),
                        contentDescription = if (isPlaying) S.cd("Pause") else S.cd("Play"),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            IconButton(onClick = onSkipNextClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_nexrify_fast_forward),
                    contentDescription = S.cd("Next"),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tutup mini player (musik tetap jalan). Muncul lagi otomatis saat lagu berganti.
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = id.nexrify.music.ui.icons.NexIcons.KeyboardArrowDown,
                        contentDescription = S.cd("Collapse"),
                        tint = colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
