package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.ObservationSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * # UserHabitRepository 单元测试 — Wave 1.5
 *
 * 验证习惯仓库的核心契约,包括新的 4-rule weighting model。
 *
 * ## 测试用例
 * 1. Empty returns empty
 * 2. Global habits only (entityId = null)
 * 3. Entity-specific habits
 * 4. Observe INFERRED increments inferredCount
 * 5. Observe USER_POSITIVE increments explicitPositive
 * 6. Observe USER_NEGATIVE increments explicitNegative
 * 7. Create new habit with correct source routing
 * 8. Delete removes habit
 */
class UserHabitRepositoryTest {

    private lateinit var repository: UserHabitRepository

    @Before
    fun setup() {
        repository = FakeUserHabitRepository()
        // Clear seeded habits to ensure tests start with empty state
        (repository as FakeUserHabitRepository).clear()
    }

    @Test
    fun `getGlobalHabits returns empty when no habits exist`() = runTest {
        val result = repository.getGlobalHabits()
        
        assertEquals(emptyList<com.smartsales.prism.domain.habit.UserHabit>(), result)
    }

    @Test
    fun `getGlobalHabits returns only global habits`() = runTest {
        // Seed: 1 global + 1 entity-specific
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        repository.observe("default_duration", "60", entityId = "client-123", ObservationSource.INFERRED)
        
        val result = repository.getGlobalHabits()
        
        assertEquals(1, result.size)
        assertEquals("preferred_meeting_time", result[0].habitKey)
        assertNull(result[0].entityId)
    }

    @Test
    fun `getByEntity returns only matching entityId`() = runTest {
        // Seed: 2 for client-123, 1 for client-456, 1 global
        repository.observe("preferred_meeting_time", "morning", entityId = "client-123", ObservationSource.INFERRED)
        repository.observe("default_duration", "60", entityId = "client-123", ObservationSource.USER_POSITIVE)
        repository.observe("preferred_location", "office", entityId = "client-456", ObservationSource.INFERRED)
        repository.observe("follow_up_interval", "7", entityId = null, ObservationSource.INFERRED)
        
        val result = repository.getByEntity("client-123")
        
        assertEquals(2, result.size)
        assertEquals("client-123", result[0].entityId)
        assertEquals("client-123", result[1].entityId)
    }

    @Test
    fun `observe INFERRED increments inferredCount`() = runTest {
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        
        val habit = repository.getHabit("preferred_meeting_time", entityId = null)
        
        assertEquals("morning", habit?.habitValue)
        assertEquals(3, habit?.inferredCount)
        assertEquals(0, habit?.explicitPositive)
        assertEquals(0, habit?.explicitNegative)
    }

    @Test
    fun `observe USER_POSITIVE increments explicitPositive`() = runTest {
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.USER_POSITIVE)
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.USER_POSITIVE)
        
        val habit = repository.getHabit("preferred_meeting_time", entityId = null)
        
        assertEquals("morning", habit?.habitValue)
        assertEquals(0, habit?.inferredCount)
        assertEquals(2, habit?.explicitPositive)
        assertEquals(0, habit?.explicitNegative)
    }

    @Test
    fun `observe USER_NEGATIVE increments explicitNegative`() = runTest {
        // Create with INFERRED first
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        // Then reject it
        repository.observe("preferred_meeting_time", "evening", entityId = null, ObservationSource.USER_NEGATIVE)
        
        val habit = repository.getHabit("preferred_meeting_time", entityId = null)
        
        assertEquals("evening", habit?.habitValue)  // Updated to newest value
        assertEquals(1, habit?.inferredCount)
        assertEquals(0, habit?.explicitPositive)
        assertEquals(1, habit?.explicitNegative)
    }

    @Test
    fun `new habit routes source correctly on creation`() = runTest {
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.USER_POSITIVE)
        
        val habit = repository.getHabit("preferred_meeting_time", entityId = null)
        
        // New habit created with USER_POSITIVE should have explicitPositive = 1
        assertEquals(0, habit?.inferredCount)
        assertEquals(1, habit?.explicitPositive)
        assertEquals(0, habit?.explicitNegative)
    }

    @Test
    fun `delete removes habit`() = runTest {
        repository.observe("preferred_meeting_time", "morning", entityId = null, ObservationSource.INFERRED)
        
        // Verify exists
        var habit = repository.getHabit("preferred_meeting_time", entityId = null)
        assertEquals("morning", habit?.habitValue)
        
        // Delete
        repository.delete("preferred_meeting_time", entityId = null)
        
        // Verify removed
        habit = repository.getHabit("preferred_meeting_time", entityId = null)
        assertNull(habit)
    }
}
