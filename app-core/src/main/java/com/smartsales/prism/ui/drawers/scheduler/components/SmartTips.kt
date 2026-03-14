package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.BorderSubtle
import com.smartsales.prism.ui.theme.TextSecondary

/**
 * 闪烁占位线 — 加载提示时的渐变动画
 */
@Composable
fun ShimmerLine(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(12.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BorderSubtle.copy(alpha = 0.3f),
                        BorderSubtle.copy(alpha = 0.6f),
                        BorderSubtle.copy(alpha = 0.3f)
                    ),
                    start = Offset(offsetX * 500f, 0f),
                    end = Offset(offsetX * 500f + 200f, 0f)
                ),
                shape = RoundedCornerShape(4.dp)
            )
    )
}

/**
 * 提示气泡 — 紧凑药丸形状 + 💡图标
 */
@Composable
fun TipBubble(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple plain text icon instead of emoji
        Text(text = "!", fontSize = 12.sp, color = AccentBlue, modifier = Modifier.padding(end = 6.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}
