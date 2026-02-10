package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.HabitContext
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake 增强学习器 — OS Model: RAM Application
 * 
 * ## 实现策略
 * 委托给 UserHabitRepository (SSD)，由 Kernel 调用填充 SessionWorkingSet (RAM)
 * 
 * ## 职责
 * - processObservations: 委托给 habitRepository.observe() (SSD 写入)
 * - loadUserHabits: 加载全局用户习惯 → Section 2
 * - loadClientHabits: 加载实体客户习惯 → Section 3
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
    
    override suspend fun loadUserHabits(): HabitContext {
        // 全局用户习惯 → RAM Section 2
        return HabitContext(
            userHabits = habitRepository.getGlobalHabits(),
            clientHabits = emptyList(),
            suggestedDefaults = emptyMap()
        )
    }
    
    override suspend fun loadClientHabits(entityIds: List<String>): HabitContext {
        // 指定实体的客户习惯 → RAM Section 3
        // 空列表 → 空习惯（不 fallback 到全局）
        val clientHabits = entityIds.flatMap { habitRepository.getByEntity(it) }
        return HabitContext(
            userHabits = emptyList(),
            clientHabits = clientHabits,
            suggestedDefaults = emptyMap()
        )
    }
}
