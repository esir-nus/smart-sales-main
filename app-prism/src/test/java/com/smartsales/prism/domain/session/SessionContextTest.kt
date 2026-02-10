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
 * SessionContext 单元测试 — Wave 1 + Wave 2 Ship Criteria
 */
class SessionContextTest {

    private lateinit var sessionContext: SessionContext

    @Before
    fun setup() {
        sessionContext = SessionContext(
            sessionId = "test-session-001",
            createdAt = 1000L
        )
    }

    @Test
    fun `initializes with empty maps and zero turn count`() {
        assertTrue("entityStates should be empty", sessionContext.entityStates.isEmpty())
        assertTrue("pathIndex should be empty", sessionContext.pathIndex.isEmpty())
        assertEquals("turnCount should be 0", 0, sessionContext.turnCount)
        assertEquals("sessionId should match", "test-session-001", sessionContext.sessionId)
        assertEquals("createdAt should match", 1000L, sessionContext.createdAt)
    }

    @Test
    fun `incrementTurn increments count`() {
        sessionContext.incrementTurn()
        assertEquals(1, sessionContext.turnCount)

        sessionContext.incrementTurn()
        assertEquals(2, sessionContext.turnCount)

        sessionContext.incrementTurn()
        assertEquals(3, sessionContext.turnCount)
    }

    @Test
    fun `reset returns fresh SessionContext`() {
        sessionContext.incrementTurn()
        sessionContext.incrementTurn()
        sessionContext.entityStates["e-001"] = EntityTrace(
            entityId = "e-001",
            state = EntityState.ACTIVE,
            confidence = 0.9f
        )
        sessionContext.pathIndex["张总"] = "e-001"

        val newContext = sessionContext.reset(
            newSessionId = "test-session-002",
            nowMillis = 3000L
        )

        assertEquals("test-session-002", newContext.sessionId)
        assertEquals(3000L, newContext.createdAt)
        assertEquals(0, newContext.turnCount)
        assertTrue(newContext.entityStates.isEmpty())
        assertTrue(newContext.pathIndex.isEmpty())

        // 旧实例不受影响
        assertEquals(2, sessionContext.turnCount)
        assertEquals(1, sessionContext.entityStates.size)
    }

    @Test
    fun `entityStates can track entities`() {
        val trace = EntityTrace(
            entityId = "p-001",
            state = EntityState.MENTIONED,
            confidence = 0.8f
        )

        sessionContext.entityStates["p-001"] = trace

        assertEquals(EntityState.MENTIONED, sessionContext.entityStates["p-001"]?.state)
        assertEquals("p-001", sessionContext.entityStates["p-001"]?.entityId)
    }

    @Test
    fun `pathIndex stores alias to entityId mappings`() {
        sessionContext.pathIndex["张总"] = "p-001"
        sessionContext.pathIndex["李经理"] = "p-002"

        assertEquals("p-001", sessionContext.pathIndex["张总"])
        assertEquals("p-002", sessionContext.pathIndex["李经理"])
        assertEquals(2, sessionContext.pathIndex.size)
    }

    // ===== Wave 2: Path Index Tests =====

    @Test
    fun `resolveAlias returns cached entityId`() {
        sessionContext.cacheAlias("张总", "p-001")

        assertEquals("p-001", sessionContext.resolveAlias("张总"))
    }

    @Test
    fun `resolveAlias returns null for unknown alias`() {
        assertNull(sessionContext.resolveAlias("unknown"))
    }

    @Test
    fun `LRU eviction at 50 entries`() {
        // 填充 50 条
        for (i in 1..50) {
            sessionContext.cacheAlias("alias-$i", "entity-$i")
        }
        assertEquals(50, sessionContext.pathIndex.size)

        // 第 51 条应淘汰最早插入的 alias-1
        sessionContext.cacheAlias("alias-51", "entity-51")
        assertEquals(50, sessionContext.pathIndex.size)
        assertNull("alias-1 should be evicted", sessionContext.resolveAlias("alias-1"))
        assertEquals("entity-51", sessionContext.resolveAlias("alias-51"))

        // alias-2 应该还在
        assertEquals("entity-2", sessionContext.resolveAlias("alias-2"))
    }

    @Test
    fun `cacheAlias overwrites existing without eviction`() {
        sessionContext.cacheAlias("张总", "p-001")
        sessionContext.cacheAlias("张总", "p-002")

        assertEquals("p-002", sessionContext.resolveAlias("张总"))
        assertEquals(1, sessionContext.pathIndex.size)
    }

    // ===== Wave 3: Smart Triggers Tests =====

    @Test
    fun `shouldLoadData returns false for ACTIVE entity`() {
        sessionContext.markActive("e-001")
        
        assertEquals(false, sessionContext.shouldLoadData("e-001"))
    }

    @Test
    fun `shouldLoadData returns true for MENTIONED entity`() {
        sessionContext.entityStates["e-002"] = EntityTrace(
            entityId = "e-002",
            state = EntityState.MENTIONED,
            confidence = 0.9f
        )
        
        assertEquals(true, sessionContext.shouldLoadData("e-002"))
    }

    @Test
    fun `shouldLoadData returns true for unknown entityId`() {
        // 无记录的实体应该加载数据
        assertEquals(true, sessionContext.shouldLoadData("e-999"))
    }

    @Test
    fun `shouldLoadData returns false for UNKNOWN state`() {
        sessionContext.entityStates["e-003"] = EntityTrace(
            entityId = "e-003",
            state = EntityState.UNKNOWN,
            confidence = 0.5f
        )
        
        // UNKNOWN 表示未解析到 ID，无法加载数据
        assertEquals(false, sessionContext.shouldLoadData("e-003"))
    }

    @Test
    fun `markActive transitions entity to ACTIVE`() {
        sessionContext.markActive("e-004", confidence = 0.95f)
        
        val trace = sessionContext.entityStates["e-004"]
        assertEquals(EntityState.ACTIVE, trace?.state)
        assertEquals("e-004", trace?.entityId)
        assertEquals(0.95f, trace?.confidence)
    }

    // ===== Wave 4: SessionWorkingSet (3-Section) Tests =====

    @Test
    fun `initializes with null habit sections`() {
        assertNull("Section 2 (userHabitContext) should start null", sessionContext.userHabitContext)
        assertNull("Section 3 (clientHabitContext) should start null", sessionContext.clientHabitContext)
        assertTrue("Section 1 (memoryHits) should start empty", sessionContext.memoryHits.isEmpty())
        assertTrue("Section 1 (entityContext) should start empty", sessionContext.entityContext.isEmpty())
    }

    @Test
    fun `getCombinedHabitContext returns null when both sections null`() {
        assertNull(sessionContext.getCombinedHabitContext())
    }

    @Test
    fun `getCombinedHabitContext returns user-only when no client habits`() {
        sessionContext.userHabitContext = HabitContext(
            userHabits = listOf(UserHabit(habitKey = "meeting_time", habitValue = "morning", entityId = null, lastObservedAt = 1000L, createdAt = 1000L)),
            clientHabits = emptyList(),
            suggestedDefaults = mapOf("time" to "09:00")
        )

        val combined = sessionContext.getCombinedHabitContext()
        assertNotNull(combined)
        assertEquals(1, combined!!.userHabits.size)
        assertEquals("meeting_time", combined.userHabits[0].habitKey)
        assertTrue("clientHabits should be empty", combined.clientHabits.isEmpty())
        assertEquals("09:00", combined.suggestedDefaults["time"])
    }

    @Test
    fun `getCombinedHabitContext returns client-only when no user habits`() {
        sessionContext.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = listOf(UserHabit(habitKey = "preferred_channel", habitValue = "wechat", entityId = "c-001", lastObservedAt = 1000L, createdAt = 1000L)),
            suggestedDefaults = emptyMap()
        )

        val combined = sessionContext.getCombinedHabitContext()
        assertNotNull(combined)
        assertTrue("userHabits should be empty", combined!!.userHabits.isEmpty())
        assertEquals(1, combined.clientHabits.size)
        assertEquals("preferred_channel", combined.clientHabits[0].habitKey)
    }

    @Test
    fun `getCombinedHabitContext merges user and client habits`() {
        sessionContext.userHabitContext = HabitContext(
            userHabits = listOf(UserHabit(habitKey = "meeting_time", habitValue = "morning", entityId = null, lastObservedAt = 1000L, createdAt = 1000L)),
            clientHabits = emptyList(),
            suggestedDefaults = mapOf("time" to "09:00")
        )
        sessionContext.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = listOf(UserHabit(habitKey = "preferred_channel", habitValue = "wechat", entityId = "c-001", lastObservedAt = 1000L, createdAt = 1000L)),
            suggestedDefaults = mapOf("channel" to "wechat")
        )

        val combined = sessionContext.getCombinedHabitContext()
        assertNotNull(combined)
        assertEquals("userHabits from Section 2", 1, combined!!.userHabits.size)
        assertEquals("clientHabits from Section 3", 1, combined.clientHabits.size)
        assertEquals("suggestedDefaults merged", 2, combined.suggestedDefaults.size)
        assertEquals("09:00", combined.suggestedDefaults["time"])
        assertEquals("wechat", combined.suggestedDefaults["channel"])
    }

    @Test
    fun `memoryHits stored on working set`() {
        val hits = listOf(
            MemoryHit(entryId = "m-001", content = "test memory", relevanceScore = 0.9f)
        )
        sessionContext.memoryHits = hits

        assertEquals(1, sessionContext.memoryHits.size)
        assertEquals("m-001", sessionContext.memoryHits[0].entryId)
    }

    @Test
    fun `entityContext stored on working set`() {
        sessionContext.entityContext["person_0"] = EntityRef(
            entityId = "p-001",
            displayName = "张总",
            entityType = "PERSON"
        )

        assertEquals(1, sessionContext.entityContext.size)
        assertEquals("p-001", sessionContext.entityContext["person_0"]?.entityId)
    }

    @Test
    fun `reset clears all sections`() {
        // Populate all sections
        sessionContext.userHabitContext = HabitContext(
            userHabits = listOf(UserHabit(habitKey = "k", habitValue = "v", entityId = null, lastObservedAt = 1000L, createdAt = 1000L)),
            clientHabits = emptyList(),
            suggestedDefaults = emptyMap()
        )
        sessionContext.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = listOf(UserHabit(habitKey = "k2", habitValue = "v2", entityId = "c-001", lastObservedAt = 1000L, createdAt = 1000L)),
            suggestedDefaults = emptyMap()
        )
        sessionContext.memoryHits = listOf(
            MemoryHit(entryId = "m-001", content = "test", relevanceScore = 1.0f)
        )
        sessionContext.entityContext["p_0"] = EntityRef("p-001", "Test", "PERSON")

        // Reset
        val fresh = sessionContext.reset("new-session", 9999L)

        // All sections should be clean
        assertNull(fresh.userHabitContext)
        assertNull(fresh.clientHabitContext)
        assertTrue(fresh.memoryHits.isEmpty())
        assertTrue(fresh.entityContext.isEmpty())
        assertTrue(fresh.entityStates.isEmpty())
        assertTrue(fresh.pathIndex.isEmpty())
    }

    @Test
    fun `typealias SessionWorkingSet equals SessionContext`() {
        // 验证 typealias 可以正常使用
        val ws: SessionWorkingSet = SessionContext(sessionId = "alias-test", createdAt = 0L)
        assertEquals("alias-test", ws.sessionId)
    }
}
