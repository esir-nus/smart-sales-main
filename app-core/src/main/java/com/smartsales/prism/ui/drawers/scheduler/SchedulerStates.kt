package com.smartsales.prism.ui.drawers.scheduler

import java.time.Instant

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
        val renderKey: String = id,
        val title: String,
        val isDone: Boolean = false,
        val isInteractive: Boolean = true,
        val sortInstant: Instant? = null,
        val hasAlarm: Boolean = false,
        val isSmartAlarm: Boolean = false, // 旧提醒提示字段，产品 UI 不应只依赖它
        val urgencyLevel: com.smartsales.prism.domain.scheduler.UrgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L3_NORMAL,
        // Expanded Fields
        val dateRange: String = "08:00 - 09:00",
        val location: String? = null,
        val notes: String? = null,
        val keyPerson: String? = null,
        val highlights: String? = null,
        val alarmCascade: List<String>? = null, // e.g. ["-30m", "0m"]
        val processingStatus: String? = null, // For Fake I/O Overlay
        val isExiting: Boolean = false, // For Reschedule Animation
        val exitDirection: ExitDirection = ExitDirection.RIGHT, // Default: slide right (to future)
        val conflictVisual: ConflictVisual = ConflictVisual.NONE, // 冲突视觉指示器
        // Wave 9: Smart Tips
        val keyPersonEntityId: String? = null,  // Entity ID for tip generation
        val tips: List<String> = emptyList(),   // LLM-generated context tips
        val tipsLoading: Boolean = false,       // Shimmer animation state
        // Wave 14: Dual-Path Resolution
        val clarificationState: com.smartsales.prism.domain.scheduler.ClarificationState? = null,
        
        // Wave 17: Path A FastTrack (Small Attention Flow)
        val isVague: Boolean = false,
        val hasConflict: Boolean = false,
        val conflictSummary: String? = null
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

/**
 * 冲突视觉状态 — 用于在卡片上显示冲突指示器
 */
enum class ConflictVisual {
    NONE,       // 正常状态，无冲突
    IN_GROUP,   // 琥珀色边框 (冲突组内的已有卡片)
    CAUSING     // 琥珀色边框 + 呼吸发光 (引发冲突的新卡片)
}
