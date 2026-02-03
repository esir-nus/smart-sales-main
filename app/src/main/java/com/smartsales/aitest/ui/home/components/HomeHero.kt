package com.smartsales.aitest.ui.home.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/components/HomeHero.kt
// 模块：:app
// 说明：首页 Hero 区域 — 包含呼吸光环 (Aurora) 和问候语
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.theme.AccentBlue
import com.smartsales.aitest.ui.theme.AccentSecondary
import com.smartsales.aitest.ui.theme.TextMuted
import com.smartsales.aitest.ui.theme.TextPrimary

/**
 * HomeHero — 首页核心视觉区域
 *
 * 视觉元素:
 * - 动态呼吸光环 (Aurora)
 * - 问候语 "下午好, Frank"
 *
 * @param greeting 问候语
 * @param userName 用户名
 */
@Composable
fun HomeHero(
    greeting: String = "下午好",
    userName: String = "Frank",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Breathing Aurora Background
        AuroraBackground()

        // 2. Greeting Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 40.dp) // 稍微下移，避开 Scheduler 区域
        ) {
            Text(
                text = "$greeting, $userName",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Text(
                text = "今天想聊点什么?",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        }
    }
}

/**
 * AuroraBackground — 动态呼吸光环
 *
 * 使用 Canvas 绘制模糊渐变圆，模拟极光效果
 */
@Composable
private fun AuroraBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // 呼吸动画: 缩放 + 透明度
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraAlpha"
    )

    Canvas(
        modifier = Modifier
            .size(300.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // 混合蓝色和绿色 (Prism 品牌色)
        val brush = Brush.radialGradient(
            colors = listOf(
                AccentBlue.copy(alpha = 0.15f),
                AccentSecondary.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )

        drawCircle(
            brush = brush,
            center = center,
            radius = radius
        )
    }
}
