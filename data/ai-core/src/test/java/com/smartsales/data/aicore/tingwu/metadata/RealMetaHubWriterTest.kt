package com.smartsales.data.aicore.tingwu.metadata

import com.smartsales.core.metahub.ConversationDerivedState
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuSmartSummary
import com.smartsales.data.aicore.debug.TingwuTraceStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RealMetaHubWriter.
 */
class RealMetaHubWriterTest {

    private lateinit var writer: RealMetaHubWriter
    private lateinit var fakeMetaHub: FakeMetaHub
    private lateinit var traceStore: TingwuTraceStore

    @Before
    fun setup() {
        fakeMetaHub = FakeMetaHub()
        traceStore = TingwuTraceStore()
        writer = RealMetaHubWriter(
            metaHub = fakeMetaHub,
            tingwuTraceStore = traceStore
        )
    }

    @Test
    fun `writeSessionMetadata writes to metaHub with correct session ID`() = runTest {
        val input = SessionMetadataWriteInput(
            jobId = "job123",
            sessionId = "session456",
            audioAssetId = "audio789",
            artifacts = TingwuJobArtifacts(
                smartSummary = TingwuSmartSummary(
                    summary = "Test summary",
                    keyPoints = listOf("point1"),
                    actionItems = emptyList()
                )
            ),
            transcriptMeta = null,
            chapters = null
        )

        val result = writer.writeSessionMetadata(input)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeMetaHub.upsertSessionCalls.size)
        assertEquals("session456", fakeMetaHub.upsertSessionCalls.first().sessionId)
    }

    @Test
    fun `writeSessionMetadata handles null artifacts gracefully`() = runTest {
        val input = SessionMetadataWriteInput(
            jobId = "job123",
            sessionId = "session456",
            audioAssetId = "audio789",
            artifacts = null,
            transcriptMeta = null,
            chapters = null
        )

        val result = writer.writeSessionMetadata(input)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `writePreprocessPatch appends patch to metaHub`() = runTest {
        traceStore.record(taskId = "job123")
        
        val input = PreprocessPatchInput(
            jobId = "job123",
            sessionId = "session456",
            transcriptMarkdown = "Hello, this is a test transcript."
        )

        val result = writer.writePreprocessPatch(input)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeMetaHub.appendM2PatchCalls.size)
        assertEquals("session456", fakeMetaHub.appendM2PatchCalls.first().first)
    }

    // =========================================================================
    // Fake for MetaHub interface
    // =========================================================================

    private class FakeMetaHub : MetaHub {
        val upsertSessionCalls = mutableListOf<SessionMetadata>()
        val appendM2PatchCalls = mutableListOf<Pair<String, M2PatchRecord>>()
        val upsertTranscriptCalls = mutableListOf<TranscriptMetadata>()

        override suspend fun upsertSession(metadata: SessionMetadata) {
            upsertSessionCalls.add(metadata)
        }

        override suspend fun getSession(sessionId: String): SessionMetadata? = null

        override suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord) {
            appendM2PatchCalls.add(sessionId to patch)
        }

        override suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState? = null

        override suspend fun upsertTranscript(metadata: TranscriptMetadata) {
            upsertTranscriptCalls.add(metadata)
        }

        override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? = null

        override suspend fun upsertExport(metadata: ExportMetadata) {}

        override suspend fun getExport(sessionId: String): ExportMetadata? = null

        override suspend fun logUsage(usage: TokenUsage) {}
    }
}
