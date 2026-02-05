package com.smartsales.prism.domain.rl

/**
 * 增强学习器 — 从 LLM 结构化输出中学习用户和客户偏好
 * 
 * ## 职责
 * - Wave 1: 接收观察 → 委托给 UserHabitRepository 存储
 * - Wave 2: Orchestrator 集成 (处理 rl_observations)
 * - Wave 3: Context Builder 集成 (提供习惯上下文)
 * 
 * 来源: docs/cerb/rl-module/spec.md L88-94
 */
interface ReinforcementLearner {
    /**
     * 处理观察记录 (由 Orchestrator 调用)
     * 
     * @param observations LLM 响应中的 rl_observations 列表
     */
    suspend fun processObservations(observations: List<RlObservation>)
    
    /**
     * 获取习惯上下文 (由 Context Builder 调用)
     * 
     * @param entityIds 相关实体 ID 列表 (null = 仅全局习惯)
     * @return 习惯上下文 (用于 LLM 提示注入)
     */
    suspend fun getHabitContext(entityIds: List<String>?): HabitContext
}
