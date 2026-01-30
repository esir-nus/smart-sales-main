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
    maxLines: Int = 20,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
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
        // 一级标题：Phase（阶段）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activity.phase.toDisplayText(),
                color = activity.phase.toColor(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            // 折叠指示器（仅在有 trace 时显示）
            if (activity.trace.isNotEmpty()) {
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        // 二级标题：Action（动作）— 可选
        activity.action?.let { action ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.toDisplayText(),
                color = Color.Cyan.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        // 三级内容：Trace（痕迹）— 可折叠
        AnimatedVisibility(
            visible = isExpanded && activity.trace.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                val displayLines = activity.trace.take(maxLines)
                displayLines.forEach { line ->
                    Text(
                        text = "> $line",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
                
                // 截断提示
                if (activity.trace.size > maxLines) {
                    Text(
                        text = "... (${activity.trace.size - maxLines} 行已隐藏)",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
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
    ActivityPhase.PLANNING -> "📝 规划分析步骤"
    ActivityPhase.EXECUTING -> "⚙️ 执行工具"
    ActivityPhase.RESPONDING -> "💬 生成回复"
    ActivityPhase.ERROR -> "⚠️ 发生错误"
}

/**
 * Phase 颜色
 */
private fun ActivityPhase.toColor(): Color = when (this) {
    ActivityPhase.PLANNING -> Color(0xFF64B5F6)   // 蓝色
    ActivityPhase.EXECUTING -> Color(0xFFFFB74D)  // 橙色
    ActivityPhase.RESPONDING -> Color(0xFF81C784) // 绿色
    ActivityPhase.ERROR -> Color(0xFFE57373)      // 红色
}

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
