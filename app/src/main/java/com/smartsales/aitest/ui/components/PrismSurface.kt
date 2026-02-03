package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/PrismSurface.kt
// 模块：:app
// 说明：Prism 玻璃表面组件 — "Sleek Glass" (Pro Max) 基础容器
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/**
 * PrismSurface — Pro Max 玻璃表面容器
 *
 * 实现 Chapter 5 VI Guide 的 "Sleek Glass" 视觉效果:
 * - 70% 透明度白色背景
 * - 0.5dp 超细边框
 * - 8dp 柔和阴影
 * - 20dp 现代圆角
 *
 * @param modifier 修饰符
 * @param shape 形状, 默认 GlassCardShape (20dp)
 * @param backgroundColor 背景色, 默认 70% 白色
 * @param elevation 阴影高度, 默认 8dp
 * @param contentAlignment 内容对齐方式
 * @param content 子内容
 */
@Composable
fun PrismSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    backgroundColor: Color = BackgroundSurface,
    elevation: Dp = 8.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(backgroundColor, shape)
            .border(0.5.dp, BorderSubtle, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * PrismSurfaceSubtle — 无阴影玻璃表面
 *
 * 适用于嵌套容器, 避免阴影叠加
 */
@Composable
fun PrismSurfaceSubtle(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    backgroundColor: Color = BackgroundSurface,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(0.5.dp, BorderSubtle, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}
