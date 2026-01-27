package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.*
import com.smartsales.domain.prism.core.fakes.FakeRelevancyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RelevancyRepositoryTest {
    
    private lateinit var repo: RelevancyRepository
    
    @Before
    fun setup() {
        repo = FakeRelevancyRepository()
    }
    
    @Test
    fun `upsert and getByEntityId round-trips`() = runTest {
        val entry = RelevancyEntry(
            entityId = "z-001",
            entityType = EntityType.PERSON,
            displayName = "张总"
        )
        
        repo.upsert(entry)
        val retrieved = repo.getByEntityId("z-001")
        
        assertEquals(entry.displayName, retrieved?.displayName)
    }
    
    @Test
    fun `findByAlias returns matching entries`() = runTest {
        val entry = RelevancyEntry(
            entityId = "z-001",
            entityType = EntityType.PERSON,
            displayName = "张伟",
            aliases = listOf(AliasMapping("张总"))
        )
        repo.upsert(entry)
        
        val results = repo.findByAlias("张总")
        
        assertEquals(1, results.size)
        assertEquals("张伟", results[0].displayName)
    }
    
    @Test
    fun `delete removes entry`() = runTest {
        repo.upsert(RelevancyEntry("z-001", EntityType.PERSON, "Test"))
        repo.delete("z-001")
        
        assertNull(repo.getByEntityId("z-001"))
    }
}
