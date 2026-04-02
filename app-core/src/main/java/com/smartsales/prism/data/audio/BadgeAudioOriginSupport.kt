package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource

private val BADGE_AUDIO_FILENAME_REGEX =
    Regex("^log_\\d{8}_\\d{6}\\.wav$", RegexOption.IGNORE_CASE)

internal data class BadgeAudioNormalizationResult(
    val entries: List<AudioFile>,
    val normalizedCount: Int
)

internal fun isBadgeLikeAudioFilename(filename: String): Boolean {
    return BADGE_AUDIO_FILENAME_REGEX.matches(filename.trim())
}

internal fun isBadgeOriginAudio(audio: AudioFile?): Boolean {
    return audio != null &&
        (audio.source == AudioSource.SMARTBADGE || isBadgeLikeAudioFilename(audio.filename))
}

internal fun normalizeBadgeOriginEntries(entries: List<AudioFile>): BadgeAudioNormalizationResult {
    var normalizedCount = 0
    val normalizedEntries = entries.map { audio ->
        if (audio.source == AudioSource.PHONE && isBadgeLikeAudioFilename(audio.filename)) {
            normalizedCount += 1
            audio.copy(source = AudioSource.SMARTBADGE)
        } else {
            audio
        }
    }
    return BadgeAudioNormalizationResult(
        entries = normalizedEntries,
        normalizedCount = normalizedCount
    )
}
