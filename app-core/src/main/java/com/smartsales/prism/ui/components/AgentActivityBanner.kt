package com.smartsales.prism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentSecondary
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextPrimary
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
    autoCollapse: Boolean = true, // 是否自动折叠（Analyst 模式为 false）
    modifier: Modifier = Modifier
) {
    // Logic: 
    // 1. Initially collapsed (header only)
    // 2. Auto-expand when trace starts (trace.size > 0 && trace.size <= 3)
    // 3. Auto-collapse when trace exceeds 3 lines (if autoCollapse=true)
    
    // COMPLETED phase starts collapsed, others follow auto-behavior
    val isCompleted = activity.phase == ActivityPhase.COMPLETED
    var isExpanded by remember { mutableStateOf(false) }
    var hasAutoExpanded by remember { mutableStateOf(false) }
    var hasAutoCollapsed by remember { mutableStateOf(false) }
    
    // Auto-behavior Logic
    LaunchedEffect(activity.trace.size) {
        if (activity.trace.isNotEmpty()) {
            if (!hasAutoExpanded && activity.trace.size <= 3) {
                isExpanded = true
                hasAutoExpanded = true
            } else if (autoCollapse && isExpanded && !hasAutoCollapsed && activity.trace.size > 3) {
                // Delay slightly to let user see "something is happening" before collapsing
                kotlinx.coroutines.delay(500)
                isExpanded = false
                hasAutoCollapsed = true
            }
        }
    }

    PrismSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Shimmer Animation for Header (disabled for COMPLETED phase)
            val infiniteTransition = rememberInfiniteTransition(label = "ThinkingShimmer")
            val alpha by infiniteTransition.animateFloat(
                initialValue = if (isCompleted) 0.7f else 0.5f,
                targetValue = if (isCompleted) 0.7f else 1.0f, // Static for completed
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ThinkingAlpha"
            )
            
            // Header text based on phase
            val headerText = if (isCompleted) "思考已完成" else (activity.action?.toDisplayText() ?: "思考中...")
            val headerColor = if (isCompleted) AccentSecondary else AccentBlue.copy(alpha = alpha)

            // 一级标题：Header (Always Visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Header title (static for COMPLETED, shimmering otherwise)
                    Text(
                        text = headerText, 
                        color = headerColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Phase Description
                    Text(
                        text = activity.phase.toDisplayText(),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                
                // Expand/Collapse Icon
                Text(
                    text = if (isExpanded) "[∧]" else "[∨]",
                    color = TextMuted,
                    fontSize = 12.sp,
                     fontFamily = FontFamily.Monospace
                )
            }
            
            // 二级内容：Trace（痕迹）— 可折叠
            
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
                        .background(Color.Black.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val displayLines = activity.trace
                    displayLines.forEachIndexed { index, line ->
                        Text(
                            text = line,
                            color = TextPrimary.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
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
    ActivityPhase.COMPLETED -> "思考已完成"
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
    ActivityAction.THINKING -> "思考中..."
    ActivityAction.PARSING -> "解析中..."
    ActivityAction.TRANSCRIBING -> "转写中..."
    ActivityAction.RETRIEVING -> "检索记忆..."
    ActivityAction.ASSEMBLING -> "整理上下文..."
    ActivityAction.STREAMING -> "生成中..."
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7)
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7)
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7)
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
