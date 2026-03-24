package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

sealed class FastTrackResult {
    data class CreateTasks(val params: CreateTasksParams) : FastTrackResult()
    data class CreateVagueTask(val params: CreateVagueTaskParams) : FastTrackResult()
    data class RescheduleTask(val params: RescheduleTaskParams) : FastTrackResult()
    data class CreateInspiration(val params: CreateInspirationParams) : FastTrackResult()
    data class NoMatch(val reason: String) : FastTrackResult()
}

@Serializable
data class CreateTasksParams(
    val unifiedId: String? = null,
    val tasks: List<TaskDefinition>
)

@Serializable
data class CreateVagueTaskParams(
    val unifiedId: String? = null,
    val title: String,
    val anchorDateIso: String,
    val timeHint: String? = null,
    val urgency: UrgencyEnum = UrgencyEnum.L3_NORMAL
)

@Serializable
data class TaskDefinition(
    val title: String,
    val startTimeIso: String,
    val durationMinutes: Int, 
    val urgency: UrgencyEnum
)

enum class UrgencyEnum {
    L1_CRITICAL, L2_IMPORTANT, L3_NORMAL, FIRE_OFF
}

@Serializable
data class RescheduleTaskParams(
    val unifiedId: String? = null,
    val targetTimeIso: String? = null,
    val resolvedTaskId: String? = null,
    val targetQuery: String? = null,
    val newStartTimeIso: String,
    val newDurationMinutes: Int? = null 
)

@Serializable
data class CreateInspirationParams(
    val unifiedId: String? = null,
    val content: String 
)
