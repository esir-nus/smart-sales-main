package com.smartsales.data.crm

import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.prism.domain.memory.CacheResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealAliasCacheTest {

    private lateinit var fakeEntityRepository: FakeEntityRepository
    private lateinit var testScope: TestScope
    private lateinit var aliasCache: RealAliasCache

    @Before
    fun setup() {
        fakeEntityRepository = FakeEntityRepository()
        testScope = TestScope()
    }
    
    // Extracted instant hydration to simulate pre-warming
    private fun createCache() {
        aliasCache = RealAliasCache(fakeEntityRepository, testScope)
    }

    @Test
    fun `match returns Miss when no candidates provided`() = testScope.runTest {
        createCache()
        advanceUntilIdle() // let init block finish
        
        val result = aliasCache.match(emptyList())
        assertTrue(result is CacheResult.Miss)
    }

    @Test
    fun `match returns ExactMatch when one candidate matches exactly one entity by display name`() = testScope.runTest {
        val entry = EntityEntry(
            entityId = "p-123",
            entityType = EntityType.PERSON,
            displayName = "张伟",
            aliasesJson = "[]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        fakeEntityRepository.save(entry)
        createCache()
        advanceUntilIdle()
        
        val result = aliasCache.match(listOf("张伟"))
        assertTrue(result is CacheResult.ExactMatch)
        assertEquals("p-123", (result as CacheResult.ExactMatch).entityId)
    }

    @Test
    fun `match returns ExactMatch when candidate matches alias case insensitively`() = testScope.runTest {
        val entry = EntityEntry(
            entityId = "c-abc",
            entityType = EntityType.ACCOUNT,
            displayName = "Apple Inc",
            aliasesJson = "[\"苹果\", \"APPL\"]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        fakeEntityRepository.save(entry)
        createCache()
        advanceUntilIdle()
        
        val result = aliasCache.match(listOf("appl"))
        assertTrue(result is CacheResult.ExactMatch)
        assertEquals("c-abc", (result as CacheResult.ExactMatch).entityId)
    }

    @Test
    fun `match returns Ambiguous when candidates match multiple entities`() = testScope.runTest {
        val entry1 = EntityEntry(
            entityId = "p-001",
            entityType = EntityType.PERSON,
            displayName = "李四",
            aliasesJson = "[\"李总\"]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        val entry2 = EntityEntry(
            entityId = "p-002",
            entityType = EntityType.PERSON,
            displayName = "李四 (北京)",
            aliasesJson = "[\"李总\"]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        fakeEntityRepository.save(entry1)
        fakeEntityRepository.save(entry2)
        createCache()
        advanceUntilIdle()
        
        val result = aliasCache.match(listOf("李总"))
        assertTrue(result is CacheResult.Ambiguous)
        val candidates = (result as CacheResult.Ambiguous).candidates
        assertEquals(2, candidates.size)
        assertTrue(candidates.map { it.entityId }.containsAll(listOf("p-001", "p-002")))
    }

    @Test
    fun `match ignores json parse errors and continues hydration`() = testScope.runTest {
        val badJsonEntry = EntityEntry(
            entityId = "bad-1",
            entityType = EntityType.PERSON,
            displayName = "王总",
            aliasesJson = "INVALID_JSON_[\"test\"]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        val goodEntry = EntityEntry(
            entityId = "good-1",
            entityType = EntityType.PERSON,
            displayName = "张四",
            aliasesJson = "[]",
            demeanorJson = "{}",
            attributesJson = "{}",
            metricsHistoryJson = "{}",
            relatedEntitiesJson = "{}",
            decisionLogJson = "{}",
            lastUpdatedAt = 0,
            createdAt = 0
        )
        fakeEntityRepository.save(badJsonEntry)
        fakeEntityRepository.save(goodEntry)
        createCache()
        advanceUntilIdle()
        
        // Display name still works even if aliases mapping failed
        val result1 = aliasCache.match(listOf("王总"))
        assertTrue(result1 is CacheResult.ExactMatch)
        assertEquals("bad-1", (result1 as CacheResult.ExactMatch).entityId)
        
        // Good entry still gets indexed
        val result2 = aliasCache.match(listOf("张四"))
        assertTrue(result2 is CacheResult.ExactMatch)
        assertEquals("good-1", (result2 as CacheResult.ExactMatch).entityId)
    }
}
