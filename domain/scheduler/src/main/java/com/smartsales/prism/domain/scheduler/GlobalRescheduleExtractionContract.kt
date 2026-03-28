package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.TargetResolutionRequest
import kotlinx.serialization.Serializable

/**
 * 全局改期提取请求。
 * 说明：只负责从当前输入提取目标线索和新时间指令，不直接决定最终命中的任务。
 */
data class GlobalRescheduleExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val recentTaskHints: List<RecentTaskHint> = emptyList(),
    val activeTaskShortlist: List<ActiveTaskContext> = emptyList()
)

data class RecentTaskHint(
    val taskId: String,
    val title: String,
    val keyPerson: String? = null,
    val location: String? = null
)

@Serializable
data class GlobalRescheduleExtractionPayload(
    /** MUST BE one of: RESCHEDULE_TARGETED, NOT_SUPPORTED */
    val decision: String = "NOT_SUPPORTED",
    val suggestedTaskId: String? = null,
    val targetQuery: String? = null,
    val targetPerson: String? = null,
    val targetLocation: String? = null,
    val timeInstruction: String? = null,
    val reason: String? = null
)

sealed interface GlobalRescheduleExtractionResult {
    data class Supported(
        val target: TargetResolutionRequest,
        val timeInstruction: String,
        val suggestedTaskId: String? = null
    ) : GlobalRescheduleExtractionResult

    data class Unsupported(val reason: String) : GlobalRescheduleExtractionResult
    data class Invalid(val reason: String) : GlobalRescheduleExtractionResult
    data class Failure(val reason: String) : GlobalRescheduleExtractionResult
}
