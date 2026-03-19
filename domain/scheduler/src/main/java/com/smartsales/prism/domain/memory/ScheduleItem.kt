package com.smartsales.prism.domain.memory

/**
 * 日程项目 — ScheduleBoard 专用模型
 * 
 * 优化用于快速冲突检测，包含持续时间和冲突策略。
 * 
 * @see docs/cerb/memory-center/spec.md §ScheduleItem
 */
data class ScheduleItem(
    val entryId: String,
    val title: String,
    val scheduledAt: Long,              // epoch millis
    val durationMinutes: Int,           // 持续时间 (分钟)
    val durationSource: DurationSource, // 持续时间来源
    val conflictPolicy: ConflictPolicy, // 冲突策略
    val participants: List<String> = emptyList(),  // 实体 ID 列表
    val location: String? = null,       // 位置实体 ID
    val isVague: Boolean = false        // Wave 17 Path A
) {
    /**
     * 计算结束时间 (epoch millis)
     */
    val endAt: Long get() = scheduledAt + (durationMinutes * 60_000L)
}

/**
 * Deterministic overlap law for exact scheduler tasks.
 *
 * A zero-duration exact task is treated as a point-in-time occupancy check rather than a
 * silently non-conflicting empty interval. This preserves conflict visibility without
 * inventing a fake duration.
 */
fun overlapsInScheduleBoard(
    proposedStart: Long,
    proposedDurationMinutes: Int,
    existingStart: Long,
    existingDurationMinutes: Int
): Boolean {
    val proposedHasSpan = proposedDurationMinutes > 0
    val existingHasSpan = existingDurationMinutes > 0
    val proposedEnd = proposedStart + (proposedDurationMinutes * 60_000L)
    val existingEnd = existingStart + (existingDurationMinutes * 60_000L)

    return when {
        proposedHasSpan && existingHasSpan -> {
            existingStart < proposedEnd && proposedStart < existingEnd
        }
        !proposedHasSpan && existingHasSpan -> {
            existingStart <= proposedStart && proposedStart < existingEnd
        }
        proposedHasSpan && !existingHasSpan -> {
            proposedStart <= existingStart && existingStart < proposedEnd
        }
        else -> proposedStart == existingStart
    }
}

/**
 * 持续时间来源
 * 
 * 记录持续时间是如何获取的，用于调试和用户反馈。
 */
enum class DurationSource {
    /** 用户明确指定 ("1小时") */
    USER_SET,
    
    /** LLM 根据任务类型推断 ("开会" → 60分钟) */
    INFERRED,
    
    /** 代理追问后用户提供 */
    FOLLOW_UP,
    
    /** 从用户习惯学习 */
    LEARNED,
    
    /** 系统默认值 (30分钟) */
    DEFAULT
}

/**
 * 冲突策略
 * 
 * 决定日程项如何与其他项目冲突检测。
 */
enum class ConflictPolicy {
    /** 独占 — 与重叠项冲突 (默认) */
    EXCLUSIVE,
    
    /** 共存 — 可与其他任务并行 (用户标记 "共存") */
    COEXISTING,
    
    /** 后台 — 低优先级，自动让步给新任务 */
    BACKGROUND
}
