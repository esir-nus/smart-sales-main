package com.smartsales.prism.domain.crm

import com.smartsales.prism.data.fakes.FakeClientProfileHub
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ClientProfileHub 单元测试
 * Wave 1: QuickContext / FocusedContext / getByAccountId
 * Wave 2: Timeline Aggregation (getByEntityId)
 */
class ClientProfileHubTest {
    
    private lateinit var entityRepository: FakeEntityRepository
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var hub: ClientProfileHub
    
    @Before
    fun setup() {
        entityRepository = FakeEntityRepository()
        memoryRepository = FakeMemoryRepository()
        memoryRepository.clear()  // 清除种子数据，确保测试隔离
        hub = FakeClientProfileHub(entityRepository, memoryRepository)
    }
    
    // ─── Wave 1 Tests ───────────────────────────────────────
    
    @Test
    fun `getQuickContext returns snapshots for given entity IDs`() = runTest {
        val account = EntityEntry(
            entityId = "a-001",
            entityType = EntityType.ACCOUNT,
            displayName = "华为技术有限公司",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val contact = EntityEntry(
            entityId = "c-001",
            entityType = EntityType.CONTACT,
            displayName = "张伟",
            accountId = "a-001",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        
        entityRepository.save(account)
        entityRepository.save(contact)
        
        val result = hub.getQuickContext(listOf("a-001", "c-001"))
        
        assertEquals(2, result.entitySnapshots.size)
        assertNotNull(result.entitySnapshots["a-001"])
        assertNotNull(result.entitySnapshots["c-001"])
        assertEquals("华为技术有限公司", result.entitySnapshots["a-001"]?.displayName)
        assertEquals("张伟", result.entitySnapshots["c-001"]?.displayName)
        assertEquals(EntityType.ACCOUNT, result.entitySnapshots["a-001"]?.entityType)
    }
    
    @Test
    fun `getFocusedContext returns entity with related contacts and deals`() = runTest {
        val account = EntityEntry(
            entityId = "a-001",
            entityType = EntityType.ACCOUNT,
            displayName = "腾讯科技",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val contact1 = EntityEntry(
            entityId = "c-001",
            entityType = EntityType.CONTACT,
            displayName = "马化腾",
            accountId = "a-001",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val contact2 = EntityEntry(
            entityId = "c-002",
            entityType = EntityType.CONTACT,
            displayName = "张小龙",
            accountId = "a-001",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val deal = EntityEntry(
            entityId = "d-001",
            entityType = EntityType.DEAL,
            displayName = "微信企业版采购",
            accountId = "a-001",
            dealValue = 500000000,
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        
        entityRepository.save(account)
        entityRepository.save(contact1)
        entityRepository.save(contact2)
        entityRepository.save(deal)
        
        val result = hub.getFocusedContext("a-001")
        
        assertEquals("腾讯科技", result.entity.displayName)
        assertEquals(2, result.relatedContacts.size)
        assertEquals(1, result.relatedDeals.size)
        assertEquals("马化腾", result.relatedContacts[0].displayName)
        assertEquals("微信企业版采购", result.relatedDeals[0].displayName)
    }
    
    @Test
    fun `getByAccountId returns contacts and deals linked to account`() = runTest {
        val account = EntityEntry(
            entityId = "a-001",
            entityType = EntityType.ACCOUNT,
            displayName = "阿里巴巴",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val contact = EntityEntry(
            entityId = "c-001",
            entityType = EntityType.CONTACT,
            displayName = "马云",
            accountId = "a-001",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val deal = EntityEntry(
            entityId = "d-001",
            entityType = EntityType.DEAL,
            displayName = "阿里云服务采购",
            accountId = "a-001",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        
        entityRepository.save(account)
        entityRepository.save(contact)
        entityRepository.save(deal)
        
        val result = entityRepository.getByAccountId("a-001")
        
        assertEquals(2, result.size)
        assertEquals(setOf("c-001", "d-001"), result.map { it.entityId }.toSet())
    }
    
    // ─── Wave 2 Tests: Timeline Aggregation ─────────────────
    

    @Test
    fun `getByEntityId filters by quoted entity ID avoiding substring collision`() = runTest {
        // "c-1" 不应匹配 "c-10"
        memoryRepository.save(MemoryEntry(
            entryId = "mem-a",
            sessionId = "s-001",
            content = "关于c-10的讨论",
            entryType = MemoryEntryType.USER_MESSAGE,
            createdAt = 1000L,
            updatedAt = 1000L,
            structuredJson = """{"relatedEntityIds":["c-10"]}"""
        ))
        memoryRepository.save(MemoryEntry(
            entryId = "mem-b",
            sessionId = "s-001",
            content = "关于c-1的讨论",
            entryType = MemoryEntryType.USER_MESSAGE,
            createdAt = 2000L,
            updatedAt = 2000L,
            structuredJson = """{"relatedEntityIds":["c-1"]}"""
        ))
        
        val resultC1 = memoryRepository.getByEntityId("c-1")
        val resultC10 = memoryRepository.getByEntityId("c-10")
        
        assertEquals(1, resultC1.size)
        assertEquals("mem-b", resultC1[0].entryId)
        assertEquals(1, resultC10.size)
        assertEquals("mem-a", resultC10[0].entryId)
    }

}
