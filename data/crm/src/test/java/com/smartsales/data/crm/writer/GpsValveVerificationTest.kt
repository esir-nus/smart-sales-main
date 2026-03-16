package com.smartsales.data.crm.writer

import com.smartsales.core.test.fakes.FakeKernelWriteBack
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.telemetry.PipelineValve

/**
 * GPS Valve Verification Test — 强制每条 PipelineValve 路径触发
 *
 * 目的: 逐个验证 RealEntityWriter 中所有 6 个 DB_WRITE_EXECUTED 阀门实际触发。
 * 方法: 拦截 PipelineValve.testInterceptor 检查输出。
 * 
 * 这不是 happy-path，这是逐路径迫发测试。
 */
class GpsValveVerificationTest {

    private lateinit var repo: FakeEntityRepository
    private lateinit var writer: RealEntityWriter
    private lateinit var writeBack: FakeKernelWriteBack
    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val capturedLogs = mutableListOf<String>()

    @Before
    fun setup() {
        repo = FakeEntityRepository()
        val timeProvider = FakeTimeProvider()
        writeBack = FakeKernelWriteBack()
        writer = RealEntityWriter(repo, timeProvider, writeBack, testScope)

        PipelineValve.testInterceptor = { checkpoint, size, summary ->
            capturedLogs.add("[$checkpoint] $summary")
        }
    }

    @org.junit.After
    fun teardown() {
        PipelineValve.testInterceptor = null
    }

    private fun getCaptured(): String {
        return capturedLogs.joinToString("\n")
    }

    private fun resetCapture() {
        capturedLogs.clear()
    }

    // ========== PATH 1: upsertFromClue — 新建 ==========
    @Test
    fun `valve fires on upsert CREATE path`() = runTest {
        val result = writer.upsertFromClue("测试GPS", null, EntityType.PERSON, "gps_test")
        val output = getCaptured()

        assertTrue(
            "PATH 1 FAIL: DB_WRITE_EXECUTED not found in stdout for CREATE.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity created to SSD")
        )
        assertTrue(
            "PATH 1 FAIL: Entity ID not logged.\nCaptured: $output",
            output.contains(result.entityId)
        )
        recordResult("P1", "CREATE Path (upsertFromClue)", true)
    }

    // ========== PATH 2: upsertFromClue — 合并（已存在实体） ==========
    @Test
    fun `valve fires on upsert MERGE path`() = runTest {
        // 先创建
        val created = writer.upsertFromClue("合并测试", null, EntityType.PERSON, "gps_test")
        resetCapture()

        // 再次 upsert 同名 → 走 merge 路径
        writer.upsertFromClue("合并测试", created.entityId, EntityType.PERSON, "gps_test_2")
        val output = getCaptured()

        assertTrue(
            "PATH 2 FAIL: DB_WRITE_EXECUTED not found for MERGE.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity write to SSD")
        )
        recordResult("P2", "MERGE Path (upsertFromClue)", true)
    }

    // ========== PATH 3: updateAttribute ==========
    @Test
    fun `valve fires on updateAttribute`() = runTest {
        val created = writer.upsertFromClue("属性测试", null, EntityType.PERSON, "gps_test")
        resetCapture()

        writer.updateAttribute(created.entityId, "notes", "GPS valve test note")
        val output = getCaptured()

        assertTrue(
            "PATH 3 FAIL: DB_WRITE_EXECUTED not found for updateAttribute.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity update attr to SSD")
        )
        recordResult("P3", "Attribute Update (updateAttribute)", true)
    }

    // ========== PATH 4: registerAlias ==========
    @Test
    fun `valve fires on registerAlias`() = runTest {
        val created = writer.upsertFromClue("别名测试", null, EntityType.PERSON, "gps_test")
        resetCapture()

        writer.registerAlias(created.entityId, "alias_gps")
        val output = getCaptured()

        assertTrue(
            "PATH 4 FAIL: DB_WRITE_EXECUTED not found for registerAlias.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity alias registered to SSD")
        )
        recordResult("P4", "Alias Registration (registerAlias)", true)
    }

    // ========== PATH 5: updateProfile ==========
    @Test
    fun `valve fires on updateProfile`() = runTest {
        val created = writer.upsertFromClue("Profile测试", null, EntityType.PERSON, "gps_test")
        resetCapture()

        writer.updateProfile(created.entityId, mapOf("jobTitle" to "GPS验证员"))
        val output = getCaptured()

        assertTrue(
            "PATH 5 FAIL: DB_WRITE_EXECUTED not found for updateProfile.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity profile updated to SSD")
        )
        recordResult("P5", "Profile Update (updateProfile)", true)
    }

    // ========== PATH 6: delete ==========
    @Test
    fun `valve fires on delete`() = runTest {
        val created = writer.upsertFromClue("删除测试", null, EntityType.PERSON, "gps_test")
        resetCapture()

        writer.delete(created.entityId)
        val output = getCaptured()

        assertTrue(
            "PATH 6 FAIL: DB_WRITE_EXECUTED not found for delete.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED") && output.contains("Entity deleted from SSD")
        )
        recordResult("P6", "Entity Deletion (delete)", true)
    }

    // ========== NEGATIVE: no-op paths should NOT fire ==========
    @Test
    fun `valve does NOT fire on updateAttribute for nonexistent entity`() = runTest {
        resetCapture()
        writer.updateAttribute("does-not-exist", "key", "value")
        val output = getCaptured()

        assertFalse(
            "NEGATIVE FAIL: DB_WRITE_EXECUTED fired on no-op updateAttribute.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED")
        )
        recordResult("N1", "NEGATIVE: updateAttribute (no exist)", true)
    }

    @Test
    fun `valve does NOT fire on registerAlias for nonexistent entity`() = runTest {
        resetCapture()
        writer.registerAlias("does-not-exist", "alias")
        val output = getCaptured()

        assertFalse(
            "NEGATIVE FAIL: DB_WRITE_EXECUTED fired on no-op registerAlias.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED")
        )
        recordResult("N2", "NEGATIVE: registerAlias (no exist)", true)
    }

    @Test
    fun `valve does NOT fire on updateProfile with empty changes`() = runTest {
        val created = writer.upsertFromClue("空变更", null, EntityType.PERSON, "gps_test")
        resetCapture()

        writer.updateProfile(created.entityId, emptyMap())
        val output = getCaptured()

        assertFalse(
            "NEGATIVE FAIL: DB_WRITE_EXECUTED fired on empty profile update.\nCaptured: $output",
            output.contains("DB_WRITE_EXECUTED")
        )
        recordResult("N3", "NEGATIVE: updateProfile (empty)", true)
    }

    companion object {
        private val results = mutableMapOf<String, Pair<String, Boolean>>()

        fun recordResult(id: String, name: String, passed: Boolean) {
            results[id] = name to passed
        }

        @org.junit.AfterClass
        @JvmStatic
        fun printDashboard() {
            println("\n===========================================================")
            println("              GPS VALVE VERIFICATION REPORT                ")
            println("===========================================================")
            println(String.format("%-5s | %-35s | %-10s", "ID", "SCENARIO", "STATUS"))
            println("-----------------------------------------------------------")
            
            // Sort to ensure consistent output order
            val sortedResults = results.entries.sortedBy { it.key }
            for ((id, data) in sortedResults) {
                val (name, passed) = data
                val statusStr = if (passed) "✅ PASS" else "❌ FAIL"
                println(String.format("%-5s | %-35s | %-10s", id, name, statusStr))
            }
            
            val passCount = results.values.count { it.second }
            val failCount = results.size - passCount
            println("-----------------------------------------------------------")
            println("TOTAL: ${results.size} | PASSED: $passCount | FAILED: $failCount")
            println("===========================================================\n")
        }
    }
}
