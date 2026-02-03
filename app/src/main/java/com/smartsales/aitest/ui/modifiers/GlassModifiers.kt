package com.smartsales.aitest.ui.modifiers

// 文件：app/src/main/java/com/smartsales/aitest/ui/modifiers/GlassModifiers.kt
// 模块：:app
// 说明：可复用的 Modifier 扩展 — "Sleek Glass" (Pro Max) 玻璃效果
//       实现 blur + border + shadow 组合效果
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.BorderSubtle
import com.smartsales.aitest.ui.theme.GlassCardShape

// ===================================================================
// Glass Surface Modifier
// ===================================================================

/**
 * 玻璃表面修饰符 — Pro Max 风格
 *
 * 实现效果:
 * - 高透明度白色背景 (70%)
 * - 超细边框 (0.5dp, 4% opacity)
 * - 深阴影 (20dp blur, 5% opacity)
 * - 20dp 圆角
 *
 * 注意: Android Compose 原生不支持 backdrop-filter blur,
 * 此处使用阴影 + 透明度模拟玻璃效果。真正的模糊需要自定义 RenderEffect。
 *
 * @param elevation 阴影高度, 默认 8dp
 * @param shape 形状, 默认 GlassCardShape (20dp 圆角)
 * @param backgroundColor 背景色, 默认 BackgroundSurface (70% 白色)
 * @param borderColor 边框色, 默认 BorderSubtle (4% 黑色)
 * @param borderWidth 边框宽度, 默认 0.5dp (Retina hairline)
 */
fun Modifier.glass(
    elevation: Dp = 8.dp,
    shape: Shape = GlassCardShape,
    backgroundColor: Color = BackgroundSurface,
    borderColor: Color = BorderSubtle,
    borderWidth: Dp = 0.5.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.05f),
        spotColor = Color.Black.copy(alpha = 0.05f)
    )
    .clip(shape)
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)

/**
 * 玻璃卡片修饰符 — 带预设阴影
 *
 * 快捷方式, 使用标准卡片配置
 */
fun Modifier.glassCard(): Modifier = glass(
    elevation = 8.dp,
    shape = GlassCardShape
)

/**
 * 轻量玻璃修饰符 — 无阴影
 *
 * 适用于嵌套表面, 避免阴影叠加
 */
fun Modifier.glassSubtle(
    shape: Shape = GlassCardShape
): Modifier = this
    .clip(shape)
    .background(BackgroundSurface, shape)
    .border(0.5.dp, BorderSubtle, shape)
