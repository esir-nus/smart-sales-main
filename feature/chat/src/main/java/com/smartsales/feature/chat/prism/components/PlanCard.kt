package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.domain.prism.core.DeliverableType
import com.smartsales.domain.prism.core.ExecutionPlan
import com.smartsales.domain.prism.core.RetrievalScope

/**
 * 计划卡片 — Analyst 模式下展示执行计划
 * @see prism-ui-ux-contract.md §1.2 Plan Card
 */
@Composable
fun PlanCard(
    plan: ExecutionPlan,
    completedSteps: Set<Int> = emptySet(),
    onRunStep: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allComplete = completedSteps.size == plan.deliverables.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A5F)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (allComplete) "✅" else "📋",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (allComplete) "计划已完成" else "分析计划",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF3A5F8A),
                thickness = 1.dp
            )

            // 步骤列表
            plan.deliverables.forEachIndexed { index, deliverable ->
                val isComplete = index in completedSteps
                val stepTitle = deliverableToTitle(deliverable)
                PlanStepRow(
                    index = index + 1,
                    title = stepTitle,
                    isComplete = isComplete,
                    onRun = { onRunStep(index) }
                )
                if (index < plan.deliverables.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 完成摘要
            if (allComplete) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF3A5F8A), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📊 ${completedSteps.size} 项任务完成",
                    color = Color(0xFF88CCFF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 将 DeliverableType 转换为中文标题
 */
private fun deliverableToTitle(type: DeliverableType): String {
    return when (type) {
        DeliverableType.CHAT_RESPONSE -> "生成对话回复"
        DeliverableType.CHAPTER -> "章节分析"
        DeliverableType.KEY_INSIGHT -> "关键洞察提取"
        DeliverableType.CHART -> "图表生成"
        DeliverableType.SCHEDULED_TASK -> "日程安排"
        DeliverableType.INSPIRATION -> "灵感记录"
    }
}

@Composable
private fun PlanStepRow(
    index: Int,
    title: String,
    isComplete: Boolean,
    onRun: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isComplete) "☑️" else "☐",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$index. $title",
                color = if (isComplete) Color(0xFF888888) else Color.White,
                fontSize = 14.sp,
                textDecoration = if (isComplete) TextDecoration.LineThrough else null
            )
        }

        if (!isComplete) {
            Text(
                text = "[运行]",
                color = Color(0xFF4FC3F7),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onRun() }
                    .padding(4.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlanCardPreview() {
    PlanCard(
        plan = ExecutionPlan(
            retrievalScope = RetrievalScope.HOT_ONLY,
            deliverables = listOf(
                DeliverableType.KEY_INSIGHT,
                DeliverableType.CHART,
                DeliverableType.CHAPTER
            )
        ),
        completedSteps = setOf(0)
    )
}
