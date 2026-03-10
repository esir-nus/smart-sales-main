package com.smartsales.data.crm.writer

import com.smartsales.core.test.fakes.FakeKernelWriteBack
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Break-It Examiner — 边界和负向测试
 *
 * 尝试破坏 EntityWriter：空值、特殊字符、并发、极端输入
 */
class EntityWriterBreakItTest {

    private lateinit var repo: FakeEntityRepository
    private lateinit var writer: RealEntityWriter
    private lateinit var writeBack: FakeKernelWriteBack

    @Before
    fun setup() {
        repo = FakeEntityRepository()
        val timeProvider = FakeTimeProvider()
        writeBack = FakeKernelWriteBack()
        writer = RealEntityWriter(repo, timeProvider, writeBack)
    }

    // --- Null / Empty / Blank ---

    @Test(expected = IllegalArgumentException::class)
    fun `upsert with empty string throws`() = runTest {
        writer.upsertFromClue("", null, EntityType.PERSON, "test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsert with whitespace-only throws`() = runTest {
        writer.upsertFromClue("   \t\n  ", null, EntityType.PERSON, "test")
    }

    @Test
    fun `updateAttribute on nonexistent entity is no-op`() = runTest {
        // 不应抛异常
        writer.updateAttribute("does-not-exist", "key", "value")
    }

    @Test
    fun `registerAlias on nonexistent entity is no-op`() = runTest {
        // 不应抛异常
        writer.registerAlias("does-not-exist", "alias")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateProfile on nonexistent entity throws`() = runTest {
        writer.updateProfile("does-not-exist", mapOf("displayName" to "test"))
    }

    // --- Special Characters / Emoji ---

    @Test
    fun `upsert with emoji in clue works`() = runTest {
        val result = writer.upsertFromClue("张总 🎯", null, EntityType.PERSON, "test")
        assertTrue(result.isNew)
        assertEquals("张总 🎯", result.displayName)

        val saved = repo.getById(result.entityId)!!
        assertEquals("张总 🎯", saved.displayName)
    }

    @Test
    fun `upsert with JSON-breaking chars in clue works`() = runTest {
        val result = writer.upsertFromClue("""张"总\n""", null, EntityType.PERSON, "test")
        assertTrue(result.isNew)

        val saved = repo.getById(result.entityId)!!
        assertNotNull(saved)
        // aliasesJson should be valid JSON
        val aliases = JSONArray(saved.aliasesJson)
        assertTrue(aliases.length() > 0)
    }

    @Test
    fun `updateAttribute with empty value works`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")
        // 空值不应崩溃
        writer.updateAttribute(created.entityId, "notes", "")

        val saved = repo.getById(created.entityId)!!
        assertNotNull(saved)
    }

    // --- Internal Key Guard ---

    @Test(expected = IllegalArgumentException::class)
    fun `updateAttribute rejects _source key`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")
        writer.updateAttribute(created.entityId, "_source", "hacked")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateAttribute rejects _first_seen key`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")
        writer.updateAttribute(created.entityId, "_first_seen", "0")
    }

    // --- updateProfile edge cases ---

    @Test
    fun `updateProfile with null values skips those fields`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf(
                "displayName" to null,  // null = 不更新
                "jobTitle" to "销售总监"
            )
        )

        // displayName 不应被更新
        val saved = repo.getById(created.entityId)!!
        assertEquals("张总", saved.displayName)

        // jobTitle 应被更新
        assertEquals(1, result.changes.size)
        assertEquals("jobTitle", result.changes[0].field)
    }

    @Test
    fun `updateProfile with unknown field is silently skipped`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = mapOf("unknownField" to "value")
        )

        assertEquals(0, result.changes.size)
    }

    @Test
    fun `updateProfile with empty map returns empty changes`() = runTest {
        val created = writer.upsertFromClue("张总", null, EntityType.PERSON, "test")

        val result = writer.updateProfile(
            entityId = created.entityId,
            updates = emptyMap()
        )

        assertEquals(0, result.changes.size)
    }

    // --- Rapid successive writes ---

    @Test
    fun `rapid displayName updates preserve all old names in aliases`() = runTest {
        val created = writer.upsertFromClue("V1", null, EntityType.PERSON, "test")
        val id = created.entityId

        // 快速连续更新 displayName
        for (i in 2..6) {
            writer.updateProfile(id, mapOf("displayName" to "V$i"))
        }

        val saved = repo.getById(id)!!
        assertEquals("V6", saved.displayName)

        val aliases = JSONArray(saved.aliasesJson)
        val aliasList = (0 until aliases.length()).map { aliases.getString(it) }

        // V1-V5 should all be in aliases
        for (i in 1..5) {
            assertTrue("V$i should be in aliases", aliasList.contains("V$i"))
        }
    }

    // --- Entity ID prefix ---

    @Test
    fun `entity ID prefixes match type`() = runTest {
        val person = writer.upsertFromClue("人", null, EntityType.PERSON, "test")
        assertTrue("PERSON prefix should be p-", person.entityId.startsWith("p-"))

        val account = writer.upsertFromClue("公司", null, EntityType.ACCOUNT, "test")
        assertTrue("ACCOUNT prefix should be a-", account.entityId.startsWith("a-"))

        val deal = writer.upsertFromClue("订单", null, EntityType.DEAL, "test")
        assertTrue("DEAL prefix should be d-", deal.entityId.startsWith("d-"))
    }
}
