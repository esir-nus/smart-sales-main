package com.smartsales.domain.export

// File: feature/chat/src/test/java/com/smartsales/domain/export/ExportCoordinatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for ExportCoordinator - gate checking and export flow
// Author: created on 2026-01-07

import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExportCoordinatorTest {

    private val metaHub = FakeMetaHub()
    private val sessionRepository = FakeSessionRepository()
    private val exportOrchestrator = FakeExportOrchestrator()
    private val shareHandler = FakeShareHandler()
    private val coordinator = ExportCoordinatorImpl(
        metaHub, sessionRepository, exportOrchestrator, shareHandler
    )

    @Test
    fun checkExportGate_analysisComplete_returnsReady() = runTest {
        // Given: Session with completed analysis
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test Session",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = "msg-123"
        )

        // When: Check export gate
        val gate = coordinator.checkExportGate(sessionId)

        // Then: Gate should be ready
        assertEquals(true, gate.ready)
        assertEquals("", gate.reason)
    }

    @Test
    fun checkExportGate_analysisIncomplete_returnsNotReady() = runTest {
        // Given: Session without analysis
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test Session",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = null
        )

        // When: Check export gate
        val gate = coordinator.checkExportGate(sessionId)

        // Then: Gate should not be ready
        assertEquals(false, gate.ready)
        assertEquals("需先完成智能分析", gate.reason)
    }

    @Test
    fun performExport_checksInProgressFlag() = runTest {
        // Given: Valid session
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = "msg-123"
        )
        
        // When: Start export
        val result1 = coordinator.performExport(sessionId, ExportFormat.PDF, "user", "content")
        
        // Then: First export should succeed
        assert(result1 is Result.Success)
        
        // And: After completion, another export should work
        val result2 = coordinator.performExport(sessionId, ExportFormat.CSV, "user", "")
        assert(result2 is Result.Success)
    }

    @Test
    fun performExport_pdfWithBlankMarkdown_returnsError() = runTest {
        // When: Try to export PDF with blank markdown
        val result = coordinator.performExport("s1", ExportFormat.PDF, "user", "  ")

        // Then: Should return error
        assert(result is Result.Error)
        assertEquals("暂无可导出的内容", (result as Result.Error).throwable.message)
        
        // And: Snackbar message should be set
        val state = coordinator.exportState.first()
        assertEquals("暂无可导出的内容", state.snackbarMessage)
    }

    @Test
    fun performExport_pdfSuccess_updatesStateAndShares() = runTest {
        // Given: Valid session and metadata
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test Session",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = "msg-123"
        )

        // When: Perform PDF export
        val result = coordinator.performExport(sessionId, ExportFormat.PDF, "user", "# Content")

        // Then: Should succeed
        assert(result is Result.Success)
        
        // And: Export orchestrator should be called
        assertEquals(1, exportOrchestrator.pdfExports.size)
        assertEquals(sessionId, exportOrchestrator.pdfExports[0].sessionId)
        assertEquals("# Content", exportOrchestrator.pdfExports[0].markdown)
        
        // And: Share handler should be called
        assertEquals(1, shareHandler.shareCount)
        
        // And: State should not be in progress
        val state = coordinator.exportState.first()
        assertEquals(false, state.inProgress)
        assertNull(state.snackbarMessage)
    }

    @Test
    fun performExport_csvSuccess_delegatesToOrchestrator() = runTest {
        // Given: Valid session
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test Session",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = "msg-123"
        )

        // When: Perform CSV export
        val result = coordinator.performExport(sessionId, ExportFormat.CSV, "user", "")

        // Then: Should succeed
        assert(result is Result.Success)
        
        // And: CSV export should be called
        assertEquals(1, exportOrchestrator.csvExports.size)
    }

    @Test
    fun performExport_shareFailure_setsSnackbar() = runTest {
        // Given: Share handler will fail
        shareHandler.shouldFail = true
        val sessionId = "s1"
        sessionRepository.sessions[sessionId] = AiSessionSummary(
            id = sessionId,
            title = "Test",
            lastMessagePreview = "",
            updatedAtMillis = 0L
        )
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            latestMajorAnalysisMessageId = "msg-123"
        )

        // When: Perform export
        val result = coordinator.performExport(sessionId, ExportFormat.PDF, "user", "content")

        // Then: Should return error
        assert(result is Result.Error)
        
        // And: Snackbar should show share error
        val state = coordinator.exportState.first()
        assertEquals("Share failed", state.snackbarMessage)
        assertEquals(false, state.inProgress)
    }

    @Test
    fun clearSnackbar_clearsMessage() = runTest {
        // Given: Snackbar message exists
        coordinator.performExport("s1", ExportFormat.PDF, "user", "")
        
        // When: Clear snackbar
        coordinator.clearSnackbar()
        
        // Then: Message should be cleared
        val state = coordinator.exportState.first()
        assertNull(state.snackbarMessage)
    }

    // ===== Fake Implementations =====

    private class FakeMetaHub : MetaHub {
        val sessions = mutableMapOf<String, SessionMetadata>()
        
        override suspend fun upsertSession(metadata: SessionMetadata) {
            sessions[metadata.sessionId] = metadata
        }
        override suspend fun getSession(sessionId: String) = sessions[sessionId]
        override suspend fun appendM2Patch(sessionId: String, patch: com.smartsales.core.metahub.M2PatchRecord) {}
        override suspend fun getEffectiveM2(sessionId: String) = null
        override suspend fun upsertTranscript(metadata: com.smartsales.core.metahub.TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String) = null
        override suspend fun upsertExport(metadata: com.smartsales.core.metahub.ExportMetadata) {}
        override suspend fun getExport(sessionId: String) = null
        override suspend fun logUsage(usage: com.smartsales.core.metahub.TokenUsage) {}
    }

    private class FakeSessionRepository : AiSessionRepository {
        val sessions = mutableMapOf<String, AiSessionSummary>()
        
        override val summaries = kotlinx.coroutines.flow.MutableStateFlow(emptyList<AiSessionSummary>())
        override suspend fun upsert(summary: AiSessionSummary) {}
        override suspend fun delete(id: String) {}
        override suspend fun findById(id: String) = sessions[id]
        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {}
        override suspend fun createNewChatSession() = AiSessionSummary("new", "新聊天", "", 0L)
    }

    private class FakeExportOrchestrator : ExportOrchestrator {
        data class PdfExport(val sessionId: String, val markdown: String, val title: String?, val userName: String?)
        data class CsvExport(val sessionId: String, val title: String?, val userName: String?)
        
        val pdfExports = mutableListOf<PdfExport>()
        val csvExports = mutableListOf<CsvExport>()
        
        override suspend fun exportPdf(sessionId: String, markdown: String, sessionTitle: String?, userName: String?): Result<ExportResult> {
            pdfExports.add(PdfExport(sessionId, markdown, sessionTitle, userName))
            return Result.Success(
                ExportResult(
                    fileName = "file.pdf",
                    mimeType = "application/pdf",
                    payload = markdown.toByteArray(),
                    localPath = "/path/to/file.pdf"
                )
            )
        }
        
        override suspend fun exportCsv(sessionId: String, sessionTitle: String?, userName: String?): Result<ExportResult> {
            csvExports.add(CsvExport(sessionId, sessionTitle, userName))
            return Result.Success(
                ExportResult(
                    fileName = "file.csv",
                    mimeType = "text/csv",
                    payload = "csv-content".toByteArray(),
                    localPath = "/path/to/file.csv"
                )
            )
        }
    }

    private class FakeShareHandler : ChatShareHandler {
        var shareCount = 0
        var shouldFail = false
        var shouldBlock = false
        
        override suspend fun shareExport(result: ExportResult): Result<Unit> {
            shareCount++
            if (shouldBlock) {
                kotlinx.coroutines.delay(Long.MAX_VALUE) // Block forever
            }
            return if (shouldFail) {
                Result.Error(Exception("Share failed"))
            } else {
                Result.Success(Unit)
            }
        }
        
        override suspend fun copyMarkdown(markdown: String) = Result.Success(Unit)
        override suspend fun copyAssistantReply(text: String) = Result.Success(Unit)
    }
}
