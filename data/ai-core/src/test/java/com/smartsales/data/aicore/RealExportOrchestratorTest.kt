package com.smartsales.data.aicore

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/RealExportOrchestratorTest.kt
// 模块：:data:ai-core
// 说明：验证导出协同器使用用户名与元数据生成符合规范的文件名
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealExportOrchestratorTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
    }

    @Test
    fun exportPdf_usesUserAndMetadataForFileName() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub().apply {
            session = SessionMetadata(
                sessionId = "s1",
                mainPerson = "王总",
                summaryTitle6Chars = "季度回顾"
            )
        }
        val exportManager = RecordingExportManager()
        val orchestrator = RealExportOrchestrator(
            metaHub = metaHub,
            exportManager = exportManager,
            exportFileStore = object : ExportFileStore {
                override fun persist(fileName: String, payload: ByteArray, mimeType: String): String = ""
            },
            dispatchers = dispatchers
        )

        val result = orchestrator.exportPdf("s1", "markdown content", sessionTitle = "季度回顾", userName = "Alice")

        assertTrue(result is Result.Success)
        val suggested = exportManager.lastSuggestedFileName
        assertTrue(suggested?.contains("Alice_王总_季度回顾") == true)
        val fileName = (result as Result.Success).data.fileName
        assertTrue(fileName.endsWith(".pdf"))
        assertTrue(fileName.contains("王总_季度回顾"))
        assertEquals(fileName, metaHub.export?.lastPdfFileName)
        val rendered = exportManager.lastMarkdown.orEmpty()
        assertTrue(rendered.contains("## 会话概要"))
        assertTrue(rendered.contains("客户/主对象：王总"))
        assertTrue(rendered.contains("会话摘要：季度回顾"))
    }

    private class RecordingExportManager : ExportManager {
        var lastSuggestedFileName: String? = null
        var lastMarkdown: String? = null
        override suspend fun exportMarkdown(
            markdown: String,
            format: ExportFormat,
            suggestedFileName: String?
        ): Result<ExportResult> {
            lastMarkdown = markdown
            lastSuggestedFileName = suggestedFileName
            val ext = if (format == ExportFormat.PDF) "pdf" else "csv"
            val safeName = suggestedFileName ?: "export"
            return Result.Success(
                ExportResult(
                    fileName = "$safeName.$ext",
                    mimeType = if (format == ExportFormat.PDF) "application/pdf" else "text/csv",
                    payload = ByteArray(0)
                )
            )
        }
    }

    private class RecordingMetaHub : MetaHub {
        var session: SessionMetadata? = null
        var export: ExportMetadata? = null
        override suspend fun upsertSession(metadata: SessionMetadata) {
            session = metadata
        }
        override suspend fun getSession(sessionId: String): SessionMetadata? = session
        override suspend fun upsertTranscript(metadata: com.smartsales.core.metahub.TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String): com.smartsales.core.metahub.TranscriptMetadata? = null
        override suspend fun upsertExport(metadata: ExportMetadata) {
            export = metadata
        }
        override suspend fun getExport(sessionId: String): ExportMetadata? = export
        override suspend fun logUsage(usage: TokenUsage) {}
    }
}
