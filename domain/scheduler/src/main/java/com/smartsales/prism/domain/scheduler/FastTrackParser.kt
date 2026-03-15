package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Path A: Lightweight Optimistic Parser (Wave 14 Dual-Path Architecture)
 * Responsible for creating 0-latency UI placeholders while Path B processes the heavy LLM.
 */
@Singleton
class FastTrackParser @Inject constructor(
    private val timeProvider: TimeProvider
) {
    /**
     * Instantiates an optimistic ScheduledTask using rudimentary heuristics.
     * @param unifiedId The cross-path synchronization token.
     * @param transcript The raw user audio transcript.
     * @return A ScheduledTask ready for immediate Path A UI rendering.
     */
    fun parseToOptimisticTask(unifiedId: String, transcript: String): ScheduledTask {
        // Broad guess for avoiding severe calendar thrashing when the real Date comes back from the LLM
        val daysToAdd = guessDayOffset(transcript)
        val optimisticStartTime = timeProvider.now.plus(daysToAdd.toLong(), ChronoUnit.DAYS)

        return ScheduledTask(
            id = unifiedId,
            timeDisplay = "处理中...", // Visual indicator for optimistic state
            title = transcript, // Temporary title until Path B (LLM) corrects it
            urgencyLevel = UrgencyLevel.L3_NORMAL,
            isDone = false,
            hasAlarm = false,
            isSmartAlarm = false,
            startTime = optimisticStartTime,
            endTime = null,
            durationMinutes = 0,
            conflictPolicy = ConflictPolicy.COEXISTING, // Optimistic tasks shouldn't trigger hard conflicts immediately
            dateRange = "", // To be filled by Path B
            highlights = "🔄 智能分析中...", // Shows in drawer UI
            clarificationState = null // Fresh tasks have no clarification yet
        )
    }

    /**
     * Dumb regex to prevent optimistic tasks from randomly appearing "Today" 
     * when the user explicitly said "Tomorrow".
     */
    private fun guessDayOffset(transcript: String): Int {
        if (transcript.contains("明天") || transcript.contains("明早") || transcript.contains("明晚")) {
            return 1
        }
        if (transcript.contains("后天")) {
            return 2
        }
        if (transcript.contains("下周一")) {
            // Rough approximation, LLM will fix exact day later
            return 3
        }
        // Default to today
        return 0
    }
}
