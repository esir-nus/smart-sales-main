package com.smartsales.prism.ui.analyst

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.analyst.PlannerStep
import com.smartsales.prism.domain.analyst.PlannerTable
import com.smartsales.prism.domain.analyst.StepStatus
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary

/**
 * Planner Table Bubble — Analyst 模式计划表格气泡
 * 
 * 展示分析计划的结构化表格。仅展示状态，不包含操作按钮。
 * 作为 AI 聊天气泡内容渲染。
 * 
 * @see prism-ui-ux-contract.md "Planner Table (Rich Chat Bubble)"
 */
@Composable
fun PlannerTableBubble(
    table: PlannerTable,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundSurface.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = table.title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(12.dp))
        
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "步骤",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(48.dp)
            )
            Text(
                text = "任务",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "状态",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(48.dp)
            )
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        
        // 步骤行
        table.steps.forEach { step ->
            PlannerStepRow(step = step)
        }
        
        // 洞察
        if (!table.insight.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "当前洞察：${table.insight}",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        
        // 就绪消息
        if (!table.readyMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✅",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = table.readyMessage,
                    color = Color(0xFF4ADE80), // 绿色
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PlannerStepRow(step: PlannerStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 步骤编号
        Text(
            text = "${step.index}",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(48.dp)
        )
        
        // 任务名
        Text(
            text = step.task,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        // 状态图标
        Text(
            text = statusToEmoji(step.status),
            fontSize = 16.sp,
            modifier = Modifier.width(48.dp)
        )
    }
}

private fun statusToEmoji(status: StepStatus): String = when (status) {
    StepStatus.COMPLETE -> "✅"
    StepStatus.IN_PROGRESS -> "⏳"
    StepStatus.PENDING -> "⏳"
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun PlannerTableBubblePreview() {
    PlannerTableBubble(
        table = PlannerTable(
            title = "周度客户分析报告",
            steps = listOf(
                PlannerStep(1, "数据汇总", StepStatus.COMPLETE),
                PlannerStep(2, "趋势分析", StepStatus.IN_PROGRESS),
                PlannerStep(3, "报告生成", StepStatus.PENDING)
            ),
            insight = "拜访量上升20%，但转化率下降...",
            readyMessage = "分析已完成，请选择后续操作"
        )
    )
}
