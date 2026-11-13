package id.nexrify.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import id.nexrify.music.data.settings.ThemeMode
import id.nexrify.music.data.settings.ThemeStyle

@Immutable
data class NexrifyColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val iconPrimary: Color,
    val isDark: Boolean,
    val accent: Color = textPrimary,
    val accentAlt: Color = accent
)

val DarkNexrifyColors = NexrifyColors(
    background = PureBlack,
    surface = DarkSurface,
    card = DarkCard,
    border = DarkBorder,
    textPrimary = PureWhite,
    textSecondary = SubtleGray,
    textMuted = MutedText,
    iconPrimary = PureWhite,
    isDark = true,
    accent = PureWhite
)

val LightNexrifyColors = NexrifyColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFAFA),
    card = Color(0xFFF4F4F5),
    border = Color(0xFFE4E4E7),
    textPrimary = Color(0xFF09090B),
    textSecondary = Color(0xFF71717A),
    textMuted = Color(0xFFA1A1AA),
    iconPrimary = Color(0xFF09090B),
    isDark = false,
    accent = Color(0xFF09090B)
)

val LocalNexrifyColors = staticCompositionLocalOf { DarkNexrifyColors }

/** True kalau tema Liquid Glass V2 aktif. Dipakai untuk tata letak khusus V2. */
val LocalIsV2 = staticCompositionLocalOf { false }

object NexrifyAppTheme {
    val colors: NexrifyColors
        @Composable
        get() = LocalNexrifyColors.current

    val isV2: Boolean
        @Composable
        get() = LocalIsV2.current
}

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = AccentWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = MutedText,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    secondary = Color(0xFF27272A),
    onSecondary = PureWhite,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7)
)

@Composable
fun NexrifyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val baseColors = if (isDark) DarkNexrifyColors else LightNexrifyColors
    // Liquid Glass keeps the selected system/app light or dark appearance.
    // Only surface translucency and edge highlights change between editions.
    val nexrifyColors = when (themeStyle) {
        ThemeStyle.CLASSIC -> baseColors
        ThemeStyle.LIQUID_GLASS -> baseColors.copy(
            background = if (isDark) Color(0xFF090B10) else Color(0xFFF5F6F8),
            surface = if (isDark) Color(0xC51B202A) else Color(0xDDFBFCFE),
            card = if (isDark) Color(0xA52A303D) else Color(0xBFE8EBF1),
            border = if (isDark) Color(0x668F9BAF) else Color(0x66939BAA),
            textSecondary = baseColors.textSecondary,
            textMuted = baseColors.textMuted,
            accent = baseColors.textPrimary
        )
        // V2: palet "Aurora Violet", ungu malam dengan aksen violet dan teal.
        ThemeStyle.LIQUID_GLASS_V2 -> baseColors.copy(
            background = if (isDark) Color(0xFF0A0712) else Color(0xFFF6F3FF),
            surface = if (isDark) Color(0xCC17112A) else Color(0xE6FFFFFF),
            card = if (isDark) Color(0x99261C45) else Color(0xB8EAE3FF),
            border = if (isDark) Color(0x6670589F) else Color(0x667C69C9),
            textPrimary = if (isDark) Color(0xFFF5F2FF) else Color(0xFF1A1233),
            textSecondary = if (isDark) Color(0xFF9A90B8) else Color(0xFF6B6485),
            textMuted = if (isDark) Color(0xFFB7AED2) else Color(0xFF8E87A8),
            iconPrimary = if (isDark) Color(0xFFF5F2FF) else Color(0xFF1A1233),
            accent = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D4AFF),
            accentAlt = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0E9F94)
        )
        ThemeStyle.VIOLET_NIGHT -> baseColors.copy(surface = Color(0xFF171224), card = Color(0xFF211936), border = Color(0xFF49356D), accent = Color(0xFFB99AFF))
        ThemeStyle.AURORA_GOLD -> baseColors.copy(surface = Color(0xFF211D16), card = Color(0xFF302719), border = Color(0xFF80652D), accent = Color(0xFFFFD477))
        ThemeStyle.OBSIDIAN -> baseColors.copy(surface = Color(0xFF101316), card = Color(0xFF1A2024), border = Color(0xFF3C5557), accent = Color(0xFF83D8CB))
    }

    val resolvedColorScheme = colorScheme.copy(
        primary = nexrifyColors.accent,
        onPrimary = nexrifyColors.background,
        secondary = nexrifyColors.textSecondary,
        onSecondary = nexrifyColors.background,
        background = nexrifyColors.background,
        onBackground = nexrifyColors.textPrimary,
        surface = nexrifyColors.surface,
        onSurface = nexrifyColors.textPrimary,
        surfaceVariant = nexrifyColors.card,
        onSurfaceVariant = nexrifyColors.textSecondary,
        outline = nexrifyColors.border
    )

    CompositionLocalProvider(
        LocalNexrifyColors provides nexrifyColors,
        LocalIsV2 provides (themeStyle == ThemeStyle.LIQUID_GLASS_V2)
    ) {
        MaterialTheme(
            colorScheme = resolvedColorScheme,
            typography = Typography,
            content = content
        )
    }
}
