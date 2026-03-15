package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.ScheduleItem

/**
 * Clean SSD envelope for Timeline queries.
 */
sealed interface SchedulerTimelineItem {
    val id: String
    val timeDisplay: String

    data class Inspiration(
        override val id: String,
        override val timeDisplay: String,
        val title: String
    ) : SchedulerTimelineItem

    data class Conflict(
        override val id: String,
        override val timeDisplay: String,
        val conflictText: String,
        val taskA: ScheduleItem,
        val taskB: ScheduleItem
    ) : SchedulerTimelineItem
}
