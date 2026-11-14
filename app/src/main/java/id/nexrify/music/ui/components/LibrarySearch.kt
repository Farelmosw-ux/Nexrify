package id.nexrify.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.nexrify.music.data.model.Track
import id.nexrify.music.i18n.S
import id.nexrify.music.ui.icons.NexIcons
import id.nexrify.music.ui.navigation.Screen
import id.nexrify.music.ui.theme.NexrifyAppTheme
import id.nexrify.music.ui.theme.Typography

/**
 * Cocokkan lagu dengan kata kunci. Setiap kata harus ada di judul atau nama artis,
 * urutannya bebas, huruf besar kecil tidak berpengaruh.
 */
fun Track.matchesQuery(query: String): Boolean = matchesText(query, title, artist)

fun matchesText(query: String, vararg fields: String): Boolean {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    val hay = fields.joinToString(" ").lowercase()
    return tokens.all { hay.contains(it) }
}

/** Kotak pencarian untuk Lagu disukai, Playlist, dan Offline. Bentuknya ikut tema (V2 lebih bulat dengan aksen). */
@Composable
fun LibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val colors = NexrifyAppTheme.colors
    val isV2 = NexrifyAppTheme.isV2
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (isV2) 26.dp else 14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isV2) 50.dp else 46.dp)
            .clip(shape)
            .background(colors.card)
            .border(
                width = if (isV2 && focused) 1.5.dp else 1.dp,
                color = if (isV2 && focused) colors.accent else colors.border,
                shape = shape
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = NexIcons.Search,
            contentDescription = null,
            tint = if (isV2 && focused) colors.accent else colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    style = Typography.bodyMedium,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NexIcons.CloseCircle,
                    contentDescription = S.clearSearchCd,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Tampilan kosong kalau pencarian tidak menemukan apa pun. */
@Composable
fun SearchNoResult(query: String, modifier: Modifier = Modifier) {
    val colors = NexrifyAppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = NexIcons.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = S.noMatchFor(query.trim()),
            style = Typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = S.noMatchHint,
            style = Typography.bodyMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}
