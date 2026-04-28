package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts

internal fun buildSpeakerAwareTranscript(artifacts: TingwuJobArtifacts?): String? {
    if (artifacts == null) return null
    val diarized = artifacts.diarizedSegments?.takeIf { it.isNotEmpty() }
    if (!diarized.isNullOrEmpty()) {
        return diarized
            .sortedWith(compareBy({ it.startMs }, { it.speakerIndex }))
            .mapNotNull { segment ->
                val text = segment.text.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val label = resolveTingwuSpeakerDisplayLabel(
                    speakerId = segment.speakerId,
                    speakerIndex = segment.speakerIndex,
                    speakerLabels = artifacts.speakerLabels
                )
                "$label：$text"
            }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
    }

    return artifacts.transcriptMarkdown?.trim()?.takeIf { it.isNotBlank() }
}

internal fun buildSpeakerAwareTranscriptPreview(artifacts: TingwuJobArtifacts?): String? {
    val transcript = buildSpeakerAwareTranscript(artifacts) ?: return null
    return transcript
        .replace(Regex("""[#>*`_\[\]\(\)]"""), " ")
        .replace(Regex("""(^|\s)-\s+""")) { match -> match.groupValues[1] }
        .replace(Regex("""\s+"""), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

internal fun resolveTingwuSpeakerDisplayLabel(
    speakerId: String?,
    speakerIndex: Int,
    speakerLabels: Map<String, String>
): String {
    val explicitSpeakerId = speakerId?.trim()?.takeIf { it.isNotBlank() }
    val speakerKey = explicitSpeakerId ?: "speaker_$speakerIndex"
    val rawLabel = speakerLabels[speakerKey]?.trim()
    val labelSpeakerNumber = rawLabel?.let(::resolveSpeakerPlaceholderNumber)
    if (!rawLabel.isNullOrBlank() && labelSpeakerNumber == null) {
        return rawLabel
    }

    val speakerNumber = labelSpeakerNumber
        ?: explicitSpeakerId?.let(::resolveSpeakerPlaceholderNumber)
    return if (speakerNumber != null && speakerNumber > 0) {
        "发言人$speakerNumber"
    } else {
        resolveFallbackSpeakerLabel(speakerId, speakerIndex)
    }
}

private fun resolveFallbackSpeakerLabel(speakerId: String?, speakerIndex: Int): String {
    val normalized = speakerId?.trim().orEmpty()
    val numericId = resolveSpeakerPlaceholderNumber(normalized)
    return if (numericId != null && numericId > 0) {
        "发言人$numericId"
    } else {
        normalized.takeIf { it.isNotBlank() } ?: "发言人${speakerIndex + 1}"
    }
}

private fun resolveSpeakerPlaceholderNumber(value: String): Int? {
    val normalized = value.trim()
    return normalized.toIntOrNull()
        ?: Regex("""(?:speaker|spk|发言人)[_\-\s]*(\d+)""", RegexOption.IGNORE_CASE)
            .matchEntire(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
}
