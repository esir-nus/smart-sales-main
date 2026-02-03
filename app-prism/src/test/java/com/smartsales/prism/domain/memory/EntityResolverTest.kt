package com.smartsales.prism.domain.memory

import com.smartsales.prism.data.fakes.FakeRelevancyRepository
import com.smartsales.prism.data.memory.RealEntityResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * EntityResolver 单元测试 — 别名消歧逻辑
 */
class EntityResolverTest {
    
    private lateinit var relevancyRepository: FakeRelevancyRepository
    private lateinit var entityResolver: EntityResolver
    
    @Before
    fun setup() {
        relevancyRepository = FakeRelevancyRepository()
        entityResolver = RealEntityResolver(relevancyRepository)
    }
    
    // ===== Auto-Resolve Scenario =====
    
    @Test
    fun `one match - auto resolve`() = runTest {
        // Given: "张总" has exactly 1 match in FakeRelevancyRepository
        
        // When
        val result = entityResolver.resolve("张总")
        
        // Then
        assertTrue("Expected AutoResolved", result is ResolutionResult.AutoResolved)
        val resolved = result as ResolutionResult.AutoResolved
        assertEquals("张伟", resolved.entry.displayName)
        assertEquals("z-001", resolved.entry.entityId)
    }
    
    // ===== Picker Scenario =====
    
    @Test
    fun `multiple matches - ambiguous`() = runTest {
        // Given: "王总" has 3 matches in FakeRelevancyRepository
        
        // When
        val result = entityResolver.resolve("王总")
        
        // Then
        assertTrue("Expected AmbiguousMatches", result is ResolutionResult.AmbiguousMatches)
        val ambiguous = result as ResolutionResult.AmbiguousMatches
        assertEquals(3, ambiguous.candidates.size)
        
        val names = ambiguous.candidates.map { it.displayName }
        assertTrue(names.contains("王明"))
        assertTrue(names.contains("王华"))
        assertTrue(names.contains("王军"))
    }
    
    // ===== NotFound Scenario =====
    
    @Test
    fun `zero matches - not found`() = runTest {
        // Given: "李总" has 0 matches in FakeRelevancyRepository
        
        // When
        val result = entityResolver.resolve("李总")
        
        // Then
        assertEquals(ResolutionResult.NotFound, result)
    }
}
