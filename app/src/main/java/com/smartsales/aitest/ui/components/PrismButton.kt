package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/PrismButton.kt
// 模块：:app
// 说明：Prism 按钮组件 — "Sleek Glass" (Pro Max) 高保真按钮
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.theme.AccentBlue
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.BorderSubtle
import com.smartsales.aitest.ui.theme.ButtonShape
import com.smartsales.aitest.ui.theme.TextPrimary

/**
 * PrismButton 变体类型
 */
enum class PrismButtonStyle {
    /** 实心主色按钮 */
    SOLID,
    /** 玻璃透明按钮 */
    GLASS,
    /** 幽灵/线框按钮 */
    GHOST
}

/**
 * PrismButton — Pro Max 高保真按钮
 *
 * 支持三种风格: Solid (实心), Glass (玻璃), Ghost (线框)
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param style 按钮风格
 * @param enabled 是否可用
 * @param leadingIcon 前置图标 (可选)
 */
@Composable
fun PrismButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PrismButtonStyle = PrismButtonStyle.SOLID,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.97f else 1f

    // 根据风格设置颜色
    val (backgroundColor, textColor, borderColor) = when (style) {
        PrismButtonStyle.SOLID -> Triple(
            if (enabled) AccentBlue else AccentBlue.copy(alpha = 0.5f),
            Color.White,
            Color.Transparent
        )
        PrismButtonStyle.GLASS -> Triple(
            BackgroundSurface,
            TextPrimary,
            BorderSubtle
        )
        PrismButtonStyle.GHOST -> Triple(
            Color.Transparent,
            if (enabled) AccentBlue else AccentBlue.copy(alpha = 0.5f),
            if (enabled) AccentBlue else AccentBlue.copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(ButtonShape)
            .background(backgroundColor, ButtonShape)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, ButtonShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

/**
 * PrismButtonPrimary — 主色实心按钮快捷方式
 */
@Composable
fun PrismButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrismButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = PrismButtonStyle.SOLID,
        enabled = enabled
    )
}

/**
 * PrismButtonSecondary — 玻璃按钮快捷方式
 */
@Composable
fun PrismButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrismButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = PrismButtonStyle.GLASS,
        enabled = enabled
    )
}
