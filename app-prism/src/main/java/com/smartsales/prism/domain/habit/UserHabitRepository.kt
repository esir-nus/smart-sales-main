package com.smartsales.prism.domain.habit

import com.smartsales.prism.domain.rl.ObservationSource

/**
 * # 用户习惯仓库接口
 *
 * 用于查询和记录用户行为偏好。用于为 LLM 提供个性化提示和智能默认值。
 *
 * ## Wave 1.5 更新
 * - `observe()` 接受 `source` 参数,根据来源路由到不同计数器
 * - 移除 `reject()` 方法 (用 `observe(source=USER_NEGATIVE)` 替代)
 * - 新增 `delete()` 用于清理低置信度习惯
 *
 * ## 注意事项
 * - 习惯仅作为 LLM 提示,不应用于硬逻辑
 * - 置信度在查询时计算 (考虑时间衰减)
 * - 每次会话重新查询,不应长期缓存
 *
 * ## 来源
 * Interface: docs/cerb/user-habit/interface.md
 */
interface UserHabitRepository {
    /**
     * 获取所有全局习惯 (非实体特定)
     */
    suspend fun getGlobalHabits(): List<UserHabit>

    /**
     * 获取特定实体的习惯 (客户/产品)
     */
    suspend fun getByEntity(entityId: String): List<UserHabit>

    /**
     * 获取特定习惯 (按 key + 可选 entityId)
     */
    suspend fun getHabit(key: String, entityId: String? = null): UserHabit?

    /**
     * 记录观察 — Wave 1.5: 根据 source 路由
     *
     * - INFERRED → inferredCount++
     * - USER_POSITIVE → explicitPositive++
     * - USER_NEGATIVE → explicitNegative++
     *
     * 如果习惯不存在,创建新习惯
     */
    suspend fun observe(
        key: String,
        value: String,
        entityId: String? = null,
        source: ObservationSource
    )

    /**
     * 删除习惯 (用于清理低置信度习惯)
     */
    suspend fun delete(key: String, entityId: String?)
}
