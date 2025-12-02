package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ChatBubble.kt
// 模块：:app
// 说明：旧版气泡占位（兼容遗留引用）
// 作者：创建于 2025-12-02

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartsales.aitest.ui.screens.home.model.ChatMessage

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    // 留空以兼容旧调用，新版气泡使用 ChatMessageBubble
}
