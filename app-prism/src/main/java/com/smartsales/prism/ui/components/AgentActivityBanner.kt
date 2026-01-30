package com.smartsales.prism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivity

/**
 * 代理活动横幅 — 两层结构展示 AI 思考过程
 * 
 * Phase（阶段）始终可见，Action（动作）可选，Trace（痕迹）可折叠。
 * 这是核心产品差异化 UI 组件。
 * 
 * @see ui_element_registry.md §6.2
 * @see Prism-V1.md §4.6.2
 */
@Composable
fun AgentActivityBanner(
    activity: AgentActivity,
    maxLines: Int = 20, // max visible lines when expanded
    modifier: Modifier = Modifier
) {
    // Logic: 
    // 1. Initially collapsed (header only)
    // 2. Auto-expand when trace starts (trace.size > 0 && trace.size <= 3)
    // 3. Auto-collapse when trace exceeds 3 lines
    
    var isExpanded by remember { mutableStateOf(false) }
    var hasAutoExpanded by remember { mutableStateOf(false) }
    var hasAutoCollapsed by remember { mutableStateOf(false) }
    
    // Auto-behavior Logic
    LaunchedEffect(activity.trace.size) {
        if (activity.trace.isNotEmpty()) {
            if (!hasAutoExpanded && activity.trace.size <= 3) {
                isExpanded = true
                hasAutoExpanded = true
            } else if (isExpanded && !hasAutoCollapsed && activity.trace.size > 3) {
                // Delay slightly to let user see "something is happening" before collapsing
                kotlinx.coroutines.delay(500)
                isExpanded = false
                hasAutoCollapsed = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color(0xFF2A2A3D), RoundedCornerShape(12.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp)
    ) {
        // Shimmer Animation for Header
        val infiniteTransition = rememberInfiniteTransition(label = "ThinkingShimmer")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ThinkingAlpha"
        )

        // 一级标题：Header (Always Visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // "思考中..." (Static if no action, or Action text)
                Text(
                    text = activity.action?.toDisplayText() ?: "思考中...", 
                    color = Color.LightGray.copy(alpha = alpha), // Shimmering Grey
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Phase Description
                Text(
                    text = activity.phase.toDisplayText(),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            // Expand/Collapse Icon
            Text(
                text = if (isExpanded) "[∧]" else "[∨]",
                color = Color.Gray,
                fontSize = 12.sp,
                 fontFamily = FontFamily.Monospace
            )
        }
        
        // 二级内容：Trace（痕迹）— 可折叠
        // Unlike previous version, NO Action Text here (moved to header)
        
        AnimatedVisibility(
            visible = isExpanded && activity.trace.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // Divider
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                val displayLines = activity.trace
                displayLines.forEachIndexed { index, line ->
                    Text(
                        text = "┃ $line",
                        color = Color.LightGray, // No shimmer for body
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        maxLines = 10, // Safety cap
                    )
                }
            }
        }
    }
}

/**
 * Phase 显示文案
 */
private fun ActivityPhase.toDisplayText(): String = when (this) {
    ActivityPhase.PLANNING -> "正在规划分析步骤"
    ActivityPhase.EXECUTING -> "正在执行工具调用"
    ActivityPhase.RESPONDING -> "正在生成回复内容"
    ActivityPhase.ERROR -> "发生错误"
}

/**
 * Phase 颜色 (Unused in DeepSeek style, kept for reference or removal)
 */
private fun ActivityPhase.toColor(): Color = Color.Gray // Normalized to Grey

/**
 * Action 显示文案
 */
private fun ActivityAction.toDisplayText(): String = when (this) {
    ActivityAction.THINKING -> "🧠 思考中..."
    ActivityAction.PARSING -> "📄 解析中..."
    ActivityAction.TRANSCRIBING -> "🎙️ 转写中..."
    ActivityAction.RETRIEVING -> "📚 检索记忆..."
    ActivityAction.ASSEMBLING -> "📋 整理上下文..."
    ActivityAction.STREAMING -> "✨ 生成中..."
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun AgentActivityBannerPreview_Planning() {
    AgentActivityBanner(
        activity = AgentActivity(
            phase = ActivityPhase.PLANNING,
            action = ActivityAction.THINKING,
            trace = listOf(
                "分析用户意图...",
                "检索相关记录...",
                "生成回复策略..."
            )
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun AgentActivityBannerPreview_Error() {
    AgentActivityBanner(
        activity = AgentActivity(
            phase = ActivityPhase.ERROR,
            action = null,
            trace = listOf("网络连接失败，请检查网络后重试")
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
private fun AgentActivityBannerPreview_PhaseOnly() {
    AgentActivityBanner(
        activity = AgentActivity(
            phase = ActivityPhase.RESPONDING,
            action = null,
            trace = emptyList()
        )
    )
}

// endregion
