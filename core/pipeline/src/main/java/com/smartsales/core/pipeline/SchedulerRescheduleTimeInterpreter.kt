package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.ExactTimeCueResolver
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleOperand
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * 共享改期时间解释器。
 *
 * 说明：统一顶层语音、follow-up 和 SIM 调度器对“推迟一小时 / 明天八点”
 * 这类新时间语义的解释规则，避免各 surface 各自漂移。
 */
object SchedulerRescheduleTimeInterpreter {

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

    suspend fun resolveNaturalInstruction(
        originalTask: ScheduledTask,
        transcript: String,
        displayedDateIso: String,
        timeProvider: TimeProvider,
        uniAExtractionService: RealUniAExtractionService
    ): Result {
        val normalizedTranscript = normalizeExactRescheduleInstruction(transcript)
        RelativeTimeResolver.resolveSignedDeltaMinutes(normalizedTranscript)?.let { delta ->
            if (originalTask.isVague) return Result.Unsupported
            return Result.Success(
                startTime = originalTask.startTime.plus(delta.offsetMinutes, ChronoUnit.MINUTES),
                durationMinutes = originalTask.durationMinutes
            )
        }
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

    fun resolveFollowUpOperand(
        originalTask: ScheduledTask,
        operand: FollowUpRescheduleOperand,
        timeProvider: TimeProvider
    ): Instant {
        return when (operand) {
            is FollowUpRescheduleOperand.DeltaFromTarget -> {
                require(!originalTask.isVague) { "delta reschedule does not support vague tasks" }
                originalTask.startTime.plus(operand.minutes.toLong(), ChronoUnit.MINUTES)
            }

            is FollowUpRescheduleOperand.RelativeDayClock -> {
                val targetDate = timeProvider.today.plusDays(operand.dayOffset.toLong())
                val targetTime = LocalTime.parse(operand.clockTime)
                targetDate.atTime(targetTime).atZone(timeProvider.zoneId).toInstant()
            }

            is FollowUpRescheduleOperand.AbsoluteStart -> parseExactInstant(operand.startTimeIso)
                ?: throw IllegalArgumentException("invalid follow-up absolute start time")
        }
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
