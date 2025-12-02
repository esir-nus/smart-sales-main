package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ChatWelcomeScreen.kt
// 模块：:app
// 说明：聊天欢迎页，展示问候与能力列表
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ChatWelcomeScreen(userName: String = "用户", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "👋", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "您好，$userName",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "我是您的 AI 销售助手",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📊 内容总结 - 快速总结通话要点", style = MaterialTheme.typography.bodyMedium)
            Text("🤔 异议分析 - 识别客户顾虑", style = MaterialTheme.typography.bodyMedium)
            Text("💡 话术辅导 - 优化沟通策略", style = MaterialTheme.typography.bodyMedium)
            Text("📝 生成日报 - 自动整理工作", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "请在下方输入或选择技能开始对话",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
