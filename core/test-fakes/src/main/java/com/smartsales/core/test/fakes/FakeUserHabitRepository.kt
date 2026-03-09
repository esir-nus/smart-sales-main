package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.ObservationSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * # Fake UserHabitRepository — 内存中存储
 *
 * 用于单元测试的 Fake 实现。
 *
 * ## Wave 1.5 更新
 * - `observe()` 根据 source 路由到不同计数器
 * - 移除置信度计算 (在查询时计算)
 * - 新增 `delete()` 方法
 *
 * NOTE: Seeded with 3 global habits for Coach Mode L2 testing.
 * Tests should call clear() in @Before to reset to empty state.
 */
@Singleton
class FakeUserHabitRepository @Inject constructor() : UserHabitRepository {

    private val habits = mutableMapOf<String, UserHabit>()

    init {
        // Seed global habits for Coach Mode L2 testing (Wave 3)
        val now = System.currentTimeMillis()
        val seedHabits = listOf(
            UserHabit(
                habitKey = "communication_style",
                habitValue = "用户偏好用案例和故事说服客户，不喜欢纯理论讲解",
                entityId = null,
                inferredCount = 5,
                lastObservedAt = now - 86400000,
                createdAt = now - 604800000
            ),
            UserHabit(
                habitKey = "meeting_time_pref",
                habitValue = "用户习惯在上午安排重要客户会议，下午处理内部事务",
                entityId = null,
                inferredCount = 8,
                lastObservedAt = now - 43200000,
                createdAt = now - 1209600000
            ),
            UserHabit(
                habitKey = "objection_handling",
                habitValue = "用户擅长用'假设成交法'处理价格异议，成功率高",
                entityId = null,
                inferredCount = 12,
                explicitPositive = 3,
                lastObservedAt = now - 21600000,
                createdAt = now - 1814400000
            )
        )
        seedHabits.forEach { habit ->
            habits[makeKey(habit.habitKey, habit.entityId)] = habit
        }
        android.util.Log.d("CoachMemory", "🌱 FakeUserHabitRepository seeded with ${seedHabits.size} global habits: ${seedHabits.map { it.habitKey }}")
    }

    override suspend fun getGlobalHabits(): List<UserHabit> {
        return habits.values.filter { it.entityId == null }
    }

    override suspend fun getByEntity(entityId: String): List<UserHabit> {
        return habits.values.filter { it.entityId == entityId }
    }

    override suspend fun getHabit(key: String, entityId: String?): UserHabit? {
        return habits[makeKey(key, entityId)]
    }

    override suspend fun observe(
        key: String,
        value: String,
        entityId: String?,
        source: ObservationSource
    ) {
        val habitKey = makeKey(key, entityId)
        val existing = habits[habitKey]
        val now = System.currentTimeMillis()

        if (existing == null) {
            // 创建新习惯,根据 source 初始化对应计数器
            habits[habitKey] = UserHabit(
                habitKey = key,
                habitValue = value,
                entityId = entityId,
                inferredCount = if (source == ObservationSource.INFERRED) 1 else 0,
                explicitPositive = if (source == ObservationSource.USER_POSITIVE) 1 else 0,
                explicitNegative = if (source == ObservationSource.USER_NEGATIVE) 1 else 0,
                lastObservedAt = now,
                createdAt = now
            )
        } else {
            // 增加对应计数器
            habits[habitKey] = existing.copy(
                habitValue = value,
                inferredCount = existing.inferredCount + 
                    if (source == ObservationSource.INFERRED) 1 else 0,
                explicitPositive = existing.explicitPositive + 
                    if (source == ObservationSource.USER_POSITIVE) 1 else 0,
                explicitNegative = existing.explicitNegative + 
                    if (source == ObservationSource.USER_NEGATIVE) 1 else 0,
                lastObservedAt = now
            )
        }
    }

    override suspend fun delete(key: String, entityId: String?) {
        habits.remove(makeKey(key, entityId))
    }

    /**
     * 清空所有习惯 — 用于测试环境重置
     */
    fun clear() {
        habits.clear()
    }

    /**
     * 生成唯一 key: "habitKey|entityId" or "habitKey|null"
     */
    private fun makeKey(key: String, entityId: String?): String {
        return "$key|$entityId"
    }
}
