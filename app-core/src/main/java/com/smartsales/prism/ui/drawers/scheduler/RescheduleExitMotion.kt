package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.domain.scheduler.ScheduledTask

/**
 * 调度改期源卡片退出动效快照。
 * 仅用于 UI 层瞬态表现，不写回领域模型。
 */
data class RescheduleExitMotion(
    val renderKey: String,
    val sourceTaskId: String,
    val sourceDayOffset: Int,
    val snapshot: ScheduledTask,
    val exitDirection: ExitDirection
)
