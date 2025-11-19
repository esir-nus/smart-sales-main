// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/FakeExportManagerTest.kt
// 模块：:data:ai-core
// 说明：验证 FakeExportManager 的导出行为
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeExportManagerTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var manager: FakeExportManager

    @Before
    fun setup() {
        manager = FakeExportManager(FakeDispatcherProvider(dispatcher))
    }

    @Test
    fun `导出PDF生成带签名前缀的文件`() = runTest(dispatcher) {
        val markdown = "# Title\ncontent"

        val result = manager.exportMarkdown(markdown, ExportFormat.PDF).assertSuccess()

        assertTrue(result.fileName.endsWith(".pdf"))
        assertEquals("application/pdf", result.mimeType)
        val prefix = String(result.payload, 0, 8, StandardCharsets.US_ASCII)
        assertEquals("%PDF-1.4", prefix.trim())
    }

    @Test
    fun `导出CSV保持原始字节内容`() = runTest(dispatcher) {
        val markdown = "col1,col2"

        val result = manager.exportMarkdown(markdown, ExportFormat.CSV).assertSuccess()

        assertTrue(result.fileName.endsWith(".csv"))
        assertEquals("text/csv", result.mimeType)
        assertTrue(result.payload.contentEquals(markdown.toByteArray()))
    }

    private fun <T> Result<T>.assertSuccess(): T =
        when (this) {
            is Result.Success -> data
            is Result.Error -> throw AssertionError("期望 Success 但得到 Error", throwable)
        }
}
