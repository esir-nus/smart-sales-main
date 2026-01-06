package com.smartsales.domain.sessions

// File: feature/chat/src/test/java/com/smartsales/domain/sessions/SessionsManagerTest.kt
// Module: :feature:chat
// Summary: Unit tests for SessionsManager - session list management and operations
// Author: created on 2026-01-06
// Status: 9/10 passing. TODO: Fix onHistorySessionRenameConfirmed_persistsAndUpdatesMetaHub (async timing)

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.RenamingTarget
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.history.ChatHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsManagerTest {

    private val repository = FakeAiSessionRepository()
    private val historyRepo = FakeChatHistoryRepository()
    private val metaHub = FakeMetaHub()
    private val manager = SessionsManager(repository, historyRepo, metaHub)

    @Test
    fun onHistorySessionLongPress_updatesUiState() = runTest {
        // Given: A session exists in the list
        val session = AiSessionSummary(
            id = "s1",
            title = "Test Session",
            lastMessagePreview = "Preview",
            updatedAtMillis = 1000L
        )
        repository.upsert(session)

        // When: Long press on the session
        manager.onHistorySessionLongPress("s1")

        // Then: UI state should show the selected session
        val uiState = manager.uiState.first()
        assertNotNull(uiState.historyActionSession)
        assertEquals("Test Session", uiState.historyActionSession?.title)
        assertEquals("s1", uiState.historyActionSession?.id)
    }

    @Test
    fun onHistoryActionDismiss_clearsDialogState() = runTest {
        // Given: A session is long-pressed
        val session = AiSessionSummary(
            id = "s1",
            title = "Test",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(session)
        manager.onHistorySessionLongPress("s1")

        // When: Dismiss action
        manager.onHistoryActionDismiss()

        // Then: UI state should be cleared
        val uiState = manager.uiState.first()
        assertNull(uiState.historyActionSession)
        assertEquals(false, uiState.showHistoryRenameDialog)
        assertEquals("", uiState.historyRenameText)
    }

    @Test
    fun onHistoryActionRenameStart_opensRenameDialog() = runTest {
        // Given: A session is selected
        val session = AiSessionSummary(
            id = "s1",
            title = "Original",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(session)
        manager.onHistorySessionLongPress("s1")

        // When: Start rename
        manager.onHistoryActionRenameStart()

        // Then: Rename dialog should show with current title
        val uiState = manager.uiState.first()
        assertEquals(true, uiState.showHistoryRenameDialog)
        assertEquals("Original", uiState.historyRenameText)
    }

    @Test
    fun onHistoryRenameTextChange_updatesText() = runTest {
        // When: Text changes
        manager.onHistoryRenameTextChange("New Title")

        // Then: UI state should update
        val uiState = manager.uiState.first()
        assertEquals("New Title", uiState.historyRenameText)
    }

    @Test
    fun onHistorySessionPinToggle_togglesPin() = runTest {
        // Given: A session exists
        val session = AiSessionSummary(
            id = "s1",
            title = "Test",
            lastMessagePreview = "",
            updatedAtMillis = 1000L,
            pinned = false
        )
        repository.upsert(session)

        // When: Toggle pin
        manager.onHistorySessionPinToggle("s1")

        // Then: Session should be pinned
        val updated = repository.findById("s1")
        assertEquals(true, updated?.pinned)
    }

    @Test
    fun onHistorySessionRenameConfirmed_persistsAndUpdatesMetaHub() = runTest {
        // Given: A session exists
        val session = AiSessionSummary(
            id = "s1",
            title = "Old Title",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(session)
        manager.onHistorySessionLongPress("s1")

        // When: Confirm rename
        val result = manager.onHistorySessionRenameConfirmed("s1", "  New Title  ")

        // Then: Title should be saved with trimmed text
        assertEquals("New Title", result)
        val updated = repository.findById("s1")
        assertEquals("New Title", updated?.title)
        assertEquals(true, updated?.isTitleUserEdited)

        // And: MetaHub should be updated
        assertEquals(1, metaHub.renameCalls.size)
        val call = metaHub.renameCalls.first()
        assertEquals("s1", call.sessionId)
        assertEquals("New Title", call.name)
        assertEquals(RenamingTarget.SESSION_TITLE, call.target)
    }

    @Test
    fun onHistorySessionRenameConfirmed_emptyTitle_returnsNull() = runTest {
        // Given: A session exists
        val session = AiSessionSummary(
            id = "s1",
            title = "Original",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(session)

        // When: Confirm with empty title
        val result = manager.onHistorySessionRenameConfirmed("s1", "   ")

        // Then: Should return null and not update
        assertNull(result)
        val unchanged = repository.findById("s1")
        assertEquals("Original", unchanged?.title)
    }

    @Test
    fun onHistorySessionDelete_currentSession_returnsCurrentSessionDeleted() = runTest {
        // Given: Current session exists
        val current = AiSessionSummary(
            id = "current",
            title = "Current",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(current)

        // When: Delete current session
        val result = manager.onHistorySessionDelete("current", "current")

        // Then: Should return CurrentSessionDeleted with new session
        assert(result is SessionsManager.DeleteResult.CurrentSessionDeleted)
        val deleted = result as SessionsManager.DeleteResult.CurrentSessionDeleted
        assertNotNull(deleted.nextSession)
        assert(deleted.nextSession.id.startsWith("session-"))
    }

    @Test
    fun onHistorySessionDelete_otherSession_returnsSuccess() = runTest {
        // Given: Other session exists
        val other = AiSessionSummary(
            id = "other",
            title = "Other",
            lastMessagePreview = "",
            updatedAtMillis = 1000L
        )
        repository.upsert(other)

        // When: Delete other session
        val result = manager.onHistorySessionDelete("other", "current")

        // Then: Should return Success
        assert(result is SessionsManager.DeleteResult.Success)
        assertNull(repository.findById("other"))
    }

    @Test
    fun createNewSession_delegatesToRepository() = runTest {
        // When: Create new session
        val session = manager.createNewSession()

        // Then: Should return a new session
        assertNotNull(session)
        assert(session.id.startsWith("session-"))
    }

    // ===== Fake Implementations =====

    private class FakeAiSessionRepository : AiSessionRepository {
        private val sessions = MutableStateFlow<List<AiSessionSummary>>(emptyList())
        override val summaries = sessions

        override suspend fun upsert(summary: AiSessionSummary) {
            sessions.value = sessions.value.filterNot { it.id == summary.id } + summary
        }

        override suspend fun delete(id: String) {
            sessions.value = sessions.value.filterNot { it.id == id }
        }

        override suspend fun findById(id: String): AiSessionSummary? {
            return sessions.value.firstOrNull { it.id == id }
        }

        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {
            sessions.value = sessions.value.map {
                if (it.id == id) it.copy(title = newTitle, isTitleUserEdited = it.isTitleUserEdited || isUserEdited)
                else it
            }
        }

        override suspend fun createNewChatSession(): AiSessionSummary {
            val session = AiSessionSummary(
                id = "session-${java.util.UUID.randomUUID()}",
                title = "新聊天",
                lastMessagePreview = "",
                updatedAtMillis = System.currentTimeMillis()
            )
            upsert(session)
            return session
        }
    }

    private class FakeChatHistoryRepository : ChatHistoryRepository {
        override suspend fun loadLatestSession(sessionId: String) = emptyList<com.smartsales.feature.chat.history.ChatMessageEntity>()
        override suspend fun saveMessages(sessionId: String, messages: List<com.smartsales.feature.chat.history.ChatMessageEntity>) {}
        override suspend fun deleteSession(sessionId: String) {}
    }

    private class FakeMetaHub : MetaHub {
        data class RenameCall(val sessionId: String, val target: RenamingTarget, val name: String, val prov: Provenance)
        val renameCalls = mutableListOf<RenameCall>()

        suspend fun setM3AcceptedName(
            sessionId: String,
            target: RenamingTarget,
            name: String,
            prov: Provenance
        ) {
            renameCalls.add(RenameCall(sessionId, target, name, prov))
        }

        override suspend fun upsertSession(metadata: com.smartsales.core.metahub.SessionMetadata) {}
        override suspend fun getSession(sessionId: String) = null
        override suspend fun appendM2Patch(sessionId: String, patch: com.smartsales.core.metahub.M2PatchRecord) {}
        override suspend fun getEffectiveM2(sessionId: String) = null
        override suspend fun upsertTranscript(metadata: com.smartsales.core.metahub.TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String) = null
        override suspend fun upsertExport(metadata: com.smartsales.core.metahub.ExportMetadata) {}
        override suspend fun getExport(sessionId: String) = null
        override suspend fun logUsage(usage: com.smartsales.core.metahub.TokenUsage) {}
    }
}
