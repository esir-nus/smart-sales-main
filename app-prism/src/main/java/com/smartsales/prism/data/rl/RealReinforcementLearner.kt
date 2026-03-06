package com.smartsales.prism.data.rl

import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.DELETION_THRESHOLD
import com.smartsales.prism.domain.rl.HabitContext
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import com.smartsales.prism.domain.rl.calculateConfidence
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real 增强学习器 — OS Model: RAM Application
 * 
 * ## 实现策略
 * 委托给 UserHabitRepository (SSD)，由 Kernel 调用填充 SessionWorkingSet (RAM)
 * 
 * ## 职责
 * - processObservations: 委托给 habitRepository.observe() (SSD 写入)
 * - loadUserHabits: 加载全局用户习惯 → Section 2 (含 W4 垃圾回收)
 * - loadClientHabits: 加载实体客户习惯 → Section 3 (含 W4 垃圾回收)
 */
@Singleton
class RealReinforcementLearner @Inject constructor(
    private val habitRepository: UserHabitRepository
) : ReinforcementLearner {
    
    override suspend fun processObservations(observations: List<RlObservation>) {
        // Wave 1.5: 委托给 UserHabitRepository,传递 source 参数
        observations.forEach { obs ->
            habitRepository.observe(
                key = obs.key,
                value = obs.value,
                entityId = obs.entityId,
                source = obs.source
            )
        }
    }
    
    override suspend fun loadUserHabits(): HabitContext {
        val now = System.currentTimeMillis()
        val allGlobal = habitRepository.getGlobalHabits()
        
        // Wave 4: 垃圾回收 — 删除置信度低于阈值的习惯
        val (alive, dead) = allGlobal.partition { calculateConfidence(it, now) >= DELETION_THRESHOLD }
        dead.forEach { habitRepository.delete(it.habitKey, it.entityId) }
        
        return HabitContext(
            userHabits = alive,
            clientHabits = emptyList(),
            suggestedDefaults = emptyMap()
        )
    }
    
    override suspend fun loadClientHabits(entityIds: List<String>): HabitContext {
        val now = System.currentTimeMillis()
        val allClient = entityIds.flatMap { habitRepository.getByEntity(it) }
        
        // Wave 4: 垃圾回收
        val (alive, dead) = allClient.partition { calculateConfidence(it, now) >= DELETION_THRESHOLD }
        dead.forEach { habitRepository.delete(it.habitKey, it.entityId) }
        
        return HabitContext(
            userHabits = emptyList(),
            clientHabits = alive,
            suggestedDefaults = emptyMap()
        )
    }
}
