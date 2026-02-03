package com.smartsales.aitest.ui.theme

// 文件：app/src/main/java/com/smartsales/aitest/ui/theme/Theme.kt
// 模块：:app
// 说明：Compose Material3 主题 — "Sleek Glass" (Pro Max)
//       映射自 prism-web-v1/src/index.css `.theme-glass-sleek`
// 作者：重写于 2026-01-30 (Chapter 5 VI Guide)

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===================================================================
// Color Scheme Composition — "Sleek Glass" (Pro Max)
// ===================================================================

/** 主色容器 - 极淡黑色覆盖在白色表面 */
private val PrimaryContainer = AccentPrimary.copy(alpha = 0.08f).compositeOver(BackgroundSurface)

/** 次要色容器 */
private val SecondaryContainer = AccentSecondary.copy(alpha = 0.12f).compositeOver(BackgroundSurface)

/** 三级色容器 */
private val TertiaryContainer = AccentTertiary.copy(alpha = 0.12f).compositeOver(BackgroundSurface)

/** 危险色容器 */
private val DangerContainer = SurfaceDanger

/**
 * Sleek Glass (Pro Max) 浅色方案
 *
 * 设计原则:
 * - 主色使用 Stark Black (#000000), 符合 Pro Max 极简美学
 * - 表面使用高透明度白色 (70%), 配合 blur 实现玻璃效果
 * - 边框使用超细 (0.04 opacity) Retina hairline
 * - 圆角使用 20dp 现代连续曲线
 */
private val SleekGlassColorScheme = lightColorScheme(
    // Primary - Stark Black (Pro Max 风格)
    primary = AccentBlue, // 保持蓝色作为交互主色
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = AccentPrimaryDeep,
    inversePrimary = AccentBlue,

    // Secondary - Green
    secondary = AccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextSecondary,

    // Tertiary - Purple
    tertiary = AccentTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TextSecondary,

    // Background & Surface - Pro Max Gray + Glass Surface
    background = BackgroundApp,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurfaceMuted,
    onSurfaceVariant = TextMuted,
    surfaceTint = AccentBlue,
    inverseSurface = Color(0xFF1C1C1E), // Dark inverse
    inverseOnSurface = BackgroundSurface,

    // Error - Red
    error = AccentDanger,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = AccentDanger,

    // Outlines
    outline = BorderSubtle,
    outlineVariant = TextTertiary,
    scrim = BackdropScrim
)

/**
 * AppTheme — "Sleek Glass" (Pro Max) 顶层主题包装
 *
 * @param darkTheme 当前未启用，保留接口
 * @param dynamicColor 当前未启用，保留接口
 * @param content 子级 Composable 内容
 */
@Suppress("UnusedParameter")
@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SleekGlassColorScheme
    val view = LocalView.current

    // 设置状态栏颜色为 Pro Max Gray, 使用深色图标
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
