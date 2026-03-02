package com.smartsales.prism.domain.session

import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.rl.HabitContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Break-It Tests — 边界条件和破坏性测试
 *
 * 目标：尝试通过边界输入破坏 SessionWorkingSet
 */
class SessionWorkingSetBreakItTest {

    private lateinit var ws: SessionWorkingSet

    @Before
    fun setup() {
        ws = SessionWorkingSet(sessionId = "break-it-session", createdAt = 0L)
    }

    // === Empty / Blank Input ===

    @Test
    fun `cacheAlias with empty string alias`() {
        ws.cacheAlias("", "entity-1")
        assertEquals("entity-1", ws.resolveAlias(""))
        assertEquals(1, ws.pathIndex.size)
    }

    @Test
    fun `cacheAlias with blank string alias`() {
        ws.cacheAlias("   ", "entity-1")
        assertEquals("entity-1", ws.resolveAlias("   "))
    }

    @Test
    fun `cacheAlias with empty entityId`() {
        ws.cacheAlias("alias", "")
        assertEquals("", ws.resolveAlias("alias"))
    }

    @Test
    fun `resolveAlias with empty string returns null when not cached`() {
        assertNull(ws.resolveAlias(""))
    }

    // === Emoji / Special Characters ===

    @Test
    fun `cacheAlias with emoji alias`() {
        ws.cacheAlias("🧑‍💼", "entity-emoji")
        assertEquals("entity-emoji", ws.resolveAlias("🧑‍💼"))
    }

    @Test
    fun `cacheAlias with Chinese characters`() {
        ws.cacheAlias("张总经理（副）", "p-001")
        assertEquals("p-001", ws.resolveAlias("张总经理（副）"))
    }

    @Test
    fun `cacheAlias with newline in alias`() {
        ws.cacheAlias("line1\nline2", "entity-nl")
        assertEquals("entity-nl", ws.resolveAlias("line1\nline2"))
    }

    // === Boundary: Eviction at MAX_PATH_INDEX_SIZE ===

    @Test
    fun `exact boundary at MAX_PATH_INDEX_SIZE`() {
        // Fill exactly to max
        for (i in 1..SessionWorkingSet.MAX_PATH_INDEX_SIZE) {
            ws.cacheAlias("a-$i", "e-$i")
        }
        assertEquals(SessionWorkingSet.MAX_PATH_INDEX_SIZE, ws.pathIndex.size)

        // Verify oldest exists
        assertEquals("e-1", ws.resolveAlias("a-1"))

        // One more triggers eviction of a-1
        ws.cacheAlias("overflow", "e-overflow")
        assertEquals(SessionWorkingSet.MAX_PATH_INDEX_SIZE, ws.pathIndex.size)
        assertNull("a-1 should be evicted", ws.resolveAlias("a-1"))
        assertEquals("e-overflow", ws.resolveAlias("overflow"))
        // a-2 still present
        assertEquals("e-2", ws.resolveAlias("a-2"))
    }

    @Test
    fun `overwriting existing alias does NOT evict at capacity`() {
        // Fill to max
        for (i in 1..SessionWorkingSet.MAX_PATH_INDEX_SIZE) {
            ws.cacheAlias("a-$i", "e-$i")
        }
        // Overwrite existing (not a new key → no eviction)
        ws.cacheAlias("a-1", "e-NEW")
        assertEquals(SessionWorkingSet.MAX_PATH_INDEX_SIZE, ws.pathIndex.size)
        assertEquals("e-NEW", ws.resolveAlias("a-1"))
        // a-2 still present (no eviction happened)
        assertEquals("e-2", ws.resolveAlias("a-2"))
    }

    // === shouldLoadData edge cases ===

    @Test
    fun `shouldLoadData with empty string entityId`() {
        // No trace exists → should load
        assertTrue(ws.shouldLoadData(""))
    }

    @Test
    fun `markActive then shouldLoadData returns false`() {
        ws.markActive("")
        assertFalse(ws.shouldLoadData(""))
    }

    // === getCombinedHabitContext edge cases ===

    @Test
    fun `getCombinedHabitContext with both empty habit lists`() {
        ws.userHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = emptyList(),
            suggestedDefaults = emptyMap()
        )
        ws.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = emptyList(),
            suggestedDefaults = emptyMap()
        )
        val combined = ws.getCombinedHabitContext()
        assertNotNull(combined)
        assertTrue(combined!!.userHabits.isEmpty())
        assertTrue(combined.clientHabits.isEmpty())
        assertTrue(combined.suggestedDefaults.isEmpty())
    }

    @Test
    fun `getCombinedHabitContext with overlapping suggestedDefaults keys`() {
        // Section 3 (client) should overwrite Section 2 (user) for same key
        ws.userHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = emptyList(),
            suggestedDefaults = mapOf("time" to "09:00")
        )
        ws.clientHabitContext = HabitContext(
            userHabits = emptyList(),
            clientHabits = emptyList(),
            suggestedDefaults = mapOf("time" to "14:00")
        )
        val combined = ws.getCombinedHabitContext()
        // user + client merged: client overwrites user due to + operator order
        assertEquals("14:00", combined!!.suggestedDefaults["time"])
    }

    // === entityContext edge cases ===

    @Test
    fun `entityContext with duplicate keys overwrites`() {
        ws.entityContext["key"] = EntityRef("e-1", "Name1", "PERSON")
        ws.entityContext["key"] = EntityRef("e-2", "Name2", "PERSON")
        assertEquals(1, ws.entityContext.size)
        assertEquals("e-2", ws.entityContext["key"]?.entityId)
    }

}
