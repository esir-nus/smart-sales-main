package com.smartsales.prism.domain.rl

import com.smartsales.prism.domain.habit.UserHabit
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 习惯置信度计算 — Wave 4: Time Decay + Deletion Cleanup
 *
 * 4-Rule 权重模型:
 * - Rule 1: INFERRED → 1x
 * - Rule 2: USER_POSITIVE → 3x
 * - Rule 3: USER_NEGATIVE → -2x
 * - Rule 4: Time Decay (half-life ~30 days)
 *
 * 来源: docs/cerb/rl-module/spec.md L212-230
 */

/** 置信度低于此阈值的习惯应被删除 (垃圾回收) */
const val DELETION_THRESHOLD = 0.1f

/**
 * 计算习惯置信度，考虑 4-Rule 权重和时间衰减
 *
 * @param habit 当前习惯记录
 * @param nowMillis 当前时间毫秒数
 * @return [0, 1] 之间的置信度分数
 */
fun calculateConfidence(habit: UserHabit, nowMillis: Long): Float {
    // Rule 4: 时间衰减 (half-life ~30 days)
    val daysSince = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(habit.lastObservedAt),
        Instant.ofEpochMilli(nowMillis)
    )
    val decayFactor = 1.0f / (1 + daysSince / 30f)

    // 加权信号求和
    val rawScore = (habit.inferredCount * 1.0f) +       // Rule 1
                   (habit.explicitPositive * 3.0f) +    // Rule 2: 3x weight
                   (habit.explicitNegative * -2.0f)     // Rule 3: negative

    // 归一化到 [0, 1] 并应用衰减
    val maxPossible = (habit.inferredCount + habit.explicitPositive) * 3.0f
    val normalized = if (maxPossible > 0) rawScore / maxPossible else 0.5f

    return (normalized * decayFactor).coerceIn(0f, 1f)
}
