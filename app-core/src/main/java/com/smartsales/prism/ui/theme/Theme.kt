package com.smartsales.prism.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity

@Immutable
data class PrismSemanticColors(
    val appBackground: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceHover: Color,
    val surfaceActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textTertiary: Color,
    val textStrong: Color,
    val borderSubtle: Color,
    val accentPrimary: Color,
    val accentPrimaryDeep: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val accentYellow: Color,
    val accentBlue: Color,
    val accentAmber: Color,
    val accentDanger: Color,
    val surfaceDanger: Color,
    val backdropScrim: Color,
    val knotPrimary: Color,
    val knotSecondary: Color
)

private val LightPrismSemanticColors = PrismSemanticColors(
    appBackground = BackgroundApp,
    surface = BackgroundSurface,
    surfaceMuted = BackgroundSurfaceMuted,
    surfaceHover = BackgroundSurfaceHover,
    surfaceActive = BackgroundSurfaceActive,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    textTertiary = TextTertiary,
    textStrong = TextStrong,
    borderSubtle = BorderSubtle,
    accentPrimary = AccentPrimary,
    accentPrimaryDeep = AccentPrimaryDeep,
    accentSecondary = AccentSecondary,
    accentTertiary = AccentTertiary,
    accentYellow = AccentYellow,
    accentBlue = AccentBlue,
    accentAmber = AccentAmber,
    accentDanger = AccentDanger,
    surfaceDanger = SurfaceDanger,
    backdropScrim = BackdropScrim,
    knotPrimary = KnotPrimary,
    knotSecondary = KnotSecondary
)

private val DarkPrismSemanticColors = PrismSemanticColors(
    appBackground = Color(0xFF0D0D12),
    surface = Color(0xCC1C1C1E),
    surfaceMuted = Color(0x14FFFFFF),
    surfaceHover = Color(0xFF2C2C2E),
    surfaceActive = Color(0xFF3A3A3C),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFAEAEB2),
    textMuted = Color(0xFF8E8E93),
    textTertiary = Color(0xFF6C6C70),
    textStrong = Color.White,
    borderSubtle = Color.White.copy(alpha = 0.12f),
    accentPrimary = Color.White,
    accentPrimaryDeep = Color(0xFFE5E5EA),
    accentSecondary = AccentSecondary,
    accentTertiary = AccentTertiary,
    accentYellow = AccentYellow,
    accentBlue = Color(0xFF0A84FF),
    accentAmber = AccentAmber,
    accentDanger = Color(0xFFFF453A),
    surfaceDanger = Color(0x33FF453A),
    backdropScrim = Color.Black.copy(alpha = 0.46f),
    knotPrimary = KnotPrimary,
    knotSecondary = KnotSecondary
)

internal val LocalPrismSemanticColors = staticCompositionLocalOf {
    LightPrismSemanticColors
}

internal val LocalPrismDarkTheme = staticCompositionLocalOf { false }

private val LightMaterialColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    tertiary = AccentTertiary,
    onTertiary = Color.White,
    background = BackgroundApp,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurfaceActive,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFE5E5EA),
    error = AccentDanger,
    onError = Color.White,
    errorContainer = SurfaceDanger,
    onErrorContainer = AccentDanger
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    secondary = AccentSecondary,
    onSecondary = Color.Black,
    tertiary = AccentTertiary,
    onTertiary = Color.White,
    background = Color(0xFF0D0D12),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0x66FFFFFF),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0x33FF453A),
    onErrorContainer = Color(0xFFFFB4AB)
)

@Composable
fun PrismTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val semanticColors = resolvePrismSemanticColors(darkTheme)
    val materialColorScheme = if (darkTheme) {
        DarkMaterialColorScheme
    } else {
        LightMaterialColorScheme
    }

    CompositionLocalProvider(
        LocalPrismSemanticColors provides semanticColors,
        LocalPrismDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography(),
            shapes = AppShapes,
            content = content
        )
    }
}

object PrismThemeDefaults {
    val colors: PrismSemanticColors
        @Composable
        get() = LocalPrismSemanticColors.current

    val isDarkTheme: Boolean
        @Composable
        get() = LocalPrismDarkTheme.current
}

fun resolvePrismDarkTheme(
    themeMode: PrismThemeMode,
    systemDarkTheme: Boolean
): Boolean = when (themeMode) {
    PrismThemeMode.SYSTEM -> systemDarkTheme
    PrismThemeMode.LIGHT -> false
    PrismThemeMode.DARK -> true
}

@Composable
fun PrismSystemBarsEffect(
    activity: ComponentActivity,
    darkTheme: Boolean
) {
    val view = LocalView.current
    SideEffect {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

internal fun resolvePrismSemanticColors(darkTheme: Boolean): PrismSemanticColors {
    return if (darkTheme) {
        DarkPrismSemanticColors
    } else {
        LightPrismSemanticColors
    }
}
