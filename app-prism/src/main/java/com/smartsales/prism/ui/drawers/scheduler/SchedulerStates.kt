package com.smartsales.prism.ui.drawers.scheduler

/**
 * Scheduler Timeline State Models
 * @see prism-ui-ux-contract.md §1.3
 */

enum class ExitDirection {
    RIGHT, // Moving to future (default)
    LEFT   // Moving to past (older days)
}

sealed class TimelineItem {
    abstract val id: String
    abstract val timeDisplay: String
    
    /**
     * Standard Task Card
     * @see prism-ui-ux-contract.md §1.3 "Task Card"
     */
    data class Task(
        override val id: String,
        override val timeDisplay: String, // "08:00"
        val title: String,
        val isDone: Boolean = false,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false, // "智能提醒" badge
        // Expanded Fields
        val dateRange: String = "08:00 - 09:00",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,
        val highlights: String? = null,
        val alarmCascade: List<String>? = null, // e.g. ["-1h", "-15m", "-5m"]
        val processingStatus: String? = null, // For Fake I/O Overlay
        val isExiting: Boolean = false, // For Reschedule Animation
        val exitDirection: ExitDirection = ExitDirection.RIGHT // Default: slide right (to future)
    ) : TimelineItem()
    
    /**
     * Inspiration Card (Multi-Select)
     * @see prism-ui-ux-contract.md §1.3 "Inspiration Card"
     */
    data class Inspiration(
        override val id: String,
        override val timeDisplay: String, // "10:30" (approx)
        val title: String,
        val isSelected: Boolean = false,
        val isSelectionMode: Boolean = false
    ) : TimelineItem()
    
    /**
     * Conflict Card (Expandable)
     * @see prism-ui-ux-contract.md §1.3 "Conflict Card"
     */
    data class Conflict(
        override val id: String,
        override val timeDisplay: String, // "12:00"
        val conflictText: String, // "Event A vs Event B"
        val taskA: com.smartsales.prism.domain.memory.ScheduleItem,
        val taskB: com.smartsales.prism.domain.memory.ScheduleItem,
        val isExpanded: Boolean = false
    ) : TimelineItem()
}
