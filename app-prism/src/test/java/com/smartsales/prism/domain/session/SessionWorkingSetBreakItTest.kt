package com.smartsales.prism.domain.session

import org.junit.Assert.*
import org.junit.Test
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.rl.HabitContext

/**
 * Break-It Examiner: Edge-case verification for SessionWorkingSet
 */
class SessionWorkingSetBreakItTest {

    @Test
    fun `getCombinedHabitContext with both null returns null`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        assertNull(ctx.getCombinedHabitContext())
    }

    @Test
    fun `overwrite memoryHits replaces previous`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        ctx.memoryHits = listOf(MemoryHit("a", "old", 1.0f))
        ctx.memoryHits = listOf(MemoryHit("b", "new", 0.5f))
        assertEquals(1, ctx.memoryHits.size)
        assertEquals("b", ctx.memoryHits[0].entryId)
    }

    @Test
    fun `entityContext handles duplicate keys`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        ctx.entityContext["key"] = EntityRef("p-001", "Alice", "PERSON")
        ctx.entityContext["key"] = EntityRef("p-002", "Bob", "PERSON")
        assertEquals(1, ctx.entityContext.size)
        assertEquals("p-002", ctx.entityContext["key"]?.entityId)
    }

    @Test
    fun `reset does not affect original instance sections`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        ctx.userHabitContext = HabitContext(emptyList(), emptyList(), emptyMap())
        ctx.memoryHits = listOf(MemoryHit("x", "data", 1.0f))

        val fresh = ctx.reset("new", 9999L)

        // Fresh is clean
        assertNull(fresh.userHabitContext)
        assertTrue(fresh.memoryHits.isEmpty())

        // Original is NOT affected
        assertNotNull(ctx.userHabitContext)
        assertEquals(1, ctx.memoryHits.size)
    }

    @Test
    fun `getCombinedHabitContext with empty lists merges correctly`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        ctx.userHabitContext = HabitContext(emptyList(), emptyList(), emptyMap())
        ctx.clientHabitContext = HabitContext(emptyList(), emptyList(), emptyMap())

        val combined = ctx.getCombinedHabitContext()
        assertNotNull(combined)
        assertTrue(combined!!.userHabits.isEmpty())
        assertTrue(combined.clientHabits.isEmpty())
        assertTrue(combined.suggestedDefaults.isEmpty())
    }

    @Test
    fun `suggestedDefaults client overrides user on key collision`() {
        val ctx = SessionContext(sessionId = "break-it", createdAt = 0L)
        ctx.userHabitContext = HabitContext(emptyList(), emptyList(), mapOf("key" to "user_val"))
        ctx.clientHabitContext = HabitContext(emptyList(), emptyList(), mapOf("key" to "client_val"))

        val combined = ctx.getCombinedHabitContext()
        // client (Section 3) should override user (Section 2) on collision
        assertEquals("client_val", combined!!.suggestedDefaults["key"])
    }
}
