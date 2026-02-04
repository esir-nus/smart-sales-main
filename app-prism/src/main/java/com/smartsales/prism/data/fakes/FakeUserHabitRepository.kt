package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.habit.UserHabitRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * # Fake UserHabitRepository — 内存中存储
 *
 * 用于单元测试的 Fake 实现。
 *
 * ## 置信度计算
 * - 新习惯: confidence = 0.5 (中性)
 * - 已有习惯: confidence = observationCount / (observationCount + rejectionCount)
 * - 除零边界: 当 total = 0 时,返回 0.5
 *
 * NOTE: No hardcoded test data. Tests should seed their own data.
 */
@Singleton
class FakeUserHabitRepository @Inject constructor() : UserHabitRepository {

    private val habits = mutableMapOf<String, UserHabit>()

    override suspend fun getGlobalHabits(): List<UserHabit> {
        return habits.values.filter { it.entityId == null }
    }

    override suspend fun getByEntity(entityId: String): List<UserHabit> {
        return habits.values.filter { it.entityId == entityId }
    }

    override suspend fun getHabit(key: String, entityId: String?): UserHabit? {
        return habits[makeKey(key, entityId)]
    }

    override suspend fun observe(key: String, value: String, entityId: String?) {
        val habitKey = makeKey(key, entityId)
        val existing = habits[habitKey]
        val now = System.currentTimeMillis()

        if (existing == null) {
            // 创建新习惯 (confidence = 0.5)
            habits[habitKey] = UserHabit(
                habitKey = key,
                habitValue = value,
                entityId = entityId,
                isExplicit = false,
                confidence = 0.5f,
                observationCount = 1,
                rejectionCount = 0,
                lastObservedAt = now,
                createdAt = now
            )
        } else {
            // 增加观察计数并重新计算置信度
            val newObsCount = existing.observationCount + 1
            habits[habitKey] = existing.copy(
                habitValue = value,
                observationCount = newObsCount,
                confidence = recalculateConfidence(newObsCount, existing.rejectionCount),
                lastObservedAt = now
            )
        }
    }

    override suspend fun reject(key: String, entityId: String?) {
        val habitKey = makeKey(key, entityId)
        val existing = habits[habitKey] ?: return

        // 增加拒绝计数并重新计算置信度
        val newRejCount = existing.rejectionCount + 1
        habits[habitKey] = existing.copy(
            rejectionCount = newRejCount,
            confidence = recalculateConfidence(existing.observationCount, newRejCount),
            lastObservedAt = System.currentTimeMillis()
        )
    }

    /**
     * 计算置信度
     * - total = 0 → 0.5 (除零边界,新习惯中性置信度)
     * - total > 0 → obs / (obs + rej)
     */
    private fun recalculateConfidence(observationCount: Int, rejectionCount: Int): Float {
        val total = observationCount + rejectionCount
        return when {
            total == 0 -> 0.5f  // 除零边界:新习惯,中性置信度
            else -> observationCount.toFloat() / total
        }
    }

    /**
     * 生成唯一 key: "habitKey|entityId" or "habitKey|null"
     */
    private fun makeKey(key: String, entityId: String?): String {
        return "$key|$entityId"
    }
}
