package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/TranscriptionChatRequest.kt
// 模块：:feature:chat
// 说明：承载音频转写导航到聊天界面的参数
// 作者：创建于 2025-11-21

data class TranscriptionChatRequest(
    val jobId: String,
    val fileName: String,
    val recordingId: String? = null
)
