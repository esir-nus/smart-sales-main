package com.smartsales.prism.domain.session

import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.rl.HabitContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SessionWorkingSet 单元测试
 */
class SessionWorkingSetTest {

    private lateinit var workingSet: SessionWorkingSet

    @Before
    fun setup() {
        workingSet = SessionWorkingSet(
            sessionId = "test-session-001",
            createdAt = 1000L
        )
    }

    @Test
    fun `initializes with empty maps`() {
        assertTrue("entityStates should be empty", workingSet.entityStates.isEmpty())
        assertTrue("pathIndex should be empty", workingSet.pathIndex.isEmpty())
        assertTrue("entityContext should be empty", workingSet.entityContext.isEmpty())
        assertNull("entityKnowledge should be null", workingSet.entityKnowledge)
        assertNull("userHabitContext should be null", workingSet.userHabitContext)
        assertNull("clientHabitContext should be null", workingSet.clientHabitContext)
        assertEquals("sessionId should match", "test-session-001", workingSet.sessionId)
        assertEquals("createdAt should match", 1000L, workingSet.createdAt)
    }

    @Test
    fun `entityStates can track entities`() {
        val trace = EntityTrace(
            entityId = "p-001",
            state = EntityState.MENTIONED,
            confidence = 0.8f
        )

        workingSet.entityStates["p-001"] = trace

        assertEquals(EntityState.MENTIONED, workingSet.entityStates["p-001"]?.state)
        assertEquals("p-001", workingSet.entityStates["p-001"]?.entityId)
    }

    @Test
    fun `pathIndex stores alias to entityId mappings`() {
        workingSet.pathIndex["张总"] = "p-001"
        workingSet.pathIndex["李经理"] = "p-002"

        assertEquals("p-001", workingSet.pathIndex["张总"])
        assertEquals("p-002", workingSet.pathIndex["李经理"])
        assertEquals(2, workingSet.pathIndex.size)
    }

    // ===== Section 1: Path Index Tests =====

    @Test
    fun `resolveAlias returns cached entityId`() {
        workingSet.cacheAlias("张总", "p-001")

        assertEquals("p-001", workingSet.resolveAlias("张总"))
    }

    @Test
    fun `resolveAlias returns null for unknown alias`() {
        assertNull(workingSet.resolveAlias("unknown"))
    }

    @Test
    fun `LRU eviction at 50 entries`() {
        // 填充 50 条
        for (i in 1..50) {
            workingSet.cacheAlias("alias-$i", "entity-$i")
        }
        assertEquals(50, workingSet.pathIndex.size)

        // 第 51 条应淘汰最早插入的 alias-1
        workingSet.cacheAlias("alias-51", "entity-51")
        assertEquals(50, workingSet.pathIndex.size)
        assertNull("alias-1 should be evicted", workingSet.resolveAlias("alias-1"))
        assertEquals("entity-51", workingSet.resolveAlias("alias-51"))

        // alias-2 应该还在
        assertEquals("entity-2", workingSet.resolveAlias("alias-2"))
    }

    @Test
    fun `cacheAlias overwrites existing without eviction`() {
        workingSet.cacheAlias("张总", "p-001")
        workingSet.cacheAlias("张总", "p-002")

        assertEquals("p-002", workingSet.resolveAlias("张总"))
        assertEquals(1, workingSet.pathIndex.size)
    }

    // ===== Section 1: Memory & Knowledge Tests =====

    @Test
    fun `shouldLoadData logic correctness`() {
        // 1. 无记录 → true
        assertTrue(workingSet.shouldLoadData("unknown-id"))
        
        // 2. MENTIONED → true
        workingSet.entityStates["e-001"] = EntityTrace("e-001", EntityState.MENTIONED, 1.0f)
        assertTrue(workingSet.shouldLoadData("e-001"))
        
        // 3. ACTIVE → false
        workingSet.markActive("e-002")
        // Manually assert state transition
        assertEquals(EntityState.ACTIVE, workingSet.entityStates["e-002"]?.state)
        // Check logic
        assertEquals(false, workingSet.shouldLoadData("e-002"))
        
        // 4. UNKNOWN → false
        workingSet.entityStates["e-003"] = EntityTrace("e-003", EntityState.UNKNOWN, 0.0f)
        assertEquals(false, workingSet.shouldLoadData("e-003"))
    }

    @Test
    fun `memoryHits stored on working set`() {
        val hits = listOf(
            MemoryHit(entryId = "m-001", content = "test memory", relevanceScore = 0.9f)
        )
        workingSet.memoryHits = hits

        assertEquals(1, workingSet.memoryHits.size)
        assertEquals("m-001", workingSet.memoryHits[0].entryId)
    }

    @Test
    fun `entityContext stored on working set`() {
        workingSet.entityContext["person_0"] = EntityRef(
            entityId = "p-001",
            displayName = "张总",
            entityType = "PERSON"
        )

        assertEquals(1, workingSet.entityContext.size)
        assertEquals("p-001", workingSet.entityContext["person_0"]?.entityId)
    }

    // ===== Section 2 + 3: Habit Tests =====

    @Test
    fun `getCombinedHabitContext merges sections`() {
        // Section 2
        workingSet.userHabitContext = HabitContext(
            userHabits = listOf(UserHabit(habitKey = "global", habitValue = "v1", entityId = null, lastObservedAt = 1000L, createdAt = 1000L)),
            clientHabits = emptyList(),
            suggestedDefaults = mapOf("def1" to "val1")
        )
        // Section 3
        workingSet.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = listOf(UserHabit(habitKey = "client", habitValue = "v2", entityId = "c-001", lastObservedAt = 1000L, createdAt = 1000L)),
            suggestedDefaults = mapOf("def2" to "val2")
        )

        val combined = workingSet.getCombinedHabitContext()
        assertNotNull(combined)
        assertEquals(1, combined!!.userHabits.size)
        assertEquals("global", combined.userHabits[0].habitKey)
        assertEquals(1, combined.clientHabits.size)
        assertEquals("client", combined.clientHabits[0].habitKey)
        assertEquals(2, combined.suggestedDefaults.size)
    }
}
