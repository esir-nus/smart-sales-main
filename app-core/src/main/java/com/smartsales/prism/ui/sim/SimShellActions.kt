package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

internal data class SimBadgeSchedulerFollowUpSeed(
    val threadId: String,
    val transcript: String,
    val tasks: List<SchedulerFollowUpTaskSummary>,
    val batchId: String? = null
)

internal enum class SimDebugFollowUpScenario {
    SINGLE,
    MULTI,
    TIME_ANCHOR_RETITLE
}

internal fun buildSimDebugFollowUpEvent(
    scenario: SimDebugFollowUpScenario,
    nowMillis: Long = System.currentTimeMillis()
): PipelineEvent.Complete {
    return when (scenario) {
        SimDebugFollowUpScenario.SINGLE -> PipelineEvent.Complete(
            result = SchedulerResult.TaskCreated(
                taskId = "debug_follow_up_single",
                title = "客户回访",
                dayOffset = 0,
                scheduledAtMillis = nowMillis + 60_000L,
                durationMinutes = 30
            ),
            filename = "debug_follow_up_single.wav",
            transcript = "提醒我一会儿回访客户"
        )

        SimDebugFollowUpScenario.MULTI -> PipelineEvent.Complete(
            result = SchedulerResult.MultiTaskCreated(
                tasks = listOf(
                    SchedulerResult.TaskCreated(
                        taskId = "debug_follow_up_multi_a",
                        title = "客户A回访",
                        dayOffset = 0,
                        scheduledAtMillis = nowMillis + 120_000L,
                        durationMinutes = 30
                    ),
                    SchedulerResult.TaskCreated(
                        taskId = "debug_follow_up_multi_b",
                        title = "客户B回访",
                        dayOffset = 0,
                        scheduledAtMillis = nowMillis + 240_000L,
                        durationMinutes = 30
                    )
                )
            ),
            filename = "debug_follow_up_multi.wav",
            transcript = "安排两个客户回访"
        )

        SimDebugFollowUpScenario.TIME_ANCHOR_RETITLE -> {
            val nineToday = ZonedDateTime.now(ZoneId.systemDefault())
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            PipelineEvent.Complete(
                result = SchedulerResult.TaskCreated(
                    taskId = "debug_time_anchor_retitle_0900",
                    title = "起床",
                    dayOffset = 0,
                    scheduledAtMillis = nineToday.toInstant().toEpochMilli(),
                    durationMinutes = 30
                ),
                filename = "debug_time_anchor_retitle.wav",
                transcript = "提醒我9点起床"
            )
        }
    }
}

internal fun handleSchedulerShelfAskAiHandoff(
    promptText: String,
    startSession: (String) -> Unit,
    closeDrawer: () -> Unit,
    emitTelemetry: (String) -> Unit = { prompt -> emitSchedulerShelfHandoffTelemetry(prompt) }
) {
    if (promptText.isBlank()) return
    emitTelemetry(promptText)
    startSession(promptText)
    closeDrawer()
}

internal fun handleBadgeSchedulerFollowUpStart(
    seed: SimBadgeSchedulerFollowUpSeed,
    startSession: (SimBadgeSchedulerFollowUpSeed) -> String?,
    startOwner: (String, String) -> Unit
): String? {
    if (seed.transcript.isBlank() || seed.tasks.isEmpty()) return null
    val sessionId = startSession(seed) ?: return null
    startOwner(sessionId, seed.threadId)
    return sessionId
}

internal fun extractBadgeSchedulerContinuitySeed(event: PipelineEvent): SimBadgeSchedulerFollowUpSeed? {
    if (event !is PipelineEvent.Complete) return null

    val transcript = event.transcript.takeIf { it.isNotBlank() } ?: return null
    return when (val result = event.result) {
        is SchedulerResult.TaskCreated -> SimBadgeSchedulerFollowUpSeed(
            threadId = UUID.randomUUID().toString(),
            transcript = transcript,
            tasks = listOf(result.toSummary())
        )

        is SchedulerResult.MultiTaskCreated -> result.tasks.takeIf { it.isNotEmpty() }?.let { tasks ->
            SimBadgeSchedulerFollowUpSeed(
                threadId = UUID.randomUUID().toString(),
                transcript = transcript,
                tasks = tasks.map { it.toSummary() },
                batchId = UUID.randomUUID().toString()
            )
        }

        else -> null
    }
}

internal fun handleBadgeSchedulerContinuityIngress(
    event: PipelineEvent,
    startSession: (SimBadgeSchedulerFollowUpSeed) -> String?,
    startOwner: (String, String) -> Unit,
    emitTelemetry: (String) -> Unit = { prompt ->
        emitBadgeSchedulerContinuityIngressTelemetry(prompt)
    }
): String? {
    val seed = extractBadgeSchedulerContinuitySeed(event) ?: return null
    emitTelemetry(seed.transcript)
    return handleBadgeSchedulerFollowUpStart(
        seed = seed,
        startSession = startSession,
        startOwner = startOwner
    )
}

internal fun handleSimNewSessionAction(
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    startNewSession: () -> Unit
) {
    if (activeFollowUp != null) {
        clearFollowUp(SimBadgeFollowUpClearReason.NEW_SESSION)
    }
    startNewSession()
}

internal fun handleSimSessionSwitchAction(
    targetSessionId: String,
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    switchSession: (String) -> Unit
) {
    if (activeFollowUp != null && activeFollowUp.boundSessionId != targetSessionId) {
        clearFollowUp(SimBadgeFollowUpClearReason.SESSION_SWITCHED)
    }
    switchSession(targetSessionId)
}

internal fun handleSimSessionDeleteAction(
    targetSessionId: String,
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    deleteSession: (String) -> Unit
) {
    if (activeFollowUp != null && activeFollowUp.boundSessionId == targetSessionId) {
        clearFollowUp(SimBadgeFollowUpClearReason.SESSION_DELETED)
    }
    deleteSession(targetSessionId)
}

private fun SchedulerResult.TaskCreated.toSummary(): SchedulerFollowUpTaskSummary {
    return SchedulerFollowUpTaskSummary(
        taskId = taskId,
        title = title,
        dayOffset = dayOffset,
        scheduledAtMillis = scheduledAtMillis,
        durationMinutes = durationMinutes
    )
}
