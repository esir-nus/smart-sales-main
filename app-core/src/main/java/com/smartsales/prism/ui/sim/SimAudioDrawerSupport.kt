package com.smartsales.prism.ui.sim

import androidx.compose.ui.graphics.Color
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.drawers.AudioStatus

internal val SimDrawerSurface = Color(0xF0141416)
internal val SimDrawerCardSurface = Color(0x14FFFFFF)
internal val SimDrawerCardSurfaceStrong = Color(0x1BFFFFFF)
internal val SimDrawerCurrentSurface = Color(0x0DFFFFFF)
internal val SimDrawerCardBorder = Color(0x14FFFFFF)
internal val SimDrawerCardBorderStrong = Color(0x520A84FF)
internal val SimDrawerDivider = Color(0x12FFFFFF)
internal val SimDrawerTextPrimary = Color(0xFFFFFFFF)
internal val SimDrawerTextSecondary = Color(0xB8FFFFFF)
internal val SimDrawerTextMuted = Color(0x80FFFFFF)
internal val SimDrawerTextFaint = Color(0x66FFFFFF)
internal val SimDrawerAccent = Color(0xFF0A84FF)
internal val SimDrawerDeleteBackground = Color(0xFFFF453A)

data class SimChatAudioSelection(
    val audioId: String,
    val title: String,
    val summary: String?,
    val status: AudioStatus
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
            if (isCurrentChatAudio) "可在当前聊天中继续处理"
            else "选择后在当前聊天中继续处理"
        }
    }

    return if (isCurrentChatAudio) {
        "当前讨论中 · $bodyText"
    } else {
        bodyText
    }
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
