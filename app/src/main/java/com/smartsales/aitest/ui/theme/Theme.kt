package com.smartsales.aitest.ui.theme

// 文件：app/src/main/java/com/smartsales/aitest/ui/theme/Theme.kt
// 模块：:app
// 说明：将 globals.css 的浅色风格映射为 Compose Material3 主题
// 作者：创建于 2025-12-02

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PrimaryContainer = AccentPrimary.copy(alpha = 0.12f).compositeOver(BackgroundSurface)
private val SecondaryContainer = AccentSecondary.copy(alpha = 0.12f).compositeOver(BackgroundSurface)
private val TertiaryContainer = AccentTertiary.copy(alpha = 0.12f).compositeOver(BackgroundSurface)
private val DangerContainer = SurfaceDanger

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = AccentPrimaryDeep,
    inversePrimary = AccentPrimary,

    secondary = AccentSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextSecondary,

    tertiary = AccentTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TextSecondary,

    background = BackgroundApp,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurfaceActive,
    onSurfaceVariant = TextMuted,
    surfaceTint = AccentPrimary,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = BackgroundSurface,

    error = AccentDanger,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = AccentDanger,

    outline = BorderSubtle,
    outlineVariant = TextTertiary,
    scrim = BackdropScrim
)

@Suppress("UnusedParameter")
@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 当前仅提供浅色方案，保持与 web globals.css 一致
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTypography,
        shapes = AppShapes,
        content = content
    )
}
