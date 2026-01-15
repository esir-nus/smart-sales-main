package com.smartsales.data.aicore.tingwu.metadata

import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuJobArtifacts

// ============================================================================
// MetaHubWriter: Lattice Box for writing Tingwu results to MetaHub
// Per Orchestrator-Lattice.md §2: DTOs + Interface + Fake in one file
// ============================================================================

/**
 * Input for session metadata writing.
 */
data class SessionMetadataWriteInput(
    val jobId: String,
    val sessionId: String,
    val audioAssetId: String,
    val artifacts: TingwuJobArtifacts?,
    val transcriptMeta: TranscriptMetadata?,
    val chapters: List<TingwuChapter>?
)

/**
 * Input for preprocess patch writing.
 */
data class PreprocessPatchInput(
    val jobId: String,
    val sessionId: String,
    val transcriptMarkdown: String
)

/**
 * MetaHubWriter: Lattice interface for writing Tingwu metadata to MetaHub.
 *
 * Responsibilities:
 * - Write session metadata (M3) with analysis source
 * - Write transcript metadata (M2B) with chapters
 * - Write preprocess patches for HUD display
 */
interface MetaHubWriter {
    /**
     * Writes session metadata and transcript chapters to MetaHub.
     */
    suspend fun writeSessionMetadata(input: SessionMetadataWriteInput): Result<Unit>

    /**
     * Appends a preprocess patch for HUD display.
     */
    suspend fun writePreprocessPatch(input: PreprocessPatchInput): Result<Unit>
}

/**
 * FakeMetaHubWriter: Test double for MetaHubWriter.
 */
class FakeMetaHubWriter : MetaHubWriter {

    val sessionMetadataWrites = mutableListOf<SessionMetadataWriteInput>()
    val preprocessPatchWrites = mutableListOf<PreprocessPatchInput>()

    var shouldFail = false
    var failureException: Exception = RuntimeException("FakeMetaHubWriter failure")

    override suspend fun writeSessionMetadata(input: SessionMetadataWriteInput): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        sessionMetadataWrites.add(input)
        return Result.success(Unit)
    }

    override suspend fun writePreprocessPatch(input: PreprocessPatchInput): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        preprocessPatchWrites.add(input)
        return Result.success(Unit)
    }

    fun reset() {
        sessionMetadataWrites.clear()
        preprocessPatchWrites.clear()
        shouldFail = false
    }
}
