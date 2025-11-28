package com.smartsales.core.util

// 文件：core/util/src/main/java/com/smartsales/core/util/DesignTokens.kt
// 模块：:core:util
// 说明：UI 设计 token 定义，统一颜色、圆角、阴影与手柄尺寸，方便多屏复用
// 作者：创建于 2025-11-28

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme

/**
 * 设计 token，用于保持多模块视觉一致。
 */
data class DesignTokens(
    val mutedSurface: Color,
    val cardShape: Shape,
    val cardBorder: Color,
    val cardElevation: Dp,
    val ctaGradient: List<Color>,
    val overlayRailWidth: Dp,
    val overlayHandleSize: DpSize,
    val overlayHandleColor: Color
)

object AppDesignTokens {
    @Composable
    fun current(): DesignTokens = DesignTokens(
        mutedSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),
        cardShape = MaterialTheme.shapes.large,
        cardBorder = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        cardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp).defaultElevation,
        ctaGradient = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
        ),
        overlayRailWidth = 68.dp,
        overlayHandleSize = DpSize(28.dp, 3.dp),
        overlayHandleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    )
}
