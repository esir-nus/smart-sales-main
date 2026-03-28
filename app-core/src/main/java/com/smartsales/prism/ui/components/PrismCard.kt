package com.smartsales.prism.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.smartsales.prism.ui.theme.GlassCardShape
import com.smartsales.prism.ui.theme.PrismThemeDefaults

/**
 * PrismCard — Pro Max 交互玻璃卡片 (Feature Module Copy)
 *
 * 在 PrismSurface 基础上增加:
 * - 点击交互
 * - 按压缩放动画 (scale = 0.98)
 * - Hover 背景变化
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
    val colors = PrismThemeDefaults.colors

    // 按压时缩放和背景变化
    val scale = if (isPressed) 0.98f else 1f
    val backgroundColor = if (isPressed) colors.surfaceHover else colors.surface

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
            .border(0.5.dp, colors.borderSubtle, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.accentPrimary.copy(alpha = 0.08f)),
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
    val colors = PrismThemeDefaults.colors
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(colors.surface, shape)
            .border(0.5.dp, colors.borderSubtle, shape),
        content = content
    )
}
