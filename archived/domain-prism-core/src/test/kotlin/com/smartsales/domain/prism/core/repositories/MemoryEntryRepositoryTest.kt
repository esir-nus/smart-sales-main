package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.entities.*
import com.smartsales.domain.prism.core.fakes.FakeMemoryEntryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MemoryEntryRepositoryTest {
    
    private lateinit var repo: MemoryEntryRepository
    
    @Before
    fun setup() {
        repo = FakeMemoryEntryRepository()
    }
    
    private fun createEntry(id: String, archived: Boolean = false) = MemoryEntryEntity(
        id = id,
        workflow = Mode.COACH,
        title = "Test",
        isArchived = archived,
        sessionId = "session-1",
        displayContent = "Content"
    )
    
    @Test
    fun `getHotZone returns non-archived entries`() = runTest {
        repo.insert(createEntry("1", archived = false))
        repo.insert(createEntry("2", archived = true))
        
        val hot = repo.getHotZone()
        
        assertEquals(1, hot.size)
        assertEquals("1", hot[0].id)
    }
    
    @Test
    fun `getCementZone returns archived entries`() = runTest {
        repo.insert(createEntry("1", archived = false))
        repo.insert(createEntry("2", archived = true))
        
        val cement = repo.getCementZone()
        
        assertEquals(1, cement.size)
        assertEquals("2", cement[0].id)
    }
    
    @Test
    fun `archive moves entry to cement`() = runTest {
        repo.insert(createEntry("1", archived = false))
        repo.archive("1")
        
        val hot = repo.getHotZone()
        val cement = repo.getCementZone()
        
        assertTrue(hot.isEmpty())
        assertEquals(1, cement.size)
    }
}
