package com.smartsales.prism.data.real

import com.smartsales.prism.data.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import com.smartsales.prism.data.fakes.FakeReinforcementLearner
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.data.fakes.FakeUserHabitRepository
import com.smartsales.prism.domain.model.Mode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for RealContextBuilder's memory search logic (Wave 3+).
 * Verifies the "First Turn Context" strategy.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealContextBuilderMemoryTest {

    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var entityRepository: FakeEntityRepository
    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: FakeReinforcementLearner

    @Before
    fun setup() {
        timeProvider = FakeTimeProvider()
        entityRepository = FakeEntityRepository()
        habitRepository = FakeUserHabitRepository()
        reinforcementLearner = FakeReinforcementLearner(habitRepository)
        memoryRepository = FakeMemoryRepository() // Initialized with seed data including "价格"

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            entityRepository = entityRepository,
            reinforcementLearner = reinforcementLearner,
            memoryRepository = memoryRepository
        )
    }

    @Test
    fun `first turn triggers memory search`() = runTest {
        // Act: First message (no history) with query that matches seed data "价格"
        val context = contextBuilder.build("关于价格的问题", Mode.COACH)

        // Assert: Should have hits because it's first turn
        assertTrue("Should have memory hits on first turn", context.memoryHits.isNotEmpty())
        
        // Match content from FakeMemoryRepository seed
        val hasPriceHit = context.memoryHits.any { it.content.contains("价格") }
        assertTrue("Should contain '价格' related memory", hasPriceHit)
    }

    @Test
    fun `subsequent turns skip memory search`() = runTest {
        // Arrange: Establish history
        contextBuilder.recordUserMessage("Hi")
        contextBuilder.recordAssistantMessage("Hello")
        
        // Act: Second turn with same query
        val context = contextBuilder.build("关于价格的问题", Mode.COACH)

        // Assert: Should NOT search (relying on session context)
        assertEquals("Should skip memory search on subsequent turns", 0, context.memoryHits.size)
    }
    
    @Test
    fun `resetSession clears history and re-enables search`() = runTest {
        // Arrange: Establish history
        contextBuilder.recordUserMessage("Hi")
        contextBuilder.recordAssistantMessage("Hello")
        
        // Reset
        contextBuilder.resetSession()
        
        // Act: New session, same query
        val context = contextBuilder.build("关于价格的问题", Mode.COACH)
        
        // Assert: Should search again
        assertTrue("Should search memory after session reset", context.memoryHits.isNotEmpty())
    }
}
