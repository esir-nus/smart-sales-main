package com.smartsales.feature.chat.home.transcription

import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/transcription/TranscriptionUiState.kt
// 模块：:feature:chat
// 说明：转写相关的 UI 状态，从 HomeViewModel 中提取
// 作者：创建于 2026-01-05

/**
 * 转写模块的 UI 状态
 */
data class TranscriptionUiState(
    val jobId: String? = null,
    val isActive: Boolean = false,
    val isFinal: Boolean = false,
    val currentMessageId: String? = null
)

/**
 * 处理后的批次数据（经过门禁和窗口过滤）
 */
data class ProcessedBatch(
    val batchIndex: Int,
    val effectiveChunk: String,
    val isFinal: Boolean,
    val event: AudioTranscriptionBatchEvent.BatchReleased
)
