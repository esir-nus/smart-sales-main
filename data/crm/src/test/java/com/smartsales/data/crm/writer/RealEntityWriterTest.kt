package com.smartsales.data.crm.writer

import com.smartsales.core.test.fakes.FakeKernelWriteBack
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * RealEntityWriter 单元测试 — 覆盖 spec 全部 test cases
 *
 * 测试范围:
 * - Wave 1: upsertFromClue, registerAlias, updateAttribute, delete
 * - Wave 2: displayName latest-write-wins, write-through, updateProfile
 */
class RealEntityWriterTest {

    private lateinit var repo: FakeEntityRepository
    private lateinit var writeBack: FakeKernelWriteBack
    private lateinit var writer: RealEntityWriter
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repo = FakeEntityRepository()
        val timeProvider = FakeTimeProvider()
        writeBack = FakeKernelWriteBack()
        writer = RealEntityWriter(repo, timeProvider, writeBack, testScope)
    }

    // ========================================
    // Wave 1: upsertFromClue
    // ========================================

    @Test
    fun `new entity returns isNew true with clue as displayName`() = runTest {
        val result = writer.upsertFromClue(
            clue = "张总",
            resolvedId = null,
            type = EntityType.PERSON,
            source = "scheduler"
        )

        assertTrue(result.isNew)
        assertEquals("张总", result.displayName)
        assertTrue(result.entityId.startsWith("p-"))

        // 验证持久化
        val saved = repo.getById(result.entityId)
        assertNotNull(saved)
        assertEquals("张总", saved!!.displayName)
    }

    @Test
    fun `existing entity via resolvedId returns isNew false`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        val updated = writer.upsertFromClue(
            clue = "张总监",
            resolvedId = created.entityId,
            type = EntityType.PERSON,
            source = "coach"
        )

        assertFalse(updated.isNew)
        assertEquals(created.entityId, updated.entityId)
    }

    @Test
    fun `existing entity via alias match returns isNew false`() = runTest {
        writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        val result = writer.upsertFromClue("张总", null, EntityType.PERSON, "coach")

        assertFalse(result.isNew)
    }

    @Test
    fun `field preservation on upsert`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")
        val entityId = created.entityId

        // 手动设置 demeanorJson 和 metricsHistoryJson
        val existing = repo.getById(entityId)!!
        val withFields = existing.copy(
            demeanorJson = """{"style":"direct"}""",
            metricsHistoryJson = """{"calls":5}"""
        )
        repo.save(withFields)

        // 用 resolvedId 触发 upsert
        writer.upsertFromClue("张总监", entityId, EntityType.PERSON, "coach")

        // 验证字段保留
        val final_ = repo.getById(entityId)!!
        assertEquals("""{"style":"direct"}""", final_.demeanorJson)
        assertEquals("""{"calls":5}""", final_.metricsHistoryJson)
    }

    @Test
    fun `stale resolvedId falls back to alias dedup`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        val result = writer.upsertFromClue(
            clue = "张总",
            resolvedId = "nonexistent-id",
            type = EntityType.PERSON,
            source = "coach"
        )

        assertFalse(result.isNew)
        assertEquals(created.entityId, result.entityId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank clue throws IllegalArgumentException`() = runTest {
        writer.upsertFromClue("  ", null, EntityType.PERSON, "test")
    }

    // ========================================
    // Wave 1: registerAlias
    // ========================================

    @Test
    fun `alias cap at 8 with FIFO tail rotation`() = runTest {
        val created = writer.upsertFromClue("alias1", null, EntityType.PERSON, "test")
        val entityId = created.entityId

        for (i in 2..8) {
            writer.registerAlias(entityId, "alias$i")
        }

        val before = repo.getById(entityId)!!
        val aliasesBefore = JSONArray(before.aliasesJson)
        assertEquals(8, aliasesBefore.length())

        // 第 9 个 → 最旧的 alias1 被淘汰
        writer.registerAlias(entityId, "alias9")

        val after = repo.getById(entityId)!!
        val aliasesAfter = JSONArray(after.aliasesJson)
        assertEquals(8, aliasesAfter.length())

        val aliasList = (0 until aliasesAfter.length()).map { aliasesAfter.getString(it) }
        assertFalse("alias1 应被淘汰", aliasList.contains("alias1"))
        assertTrue("alias9 应存在", aliasList.contains("alias9"))
    }

    // ========================================
    // Wave 1: delete
    // ========================================

    @Test
    fun `delete removes entity`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")
        assertNotNull(repo.getById(created.entityId))

        writer.delete(created.entityId)
        assertNull(repo.getById(created.entityId))
    }

    @Test
    fun `delete nonexistent is no-op`() = runTest {
        writer.delete("does-not-exist")
    }

    // ========================================
    // Wave 2 → Wave 5: displayName Immutable (Canonical)
    // ========================================

    @Test
    fun `displayName is immutable during upsert`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        // 用新 clue 更新 — displayName 应保持不变
        val updated = writer.upsertFromClue(
            clue = "张总监",
            resolvedId = created.entityId,
            type = EntityType.PERSON,
            source = "coach"
        )

        // displayName 保持 canonical name 不变
        assertEquals("张总", updated.displayName)

        // 验证 SSD
        val saved = repo.getById(created.entityId)!!
        assertEquals("张总", saved.displayName)
    }

    @Test
    fun `aliasesJson unchanged by upsert - curated only`() = runTest {
        val created = writer.upsertFromClue("A", null, EntityType.PERSON, "s")
        writer.upsertFromClue("B", created.entityId, EntityType.PERSON, "s")
        writer.upsertFromClue("C", created.entityId, EntityType.PERSON, "s")

        val final_ = repo.getById(created.entityId)!!
        // displayName 保持原始值
        assertEquals("A", final_.displayName)

        // aliases 仅包含初始创建时的 ["A"]，不自动积累
        val aliases = JSONArray(final_.aliasesJson)
        val aliasList = (0 until aliases.length()).map { aliases.getString(it) }
        assertEquals("aliases 应仅含初始值", 1, aliasList.size)
        assertTrue("初始 clue 应在 aliases 中", aliasList.contains("A"))
        assertFalse("B 不应自动加入 aliases", aliasList.contains("B"))
        assertFalse("C 不应自动加入 aliases", aliasList.contains("C"))
    }

    // ========================================
    // Wave 2: updateProfile
    // ========================================

    @Test
    fun `updateProfile detects displayName change`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf("displayName" to "张总监")
        )

        assertEquals(1, result.changes.size)
        assertEquals("displayName", result.changes[0].field)
        assertEquals("张总", result.changes[0].oldValue)
        assertEquals("张总监", result.changes[0].newValue)

        // 验证 SSD 更新
        val saved = repo.getById(created.entityId)!!
        assertEquals("张总监", saved.displayName)
    }

    @Test
    fun `updateProfile with no changes returns empty list`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf("displayName" to "张总")  // 相同值
        )

        assertEquals(0, result.changes.size)
    }

    @Test
    fun `updateProfile updates multiple fields successfully`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        // 先设置 jobTitle
        writer.updateAttribute(created.entityId, "jobTitle", "销售经理")

        writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf(
                "displayName" to "张总监",
                "jobTitle" to "销售总监"
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateProfile throws for nonexistent entity`() = runTest {
        writer.updateProfile(
            entityId = "does-not-exist",
            updates = mapOf("displayName" to "test")
        )
    }

    @Test
    fun `updateProfile detects nextAction change`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf("nextAction" to "安排下周回访")
        )

        assertEquals(1, result.changes.size)
        assertEquals("nextAction", result.changes[0].field)
        assertNull(result.changes[0].oldValue) // 初始为空
        assertEquals("安排下周回访", result.changes[0].newValue)

        // 验证 SSD 持久化
        val saved = repo.getById(created.entityId)!!
        assertEquals("安排下周回访", saved.nextAction)
    }

    // ========================================
    // Wave 2: write-through (registerAlias, updateAttribute)
    // ========================================

    @Test
    fun `updateAttribute triggers write-through`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        writer.updateAttribute(created.entityId, "region", "华东")

        // 验证 SSD 持久化
        val saved = repo.getById(created.entityId)!!
        val attrs = JSONObject(saved.attributesJson)
        assertEquals("华东", attrs.getString("region"))
    }

    @Test
    fun `registerAlias dedup is case-insensitive`() = runTest {
        val created = writer.upsertFromClue("Zhang", null, EntityType.PERSON, "test")
        writer.registerAlias(created.entityId, "zhang")  // 大小写不同

        val saved = repo.getById(created.entityId)!!
        val aliases = JSONArray(saved.aliasesJson)
        // 不应重复
        assertEquals(1, aliases.length())
    }

    @Test
    fun `updateAttribute rejects internal keys`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        try {
            writer.updateAttribute(created.entityId, "_source", "hack")
            fail("应抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("内部保留"))
        }
    }
}
