package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerViewModelTest {

    @Test
    fun `toggleExpandedAudioIds adds audio id when card is first opened`() {
        val expandedIds = toggleExpandedAudioIds(emptySet(), "audio-1")

        assertEquals(setOf("audio-1"), expandedIds)
    }

    @Test
    fun `toggleExpandedAudioIds removes audio id when card is collapsed again`() {
        val expandedIds = toggleExpandedAudioIds(setOf("audio-1"), "audio-1")

        assertEquals(emptySet<String>(), expandedIds)
    }

    @Test
    fun `canStartSimAudioSync blocks when auto sync already attempted`() {
        assertFalse(
            canStartSimAudioSync(
                hasAttemptedAutoSync = true,
                isSyncing = false
            )
        )
    }

    @Test
    fun `canStartSimAudioSync blocks when sync is already running`() {
        assertFalse(
            canStartSimAudioSync(
                hasAttemptedAutoSync = false,
                isSyncing = true
            )
        )
    }

    @Test
    fun `canStartSimAudioSync allows first idle sync attempt`() {
        assertTrue(
            canStartSimAudioSync(
                hasAttemptedAutoSync = false,
                isSyncing = false
            )
        )
    }

    @Test
    fun `shouldShowSimAudioAutoSyncMessage only returns true when imports were added`() {
        assertFalse(shouldShowSimAudioAutoSyncMessage(0))
        assertTrue(shouldShowSimAudioAutoSyncMessage(2))
    }

    @Test
    fun `buildSimAudioStatusLabel shows current-discussion label in chat reselect mode`() {
        assertEquals(
            "当前讨论中",
            buildSimAudioStatusLabel(
                status = AudioStatus.TRANSCRIBED,
                mode = SimAudioDrawerMode.CHAT_RESELECT,
                isCurrentChatAudio = true
            )
        )
    }

    @Test
    fun `buildSimAudioStatusLabel keeps transcription status in browse mode`() {
        assertEquals(
            "转写中",
            buildSimAudioStatusLabel(
                status = AudioStatus.TRANSCRIBING,
                mode = SimAudioDrawerMode.BROWSE,
                isCurrentChatAudio = false
            )
        )
    }

    @Test
    fun `buildSimAudioSelectHelperText only explains pending and transcribing when selectable`() {
        assertEquals(
            "可在当前聊天中继续处理",
            buildSimAudioSelectHelperText(
                status = AudioStatus.PENDING,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            "将在聊天中继续处理",
            buildSimAudioSelectHelperText(
                status = AudioStatus.TRANSCRIBING,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            null,
            buildSimAudioSelectHelperText(
                status = AudioStatus.TRANSCRIBED,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            null,
            buildSimAudioSelectHelperText(
                status = AudioStatus.PENDING,
                isCurrentChatAudio = true
            )
        )
    }

    @Test
    fun `buildSimAudioSelectPreview prefers transcript preview for transcribed audio`() {
        val entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = "摘要预览", preview = "通用预览")

        assertEquals(
            "转写正文预览",
            buildSimAudioSelectPreview(
                entry = entry,
                transcriptPreview = "转写正文预览"
            )
        )
    }

    @Test
    fun `buildSimAudioSelectPreview falls back to summary and entry preview when transcript is unavailable`() {
        assertEquals(
            "摘要预览",
            buildSimAudioSelectPreview(
                entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = "摘要预览", preview = "通用预览"),
                transcriptPreview = null
            )
        )
        assertEquals(
            "通用预览",
            buildSimAudioSelectPreview(
                entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = null, preview = "通用预览"),
                transcriptPreview = null
            )
        )
    }

    @Test
    fun `buildSimAudioTranscriptPreview strips markdown decorations and normalizes whitespace`() {
        val preview = buildSimAudioTranscriptPreview(
            TingwuJobArtifacts(
                transcriptMarkdown = "# 标题\n- 第一行 **内容**\n> 第二行"
            )
        )

        assertEquals("标题 第一行 内容 第二行", preview)
    }

    private fun testEntry(
        status: AudioStatus,
        summary: String?,
        preview: String
    ): SimAudioEntry {
        return SimAudioEntry(
            item = AudioItemState(
                id = "audio-1",
                filename = "demo.mp3",
                timeDisplay = "10:00",
                source = AudioSource.PHONE,
                status = status,
                summary = summary
            ),
            preview = preview
        )
    }
}
