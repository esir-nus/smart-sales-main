package com.smartsales.prism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 思考盒子 — Analyst 模式下展示 AI 思考过程
 * @see prism-ui-ux-contract.md §1.2 Thinking Box
 */
@Composable
fun ThinkingBox(
    content: String,
    isComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    // 3 秒后自动折叠
    LaunchedEffect(isComplete) {
        if (!isComplete) {
            delay(3000)
            isExpanded = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧠",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isComplete) "思考完成" else "思考中...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (isExpanded) "[∧]" else "[∨]",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }

            // 折叠内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = Color(0xFF333333), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content,
                        color = Color(0xFFAAFFAA),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ThinkingBoxPreview() {
    ThinkingBox(
        content = "> 检索Relevancy Library...\n> 找到3条相关记录\n> 分析客户偏好...",
        isComplete = false
    )
}
