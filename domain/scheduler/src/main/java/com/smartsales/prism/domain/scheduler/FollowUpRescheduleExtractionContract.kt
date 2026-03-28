package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

/**
 * 跟进改期 V2 影子提取请求。
 * 说明：只服务于已选中任务的 follow-up 改期时间语义，不负责目标解析。
 */
data class FollowUpRescheduleExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val selectedTaskStartIso: String,
    val selectedTaskDurationMinutes: Int,
    val selectedTaskTitle: String,
    val selectedTaskLocation: String? = null,
    val selectedTaskPerson: String? = null
)

/**
 * 跟进改期 V2 影子提取响应。
 * 必须由轻量模型严格输出，并由 SchedulerLinter 直接反序列化。
 */
@Serializable
data class FollowUpRescheduleExtractionPayload(
    /** MUST BE one of: RESCHEDULE_EXACT, NOT_SUPPORTED */
    val decision: String = "NOT_SUPPORTED",
    /** RESCHEDULE_EXACT 时必须提供；NOT_SUPPORTED 时必须为空。 */
    val timeKind: String? = null,
    val deltaFromTargetMinutes: Int? = null,
    val relativeDayOffset: Int? = null,
    val clockTime: String? = null,
    val absoluteStartIso: String? = null,
    val reason: String? = null
)

enum class FollowUpRescheduleTimeKind {
    DELTA_FROM_TARGET,
    RELATIVE_DAY_CLOCK,
    ABSOLUTE
}

sealed interface FollowUpRescheduleOperand {
    data class DeltaFromTarget(val minutes: Int) : FollowUpRescheduleOperand
    data class RelativeDayClock(
        val dayOffset: Int,
        val clockTime: String
    ) : FollowUpRescheduleOperand

    data class AbsoluteStart(val startTimeIso: String) : FollowUpRescheduleOperand
}

sealed interface FollowUpRescheduleExtractionResult {
    data class Supported(
        val timeKind: FollowUpRescheduleTimeKind,
        val operand: FollowUpRescheduleOperand
    ) : FollowUpRescheduleExtractionResult

    data class Unsupported(val reason: String) : FollowUpRescheduleExtractionResult
    data class Invalid(val reason: String) : FollowUpRescheduleExtractionResult
    data class Failure(val reason: String) : FollowUpRescheduleExtractionResult
}
