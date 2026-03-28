package com.smartsales.prism.domain.memory

import com.smartsales.prism.domain.scheduler.UrgencyLevel

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
    val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
    val conflictPolicy: ConflictPolicy, // 冲突策略
    val participants: List<String> = emptyList(),  // 实体 ID 列表
    val location: String? = null,       // 位置实体 ID
    val isVague: Boolean = false        // Wave 17 Path A
) {
    /**
     * 计算结束时间 (epoch millis)
     */
    val endAt: Long get() = scheduledAt + (durationMinutes * 60_000L)

    /**
     * 冲突检测专用占用窗口。
     *
     * 与持久化 duration 分离，避免把“为了冲突判断而推断的时长”
     * 直接污染为用户可见的真实任务时长。
     */
    val effectiveConflictDurationMinutes: Int
        get() = effectiveConflictOccupancyMinutes(
            title = title,
            urgencyLevel = urgencyLevel,
            explicitDurationMinutes = durationMinutes
        )
}

private val TRANSPORT_CONFLICT_KEYWORDS = listOf(
    "高铁", "火车", "列车", "动车", "航班", "飞机", "机场", "车站", "站台",
    "登机", "起飞", "落地", "值机", "安检", "接机", "送机", "送站", "接站",
    "乘机", "乘车", "train", "flight", "airport", "station", "boarding"
)

private val APPOINTMENT_CONFLICT_KEYWORDS = listOf(
    "开会", "会议", "电话", "通话", "面试", "汇报", "复盘", "拜访", "见客户",
    "会面", "约见", "接人", "接老板", "接张总", "review", "call", "meeting",
    "interview", "pickup", "visit", "appointment"
)

/**
 * 计算精确任务在冲突检测中的有效占用时长。
 *
 * 规则：
 * 1. FIRE_OFF 永远不参与冲突，直接返回 0
 * 2. 显式 duration > 0 时优先使用显式值
 * 3. 否则尝试使用任务语义关键词推断占用窗口
 * 4. 再退回到 urgency 默认窗口
 */
fun effectiveConflictOccupancyMinutes(
    title: String,
    urgencyLevel: UrgencyLevel,
    explicitDurationMinutes: Int
): Int {
    if (bypassesConflictEvaluation(urgencyLevel)) return 0
    if (explicitDurationMinutes > 0) return explicitDurationMinutes

    inferSemanticConflictOccupancyMinutes(title)?.let { return it }

    return when (urgencyLevel) {
        UrgencyLevel.L1_CRITICAL -> 120
        UrgencyLevel.L2_IMPORTANT -> 60
        UrgencyLevel.L3_NORMAL -> 30
        UrgencyLevel.FIRE_OFF -> 0
    }
}

fun bypassesConflictEvaluation(urgencyLevel: UrgencyLevel): Boolean {
    return urgencyLevel == UrgencyLevel.FIRE_OFF
}

private fun inferSemanticConflictOccupancyMinutes(title: String): Int? {
    val normalized = title.lowercase().trim()
    if (normalized.isBlank()) return null

    return when {
        TRANSPORT_CONFLICT_KEYWORDS.any(normalized::contains) -> 120
        APPOINTMENT_CONFLICT_KEYWORDS.any(normalized::contains) -> 60
        else -> null
    }
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
