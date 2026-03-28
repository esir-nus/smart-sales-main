package com.smartsales.prism.ui.sim

import com.smartsales.core.pipeline.RealFollowUpRescheduleExtractionService
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleOperand
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * 跟进改期 V2 影子解释器。
 * 说明：只把 V2 提取结果转换成闭式时间操作数，不做任何写入。
 */
internal object SimFollowUpRescheduleShadowInterpreter {

    enum class TranscriptClass {
        DELTA,
        RELATIVE_DAY_CLOCK,
        PAGE_RELATIVE_DAY,
        OTHER
    }

    sealed interface Result {
        data class Success(
            val startTime: Instant,
            val transcriptClass: TranscriptClass
        ) : Result

        data class Unsupported(
            val reason: String,
            val transcriptClass: TranscriptClass
        ) : Result

        data class Invalid(
            val reason: String,
            val transcriptClass: TranscriptClass
        ) : Result

        data class Failure(
            val reason: String,
            val transcriptClass: TranscriptClass
        ) : Result
    }

    suspend fun resolve(
        originalTask: ScheduledTask,
        transcript: String,
        timeProvider: TimeProvider,
        extractionService: RealFollowUpRescheduleExtractionService
    ): Result {
        val transcriptClass = classifyTranscript(transcript)
        return when (
            val extracted = extractionService.extract(
                FollowUpRescheduleExtractionRequest(
                    transcript = transcript,
                    nowIso = timeProvider.now.toString(),
                    timezone = timeProvider.zoneId.id,
                    selectedTaskStartIso = originalTask.startTime.toString(),
                    selectedTaskDurationMinutes = originalTask.durationMinutes,
                    selectedTaskTitle = originalTask.title,
                    selectedTaskLocation = originalTask.location,
                    selectedTaskPerson = originalTask.keyPerson
                )
            )
        ) {
            is FollowUpRescheduleExtractionResult.Supported -> {
                Result.Success(
                    startTime = resolveSupportedOperand(
                        operand = extracted.operand,
                        originalTask = originalTask,
                        timeProvider = timeProvider
                    ),
                    transcriptClass = transcriptClass
                )
            }

            is FollowUpRescheduleExtractionResult.Unsupported -> {
                Result.Unsupported(extracted.reason, transcriptClass)
            }

            is FollowUpRescheduleExtractionResult.Invalid -> {
                Result.Invalid(extracted.reason, transcriptClass)
            }

            is FollowUpRescheduleExtractionResult.Failure -> {
                Result.Failure(extracted.reason, transcriptClass)
            }
        }
    }

    private fun resolveSupportedOperand(
        operand: FollowUpRescheduleOperand,
        originalTask: ScheduledTask,
        timeProvider: TimeProvider
    ): Instant {
        return when (operand) {
            is FollowUpRescheduleOperand.DeltaFromTarget -> {
                originalTask.startTime.plus(operand.minutes.toLong(), ChronoUnit.MINUTES)
            }

            is FollowUpRescheduleOperand.RelativeDayClock -> {
                val targetDate = timeProvider.today.plusDays(operand.dayOffset.toLong())
                val targetTime = LocalTime.parse(operand.clockTime)
                targetDate.atTime(targetTime).atZone(timeProvider.zoneId).toInstant()
            }

            is FollowUpRescheduleOperand.AbsoluteStart -> parseExactInstant(operand.startTimeIso)
        }
    }

    private fun parseExactInstant(raw: String): Instant {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrThrow()
    }

    private fun classifyTranscript(transcript: String): TranscriptClass {
        val normalized = transcript.lowercase()
        return when {
            normalized.contains("下一天") ||
                normalized.contains("后一天") ||
                normalized.contains("next day") ||
                normalized.contains("nextday") -> TranscriptClass.PAGE_RELATIVE_DAY

            com.smartsales.prism.domain.scheduler.RelativeTimeResolver
                .resolveSignedDeltaMinutes(transcript) != null -> TranscriptClass.DELTA

            looksLikeRelativeDayClock(normalized) -> TranscriptClass.RELATIVE_DAY_CLOCK

            else -> TranscriptClass.OTHER
        }
    }

    private fun looksLikeRelativeDayClock(normalized: String): Boolean {
        val hasDayCue = normalized.contains("今天") ||
            normalized.contains("今晚") ||
            normalized.contains("today") ||
            normalized.contains("明天") ||
            normalized.contains("tomorrow") ||
            normalized.contains("后天") ||
            normalized.contains("day after tomorrow")
        if (!hasDayCue) return false

        val hasClockCue = Regex("""\b\d{1,2}:\d{2}\b""").containsMatchIn(normalized) ||
            Regex("""(?:凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?[零一二两三四五六七八九十百\d]{1,3}点半?""")
                .containsMatchIn(normalized) ||
            Regex("""\b\d{1,2}([:.]\d{2})?\s*(am|pm|a\.m\.|p\.m\.)\b""")
                .containsMatchIn(normalized)
        return hasClockCue
    }
}
