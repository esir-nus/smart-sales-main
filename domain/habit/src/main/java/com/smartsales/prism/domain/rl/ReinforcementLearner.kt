package com.smartsales.prism.domain.rl

/**
 * 增强学习器 — 从 LLM 结构化输出中学习用户和客户偏好
 * 
 * ## OS Model: RAM Application
 * - Kernel 调用 loadUserHabits/loadClientHabits 填充 SessionWorkingSet
 * - processObservations 写入 SSD（未来: 同时更新 RAM，write-through）
 * 
 * 来源: docs/cerb/rl-module/spec.md L172-182
 */
interface ReinforcementLearner {
    /**
     * 处理观察记录 (由 Orchestrator 调用)
     * 
     * @param observations LLM 响应中的 rl_observations 列表
     */
    suspend fun processObservations(observations: List<RlObservation>)
    
    /**
     * 加载全局用户习惯 → Kernel 填充 RAM Section 2
     * 
     * @return 仅包含 userHabits 的 HabitContext（clientHabits 为空）
     */
    suspend fun loadUserHabits(): HabitContext
    
    /**
     * 加载指定实体的客户习惯 → Kernel 填充 RAM Section 3
     * 
     * 空列表返回空习惯（不会 fallback 到全局习惯）
     * 
     * @param entityIds 活跃实体 ID 列表（非空）
     * @return 仅包含 clientHabits 的 HabitContext（userHabits 为空）
     */
    suspend fun loadClientHabits(entityIds: List<String>): HabitContext
}
