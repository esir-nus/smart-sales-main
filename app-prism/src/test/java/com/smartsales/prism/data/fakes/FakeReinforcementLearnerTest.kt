package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * # FakeReinforcementLearner 单元测试
 * 
 * 验证 RL Module Wave 1 的核心契约:
 * - processObservations() 委托给 UserHabitRepository
 * - getHabitContext() 正确聚合全局 + 实体习惯
 * 
 * ## 测试用例
 * 1. processObservations with empty list (no-op)
 * 2. processObservations with single observation
 * 3. processObservations with multiple observations
 * 4. getHabitContext with null entityIds (global only)
 * 5. getHabitContext with empty entityIds list
 * 6. getHabitContext with single entityId
 * 7. getHabitContext with multiple entityIds
 */
class FakeReinforcementLearnerTest {

    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: ReinforcementLearner

    @Before
    fun setup() {
        habitRepository = FakeUserHabitRepository()
        habitRepository.clear()  // Reset seed data for test isolation
        reinforcementLearner = FakeReinforcementLearner(habitRepository)
    }

    @Test
    fun `processObservations with empty list does nothing`() = runTest {
        reinforcementLearner.processObservations(emptyList())
        
        val habits = habitRepository.getGlobalHabits()
        assertEquals(0, habits.size)
    }

    @Test
    fun `processObservations creates habit via repository`() = runTest {
        val observation = RlObservation(
            entityId = null,
            key = "preferred_meeting_time",
            value = "morning",
            source = ObservationSource.USER_POSITIVE,
            evidence = "用户说我喜欢早上开会"
        )
        
        reinforcementLearner.processObservations(listOf(observation))
        
        val habit = habitRepository.getHabit("preferred_meeting_time", entityId = null)
        assertEquals("morning", habit?.habitValue)
        assertEquals(1, habit?.explicitPositive)
        assertEquals(0, habit?.inferredCount)
    }

    @Test
    fun `processObservations handles multiple observations`() = runTest {
        val observations = listOf(
            RlObservation(
                entityId = null,
                key = "preferred_meeting_time",
                value = "morning",
                source = ObservationSource.USER_POSITIVE,
                evidence = null
            ),
            RlObservation(
                entityId = "client-123",
                key = "default_duration",
                value = "60",
                source = ObservationSource.INFERRED,
                evidence = "过去3次会议都是1小时"
            )
        )
        
        reinforcementLearner.processObservations(observations)
        
        val globalHabit = habitRepository.getHabit("preferred_meeting_time", entityId = null)
        val entityHabit = habitRepository.getHabit("default_duration", entityId = "client-123")
        
        assertEquals("morning", globalHabit?.habitValue)
        assertEquals("60", entityHabit?.habitValue)
    }

    @Test
    fun `getHabitContext with null entityIds returns global habits only`() = runTest {
        // Seed: 1 global + 1 entity-specific
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.getHabitContext(entityIds = null)
        
        assertEquals(1, context.userHabits.size)
        assertEquals(0, context.clientHabits.size)
        assertEquals("preferred_meeting_time", context.userHabits[0].habitKey)
    }

    @Test
    fun `getHabitContext with empty list returns global habits only`() = runTest {
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.getHabitContext(entityIds = emptyList())
        
        assertEquals(1, context.userHabits.size)
        assertEquals(0, context.clientHabits.size)
    }

    @Test
    fun `getHabitContext aggregates global and entity habits`() = runTest {
        // Seed: 1 global, 2 for client-123, 1 for client-456
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("preferred_location", "office", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("follow_up_interval", "7", entityId = "client-456", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.getHabitContext(entityIds = listOf("client-123"))
        
        assertEquals(1, context.userHabits.size)
        assertEquals(2, context.clientHabits.size)
        assertEquals("preferred_meeting_time", context.userHabits[0].habitKey)
    }

    @Test
    fun `getHabitContext with multiple entityIds aggregates all`() = runTest {
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("preferred_location", "office", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("follow_up_interval", "7", entityId = "client-456", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.getHabitContext(
            entityIds = listOf("client-123", "client-456")
        )
        
        assertEquals(0, context.userHabits.size)
        assertEquals(3, context.clientHabits.size)
    }

    @Test
    fun `getHabitContext suggestedDefaults is empty in Wave 1`() = runTest {
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        
        val context = reinforcementLearner.getHabitContext(entityIds = null)
        
        assertEquals(0, context.suggestedDefaults.size)
    }
}
