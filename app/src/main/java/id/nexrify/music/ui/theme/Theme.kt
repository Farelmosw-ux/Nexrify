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
    val accent: Color = textPrimary
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

object NexrifyAppTheme {
    val colors: NexrifyColors
        @Composable
        get() = LocalNexrifyColors.current
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
    val themedDark = baseColors.copy(background = Color(0xFF08090D), textPrimary = Color.White, textSecondary = Color(0xFFB9BAC5), textMuted = Color(0xFF858795), iconPrimary = Color.White, isDark = true)
    val nexrifyColors = when (themeStyle) {
        ThemeStyle.CLASSIC -> baseColors
        ThemeStyle.LIQUID_GLASS -> themedDark.copy(surface = Color(0xCC171923), card = Color(0x991F2230), border = Color(0x665E63A5), accent = Color(0xFFB9A7FF))
        ThemeStyle.VIOLET_NIGHT -> themedDark.copy(surface = Color(0xFF171224), card = Color(0xFF211936), border = Color(0xFF49356D), accent = Color(0xFFB99AFF))
        ThemeStyle.AURORA_GOLD -> themedDark.copy(surface = Color(0xFF211D16), card = Color(0xFF302719), border = Color(0xFF80652D), accent = Color(0xFFFFD477))
        ThemeStyle.OBSIDIAN -> themedDark.copy(surface = Color(0xFF101316), card = Color(0xFF1A2024), border = Color(0xFF3C5557), accent = Color(0xFF83D8CB))
    }

    CompositionLocalProvider(LocalNexrifyColors provides nexrifyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
