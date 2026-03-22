package com.smartsales.prism.data.session

import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.UiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SimSessionRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `repository round trips durable session metadata and supported message types`() {
        val repository = SimSessionRepository(tempFolder.root)

        repository.saveSession(
            preview = SessionPreview(
                id = "session_1",
                clientName = "客户录音",
                summary = "摘要",
                timestamp = 123L,
                isPinned = true,
                linkedAudioId = "audio_1",
                sessionKind = SessionKind.SCHEDULER_FOLLOW_UP,
                schedulerFollowUpContext = SchedulerFollowUpContext(
                    sourceBadgeThreadId = "thread_1",
                    boundTaskIds = listOf("task_1"),
                    taskSummaries = listOf(
                        SchedulerFollowUpTaskSummary(
                            taskId = "task_1",
                            title = "客户回访",
                            dayOffset = 0,
                            scheduledAtMillis = 123456L,
                            durationMinutes = 30
                        )
                    ),
                    createdAt = 123L,
                    updatedAt = 456L
                )
            ),
            messages = listOf(
                ChatMessage.User(
                    id = "user_1",
                    timestamp = 111L,
                    content = "用户提问"
                ),
                ChatMessage.Ai(
                    id = "ai_1",
                    timestamp = 222L,
                    uiState = UiState.Response(
                        content = "AI 回复",
                        structuredJson = "{\"ok\":true}",
                        suggestAnalyst = true
                    )
                ),
                ChatMessage.Ai(
                    id = "ai_2",
                    timestamp = 333L,
                    uiState = UiState.AudioArtifacts(
                        audioId = "audio_1",
                        title = "客户录音",
                        artifactsJson = "{\"transcript\":\"完整转写\"}"
                    )
                ),
                ChatMessage.Ai(
                    id = "ai_3",
                    timestamp = 444L,
                    uiState = UiState.Error("失败了", retryable = false)
                )
            )
        )

        val stored = repository.loadSessions().single()

        assertEquals("session_1", stored.preview.id)
        assertEquals("audio_1", stored.preview.linkedAudioId)
        assertEquals(SessionKind.SCHEDULER_FOLLOW_UP, stored.preview.sessionKind)
        assertEquals("thread_1", stored.preview.schedulerFollowUpContext?.sourceBadgeThreadId)
        assertTrue(stored.preview.isPinned)
        assertEquals(4, stored.messages.size)
        assertTrue(stored.messages[0] is ChatMessage.User)
        assertTrue((stored.messages[1] as ChatMessage.Ai).uiState is UiState.Response)
        assertTrue((stored.messages[2] as ChatMessage.Ai).uiState is UiState.AudioArtifacts)
        assertTrue((stored.messages[3] as ChatMessage.Ai).uiState is UiState.Error)
    }

    @Test
    fun `repository drops transient ui states from durable history`() {
        val repository = SimSessionRepository(tempFolder.root)

        repository.saveSession(
            preview = SessionPreview(
                id = "session_2",
                clientName = "瞬时状态",
                summary = "摘要",
                timestamp = 123L
            ),
            messages = listOf(
                ChatMessage.Ai(
                    id = "thinking_1",
                    timestamp = 111L,
                    uiState = UiState.Thinking("处理中")
                ),
                ChatMessage.Ai(
                    id = "stream_1",
                    timestamp = 222L,
                    uiState = UiState.Streaming("partial")
                ),
                ChatMessage.Ai(
                    id = "response_1",
                    timestamp = 333L,
                    uiState = UiState.Response("稳定回复")
                )
            )
        )

        val stored = repository.loadSessions().single()

        assertEquals(1, stored.messages.size)
        assertEquals(
            "稳定回复",
            ((stored.messages.single() as ChatMessage.Ai).uiState as UiState.Response).content
        )
    }

    @Test
    fun `repository deletes metadata and message file together`() {
        val repository = SimSessionRepository(tempFolder.root)
        repository.saveSession(
            preview = SessionPreview(
                id = "session_3",
                clientName = "待删除",
                summary = "摘要",
                timestamp = 123L
            ),
            messages = listOf(
                ChatMessage.User(
                    id = "user_3",
                    timestamp = 111L,
                    content = "hello"
                )
            )
        )

        repository.deleteSession("session_3")

        assertTrue(repository.loadSessions().isEmpty())
    }

    @Test
    fun `repository writes only sim namespaced session files`() {
        val repository = SimSessionRepository(tempFolder.root)

        repository.saveSession(
            preview = SessionPreview(
                id = "session_iso_1",
                clientName = "隔离验证",
                summary = "摘要",
                timestamp = 123L
            ),
            messages = listOf(
                ChatMessage.User(
                    id = "user_iso_1",
                    timestamp = 111L,
                    content = "hello"
                )
            )
        )

        assertTrue(File(tempFolder.root, "sim_session_metadata.json").exists())
        assertTrue(File(tempFolder.root, "sim_session_session_iso_1_messages.json").exists())
        assertFalse(File(tempFolder.root, "session_metadata.json").exists())
        assertFalse(File(tempFolder.root, "session_session_iso_1_messages.json").exists())
    }
}
