package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/RealExportManagerTest.kt
// 模块：:data:ai-core
// 说明：验证真实导出管理器的字节生成与文件落盘行为
// 作者：创建于 2025-11-16
@OptIn(ExperimentalCoroutinesApi::class)
class RealExportManagerTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)
    private val fileStore = RecordingFileStore()
    private val manager = RealExportManager(dispatchers, fileStore)

    @Test
    fun exportMarkdown_writesBytesAndReturnsPath() = runTest(dispatcher) {
        val result = manager.exportMarkdown("# 标题\n- 项目", ExportFormat.CSV)
        advanceUntilIdle()
        assertTrue(result is Result.Success)
        val export = result.data
        assertTrue(export.payload.isNotEmpty())
        assertEquals(fileStore.lastPath, export.localPath)
        assertTrue(fileStore.lastPayload.contentEquals(export.payload))
    }

    private class RecordingFileStore : ExportFileStore {
        var lastPath: String? = null
        var lastPayload: ByteArray = ByteArray(0)

        override fun persist(fileName: String, payload: ByteArray, mimeType: String): String {
            lastPayload = payload
            lastPath = "/tmp/$fileName"
            return lastPath!!
        }
    }
}
