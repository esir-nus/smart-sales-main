package com.smartsales.aitest.ui

// 文件：app/src/main/java/com/smartsales/aitest/ui/DesignKit.kt
// 模块：:app
// 说明：对齐 React 参考的轻量视觉规范，便于 Compose 界面复用
// 作者：创建于 2025-02-28

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 统一的色板，向 React UI 靠近。 */
object AppPalette {
    val Background = Color(0xFFF2F2F7)
    val Card = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5E5EA)
    val MutedText = Color(0xFF8E8E93)
    val Accent = Color(0xFF007AFF)
    val Success = Color(0xFF34C759)
}

/** 圆角与阴影设置，模拟轻量新拟态。 */
object AppShapes {
    val CardShape = RoundedCornerShape(16.dp)
    val ButtonShape = RoundedCornerShape(14.dp)
}

/** 卡片基础样式，带浅阴影与细边。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = AppShapes.CardShape, clip = false),
        shape = AppShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
        border = BorderStroke(1.dp, AppPalette.Border),
        content = { content() }
    )
}

/** 细边按钮，偏 React “上传新文件” 的视觉。 */
@Composable
fun AppGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.ButtonShape,
        border = BorderStroke(1.dp, AppPalette.Border),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.Accent)
    ) {
        Text(text = text, color = AppPalette.Accent)
    }
}

/** 标签块，用于 Applied/状态提示。 */
@Composable
fun AppBadge(
    text: String,
    background: Color = AppPalette.Border,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
