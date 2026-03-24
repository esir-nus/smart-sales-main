package com.smartsales.prism.ui.sim

import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.prism.domain.scheduler.ExactTimeCueResolver
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * SIM 改期时间解释器。
 *
 * 优先支持基于原任务时间的显式相对改期（如“推迟1小时”“提前半小时”），
 * 其余情况回退到既有的明确时间改期解析。
 */
internal object SimRescheduleTimeInterpreter {

    private val EXACT_RESCHEDULE_PREFIXES = listOf(
        "改期到",
        "改到",
        "改成",
        "挪到",
        "推迟到",
        "提前到",
        "reschedule to",
        "move to"
    )

    sealed interface Result {
        data class Success(
            val startTime: Instant,
            val durationMinutes: Int? = null
        ) : Result

        data object Unsupported : Result
        data object InvalidExactTime : Result
    }

    suspend fun resolve(
        originalTask: ScheduledTask,
        transcript: String,
        displayedDateIso: String,
        timeProvider: TimeProvider,
        uniAExtractionService: RealUniAExtractionService
    ): Result {
        RelativeTimeResolver.resolveSignedDeltaMinutes(transcript)?.let { delta ->
            return Result.Success(
                startTime = originalTask.startTime.plus(delta.offsetMinutes, ChronoUnit.MINUTES),
                durationMinutes = null
            )
        }

        val normalizedTranscript = normalizeExactRescheduleInstruction(transcript)
        ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = normalizedTranscript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = displayedDateIso
        )?.let { startTimeIso ->
            val newStart = parseExactInstant(startTimeIso) ?: return Result.InvalidExactTime
            return Result.Success(
                startTime = newStart,
                durationMinutes = null
            )
        }

        val exactResult = uniAExtractionService.extract(
            UniAExtractionRequest(
                transcript = normalizedTranscript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = originalTask.id,
                displayedDateIso = displayedDateIso
            )
        )
        val taskDefinition = (exactResult as? FastTrackResult.CreateTasks)
            ?.params
            ?.tasks
            ?.singleOrNull()
            ?: return Result.Unsupported

        val newStart = parseExactInstant(taskDefinition.startTimeIso)
            ?: return Result.InvalidExactTime

        return Result.Success(
            startTime = newStart,
            durationMinutes = taskDefinition.durationMinutes.takeIf { it > 0 }
        )
    }

    private fun parseExactInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }

    private fun normalizeExactRescheduleInstruction(transcript: String): String {
        val trimmed = transcript.trim()
        val prefix = EXACT_RESCHEDULE_PREFIXES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            ?: return trimmed
        return trimmed.removePrefix(prefix).trim()
    }
}
