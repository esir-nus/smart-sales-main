package com.smartsales.prism.ui.sim

/**
 * SIM 语音草稿交互模式。
 */
enum class SimVoiceDraftInteractionMode {
    HOLD_TO_SEND,
    TAP_TO_SEND
}

/**
 * SIM 语音草稿状态。
 */
data class SimVoiceDraftUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val awaitingMicPermission: Boolean = false,
    val interactionMode: SimVoiceDraftInteractionMode = SimVoiceDraftInteractionMode.HOLD_TO_SEND,
    val liveTranscript: String = "",
    val errorMessage: String? = null
)
