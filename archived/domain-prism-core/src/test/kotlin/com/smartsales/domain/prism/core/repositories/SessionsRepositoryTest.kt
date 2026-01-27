package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.entities.Session
import com.smartsales.domain.prism.core.fakes.FakeSessionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionsRepositoryTest {
    
    private lateinit var repo: SessionsRepository
    
    @Before
    fun setup() {
        repo = FakeSessionsRepository()
    }
    
    @Test
    fun `insert and getById round-trips`() = runTest {
        val session = Session("s-1", "Test Session", Mode.COACH)
        repo.insert(session)
        
        val retrieved = repo.getById("s-1")
        
        assertEquals("Test Session", retrieved?.title)
    }
    
    @Test
    fun `pin updates isPinned`() = runTest {
        repo.insert(Session("s-1", "Test", Mode.COACH, isPinned = false))
        repo.pin("s-1", true)
        
        val pinned = repo.getPinned()
        
        assertEquals(1, pinned.size)
    }
    
    @Test
    fun `getAll returns sorted by updatedAt descending`() = runTest {
        repo.insert(Session("s-1", "Old", Mode.COACH, updatedAt = 1000))
        repo.insert(Session("s-2", "New", Mode.COACH, updatedAt = 2000))
        
        val all = repo.getAll()
        
        assertEquals("s-2", all[0].id)
    }
}
