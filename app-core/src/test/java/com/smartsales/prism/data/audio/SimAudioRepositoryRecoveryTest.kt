package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioRepositoryRecoveryTest {

    @Test
    fun `recoverOrphanedSimTranscriptions resets transcribing entry without active job id`() {
        val recovered = recoverOrphanedSimTranscriptions(
            listOf(
                AudioFile(
                    id = "audio-1",
                    filename = "Pending.mp3",
                    timeDisplay = "Now",
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.TRANSCRIBING,
                    progress = 0.42f
                )
            )
        ).single()

        assertEquals(TranscriptionStatus.PENDING, recovered.status)
        assertEquals(0f, recovered.progress, 0f)
        assertEquals(ORPHANED_SIM_TRANSCRIPTION_MESSAGE, recovered.lastErrorMessage)
        assertNull(recovered.activeJobId)
    }

    @Test
    fun `recoverOrphanedSimTranscriptions keeps active in flight entry with job id`() {
        val original = AudioFile(
            id = "audio-2",
            filename = "Live.mp3",
            timeDisplay = "Now",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.TRANSCRIBING,
            progress = 0.42f,
            activeJobId = "job-123"
        )

        val recovered = recoverOrphanedSimTranscriptions(listOf(original)).single()

        assertEquals(original, recovered)
        assertTrue(recovered.activeJobId == "job-123")
    }

    @Test
    fun `recoverOrphanedSimTranscriptions preserves test import flag`() {
        val recovered = recoverOrphanedSimTranscriptions(
            listOf(
                AudioFile(
                    id = "audio-3",
                    filename = "Imported.mp3",
                    timeDisplay = "Now",
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.TRANSCRIBING,
                    progress = 0.42f,
                    isTestImport = true
                )
            )
        ).single()

        assertTrue(recovered.isTestImport)
        assertEquals(TranscriptionStatus.PENDING, recovered.status)
        assertEquals(ORPHANED_SIM_TRANSCRIPTION_MESSAGE, recovered.lastErrorMessage)
    }
}
