package com.smartsales.prism.ui.components

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
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.BorderSubtle
import com.smartsales.prism.ui.theme.GlassCardShape

/**
 * PrismSurface — Pro Max 玻璃表面容器 (Feature Module Copy)
 *
 * 实现 Chapter 5 VI Guide 的 "Sleek Glass" 视觉效果:
 * - 70% 透明度白色背景
 * - 0.5dp 超细边框
 * - 8dp 柔和阴影
 * - 20dp 现代圆角
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
