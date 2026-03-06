package com.smartsales.prism.data.rl

import com.smartsales.prism.data.fakes.FakeUserHabitRepository

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * # RealReinforcementLearner 单元测试
 * 
 * 验证 RL Module OS Model 升级后的契约:
 * - processObservations() 委托给 UserHabitRepository
 * - loadUserHabits() 仅返回全局习惯 (Section 2)
 * - loadClientHabits() 仅返回实体习惯 (Section 3)
 * 
 * ## 测试用例
 * 1. processObservations with empty list (no-op)
 * 2. processObservations with single observation
 * 3. processObservations with multiple observations
 * 4. loadUserHabits returns global habits only
 * 5. loadUserHabits excludes entity-specific habits
 * 6. loadClientHabits with single entityId
 * 7. loadClientHabits with multiple entityIds
 * 8. loadClientHabits with empty list returns empty
 * 9. suggestedDefaults is empty
 */
class RealReinforcementLearnerTest {

    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: ReinforcementLearner

    @Before
    fun setup() {
        habitRepository = FakeUserHabitRepository()
        habitRepository.clear()  // Reset seed data for test isolation
        reinforcementLearner = RealReinforcementLearner(habitRepository)
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

    // === loadUserHabits (Section 2) ===

    @Test
    fun `loadUserHabits returns global habits only`() = runTest {
        // Seed: 1 global + 1 entity-specific
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.loadUserHabits()
        
        assertEquals(1, context.userHabits.size)
        assertEquals(0, context.clientHabits.size)
        assertEquals("preferred_meeting_time", context.userHabits[0].habitKey)
    }

    @Test
    fun `loadUserHabits with no data returns empty`() = runTest {
        val context = reinforcementLearner.loadUserHabits()
        
        assertEquals(0, context.userHabits.size)
        assertEquals(0, context.clientHabits.size)
    }

    // === loadClientHabits (Section 3) ===

    @Test
    fun `loadClientHabits returns only entity habits`() = runTest {
        // Seed: 1 global, 2 for client-123, 1 for client-456
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("preferred_location", "office", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("follow_up_interval", "7", entityId = "client-456", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.loadClientHabits(listOf("client-123"))
        
        // 只返回 client-123 的习惯，不包含全局习惯
        assertEquals(0, context.userHabits.size)
        assertEquals(2, context.clientHabits.size)
    }

    @Test
    fun `loadClientHabits with multiple entityIds aggregates all`() = runTest {
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("preferred_location", "office", entityId = "client-123", ObservationSource.INFERRED)
        habitRepository.observe("follow_up_interval", "7", entityId = "client-456", ObservationSource.INFERRED)
        
        val context = reinforcementLearner.loadClientHabits(
            listOf("client-123", "client-456")
        )
        
        assertEquals(0, context.userHabits.size)
        assertEquals(3, context.clientHabits.size)
    }

    @Test
    fun `loadClientHabits with empty list returns empty habits`() = runTest {
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        habitRepository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        
        // 空列表 → 空习惯（不 fallback 到全局）
        val context = reinforcementLearner.loadClientHabits(emptyList())
        
        assertEquals(0, context.userHabits.size)
        assertEquals(0, context.clientHabits.size)
    }

    @Test
    fun `suggestedDefaults is empty`() = runTest {
        habitRepository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        
        val userContext = reinforcementLearner.loadUserHabits()
        val clientContext = reinforcementLearner.loadClientHabits(emptyList())
        
        assertEquals(0, userContext.suggestedDefaults.size)
        assertEquals(0, clientContext.suggestedDefaults.size)
    }
}

