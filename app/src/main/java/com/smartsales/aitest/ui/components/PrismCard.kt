package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/PrismCard.kt
// 模块：:app
// 说明：Prism 玻璃卡片组件 — "Sleek Glass" (Pro Max) 交互卡片
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.BackgroundSurfaceHover
import com.smartsales.aitest.ui.theme.BorderSubtle
import com.smartsales.aitest.ui.theme.GlassCardShape

/**
 * PrismCard — Pro Max 交互玻璃卡片
 *
 * 在 PrismSurface 基础上增加:
 * - 点击交互
 * - 按压缩放动画 (scale = 0.98)
 * - Hover 背景变化
 *
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param shape 形状, 默认 GlassCardShape (20dp)
 * @param elevation 阴影高度, 默认 8dp
 * @param enabled 是否可点击
 * @param content 子内容
 */
@Composable
fun PrismCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    elevation: Dp = 8.dp,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压时缩放和背景变化
    val scale = if (isPressed) 0.98f else 1f
    val backgroundColor = if (isPressed) BackgroundSurfaceHover else BackgroundSurface

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(backgroundColor, shape)
            .border(0.5.dp, BorderSubtle, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.Black.copy(alpha = 0.05f)),
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}

/**
 * PrismCardStatic — 非交互玻璃卡片
 *
 * 纯展示用途, 无点击效果
 */
@Composable
fun PrismCardStatic(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    elevation: Dp = 8.dp,
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
            .background(BackgroundSurface, shape)
            .border(0.5.dp, BorderSubtle, shape),
        content = content
    )
}
