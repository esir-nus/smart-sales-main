package com.smartsales.prism.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay

/**
 * 响应气泡 — 展示 AI 响应内容
 * @see prism-ui-ux-contract.md §1.2 Response Bubble
 */
@Composable
fun ResponseBubble(
    uiState: UiState,
    onConfirmPlan: () -> Unit = {},
    onAmendPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Idle -> {
            // 空状态，不展示
        }
        is UiState.Loading -> {
            LoadingBubble(modifier)
        }
        is UiState.Thinking -> {
            ThinkingIndicator(hint = uiState.hint, modifier = modifier)
        }
        is UiState.Streaming -> {
            StreamingBubble(content = uiState.partialContent, modifier = modifier)
        }
        is UiState.Response -> {
            CompleteBubble(content = uiState.content, modifier = modifier)
        }
        is UiState.AudioArtifacts -> {
            CompleteBubble(content = "已生成音频结构化结果，请在对话历史中查看。", modifier = modifier)
        }
        
        // V2: Markdown Strategy 展示
        is UiState.MarkdownStrategyState -> {
            com.smartsales.prism.ui.analyst.MarkdownStrategyBubble(
                title = uiState.title,
                content = uiState.markdownContent,
                onConfirm = onConfirmPlan,
                onAmend = onAmendPlan,
                modifier = modifier
            )
        }
        
        is UiState.SchedulerTaskCreated -> {
            CompleteBubble(content = "已创建任务: ${uiState.title}", modifier = modifier)
        }
        
        is UiState.SchedulerMultiTaskCreated -> {
            CompleteBubble(
                content = "已创建 ${uiState.tasks.size} 个任务",
                modifier = modifier
            )
        }
        
        is UiState.AwaitingClarification -> {
            ClarifyingBubble(question = uiState.question, modifier = modifier)
        }
        
        is UiState.Error -> {
            ErrorBubble(message = uiState.message, modifier = modifier)
        }
        
        is UiState.Toast -> {
            // Toast 通过 Toast 机制显示，不进入聊天气泡
        }
        
        is UiState.ExecutingTool -> {
            // ExecutingTool (TaskBoard bypass loading) is rendered as an AgentActivity or skipped here since it's handled in AgentChatScreen history rendering directly
        }
        
        is UiState.BadgeDelegationHint -> {
            // Already handled in AgentChatScreen explicitly. Ignored here.
        }

    }
}

@Composable
private fun LoadingBubble(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF4FC3F7),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "加载中...",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(hint: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color(0xFF4FC3F7),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = hint ?: "正在思考...",
                color = Color(0xFFAAFFAA),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun StreamingBubble(content: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 闪烁光标
            var cursorVisible by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(500)
                    cursorVisible = !cursorVisible
                }
            }
            if (cursorVisible) {
                Text(
                    text = "▌",
                    color = Color(0xFF4FC3F7),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CompleteBubble(content: String, modifier: Modifier = Modifier) {
    val parts = content.split("---EXPAND---")
    val fullText = if (parts.size > 1) {
        (parts[0].trim() + "\n\n" + parts.drop(1).joinToString("\n").trim()).trim()
    } else {
        content
    }
    CopyableBubble(textToCopy = fullText, modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
        ) {
            if (parts.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(16.dp)) {
                MarkdownText(
                    text = parts[0].trim(),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF444455), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MarkdownText(
                        text = parts.drop(1).joinToString("\n").trim(),
                        color = Color(0xFFCCCCCC), // Slightly dimmed for expanded content
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = false },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("收起原文", color = Color(0xFF4FC3F7), fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("展开完整解析与原文...", color = Color(0xFF4FC3F7), fontSize = 12.sp)
                    }
                }
            }
        } else {
            MarkdownText(
                text = content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    }
}

@Composable
private fun ErrorBubble(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2020))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ClarifyingBubble(question: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF88CCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "需要更多信息",
                    color = Color(0xFF88CCFF),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ResponseBubbleStreamingPreview() {
    ResponseBubble(
        uiState = UiState.Streaming("根据您与张总的交流记录，我发现他对A3打印机方案表现出较高兴趣...")
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ResponseBubbleCompletePreview() {
    ResponseBubble(
        uiState = UiState.Response(
            content = "分析完成。张总对产品认可度较高，建议跟进售后方案。",
            structuredJson = null
        )
    )
}
