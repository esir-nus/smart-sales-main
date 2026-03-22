package com.smartsales.prism.data.audio

import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDebugScenarioTest {

    @Test
    fun `debug failure scenario id is recognized`() {
        assertTrue(isSimDebugFailureScenario("sim_debug_failure"))
        assertFalse(isSimDebugFailureScenario("sim_wave2_seed"))
    }

    @Test
    fun `missing sections artifacts omit optional sections`() {
        val artifacts = buildSimDebugMissingSectionsArtifacts()

        assertTrue(artifacts.transcriptMarkdown?.isNotBlank() == true)
        assertTrue(artifacts.smartSummary?.summary?.isNotBlank() == true)
        assertTrue(artifacts.smartSummary?.keyPoints?.isEmpty() == true)
        assertNull(artifacts.chapters)
        assertNull(artifacts.diarizedSegments)
        assertTrue(artifacts.speakerLabels.isEmpty())
        assertNull(artifacts.meetingAssistanceRaw)
    }

    @Test
    fun `fallback artifacts preserve provider led raw output`() {
        val artifacts = buildSimDebugFallbackArtifacts()

        assertTrue(artifacts.transcriptMarkdown?.contains("[Provider Raw Transcript]") == true)
        assertNull(artifacts.smartSummary)
        assertTrue(artifacts.meetingAssistanceRaw?.contains("原始输出") == true)
        assertNull(artifacts.chapters)
    }

    @Test
    fun `upsert debug scenario entry refreshes matching fixture instead of duplicating`() {
        val existing = AudioFile(
            id = "sim_debug_failure",
            filename = "Old.mp3",
            timeDisplay = "Old",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.PENDING
        )
        val refreshed = existing.copy(filename = "New.mp3", timeDisplay = "Debug Scenario")

        val updated = upsertSimDebugScenarioEntry(listOf(existing), refreshed)

        assertEquals(1, updated.size)
        assertEquals("New.mp3", updated.single().filename)
        assertEquals("Debug Scenario", updated.single().timeDisplay)
    }

    @Test
    fun `existingSimBadgeFilenames only returns badge sourced filenames`() {
        val filenames = existingSimBadgeFilenames(
            listOf(
                AudioFile(
                    id = "audio_badge_1",
                    filename = "badge_1.wav",
                    timeDisplay = "Now",
                    source = AudioSource.SMARTBADGE,
                    status = TranscriptionStatus.PENDING
                ),
                AudioFile(
                    id = "audio_phone_1",
                    filename = "phone_1.wav",
                    timeDisplay = "Now",
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.PENDING
                ),
                AudioFile(
                    id = "audio_badge_2",
                    filename = "badge_2.wav",
                    timeDisplay = "Now",
                    source = AudioSource.SMARTBADGE,
                    status = TranscriptionStatus.TRANSCRIBED
                )
            )
        )

        assertEquals(setOf("badge_1.wav", "badge_2.wav"), filenames)
    }

    @Test
    fun `selectNewSimBadgeFilenames filters local duplicates and repeated badge list items`() {
        val newFiles = selectNewSimBadgeFilenames(
            badgeFilenames = listOf("badge_1.wav", "badge_2.wav", "badge_2.wav", "badge_3.wav"),
            existingBadgeFilenames = setOf("badge_1.wav", "badge_3.wav")
        )

        assertEquals(listOf("badge_2.wav"), newFiles)
    }

    @Test
    fun `simBadgeSyncSuccessMessage reflects imported count`() {
        assertEquals("未发现新的徽章录音", simBadgeSyncSuccessMessage(0))
        assertEquals("已同步 2 条徽章录音", simBadgeSyncSuccessMessage(2))
    }

    @Test
    fun `buildSimBadgeSyncListFailureMessage maps raw oss unknown null to friendly offline copy`() {
        assertEquals(
            SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE,
            buildSimBadgeSyncListFailureMessage("oss_unknown null")
        )
    }

    @Test
    fun `buildSimBadgeSyncDownloadFailureMessage keeps non connectivity failures human readable`() {
        assertEquals(
            "发现新的徽章录音，但暂时无法下载。请稍后重试。",
            buildSimBadgeSyncDownloadFailureMessage("permission denied")
        )
    }

    @Test
    fun `emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry emits summary and log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                detail = "listRecordings failed: badge offline",
                log = { message -> logs += message }
            )

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY
                )
            )
            assertEquals(
                listOf(
                    "SIM audio sync failed while connectivity unavailable: listRecordings failed: badge offline"
                ),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `badge sync request and completion telemetry emit expected summaries and logs`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSimAudioBadgeSyncRequestedTelemetry(
                trigger = SimBadgeSyncTrigger.MANUAL,
                log = { message -> logs += message }
            )
            emitSimAudioBadgeSyncCompletedTelemetry(
                trigger = SimBadgeSyncTrigger.AUTO,
                importedCount = 3,
                log = { message -> logs += message }
            )

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY
                )
            )
            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY
                )
            )
            assertEquals(
                listOf(
                    "SIM audio badge sync requested: trigger=manual",
                    "SIM audio badge sync completed: trigger=auto importedCount=3"
                ),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }
}
