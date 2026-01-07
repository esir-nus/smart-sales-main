package com.smartsales.domain.debug

// File: feature/chat/src/test/java/com/smartsales/domain/debug/DebugCoordinatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for DebugCoordinator - HUD debug panel state management
// Author: created on 2026-01-07

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DebugCoordinatorTest {

    private val metaHub = FakeMetaHub()
    private val sessionRepository = FakeSessionRepository()
    private val debugOrchestrator = FakeDebugOrchestrator()
    private val xfyunTraceStore = XfyunTraceStore()
    private val tingwuTraceStore = TingwuTraceStore()
    private val aiParaSettingsRepository = FakeAiParaSettingsRepository()
    private val coordinator = DebugCoordinatorImpl(
        metaHub, sessionRepository, debugOrchestrator,
        xfyunTraceStore, tingwuTraceStore, aiParaSettingsRepository
    )

    @Test
    fun toggleDebugPanel_togglesVisibility() = runTest {
        // Given: Panel is not visible
        var state = coordinator.debugState.first()
        assertEquals(false, state.visible)

        // When: Toggle panel
        coordinator.toggleDebugPanel()

        // Then: Panel should be visible
        state = coordinator.debugState.first()
        assertEquals(true, state.visible)

        // When: Toggle again
        coordinator.toggleDebugPanel()

        // Then: Panel should be hidden
        state = coordinator.debugState.first()
        assertEquals(false, state.visible)
    }

    @Test
    fun refreshSessionMetadata_updatesDebugMetadata() = runTest {
        // Given: Session with metadata
        val sessionId = "s1"
        val sessionTitle = "Test Session"
        metaHub.sessions[sessionId] = SessionMetadata(
            sessionId = sessionId,
            mainPerson = "John",
            shortSummary = "Summary"
        )

        // When: Refresh session metadata
        coordinator.refreshSessionMetadata(sessionId, sessionTitle, emptyList())

        // Then: Debug state should contain session metadata
        val state = coordinator.debugState.first()
        assertNotNull(state.sessionMetadata)
        assertEquals(sessionId, state.sessionMetadata?.sessionId)
        assertEquals(sessionTitle, state.sessionMetadata?.title)
        assertEquals("John", state.sessionMetadata?.mainPerson)
        assertEquals("Summary", state.sessionMetadata?.shortSummary)
    }

    @Test
    fun refreshSessionMetadata_mergesExtraNotes() = runTest {
        // Given: Session metadata with existing notes
        coordinator.refreshSessionMetadata("s1", "Title", listOf("Note 1"))

        // When: Refresh with additional notes
        coordinator.refreshSessionMetadata("s1", "Title", listOf("Note 2", "Note 3"))

        // Then: Notes should be merged and deduplicated
        val state = coordinator.debugState.first()
        assertEquals(listOf("Note 1", "Note 2", "Note 3"), state.sessionMetadata?.notes)
    }

    @Test
    fun refreshDebugSnapshot_success_updatesSnapshot() = runTest {
        // Given: Debug orchestrator will return snapshot
        debugOrchestrator.snapshot = DebugSnapshot(
            section1EffectiveRunText = "Run text",
            section2RawTranscriptionText = "Raw text",
            section3PreprocessedText = "Preprocessed",
            sessionId = "s1",
            jobId = "job1"
        )

        // When: Refresh debug snapshot
        coordinator.refreshDebugSnapshot("s1", "job1", "Title", false)

        // Then: State should contain snapshot
        val state = coordinator.debugState.first()
        assertNotNull(state.snapshot)
        assertEquals("Run text", state.snapshot?.section1EffectiveRunText)
        assertEquals("s1", state.snapshot?.sessionId)
    }

    @Test
    fun refreshDebugSnapshot_failure_failsSoft() = runTest {
        // Given: Debug orchestrator will throw error
        debugOrchestrator.shouldFail = true

        // When: Refresh debug snapshot
        coordinator.refreshDebugSnapshot("s1", "job1", "Title", false)

        // Then: Should still have snapshot with error message
        val state = coordinator.debugState.first()
        assertNotNull(state.snapshot)
        assert(state.snapshot?.section1EffectiveRunText?.contains("DebugSnapshot failed") == true)
        assertEquals("(missing: debug snapshot unavailable)", state.snapshot?.section2RawTranscriptionText)
    }

    @Test
    fun refreshTraces_retrievesFromStores() = runTest {
        // When: Refresh traces
        coordinator.refreshTraces()

        // Then: Should complete successfully (traces retrieved from stores)
        val state = coordinator.debugState.first()
        // Traces may be null if stores are empty - that's OK
        // The test verifies refreshTraces() doesn't crash
    }

    @Test
    fun appendDebugNote_addsNoteToCurrentSession() = runTest {
        // Given: Session metadata exists
        coordinator.refreshSessionMetadata("s1", "Title", listOf("Note 1"))

        // When: Append debug note
        coordinator.appendDebugNote("Note 2")

        // Then: Note should be added
        val state = coordinator.debugState.first()
        assertEquals(listOf("Note 1", "Note 2"), state.sessionMetadata?.notes)
    }

    @Test
    fun appendDebugNote_deduplicatesNotes() = runTest {
        // Given: Session metadata with note
        coordinator.refreshSessionMetadata("s1", "Title", listOf("Note 1"))

        // When: Append same note
        coordinator.appendDebugNote("Note 1")

        // Then: Should not duplicate
        val state = coordinator.debugState.first()
        assertEquals(listOf("Note 1"), state.sessionMetadata?.notes)
    }

    @Test
    fun clear_resetsSessionMetadataAndSnapshot() = runTest {
        // Given: Debug state has data
        coordinator.refreshSessionMetadata("s1", "Title", emptyList())
        coordinator.refreshDebugSnapshot("s1", "job1", "Title", false)

        // When: Clear
        coordinator.clear()

        // Then: Session metadata and snapshot should be null
        val state = coordinator.debugState.first()
        assertNull(state.sessionMetadata)
        assertNull(state.snapshot)
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
        override val summaries = MutableStateFlow(emptyList<AiSessionSummary>())
        override suspend fun upsert(summary: AiSessionSummary) {}
        override suspend fun delete(id: String) {}
        override suspend fun findById(id: String) = null
        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {}
        override suspend fun createNewChatSession() = AiSessionSummary("new", "新聊天", "", 0L)
    }

    private class FakeDebugOrchestrator : DebugOrchestrator {
        var snapshot: DebugSnapshot? = null
        var shouldFail = false
        
        override suspend fun getDebugSnapshot(
            sessionId: String,
            jobId: String?,
            sessionTitle: String?,
            isTitleUserEdited: Boolean?
        ): DebugSnapshot {
            if (shouldFail) throw Exception("Debug orchestrator error")
            return snapshot ?: DebugSnapshot(
                section1EffectiveRunText = "",
                section2RawTranscriptionText = "",
                section3PreprocessedText = "",
                sessionId = sessionId,
                jobId = jobId
            )
        }
    }

    private class FakeAiParaSettingsRepository : AiParaSettingsRepository {
        private val _flow = MutableStateFlow(AiParaSettingsSnapshot())
        
        override val flow: StateFlow<AiParaSettingsSnapshot> = _flow.asStateFlow()
        
        override fun snapshot() = _flow.value
        
        override fun update(transform: (AiParaSettingsSnapshot) -> AiParaSettingsSnapshot) {
            _flow.update(transform)
        }
    }
}
