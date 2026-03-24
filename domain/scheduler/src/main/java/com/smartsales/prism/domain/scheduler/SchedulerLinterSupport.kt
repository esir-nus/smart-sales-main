package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.core.TaskMutation
import java.time.OffsetDateTime
import kotlinx.serialization.json.Json

internal fun createSchedulerLinterJsonInterpreter(): Json {
    return Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}

internal fun schedulerLinterParseStrictOffsetDateTime(raw: String): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(raw)
    } catch (_: Exception) {
        null
    }
}

internal fun schedulerLinterNormalizeUrgency(raw: String): UrgencyEnum {
    return try {
        UrgencyEnum.valueOf(raw.uppercase())
    } catch (_: IllegalArgumentException) {
        when {
            raw.uppercase().startsWith("L1") -> UrgencyEnum.L1_CRITICAL
            raw.uppercase().startsWith("L2") -> UrgencyEnum.L2_IMPORTANT
            raw.uppercase() == "FIRE_OFF" -> UrgencyEnum.FIRE_OFF
            else -> UrgencyEnum.L3_NORMAL
        }
    }
}

internal fun schedulerLinterNormalizeLegacyUrgency(raw: String): UrgencyLevel {
    return try {
        UrgencyLevel.valueOf(raw.uppercase())
    } catch (_: IllegalArgumentException) {
        when {
            raw.uppercase().startsWith("L1") -> UrgencyLevel.L1_CRITICAL
            raw.uppercase().startsWith("L2") -> UrgencyLevel.L2_IMPORTANT
            raw.uppercase() == "FIRE_OFF" -> UrgencyLevel.FIRE_OFF
            else -> UrgencyLevel.L3_NORMAL
        }
    }
}

internal fun schedulerLinterCleanJson(input: String): String {
    return input
        .replace("```json", "")
        .replace("```", "")
        .trim()
}

internal fun schedulerLinterParseDuration(durationStr: String): Int? {
    val lower = durationStr.lowercase().trim()
    return try {
        if (lower.endsWith("min") || lower.endsWith("m")) {
            lower.filter { it.isDigit() }.toInt()
        } else if (lower.endsWith("h") || lower.endsWith("hour")) {
            val num = lower.filter { it.isDigit() || it == '.' }.toFloat()
            (num * 60).toInt()
        } else if (lower.all { it.isDigit() }) {
            lower.toInt()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

internal fun schedulerLinterNormalizeTime(dateTimeStr: String): String {
    return dateTimeStr
        .replace(Regex("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2})"), "$1 $2")
        .trim()
}

internal fun schedulerLinterParseTaskDefinition(taskMutation: TaskMutation): TaskDefinition? {
    val title = taskMutation.title
    if (title.isBlank()) return null
    if (taskMutation.startTime.isBlank()) return null

    val normalizedStartTime = schedulerLinterNormalizeTime(taskMutation.startTime)
    val explicitDuration = taskMutation.duration
    val durationMinutes = when {
        !explicitDuration.isNullOrBlank() -> schedulerLinterParseDuration(explicitDuration) ?: 0
        else -> 0
    }

    return TaskDefinition(
        title = title,
        startTimeIso = normalizedStartTime,
        durationMinutes = durationMinutes,
        urgency = schedulerLinterNormalizeUrgency(taskMutation.urgency)
    )
}

internal fun schedulerLinterContainsPageRelativeDayCue(transcript: String): Boolean {
    val normalized = transcript.lowercase()
    return normalized.contains("下一天") ||
        normalized.contains("后一天") ||
        normalized.contains("nextday") ||
        normalized.contains("next day")
}
