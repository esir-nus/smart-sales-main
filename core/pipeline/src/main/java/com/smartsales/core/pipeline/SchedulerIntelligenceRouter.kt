package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.time.TimeProvider

/**
 * 调度器智能路由器。
 *
 * 说明：统一 scheduler intent 的 surface 级路由判断，
 * 让 create / global reschedule / follow-up reschedule 不再分散在 UI 层各自判断。
 */
class SchedulerIntelligenceRouter(
    private val timeProvider: TimeProvider,
    private val createInterpreter: SchedulerPathACreateInterpreter? = null,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService? = null,
    private val followUpRescheduleExtractionService: RealFollowUpRescheduleExtractionService? = null
) {

    enum class SchedulerSurface {
        TOP_LEVEL_VOICE,
        PATH_B_TEXT,
        SCHEDULER_DRAWER,
        FOLLOW_UP_SESSION,
        ONBOARDING_SANDBOX
    }

    enum class SchedulerIntentKind {
        NONE,
        CREATE,
        RESCHEDULE,
        DELETE_UNSUPPORTED
    }

    enum class SchedulerTaskShape {
        SINGLE_EXACT,
        SINGLE_VAGUE,
        BATCH,
        TARGETED_UPDATE,
        UNSUPPORTED
    }

    enum class SchedulerRouteOwner {
        UNI_M,
        UNI_A,
        UNI_B,
        GLOBAL_RESCHEDULE,
        FOLLOW_UP_RESCHEDULE,
        REJECT
    }

    data class RouteMetadata(
        val surface: SchedulerSurface,
        val intentKind: SchedulerIntentKind,
        val taskShape: SchedulerTaskShape,
        val owner: SchedulerRouteOwner,
        val schedulerTerminalOnCommit: Boolean,
        val reason: String? = null
    )

    data class GeneralContext(
        val transcript: String,
        val surface: SchedulerSurface,
        val displayedDateIso: String?,
        val activeTaskShortlist: List<ActiveTaskContext> = emptyList(),
        val uniMTimeoutMs: Long? = null
    )

    data class FollowUpContext(
        val transcript: String,
        val selectedTask: ScheduledTask? = null,
        val activeTaskShortlist: List<ActiveTaskContext> = emptyList()
    )

    sealed interface Decision {
        val metadata: RouteMetadata

        data class Create(
            val result: SchedulerPathACreateInterpreter.Result,
            override val metadata: RouteMetadata
        ) : Decision

        data class GlobalReschedule(
            val extracted: GlobalRescheduleExtractionResult.Supported,
            override val metadata: RouteMetadata
        ) : Decision

        data class FollowUpReschedule(
            val selectedTask: ScheduledTask,
            val extracted: FollowUpRescheduleExtractionResult.Supported,
            override val metadata: RouteMetadata
        ) : Decision

        data class Reject(
            val message: String,
            override val metadata: RouteMetadata
        ) : Decision

        data class NotMatched(
            val reason: String,
            override val metadata: RouteMetadata
        ) : Decision
    }

    suspend fun routeGeneral(context: GeneralContext): Decision {
        val transcript = context.transcript.trim()
        if (looksLikeDeletionTranscript(transcript)) {
            return Decision.Reject(
                message = deletionUnsupportedMessage(context.surface),
                metadata = RouteMetadata(
                    surface = context.surface,
                    intentKind = SchedulerIntentKind.DELETE_UNSUPPORTED,
                    taskShape = SchedulerTaskShape.UNSUPPORTED,
                    owner = SchedulerRouteOwner.REJECT,
                    schedulerTerminalOnCommit = false,
                    reason = "delete unsupported"
                )
            )
        }

        if (mightExpressReschedule(transcript) && globalRescheduleExtractionService != null) {
            when (
                val extracted = globalRescheduleExtractionService.extract(
                    GlobalRescheduleExtractionRequest(
                        transcript = transcript,
                        nowIso = timeProvider.now.toString(),
                        timezone = timeProvider.zoneId.id,
                        activeTaskShortlist = context.activeTaskShortlist
                    )
                )
            ) {
                is GlobalRescheduleExtractionResult.Supported -> {
                    return Decision.GlobalReschedule(
                        extracted = extracted,
                        metadata = RouteMetadata(
                            surface = context.surface,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.TARGETED_UPDATE,
                            owner = SchedulerRouteOwner.GLOBAL_RESCHEDULE,
                            schedulerTerminalOnCommit = true
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Unsupported -> {
                    return Decision.Reject(
                        message = unsupportedRescheduleMessage(context.surface),
                        metadata = RouteMetadata(
                            surface = context.surface,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Invalid -> {
                    return Decision.Reject(
                        message = invalidRescheduleMessage(context.surface),
                        metadata = RouteMetadata(
                            surface = context.surface,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Failure -> {
                    return Decision.Reject(
                        message = failureRescheduleMessage(context.surface),
                        metadata = RouteMetadata(
                            surface = context.surface,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
            }
        }

        val interpreter = createInterpreter
            ?: error("createInterpreter is required for general scheduler routing")
        return when (
            val result = interpreter.interpret(
                transcript = transcript,
                displayedDateIso = context.displayedDateIso,
                uniMTimeoutMs = context.uniMTimeoutMs
            )
        ) {
            is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                Decision.Create(result, result.toMetadata(context.surface))
            }
            is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                Decision.Create(result, result.toMetadata(context.surface))
            }
            is SchedulerPathACreateInterpreter.Result.DirectFailure -> {
                Decision.Reject(
                    message = result.message,
                    metadata = RouteMetadata(
                        surface = context.surface,
                        intentKind = SchedulerIntentKind.CREATE,
                        taskShape = SchedulerTaskShape.UNSUPPORTED,
                        owner = SchedulerRouteOwner.REJECT,
                        schedulerTerminalOnCommit = false,
                        reason = result.message
                    )
                )
            }
            is SchedulerPathACreateInterpreter.Result.NotMatched -> {
                Decision.NotMatched(
                    reason = result.reason,
                    metadata = RouteMetadata(
                        surface = context.surface,
                        intentKind = SchedulerIntentKind.NONE,
                        taskShape = SchedulerTaskShape.UNSUPPORTED,
                        owner = SchedulerRouteOwner.REJECT,
                        schedulerTerminalOnCommit = false,
                        reason = result.reason
                    )
                )
            }
        }
    }

    suspend fun routeFollowUp(context: FollowUpContext): Decision {
        val transcript = context.transcript.trim()
        if (looksLikeDeletionTranscript(transcript)) {
            return Decision.Reject(
                message = deletionUnsupportedMessage(SchedulerSurface.FOLLOW_UP_SESSION),
                metadata = RouteMetadata(
                    surface = SchedulerSurface.FOLLOW_UP_SESSION,
                    intentKind = SchedulerIntentKind.DELETE_UNSUPPORTED,
                    taskShape = SchedulerTaskShape.UNSUPPORTED,
                    owner = SchedulerRouteOwner.REJECT,
                    schedulerTerminalOnCommit = false,
                    reason = "delete unsupported"
                )
            )
        }

        if (mightExpressReschedule(transcript) && globalRescheduleExtractionService != null) {
            when (
                val extracted = globalRescheduleExtractionService.extract(
                    GlobalRescheduleExtractionRequest(
                        transcript = transcript,
                        nowIso = timeProvider.now.toString(),
                        timezone = timeProvider.zoneId.id,
                        activeTaskShortlist = context.activeTaskShortlist
                    )
                )
            ) {
                is GlobalRescheduleExtractionResult.Supported -> {
                    return Decision.GlobalReschedule(
                        extracted = extracted,
                        metadata = RouteMetadata(
                            surface = SchedulerSurface.FOLLOW_UP_SESSION,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.TARGETED_UPDATE,
                            owner = SchedulerRouteOwner.GLOBAL_RESCHEDULE,
                            schedulerTerminalOnCommit = true
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Failure -> {
                    return Decision.Reject(
                        message = "改期目标解析失败，请稍后重试。",
                        metadata = RouteMetadata(
                            surface = SchedulerSurface.FOLLOW_UP_SESSION,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Unsupported -> {
                    return Decision.Reject(
                        message = unsupportedRescheduleMessage(SchedulerSurface.FOLLOW_UP_SESSION),
                        metadata = RouteMetadata(
                            surface = SchedulerSurface.FOLLOW_UP_SESSION,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
                is GlobalRescheduleExtractionResult.Invalid -> {
                    return Decision.Reject(
                        message = invalidRescheduleMessage(SchedulerSurface.FOLLOW_UP_SESSION),
                        metadata = RouteMetadata(
                            surface = SchedulerSurface.FOLLOW_UP_SESSION,
                            intentKind = SchedulerIntentKind.RESCHEDULE,
                            taskShape = SchedulerTaskShape.UNSUPPORTED,
                            owner = SchedulerRouteOwner.REJECT,
                            schedulerTerminalOnCommit = false,
                            reason = extracted.reason
                        )
                    )
                }
            }
        }

        return Decision.Reject(
            message = unsupportedRescheduleMessage(SchedulerSurface.FOLLOW_UP_SESSION),
            metadata = RouteMetadata(
                surface = SchedulerSurface.FOLLOW_UP_SESSION,
                intentKind = SchedulerIntentKind.RESCHEDULE,
                taskShape = SchedulerTaskShape.UNSUPPORTED,
                owner = SchedulerRouteOwner.REJECT,
                schedulerTerminalOnCommit = false,
                reason = "follow-up explicit target required"
            )
        )
    }

    fun mightExpressReschedule(text: String): Boolean {
        val normalized = text.lowercase()
        return RESCHEDULE_KEYWORDS.any { normalized.contains(it) }
    }

    fun looksLikeDeletionTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return DELETE_KEYWORDS.any { normalized.contains(it) }
    }

    private fun SchedulerPathACreateInterpreter.Result.toMetadata(
        surface: SchedulerSurface
    ): RouteMetadata {
        return when (this) {
            is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                val owner = when (telemetry.routeStage) {
                    SchedulerPathACreateInterpreter.RouteStage.UNI_A,
                    SchedulerPathACreateInterpreter.RouteStage.DETERMINISTIC_RELATIVE,
                    SchedulerPathACreateInterpreter.RouteStage.DETERMINISTIC_DAY_CLOCK,
                    SchedulerPathACreateInterpreter.RouteStage.DETERMINISTIC_CHAINED_DAY_CLOCK -> SchedulerRouteOwner.UNI_A
                    SchedulerPathACreateInterpreter.RouteStage.UNI_B -> SchedulerRouteOwner.UNI_B
                    SchedulerPathACreateInterpreter.RouteStage.UNI_M -> SchedulerRouteOwner.UNI_M
                    SchedulerPathACreateInterpreter.RouteStage.NO_MATCH -> SchedulerRouteOwner.REJECT
                }
                RouteMetadata(
                    surface = surface,
                    intentKind = SchedulerIntentKind.CREATE,
                    taskShape = classifySingleShape(intent),
                    owner = owner,
                    schedulerTerminalOnCommit = true
                )
            }
            is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                RouteMetadata(
                    surface = surface,
                    intentKind = SchedulerIntentKind.CREATE,
                    taskShape = SchedulerTaskShape.BATCH,
                    owner = SchedulerRouteOwner.UNI_M,
                    schedulerTerminalOnCommit = true
                )
            }
            is SchedulerPathACreateInterpreter.Result.DirectFailure -> RouteMetadata(
                surface = surface,
                intentKind = SchedulerIntentKind.CREATE,
                taskShape = SchedulerTaskShape.UNSUPPORTED,
                owner = SchedulerRouteOwner.REJECT,
                schedulerTerminalOnCommit = false,
                reason = message
            )
            is SchedulerPathACreateInterpreter.Result.NotMatched -> RouteMetadata(
                surface = surface,
                intentKind = SchedulerIntentKind.NONE,
                taskShape = SchedulerTaskShape.UNSUPPORTED,
                owner = SchedulerRouteOwner.REJECT,
                schedulerTerminalOnCommit = false,
                reason = reason
            )
        }
    }

    private fun classifySingleShape(intent: com.smartsales.prism.domain.scheduler.FastTrackResult): SchedulerTaskShape {
        return when (intent) {
            is com.smartsales.prism.domain.scheduler.FastTrackResult.CreateVagueTask -> SchedulerTaskShape.SINGLE_VAGUE
            is com.smartsales.prism.domain.scheduler.FastTrackResult.CreateTasks -> SchedulerTaskShape.SINGLE_EXACT
            else -> SchedulerTaskShape.UNSUPPORTED
        }
    }

    private fun unsupportedRescheduleMessage(surface: SchedulerSurface): String {
        return when (surface) {
            SchedulerSurface.SCHEDULER_DRAWER -> "SIM 当前仅支持明确目标 + 明确时间改期"
            SchedulerSurface.ONBOARDING_SANDBOX -> "当前体验仅支持明确目标 + 明确时间修改"
            SchedulerSurface.TOP_LEVEL_VOICE -> "当前仅支持明确目标 + 明确时间改期，请说出要改的日程和时间。"
            SchedulerSurface.PATH_B_TEXT -> "当前仅支持明确目标 + 明确时间改期，请说清楚要改的日程和时间。"
            SchedulerSurface.FOLLOW_UP_SESSION -> "当前跟进只支持明确目标 + 明确时间改期，请直接说出要改的日程和时间。"
        }
    }

    private fun invalidRescheduleMessage(surface: SchedulerSurface): String {
        return when (surface) {
            SchedulerSurface.SCHEDULER_DRAWER -> "改期目标或时间无法解析，请换一种明确说法"
            SchedulerSurface.ONBOARDING_SANDBOX -> "修改目标或时间无法解析，请换一种明确说法"
            SchedulerSurface.TOP_LEVEL_VOICE -> "改期目标或时间无法解析，请换一种明确说法。"
            SchedulerSurface.PATH_B_TEXT -> "改期目标或时间无法解析，请换一种明确说法。"
            SchedulerSurface.FOLLOW_UP_SESSION -> "改期目标或时间无法解析，请换一种明确说法。"
        }
    }

    private fun failureRescheduleMessage(surface: SchedulerSurface): String {
        return when (surface) {
            SchedulerSurface.SCHEDULER_DRAWER -> "改期目标解析失败，请稍后重试"
            SchedulerSurface.ONBOARDING_SANDBOX -> "日程修改解析失败，请稍后重试"
            SchedulerSurface.TOP_LEVEL_VOICE -> "改期解析失败，请稍后重试。"
            SchedulerSurface.PATH_B_TEXT -> "改期解析失败，请稍后重试。"
            SchedulerSurface.FOLLOW_UP_SESSION -> "改期目标解析失败，请稍后重试。"
        }
    }

    private fun deletionUnsupportedMessage(surface: SchedulerSurface): String {
        return when (surface) {
            SchedulerSurface.SCHEDULER_DRAWER -> "SIM 当前不支持语音删除，请在面板手动操作"
            SchedulerSurface.ONBOARDING_SANDBOX -> "当前体验仅支持创建或修改日程，请换一种说法"
            SchedulerSurface.TOP_LEVEL_VOICE -> "当前不支持语音删除，请在日程面板手动操作。"
            SchedulerSurface.PATH_B_TEXT -> "当前不支持直接删除日程，请在日程面板手动操作。"
            SchedulerSurface.FOLLOW_UP_SESSION -> "当前跟进暂不支持直接删除，请使用快捷操作。"
        }
    }

    companion object {
        private val RESCHEDULE_KEYWORDS = listOf(
            "改期到",
            "改到",
            "改成",
            "改期",
            "挪到",
            "推迟到",
            "提前到",
            "往后推",
            "往前提",
            "推迟",
            "推后",
            "延后到",
            "延期到",
            "延后",
            "延期",
            "提前",
            "提早",
            "reschedule to",
            "reschedule",
            "move to",
            "move "
        )

        private val DELETE_KEYWORDS = listOf(
            "删除",
            "取消",
            "删掉",
            "delete",
            "cancel",
            "remove"
        )
    }
}
