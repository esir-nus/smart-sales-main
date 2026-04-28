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
                val speakerKey = segment.speakerId ?: "speaker_${segment.speakerIndex}"
                val label = artifacts.speakerLabels[speakerKey]
                    ?.takeIf { it.isNotBlank() }
                    ?: segment.speakerId?.takeIf { it.isNotBlank() }
                    ?: "说话人 ${segment.speakerIndex + 1}"
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
