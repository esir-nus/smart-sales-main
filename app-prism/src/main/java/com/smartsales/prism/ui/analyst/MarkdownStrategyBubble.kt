package com.smartsales.prism.ui.analyst

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.components.MarkdownText

/**
 * Markdown Strategy Bubble — Analyst 模式策略气泡
 * 
 * 展示分析策略的 Markdown 结构体。不包含操作按钮。
 * 作为 AI 聊天气泡内容渲染。
 * 
 * @see prism-ui-ux-contract.md "Markdown Strategy (Rich Chat Bubble)"
 */
@Composable
fun MarkdownStrategyBubble(
    content: String,
    onConfirm: () -> Unit = {},
    onAmend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundSurface.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        MarkdownText(
            text = content,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onAmend) {
                Text("修改计划", color = Color(0xFF88CCFF))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
            ) {
                Text("执行此计划", color = Color(0xFF0D1117))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun MarkdownStrategyBubblePreview() {
    MarkdownStrategyBubble(
        content = """
            ### 周度客户分析报告
            
            * 数据汇总：提取本周所有拜访记录。
            * 趋势分析：对比上周转化率指标。
            * 报告生成：输出分析结论。
            
            **当前洞察**: 拜访量上升20%，但转化率下降...
        """.trimIndent()
    )
}
