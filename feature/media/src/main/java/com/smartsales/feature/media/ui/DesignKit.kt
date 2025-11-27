package com.smartsales.feature.media.ui

// 文件：feature/media/src/main/java/com/smartsales/feature/media/ui/DesignKit.kt
// 模块：:feature:media
// 说明：媒体模块的轻量视觉组件，对齐 React 参考的卡片与标签风格
// 作者：创建于 2025-11-27

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 统一的色板，与 app 侧保持一致，减少跨模块色差。 */
object AppPalette {
    val Background = Color(0xFFF2F2F7)
    val Card = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5E5EA)
    val MutedText = Color(0xFF8E8E93)
    val Accent = Color(0xFF007AFF)
    val Success = Color(0xFF34C759)
}

/** 圆角与阴影参数，方便卡片统一。 */
object AppShapes {
    val CardShape = RoundedCornerShape(16.dp)
    val ButtonShape = RoundedCornerShape(14.dp)
}

/** 带阴影与描边的卡片容器。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, AppPalette.Border),
    containerColor: Color = AppPalette.Card,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = AppShapes.CardShape, clip = false),
        shape = AppShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        content = { content() }
    )
}

/** 细边按钮，默认使用品牌蓝色文字，可选前置图标。 */
@Composable
fun AppGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = AppShapes.ButtonShape,
        border = BorderStroke(1.dp, AppPalette.Border),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.Accent)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text = text, color = AppPalette.Accent)
        }
    }
}

/** 标签组件，常用于状态提示或章节标记。 */
@Composable
fun AppBadge(
    text: String,
    background: Color = AppPalette.Border,
    contentColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = contentColor,
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
