package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * FakeMemoryRepository 单元测试
 * 验证 Lazy Compaction 逻辑（分层读取）
 */
class FakeMemoryRepositoryTest {

    private lateinit var repo: FakeMemoryRepository
    private val sessionId = "test-session"
    
    // 固定时间点用于测试
    private val now = 1704067200000L // 2024-01-01 00:00:00 UTC
    
    @Before
    fun setup() {
        repo = FakeMemoryRepository()
        repo.currentTimeProvider = { now }
    }
    
    // Helper: 创建测试条目
    private fun createEntry(
        id: String,
        daysAgo: Int,
        isArchived: Boolean = false,
        hasScheduledAt: Boolean = true
    ): MemoryEntry {
        val scheduledAt = if (hasScheduledAt) {
            now - TimeUnit.DAYS.toMillis(daysAgo.toLong())
        } else null
        
        return MemoryEntry(
            entryId = id,
            sessionId = sessionId,
            content = "Test entry $id",
            entryType = MemoryEntryType.SCHEDULE_ITEM,
            createdAt = now,
            updatedAt = now,
            isArchived = isArchived,
            scheduledAt = scheduledAt
        )
    }
    
    @Test
    fun `getActiveEntries respects FREE tier 7-day window`() = runTest {
        // Setup: entries at 5, 10, 20 days ago (all archived)
        repo.save(createEntry("e1", daysAgo = 5, isArchived = true))
        repo.save(createEntry("e2", daysAgo = 10, isArchived = true))
        repo.save(createEntry("e3", daysAgo = 20, isArchived = true))
        
        val results = repo.getActiveEntries(sessionId, SubscriptionTier.FREE)
        
        // FREE = 7 days, only 5-day-old entry is within window
        assertEquals(1, results.size)
        assertEquals("e1", results[0].entryId)
    }
    
    @Test
    fun `getActiveEntries respects PRO tier 14-day window`() = runTest {
        // Setup: entries at 5, 10, 20 days ago (all archived)
        repo.save(createEntry("e1", daysAgo = 5, isArchived = true))
        repo.save(createEntry("e2", daysAgo = 10, isArchived = true))
        repo.save(createEntry("e3", daysAgo = 20, isArchived = true))
        
        val results = repo.getActiveEntries(sessionId, SubscriptionTier.PRO)
        
        // PRO = 14 days, 5 and 10-day-old entries are within window
        assertEquals(2, results.size)
    }
    
    @Test
    fun `getActiveEntries respects ENTERPRISE tier 30-day window`() = runTest {
        // Setup: entries at 5, 10, 20 days ago (all archived)
        repo.save(createEntry("e1", daysAgo = 5, isArchived = true))
        repo.save(createEntry("e2", daysAgo = 10, isArchived = true))
        repo.save(createEntry("e3", daysAgo = 20, isArchived = true))
        
        val results = repo.getActiveEntries(sessionId, SubscriptionTier.ENTERPRISE)
        
        // ENTERPRISE = 30 days, all entries are within window
        assertEquals(3, results.size)
    }
    
    @Test
    fun `unarchived entries always appear in Active zone regardless of age`() = runTest {
        // 100-day-old entry that's NOT archived
        repo.save(createEntry("e1", daysAgo = 100, isArchived = false))
        
        // Even FREE tier (7 days) should see this entry because it's not archived
        val results = repo.getActiveEntries(sessionId, SubscriptionTier.FREE)
        
        assertEquals(1, results.size)
        assertEquals("e1", results[0].entryId)
    }
    
    @Test
    fun `getArchivedEntries returns archived entries outside window`() = runTest {
        // Setup: mix of archived and non-archived
        repo.save(createEntry("e1", daysAgo = 5, isArchived = true))   // In Active (within 7d)
        repo.save(createEntry("e2", daysAgo = 10, isArchived = true))  // In Archived for FREE
        repo.save(createEntry("e3", daysAgo = 20, isArchived = false)) // Always Active (not archived)
        
        val archived = repo.getArchivedEntries(sessionId, SubscriptionTier.FREE)
        
        // FREE = 7 days, e2 (10d ago) is archived + outside window = Archived
        assertEquals(1, archived.size)
        assertEquals("e2", archived[0].entryId)
    }
    
    @Test
    fun `markAsArchived action moves entry from Active to Archived for old entries`() = runTest {
        // Entry 20 days old, not archived
        repo.save(createEntry("e1", daysAgo = 20, isArchived = false))
        
        // Before archive: in Active (not archived)
        val activeBefore = repo.getActiveEntries(sessionId, SubscriptionTier.FREE)
        assertEquals(1, activeBefore.size)
        
        // Archive it
        repo.markAsArchived("e1")
        
        // After archive: in Archived (archived + outside 7d window)
        val activeAfter = repo.getActiveEntries(sessionId, SubscriptionTier.FREE)
        val archivedAfter = repo.getArchivedEntries(sessionId, SubscriptionTier.FREE)
        
        assertEquals(0, activeAfter.size)
        assertEquals(1, archivedAfter.size)
    }
    
    @Test
    fun `markAsArchived recent entry keeps it in Active zone`() = runTest {
        // Entry 3 days old
        repo.save(createEntry("e1", daysAgo = 3, isArchived = false))
        
        // Archive it
        repo.markAsArchived("e1")
        
        // Still in Active for FREE (archived but within 7d window)
        val active = repo.getActiveEntries(sessionId, SubscriptionTier.FREE)
        assertEquals(1, active.size)
        
        // Not in Archived (within window)
        val archived = repo.getArchivedEntries(sessionId, SubscriptionTier.FREE)
        assertEquals(0, archived.size)
    }
}
