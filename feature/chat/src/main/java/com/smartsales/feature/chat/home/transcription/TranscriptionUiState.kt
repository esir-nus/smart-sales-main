package com.smartsales.feature.chat.home.transcription

/**
 * 转写模块的 UI 状态
 */
data class TranscriptionUiState(
    val jobId: String? = null,
    val isActive: Boolean = false,
    val isFinal: Boolean = false,
    val currentMessageId: String? = null
)
