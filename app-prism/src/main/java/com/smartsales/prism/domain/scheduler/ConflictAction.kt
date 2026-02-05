package com.smartsales.prism.domain.scheduler

/**
 * 冲突解决动作 — 由 ConflictCard UI 传递给 ViewModel
 */
data class ConflictAction(
    val action: ActionType,
    val taskToRemove: String?,       // KEEP_A/KEEP_B: ID of task to delete
    val taskToReschedule: String?,   // RESCHEDULE: ID of task to move
    val rescheduleText: String?,     // RESCHEDULE: Natural language time (e.g. "tomorrow 3pm")
    val reply: String
)

enum class ActionType {
    KEEP_A,      // 保留任务A，删除任务B
    KEEP_B,      // 保留任务B，删除任务A
    RESCHEDULE,  // 改期 (Wave 6+ deferred)
    NONE         // 不做任何操作
}
