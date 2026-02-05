package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.HabitContext
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake 增强学习器 — Wave 1 骨架实现
 * 
 * ## 实现策略
 * `FakeReinforcementLearner` 是 `UserHabitRepository` 的外观 (Facade):
 * - 不重复存储逻辑
 * - 委托给已有的 UserHabitRepository
 * - Wave 2+ 会添加 LLM 集成逻辑
 * 
 * ## Wave 1 职责
 * - processObservations: 委托给 habitRepository.observe()
 * - getHabitContext: 聚合全局 + 实体习惯
 */
@Singleton
class FakeReinforcementLearner @Inject constructor(
    private val habitRepository: UserHabitRepository
) : ReinforcementLearner {
    
    override suspend fun processObservations(observations: List<RlObservation>) {
        // Wave 1.5: 委托给 UserHabitRepository,传递 source 参数
        observations.forEach { obs ->
            habitRepository.observe(
                key = obs.key,
                value = obs.value,
                entityId = obs.entityId,
                source = obs.source  // NEW: Pass source for routing
            )
        }
    }
    
    override suspend fun getHabitContext(entityIds: List<String>?): HabitContext {
        // 聚合全局习惯 + 特定实体习惯
        val globalHabits = habitRepository.getGlobalHabits()
        val clientHabits = entityIds?.flatMap { 
            habitRepository.getByEntity(it) 
        } ?: emptyList()
        
        return HabitContext(
            userHabits = globalHabits,
            clientHabits = clientHabits,
            suggestedDefaults = emptyMap()  // Wave 3: 智能默认值生成
        )
    }
}
