package com.smartsales.feature.chat

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportResult

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/ChatShareHandler.kt
// 模块：:feature:chat
// 说明：定义聊天模块的分享与剪贴板钩子
// 作者：创建于 2025-11-16
interface ChatShareHandler {
    suspend fun copyMarkdown(markdown: String): Result<Unit>
    suspend fun copyAssistantReply(text: String): Result<Unit>
    suspend fun shareExport(result: ExportResult): Result<Unit>
}
