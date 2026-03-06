package com.smartsales.prism.ui.analyst

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary

/**
 * Task Board — Analyst 模式顶部任务板
 * 
 * 垂直编号列表，显示工作流快捷选项。
 * 
 * @see prism-ui-ux-contract.md "Task Board (Sticky Top Layer)"
 */
@Composable
fun TaskBoard(
    items: List<TaskBoardItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundSurface.copy(alpha = 0.95f),
                        BackgroundSurface.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        items.forEachIndexed { index, item ->
            TaskBoardRow(
                index = index + 1,
                item = item,
                onClick = { onItemClick(item.id) }
            )
            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TaskBoardRow(
    index: Int,
    item: TaskBoardItem,
    onClick: () -> Unit
) {
    val isCustom = item.id == "custom"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCustom) Color.Transparent
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 编号
        if (!isCustom) {
            Text(
                text = "$index.",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(24.dp)
            )
        }
        
        // 图标
        Text(
            text = item.icon,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        
        // 标题 + 描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = if (isCustom) TextSecondary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCustom) FontWeight.Normal else FontWeight.Medium
            )
            if (!isCustom && item.description.isNotBlank()) {
                Text(
                    text = "— ${item.description}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun TaskBoardPreview() {
    TaskBoard(
        items = listOf(
            TaskBoardItem("1", "📊", "周度销售分析", "汇总本周拜访数据，生成趋势报告"),
            TaskBoardItem("2", "📈", "竞品对比分析", "对比主要竞品的价格、功能、市场策略"),
            TaskBoardItem("3", "📝", "会议纪要整理", "从录音中提取要点、行动项、决策"),
            TaskBoardItem("custom", "💡", "你也可以说出自己的需求...", "")
        ),
        onItemClick = {}
    )
}
