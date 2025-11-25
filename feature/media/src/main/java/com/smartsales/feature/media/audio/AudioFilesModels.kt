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
    val transcriptPreviewRecording: AudioRecordingUi? = null,
    val tingwuTaskIds: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val loadErrorMessage: String? = null
)

data class AudioRecordingUi(
    val id: String,
    val title: String,
    val fileName: String,
    val durationMillis: Long? = null,
    val createdAtMillis: Long? = null,
    val createdAtText: String = "",
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NONE,
    val transcriptPreview: String? = null,
    val fullTranscriptMarkdown: String? = null,
    val isPlaying: Boolean = false,
    val hasLocalCopy: Boolean = false
)

enum class TranscriptionStatus {
    NONE,
    IN_PROGRESS,
    DONE,
    ERROR
}
