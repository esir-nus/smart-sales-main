package com.smartsales.prism.ui.sim

import androidx.compose.ui.graphics.Color
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.drawers.AudioStatus

internal const val SIM_AUDIO_DEMO_SEED_ID = "sim_wave2_seed"

internal val SimDrawerSurface = Color(0xF0141416)
internal val SimDrawerCardSurface = Color(0x14FFFFFF)
internal val SimDrawerCardSurfaceStrong = Color(0x1BFFFFFF)
internal val SimDrawerCurrentSurface = Color(0x0DFFFFFF)
internal val SimDrawerCardBorder = Color(0x14FFFFFF)
internal val SimDrawerCardBorderStrong = Color(0x520A84FF)
internal val SimDrawerDivider = Color(0x12FFFFFF)
internal val SimDrawerDividerStrong = Color(0x1FFFFFFF)
internal val SimDrawerTextPrimary = Color(0xFFFFFFFF)
internal val SimDrawerTextSecondary = Color(0xB8FFFFFF)
internal val SimDrawerTextMuted = Color(0x80FFFFFF)
internal val SimDrawerTextFaint = Color(0x66FFFFFF)
internal val SimDrawerAccent = Color(0xFF0A84FF)
internal val SimDrawerAccentSuccess = Color(0xFF34C759)
internal val SimDrawerDeleteBackground = Color(0xFFFF453A)
internal val SimDrawerBlockedText = Color(0xFFA0A0A5)

internal enum class SimAudioSyncFeedback {
    SYNCED,
    ERROR,
    DENIED
}

internal enum class SimAudioSyncVisualState {
    READY,
    SYNCING,
    SYNCED,
    BLOCKED,
    RECONNECTING,
    ERROR
}

data class SimChatAudioSelection(
    val audioId: String,
    val title: String,
    val summary: String?,
    val status: AudioStatus,
    val localAvailability: AudioLocalAvailability = AudioLocalAvailability.READY
)

internal fun buildSimAudioSelectBodyText(
    entry: SimAudioEntry,
    transcriptPreview: String?,
    isCurrentChatAudio: Boolean
): String {
    val bodyText = when (entry.item.status) {
        AudioStatus.TRANSCRIBED -> {
            transcriptPreview
                ?.takeIf { it.isNotBlank() }
                ?: entry.item.summary
                ?: entry.preview
        }

        AudioStatus.TRANSCRIBING -> {
            if (isCurrentChatAudio) "转写中，当前聊天会继续处理"
            else "转写中，选择后将在当前聊天继续处理"
        }

        AudioStatus.PENDING -> {
            when (entry.localAvailability) {
                AudioLocalAvailability.READY -> {
                    if (isCurrentChatAudio) "可在当前聊天中继续处理"
                    else "选择后在当前聊天中继续处理"
                }
                AudioLocalAvailability.DOWNLOADING -> "录音正在后台同步，暂不可用于聊天"
                AudioLocalAvailability.QUEUED -> "录音等待后台同步，暂不可用于聊天"
                AudioLocalAvailability.FAILED -> "录音同步失败，请先重试同步"
            }
        }
    }

    return if (isCurrentChatAudio) {
        "当前讨论中 · $bodyText"
    } else {
        bodyText
    }
}

internal fun resolveSimAudioSyncVisualState(
    connectionState: ConnectionState,
    isSyncing: Boolean,
    syncFeedback: SimAudioSyncFeedback?
): SimAudioSyncVisualState {
    return when {
        isSyncing -> SimAudioSyncVisualState.SYNCING
        syncFeedback == SimAudioSyncFeedback.SYNCED -> SimAudioSyncVisualState.SYNCED
        syncFeedback == SimAudioSyncFeedback.ERROR -> SimAudioSyncVisualState.ERROR
        connectionState == ConnectionState.CONNECTED -> SimAudioSyncVisualState.READY
        connectionState == ConnectionState.RECONNECTING -> SimAudioSyncVisualState.RECONNECTING
        else -> SimAudioSyncVisualState.BLOCKED
    }
}

internal fun resolveSimAudioSyncLabel(
    visualState: SimAudioSyncVisualState,
    connectionState: ConnectionState
): String {
    return when (visualState) {
        SimAudioSyncVisualState.READY -> "Badge 已连接"
        SimAudioSyncVisualState.SYNCING -> "正在同步..."
        SimAudioSyncVisualState.SYNCED -> "已同步"
        SimAudioSyncVisualState.RECONNECTING -> "Badge 重连中..."
        SimAudioSyncVisualState.ERROR -> "同步失败"
        SimAudioSyncVisualState.BLOCKED -> {
            if (connectionState == ConnectionState.NEEDS_SETUP) "需要配网" else "Badge 未连接"
        }
    }
}

internal fun canTriggerSimAudioSync(
    visualState: SimAudioSyncVisualState
): Boolean {
    return visualState != SimAudioSyncVisualState.BLOCKED &&
        visualState != SimAudioSyncVisualState.RECONNECTING &&
        visualState != SimAudioSyncVisualState.SYNCING
}

internal fun shouldShowSimAudioBrowseHelperDeck(
    entries: List<SimAudioEntry>,
    mode: RuntimeAudioDrawerMode
): Boolean {
    return mode == RuntimeAudioDrawerMode.BROWSE &&
        entries.size == 1 &&
        entries.singleOrNull()?.isBuiltInSeed == true
}

internal fun buildSimAudioTranscriptPreview(artifacts: TingwuJobArtifacts?): String? {
    val markdown = artifacts?.transcriptMarkdown?.takeIf { it.isNotBlank() } ?: return null
    return markdown
        .replace(Regex("""[#>*`_\-\[\]\(\)]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

internal fun buildTransparentStateLabel(progress: Float): String {
    return when {
        progress < 0.35f -> "正在整理转写..."
        progress < 0.7f -> "正在提取摘要与重点..."
        else -> "正在生成章节与说话人..."
    }
}
