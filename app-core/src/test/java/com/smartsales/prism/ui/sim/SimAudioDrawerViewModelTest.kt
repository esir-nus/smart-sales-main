package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource as DomainAudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
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
    fun `resolveSimBadgeDeleteConfirmationRequest requires confirmation for first smartbadge delete`() {
        val request = resolveSimBadgeDeleteConfirmationRequest(
            audio = AudioFile(
                id = "badge-1",
                filename = "log_20260331_101500.wav",
                timeDisplay = "Now",
                source = DomainAudioSource.SMARTBADGE,
                status = TranscriptionStatus.PENDING
            ),
            hasConfirmedBadgeDeleteThisSession = false
        )

        assertEquals(
            SimBadgeDeleteConfirmationRequest(
                audioId = "badge-1",
                filename = "log_20260331_101500.wav"
            ),
            request
        )
    }

    @Test
    fun `resolveSimBadgeDeleteConfirmationRequest requires confirmation for legacy badge-like filename`() {
        val request = resolveSimBadgeDeleteConfirmationRequest(
            audio = AudioFile(
                id = "badge-legacy",
                filename = "log_20260331_101500.wav",
                timeDisplay = "Now",
                source = DomainAudioSource.PHONE,
                status = TranscriptionStatus.PENDING
            ),
            hasConfirmedBadgeDeleteThisSession = false
        )

        assertEquals(
            SimBadgeDeleteConfirmationRequest(
                audioId = "badge-legacy",
                filename = "log_20260331_101500.wav"
            ),
            request
        )
    }

    @Test
    fun `resolveSimBadgeDeleteConfirmationRequest skips confirmation after session approval or for plain phone audio`() {
        assertEquals(
            null,
            resolveSimBadgeDeleteConfirmationRequest(
                audio = AudioFile(
                    id = "badge-1",
                    filename = "log_20260331_101500.wav",
                    timeDisplay = "Now",
                    source = DomainAudioSource.SMARTBADGE,
                    status = TranscriptionStatus.PENDING
                ),
                hasConfirmedBadgeDeleteThisSession = true
            )
        )
        assertEquals(
            null,
            resolveSimBadgeDeleteConfirmationRequest(
                audio = AudioFile(
                    id = "phone-1",
                    filename = "memo.mp3",
                    timeDisplay = "Now",
                    source = DomainAudioSource.PHONE,
                    status = TranscriptionStatus.PENDING
                ),
                hasConfirmedBadgeDeleteThisSession = false
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText prefixes current-discussion marker in chat reselect mode`() {
        val entry = testEntry(
            status = AudioStatus.TRANSCRIBED,
            summary = "摘要预览",
            preview = "通用预览"
        )

        assertEquals(
            "当前讨论中 · 转写正文预览",
            buildSimAudioSelectBodyText(
                entry = entry,
                transcriptPreview = "转写正文预览",
                isCurrentChatAudio = true
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText explains pending and transcribing chat continuation`() {
        assertEquals(
            "选择后在当前聊天中继续处理",
            buildSimAudioSelectBodyText(
                entry = testEntry(
                    status = AudioStatus.PENDING,
                    summary = null,
                    preview = "通用预览",
                    localAvailability = AudioLocalAvailability.READY
                ),
                transcriptPreview = null,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            "转写中，选择后将在当前聊天继续处理",
            buildSimAudioSelectBodyText(
                entry = testEntry(status = AudioStatus.TRANSCRIBING, summary = null, preview = "通用预览"),
                transcriptPreview = null,
                isCurrentChatAudio = false
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText keeps current pending and transcribing copy concise`() {
        assertEquals(
            "当前讨论中 · 可在当前聊天中继续处理",
            buildSimAudioSelectBodyText(
                entry = testEntry(
                    status = AudioStatus.PENDING,
                    summary = null,
                    preview = "通用预览",
                    localAvailability = AudioLocalAvailability.READY
                ),
                transcriptPreview = null,
                isCurrentChatAudio = true
            )
        )
        assertEquals(
            "当前讨论中 · 转写中，当前聊天会继续处理",
            buildSimAudioSelectBodyText(
                entry = testEntry(status = AudioStatus.TRANSCRIBING, summary = null, preview = "通用预览"),
                transcriptPreview = null,
                isCurrentChatAudio = true
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText prefers transcript preview for transcribed audio`() {
        val entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = "摘要预览", preview = "通用预览")

        assertEquals(
            "转写正文预览",
            buildSimAudioSelectBodyText(
                entry = entry,
                transcriptPreview = "转写正文预览",
                isCurrentChatAudio = false
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText falls back to summary and entry preview when transcript is unavailable`() {
        assertEquals(
            "摘要预览",
            buildSimAudioSelectBodyText(
                entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = "摘要预览", preview = "通用预览"),
                transcriptPreview = null,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            "通用预览",
            buildSimAudioSelectBodyText(
                entry = testEntry(status = AudioStatus.TRANSCRIBED, summary = null, preview = "通用预览"),
                transcriptPreview = null,
                isCurrentChatAudio = false
            )
        )
    }

    @Test
    fun `buildSimAudioSelectBodyText marks non-ready pending entries as unavailable for chat`() {
        assertEquals(
            "录音等待后台同步，暂不可用于聊天",
            buildSimAudioSelectBodyText(
                entry = testEntry(
                    status = AudioStatus.PENDING,
                    summary = null,
                    preview = "通用预览",
                    localAvailability = AudioLocalAvailability.QUEUED
                ),
                transcriptPreview = null,
                isCurrentChatAudio = false
            )
        )
        assertEquals(
            "录音同步失败，请先重试同步",
            buildSimAudioSelectBodyText(
                entry = testEntry(
                    status = AudioStatus.PENDING,
                    summary = null,
                    preview = "通用预览",
                    localAvailability = AudioLocalAvailability.FAILED
                ),
                transcriptPreview = null,
                isCurrentChatAudio = false
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
        preview: String,
        localAvailability: AudioLocalAvailability = AudioLocalAvailability.READY
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
            preview = preview,
            localAvailability = localAvailability
        )
    }
}
