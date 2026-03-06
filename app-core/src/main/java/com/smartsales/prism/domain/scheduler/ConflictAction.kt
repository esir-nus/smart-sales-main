package com.smartsales.prism.domain.scheduler

/**
 * 冲突解决结果 — 包含一个或多个动作（支持复合指令如"取消A，改期B"）
 */
data class ConflictResolution(
    val actions: List<ConflictAction>,
    val reply: String
)

/**
 * 单个冲突解决动作
 */
data class ConflictAction(
    val action: ActionType,
    val taskToRemove: String?,       // KEEP_A/KEEP_B: ID of task to delete
    val taskToReschedule: String?,   // RESCHEDULE: ID of task to move
    val rescheduleText: String?      // RESCHEDULE: Natural language time (e.g. "tomorrow 3pm")
)

enum class ActionType {
    KEEP_A,      // 保留任务A，删除任务B
    KEEP_B,      // 保留任务B，删除任务A
    RESCHEDULE,  // 改期
    COEXIST,     // 两个都保留，清除重叠提示
    NONE         // 不做任何操作
}
