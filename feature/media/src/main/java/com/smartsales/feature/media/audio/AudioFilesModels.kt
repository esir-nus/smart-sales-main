package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesModels.kt
// 模块：:feature:media
// 说明：定义音频库的 UI 状态与展示模型
// 作者：创建于 2025-11-21

data class AudioFilesUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val recordings: List<AudioRecordingUi> = emptyList(),
    val selectedRecordingId: String? = null,
    val errorMessage: String? = null
)

data class AudioRecordingUi(
    val id: String,
    val title: String,
    val fileName: String,
    val durationMillis: Long? = null,
    val createdAtMillis: Long? = null,
    val createdAtText: String = "",
    val transcriptionStatus: AudioTranscriptionStatus = AudioTranscriptionStatus.None,
    val isPlaying: Boolean = false,
    val hasLocalCopy: Boolean = false
)

enum class AudioTranscriptionStatus {
    None,
    InProgress,
    Done,
    Error
}
