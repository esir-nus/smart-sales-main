package com.smartsales.prism.domain.session

import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.rl.HabitContext

/**
 * 会话工作集 — 每个会话的 "RAM"
 *
 * OS Model: Kernel 管理的工作区，Application 通过 EnhancedContext 只读访问。
 *
 * Section 1: 蒸馏记忆 — 实体状态、路径索引、实体引用、实体知识图谱
 * Section 2: 用户习惯 — 全局，会话首轮加载一次
 * Section 3: 客户习惯 — 上下文相关，实体 ACTIVE 时自动填充
 *
 * 生命周期：Kernel 创建（会话开始），Kernel 销毁（resetSession）。
 *
 * @see docs/cerb/session-context/spec.md
 */
class SessionWorkingSet(
    val sessionId: String,
    val createdAt: Long
) {
    companion object {
        /** 路径索引最大容量 — 超过时淘汰最早插入的条目 */
        const val MAX_PATH_INDEX_SIZE = 50
    }

    // ========================================
    // Section 1: 蒸馏记忆 (Distilled Memory)
    // ========================================

    /** 实体状态跟踪 — entityId → EntityTrace */
    val entityStates: MutableMap<String, EntityTrace> = mutableMapOf()

    /** 别名 → entityId 快速查找缓存（插入顺序，非严格 LRU） */
    val pathIndex: MutableMap<String, String> = mutableMapOf()

    /** 实体知识图谱 JSON — 首轮由 Kernel 构建 */
    var entityKnowledge: String? = null

    /** Sticky Notes: 近期日程摘要 — 首轮由 Kernel 构建 */
    var scheduleContext: String? = null

    /** 实体引用 — EntityWriter write-through 填充 */
    val entityContext: MutableMap<String, EntityRef> = mutableMapOf()

    // ========================================
    // Section 2: 用户习惯 (User Habits — Global)
    // ========================================

    /** 全局用户偏好 — 会话开始时加载一次 */
    var userHabitContext: HabitContext? = null

    // ========================================
    // Section 3: 客户习惯 (Client Habits — Contextual)
    // ========================================

    /** 实体相关偏好 — markActive 时自动填充 */
    var clientHabitContext: HabitContext? = null

    // ========================================
    // Section 1 操作
    // ========================================

    /** 从缓存解析别名 → entityId（O(1) 查找） */
    fun resolveAlias(alias: String): String? = pathIndex[alias]

    /**
     * 缓存别名 → entityId 映射（插入顺序淘汰）
     */
    fun cacheAlias(alias: String, entityId: String) {
        if (pathIndex.size >= MAX_PATH_INDEX_SIZE && !pathIndex.containsKey(alias)) {
            val oldest = pathIndex.keys.first()
            pathIndex.remove(oldest)
        }
        pathIndex[alias] = entityId
    }

    /**
     * 判断是否需要加载实体数据
     *
     * @return true = 需要加载（无记录或 MENTIONED），false = 已 ACTIVE 或 UNKNOWN
     */
    fun shouldLoadData(entityId: String): Boolean {
        val trace = entityStates[entityId]
        return when {
            trace == null -> true
            trace.state == EntityState.MENTIONED -> true
            trace.state == EntityState.ACTIVE -> false
            trace.state == EntityState.UNKNOWN -> false
            else -> true
        }
    }

    /** 标记实体为 ACTIVE 状态 */
    fun markActive(entityId: String, confidence: Float = 1.0f) {
        entityStates[entityId] = EntityTrace(
            entityId = entityId,
            state = EntityState.ACTIVE,
            confidence = confidence
        )
    }

    // ========================================
    // Section 2 + 3 合并
    // ========================================

    /**
     * 合并用户习惯 + 客户习惯为统一 HabitContext
     *
     * 消费方（EnhancedContext.habitContext）无需感知分区。
     */
    fun getCombinedHabitContext(): HabitContext? {
        val user = userHabitContext
        val client = clientHabitContext
        if (user == null && client == null) return null
        return HabitContext(
            userHabits = user?.userHabits ?: emptyList(),
            clientHabits = client?.clientHabits ?: emptyList(),
            suggestedDefaults = (user?.suggestedDefaults ?: emptyMap()) +
                (client?.suggestedDefaults ?: emptyMap())
        )
    }
}
