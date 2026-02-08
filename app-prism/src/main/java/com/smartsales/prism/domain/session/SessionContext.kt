package com.smartsales.prism.domain.session

/**
 * 会话上下文 — ContextBuilder 的智能缓存层
 *
 * 生命周期：会话开始时创建，会话结束或进程死亡时销毁。
 * 不是持久化存储，而是会话内的 O(1) 实体解析缓存。
 *
 * @param sessionId 当前会话 ID
 * @param createdAt 会话创建时间戳（由 TimeProvider 提供，便于测试）
 */
data class SessionContext(
    val sessionId: String,
    val createdAt: Long
) {
    companion object {
        /** 路径索引最大容量（超过时淘汰最早插入的条目） */
        const val MAX_PATH_INDEX_SIZE = 50
    }

    /** 实体状态跟踪 — entityId → EntityTrace */
    val entityStates: MutableMap<String, EntityTrace> = mutableMapOf()

    /**
     * 别名 → entityId 的快速查找缓存
     *
     * 使用 Kotlin mutableMapOf()（JVM 上为 LinkedHashMap，保持插入顺序）。
     * 注意：这是插入顺序淘汰，非严格 LRU（访问不更新顺序）。
     * 对于别名场景足够用 — 别名通常插入一次、多次查找。
     */
    val pathIndex: MutableMap<String, String> = mutableMapOf()

    /** 当前会话轮次计数 */
    var turnCount: Int = 0
        private set

    /**
     * 递增轮次计数（每次 build() 调用时触发）
     */
    fun incrementTurn() {
        turnCount++
    }

    /**
     * 从缓存解析别名 → entityId（O(1) 查找）
     *
     * @return entityId 或 null（缓存未命中）
     */
    fun resolveAlias(alias: String): String? = pathIndex[alias]

    /**
     * 缓存别名 → entityId 映射
     *
     * 注意：仅缓存第一个候选的 entityId。
     * 如果 findByAlias 返回多个候选，后续缓存命中只返回第一个。
     * 当前 Scheduler 管线只使用 person_candidate_0，行为一致。
     *
     * @param alias 用户使用的别名（如 "张总"）
     * @param entityId 解析后的实体 ID（如 "p-001"）
     */
    fun cacheAlias(alias: String, entityId: String) {
        // 插入顺序淘汰：超出容量时移除最早插入的条目
        if (pathIndex.size >= MAX_PATH_INDEX_SIZE && !pathIndex.containsKey(alias)) {
            val oldest = pathIndex.keys.first()
            pathIndex.remove(oldest)
        }
        pathIndex[alias] = entityId
    }

    /**
     * 判断是否需要加载实体数据（Wave 3: Smart Triggers）
     *
     * @param entityId 实体 ID
     * @return true 如果需要加载数据，false 如果数据已加载或不应加载
     */
    fun shouldLoadData(entityId: String): Boolean {
        val trace = entityStates[entityId]
        return when {
            trace == null -> true  // 无记录 → 需要加载
            trace.state == EntityState.MENTIONED -> true  // 已解析但未加载 → 需要加载
            trace.state == EntityState.ACTIVE -> false  // 已激活（会话内有效）→ 跳过
            trace.state == EntityState.UNKNOWN -> false  // 未解析到 ID → 无法加载
            else -> true
        }
    }

    /**
     * 标记实体为 ACTIVE 状态（数据已加载到上下文）
     *
     * @param entityId 实体 ID
     * @param confidence 置信度（默认 1.0）
     */
    fun markActive(entityId: String, confidence: Float = 1.0f) {
        entityStates[entityId] = EntityTrace(
            entityId = entityId,
            state = EntityState.ACTIVE,
            confidence = confidence
        )
    }

    /**
     * 重置会话上下文（模式切换或新会话时调用）
     *
     * @param newSessionId 新会话 ID
     * @param nowMillis 当前时间戳
     */
    fun reset(newSessionId: String, nowMillis: Long): SessionContext {
        return SessionContext(
            sessionId = newSessionId,
            createdAt = nowMillis
        )
    }
}

