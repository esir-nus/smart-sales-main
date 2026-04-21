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
    val activeTaskShortlist: List<ActiveTaskContext> = emptyList()
)

@Serializable
data class GlobalRescheduleExtractionPayload(
    /** MUST BE one of: RESCHEDULE_TARGETED, NOT_SUPPORTED */
    val decision: String = "NOT_SUPPORTED",
    val suggestedTaskId: String? = null,
    val preferredTaskIds: List<String> = emptyList(),
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
        val suggestedTaskId: String? = null,
        val preferredTaskIds: List<String> = emptyList()
    ) : GlobalRescheduleExtractionResult {
        fun filterByOwnership(ownedIds: Set<String>): Supported {
            val filteredPreferredTaskIds = preferredTaskIds
                .mapNotNull { candidateId ->
                    candidateId.trim().takeIf { it.isNotBlank() && it in ownedIds }
                }
                .distinct()
            return copy(
                suggestedTaskId = suggestedTaskId
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it in ownedIds }
                    ?: filteredPreferredTaskIds.firstOrNull(),
                preferredTaskIds = filteredPreferredTaskIds
            )
        }
    }

    data class Unsupported(val reason: String) : GlobalRescheduleExtractionResult
    data class Invalid(val reason: String) : GlobalRescheduleExtractionResult
    data class Failure(val reason: String) : GlobalRescheduleExtractionResult
}
