package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/PrismInput.kt
// 模块：:app
// 说明：Prism 输入框组件 — "Sleek Glass" (Pro Max) 搜索/聊天输入
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.BorderSubtle
import com.smartsales.aitest.ui.theme.ButtonShape
import com.smartsales.aitest.ui.theme.TextMuted
import com.smartsales.aitest.ui.theme.TextPrimary
import com.smartsales.aitest.ui.theme.TextSecondary

/**
 * PrismInput — Pro Max 玻璃输入框
 *
 * 特性:
 * - 玻璃背景 + 超细边框
 * - 闪烁占位符动画 (可选)
 * - 前置/后置图标支持
 *
 * @param value 当前输入值
 * @param onValueChange 值变化回调
 * @param modifier 修饰符
 * @param placeholder 占位符文字
 * @param leadingIcon 前置图标
 * @param trailingIcon 后置图标
 * @param shimmerPlaceholder 是否启用闪烁动画
 */
@Composable
fun PrismInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shimmerPlaceholder: Boolean = false,
    singleLine: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // 闪烁渐变
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            TextMuted.copy(alpha = 0.4f),
            TextMuted.copy(alpha = 0.8f),
            TextMuted.copy(alpha = 0.4f)
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset, 0f)
    )

    Box(
        modifier = modifier
            .clip(ButtonShape)
            .background(BackgroundSurface, ButtonShape)
            .border(0.5.dp, BorderSubtle, ButtonShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 前置图标
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 输入框
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            // 简化版闪烁效果 - 使用透明度变化模拟闪烁
                            // 注: 完整的 brush shimmer 需要更高版本的 Compose
                            val shimmerAlpha = if (shimmerPlaceholder) {
                                0.4f + 0.4f * kotlin.math.sin(shimmerOffset * 0.01f).toFloat()
                            } else {
                                1f
                            }
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted.copy(alpha = shimmerAlpha)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 后置图标
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(12.dp))
                it()
            }
        }
    }
}

/**
 * PrismSearchInput — 搜索输入框快捷方式
 */
@Composable
fun PrismSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索..."
) {
    PrismInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = placeholder,
        shimmerPlaceholder = true
    )
}
