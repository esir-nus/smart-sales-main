package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.UserHabit
import com.smartsales.domain.prism.core.fakes.FakeUserHabitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserHabitRepositoryTest {
    
    private lateinit var repo: UserHabitRepository
    
    @Before
    fun setup() {
        repo = FakeUserHabitRepository()
    }
    
    @Test
    fun `upsert and getByKey round-trips`() = runTest {
        val habit = UserHabit(
            habitKey = "meeting_time",
            habitValue = "morning",
            confidence = 0.8f
        )
        repo.upsert(habit)
        
        val retrieved = repo.getByKey("meeting_time")
        
        assertEquals("morning", retrieved?.habitValue)
    }
    
    @Test
    fun `getForEntity filters by entityId`() = runTest {
        repo.upsert(UserHabit("h1", "v1", entityId = "z-001"))
        repo.upsert(UserHabit("h2", "v2", entityId = "z-002"))
        repo.upsert(UserHabit("h3", "v3", entityId = null))
        
        val forEntity = repo.getForEntity("z-001")
        
        assertEquals(1, forEntity.size)
        assertEquals("h1", forEntity[0].habitKey)
    }
    
    @Test
    fun `delete removes habit`() = runTest {
        repo.upsert(UserHabit("h1", "v1"))
        repo.delete("h1")
        
        assertNull(repo.getByKey("h1"))
    }
}
