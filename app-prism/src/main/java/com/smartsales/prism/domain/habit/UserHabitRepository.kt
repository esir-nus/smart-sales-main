package com.smartsales.prism.domain.habit

/**
 * # 用户习惯仓库接口
 *
 * 用于查询和记录用户行为偏好。用于为 LLM 提供个性化提示和智能默认值。
 *
 * ## 注意事项
 * - 习惯仅作为 LLM 提示,不应用于硬逻辑
 * - 低置信度习惯 (confidence < 0.7) 需谨慎使用
 * - 尊重明确设置的习惯 (isExplicit = true)
 * - 每次会话重新查询,不应长期缓存
 *
 * ## 来源
 * Interface: docs/cerb/user-habit/interface.md L16-41
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
     * 记录观察 (创建新习惯或增加计数,更新置信度)
     *
     * - 如果习惯不存在,创建新习惯 (confidence = 0.5)
     * - 如果习惯存在,增加 observationCount,重新计算置信度
     */
    suspend fun observe(key: String, value: String, entityId: String? = null)

    /**
     * 记录拒绝 (用户覆盖建议)
     *
     * 增加 rejectionCount,重新计算置信度
     */
    suspend fun reject(key: String, entityId: String? = null)
}
