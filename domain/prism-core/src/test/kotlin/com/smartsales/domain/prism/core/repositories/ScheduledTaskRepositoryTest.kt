package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.*
import com.smartsales.domain.prism.core.fakes.FakeScheduledTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScheduledTaskRepositoryTest {
    
    private lateinit var repo: ScheduledTaskRepository
    
    @Before
    fun setup() {
        repo = FakeScheduledTaskRepository()
    }
    
    @Test
    fun `getUpcoming returns future tasks sorted`() = runTest {
        val now = System.currentTimeMillis()
        repo.insert(ScheduledTask("t-1", "Past", scheduledAt = now - 10000))
        repo.insert(ScheduledTask("t-2", "Soon", scheduledAt = now + 10000))
        repo.insert(ScheduledTask("t-3", "Later", scheduledAt = now + 20000))
        
        val upcoming = repo.getUpcoming(limit = 2)
        
        assertEquals(2, upcoming.size)
        assertEquals("t-2", upcoming[0].id)
    }
    
    @Test
    fun `getForDateRange filters correctly`() = runTest {
        repo.insert(ScheduledTask("t-1", "In Range", scheduledAt = 5000))
        repo.insert(ScheduledTask("t-2", "Out of Range", scheduledAt = 15000))
        
        val inRange = repo.getForDateRange(0, 10000)
        
        assertEquals(1, inRange.size)
        assertEquals("t-1", inRange[0].id)
    }
}
