package com.smartsales.prism.data.real

import com.smartsales.prism.data.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * RealEntityWriter 单元测试 — 使用 FakeEntityRepository 做存储
 */
class RealEntityWriterTest {

    private lateinit var repo: FakeEntityRepository
    private lateinit var writer: RealEntityWriter

    @Before
    fun setup() {
        repo = FakeEntityRepository()
        writer = RealEntityWriter(repo, FakeTimeProvider())
    }

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
        // 先创建
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        // 再用 resolvedId 更新
        val updated = writer.upsertFromClue(
            clue = "张总监",
            resolvedId = created.entityId,
            type = EntityType.PERSON,
            source = "coach"
        )

        assertFalse(updated.isNew)
        assertEquals(created.entityId, updated.entityId)
        // displayName first-write-wins: 保留 "张总"
        assertEquals("张总", updated.displayName)
    }

    @Test
    fun `existing entity via alias match returns isNew false`() = runTest {
        // 创建实体
        writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        // 用相同 clue，无 resolvedId → 别名匹配
        val result = writer.upsertFromClue("张总", null, EntityType.PERSON, "coach")

        assertFalse(result.isNew)
        assertEquals("张总", result.displayName)
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
    fun `alias cap at 8 with FIFO tail rotation`() = runTest {
        val created = writer.upsertFromClue("alias1", null, EntityType.PERSON, "test")
        val entityId = created.entityId

        // 注册到 8 个别名
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

        // alias1 应该被 FIFO 淘汰
        val aliasList = (0 until aliasesAfter.length()).map { aliasesAfter.getString(it) }
        assertFalse("alias1 应被淘汰", aliasList.contains("alias1"))
        assertTrue("alias9 应存在", aliasList.contains("alias9"))
    }

    @Test
    fun `stale resolvedId falls back to alias dedup`() = runTest {
        // 创建实体
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "scheduler")

        // 用一个不存在的 resolvedId
        val result = writer.upsertFromClue(
            clue = "张总",
            resolvedId = "nonexistent-id",
            type = EntityType.PERSON,
            source = "coach"
        )

        // 应通过别名匹配到已有实体
        assertFalse(result.isNew)
        assertEquals(created.entityId, result.entityId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank clue throws IllegalArgumentException`() = runTest {
        writer.upsertFromClue("  ", null, EntityType.PERSON, "test")
    }

    @Test
    fun `delete removes entity`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")
        assertNotNull(repo.getById(created.entityId))

        writer.delete(created.entityId)
        assertNull(repo.getById(created.entityId))
    }

    @Test
    fun `delete nonexistent is no-op`() = runTest {
        // 不应抛异常
        writer.delete("does-not-exist")
    }
}
