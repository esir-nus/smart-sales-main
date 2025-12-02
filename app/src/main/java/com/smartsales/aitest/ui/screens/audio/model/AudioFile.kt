package com.smartsales.aitest.ui.screens.audio.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/model/AudioFile.kt
// 模块：:app
// 说明：底部导航 Audio 页的 UI 专用录音模型（仅本地模拟）
// 作者：创建于 2025-12-02

data class AudioFile(
    val id: String,
    val fileName: String,
    val duration: Int, // seconds
    val recordedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NONE,
    val transcript: String? = null,
    val fileSize: Long = 0L
)

enum class SyncStatus { LOCAL, SYNCING, SYNCED }

enum class TranscriptionStatus { NONE, PROCESSING, COMPLETED, FAILED }
