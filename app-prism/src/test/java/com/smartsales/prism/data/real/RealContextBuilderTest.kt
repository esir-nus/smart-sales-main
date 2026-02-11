package com.smartsales.prism.data.real

import com.smartsales.prism.data.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeReinforcementLearner
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.data.fakes.FakeUserHabitRepository
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * RealContextBuilder 测试 — Wave 3 习惯注入验证
 * 
 * 测试目标:
 * 1. build() 调用 loadUserHabits() — 全局习惯
 * 2. EntityWriter write-through 更新 RAM Section 1
 */
class RealContextBuilderTest {
    
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: FakeReinforcementLearner
    private lateinit var memoryRepository: FakeMemoryRepository
    
    @Before
    fun setup() {
        timeProvider = FakeTimeProvider()
        habitRepository = FakeUserHabitRepository()
        habitRepository.clear()  // Reset seed data for test isolation
        reinforcementLearner = FakeReinforcementLearner(habitRepository)
        memoryRepository = FakeMemoryRepository()
        
        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = reinforcementLearner,
            memoryRepository = memoryRepository,
            entityRepository = FakeEntityRepository()
        )
    }
    
    @Test
    fun `build() injects global habits`() = runTest {
        // Arrange: 添加全局习惯
        habitRepository.observe(
            key = "preferred_meeting_time",
            value = "morning",
            entityId = null,
            source = ObservationSource.USER_POSITIVE
        )
        
        // Act
        val context = contextBuilder.build("test input", Mode.COACH)
        
        // Assert
        assertNotNull(context.habitContext)
        assertEquals(1, context.habitContext!!.userHabits.size)
        assertEquals("preferred_meeting_time", context.habitContext!!.userHabits[0].habitKey)
        assertEquals(0, context.habitContext!!.clientHabits.size) // null entityIds
    }
    
    @Test
    fun `build() includes all EnhancedContext fields`() = runTest {
        // Act
        val context = contextBuilder.build("test input", Mode.ANALYST)
        
        // Assert: Verify all fields are set
        assertEquals("test input", context.userText)
        assertEquals(Mode.ANALYST, context.modeMetadata.currentMode)
        assertNotNull(context.currentDate)
        assertNotNull(context.habitContext) // Wave 3 field
    }
    
    @Test
    fun `habitContext is populated even with no habits`() = runTest {
        // Act: No habits seeded
        val context = contextBuilder.build("test input", Mode.COACH)
        
        // Assert: habitContext should exist but be empty
        assertNotNull(context.habitContext)
        assertEquals(0, context.habitContext!!.userHabits.size)
        assertEquals(0, context.habitContext!!.clientHabits.size)
    }
    
    @Test
    fun `multiple habit observations aggregate correctly`() = runTest {
        // Arrange: 添加多个全局习惯
        habitRepository.observe(
            key = "preferred_meeting_time",
            value = "morning",
            entityId = null,
            source = ObservationSource.USER_POSITIVE
        )
        habitRepository.observe(
            key = "communication_style",
            value = "direct",
            entityId = null,
            source = ObservationSource.INFERRED
        )
        
        // Act
        val context = contextBuilder.build("test input", Mode.COACH)
        
        // Assert
        assertNotNull(context.habitContext)
        assertEquals(2, context.habitContext!!.userHabits.size)
    }
}

