package com.smartsales.feature.chat

import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportResult
import java.util.UUID

// 文件路径: feature/chat/src/main/java/com/smartsales/feature/chat/ChatModels.kt
// 文件作用: 定义聊天状态、消息与导出/转写相关模型
// 最近修改: 2025-11-14
enum class ChatSkill(val tag: String, val label: String) {
    Analyze(tag = "analyze", label = "分析"),
    Pdf(tag = "pdf", label = "导出PDF"),
    Csv(tag = "csv", label = "导出CSV")
}

enum class ChatRole { User, Assistant, Transcript }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromUser(content: String) = ChatMessage(role = ChatRole.User, content = content)
        fun fromAssistant(content: String) = ChatMessage(role = ChatRole.Assistant, content = content)
        fun fromTranscript(content: String) = ChatMessage(role = ChatRole.Transcript, content = content)
    }
}

sealed class ChatExportState {
    data object Idle : ChatExportState()
    data class InProgress(val format: ExportFormat) : ChatExportState()
    data class Completed(val result: ExportResult) : ChatExportState()
    data class Failed(val reason: String) : ChatExportState()
}

sealed class TranscriptState {
    data object Idle : TranscriptState()
    data class InProgress(val jobId: String, val percent: Int, val sourceName: String) : TranscriptState()
    data class Ready(val jobId: String, val markdown: String, val sourceName: String) : TranscriptState()
    data class Error(val jobId: String?, val reason: String) : TranscriptState()
}

data class ChatState(
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val selectedSkills: Set<ChatSkill> = setOf(ChatSkill.Analyze),
    val isSending: Boolean = false,
    val structuredMarkdown: String? = null,
    val exportState: ChatExportState = ChatExportState.Idle,
    val transcriptState: TranscriptState = TranscriptState.Idle,
    val clipboardMessage: String? = null,
    val errorMessage: String? = null
)
