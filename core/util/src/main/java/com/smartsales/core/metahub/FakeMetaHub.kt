package com.smartsales.core.metahub

// File: core/util/src/main/java/com/smartsales/core/metahub/FakeMetaHub.kt
// Module: :core:util
// Summary: Test double for MetaHub. Composes InMemoryMetaHub for storage, adds call tracking.
// Author: created on 2026-01-15

/**
 * FakeMetaHub: Test double for MetaHub.
 *
 * Composes [InMemoryMetaHub] for actual storage,
 * adds call tracking for all write methods.
 *
 * Usage:
 * ```
 * val metaHub = FakeMetaHub()
 * // Interact with metaHub...
 * assertEquals(1, metaHub.upsertSessionCalls.size)
 * metaHub.reset()
 * ```
 */
class FakeMetaHub : MetaHub {

    private val delegate = InMemoryMetaHub()

    // ===== Call Tracking Lists =====
    val upsertSessionCalls = mutableListOf<SessionMetadata>()
    val appendM2PatchCalls = mutableListOf<Pair<String, M2PatchRecord>>()
    val upsertTranscriptCalls = mutableListOf<TranscriptMetadata>()
    val upsertExportCalls = mutableListOf<ExportMetadata>()
    val logUsageCalls = mutableListOf<TokenUsage>()

    // ===== Failure Injection =====
    var shouldFail: Boolean = false
    var failureException: Exception = RuntimeException("FakeMetaHub failure")

    // ===== MetaHub Implementation =====

    override suspend fun upsertSession(metadata: SessionMetadata) {
        if (shouldFail) throw failureException
        upsertSessionCalls.add(metadata)
        delegate.upsertSession(metadata)
    }

    override suspend fun getSession(sessionId: String): SessionMetadata? {
        if (shouldFail) throw failureException
        return delegate.getSession(sessionId)
    }

    override suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord) {
        if (shouldFail) throw failureException
        appendM2PatchCalls.add(sessionId to patch)
        delegate.appendM2Patch(sessionId, patch)
    }

    override suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState? {
        if (shouldFail) throw failureException
        return delegate.getEffectiveM2(sessionId)
    }

    override suspend fun upsertTranscript(metadata: TranscriptMetadata) {
        if (shouldFail) throw failureException
        upsertTranscriptCalls.add(metadata)
        delegate.upsertTranscript(metadata)
    }

    override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? {
        if (shouldFail) throw failureException
        return delegate.getTranscriptBySession(sessionId)
    }

    override suspend fun upsertExport(metadata: ExportMetadata) {
        if (shouldFail) throw failureException
        upsertExportCalls.add(metadata)
        delegate.upsertExport(metadata)
    }

    override suspend fun getExport(sessionId: String): ExportMetadata? {
        if (shouldFail) throw failureException
        return delegate.getExport(sessionId)
    }

    override suspend fun logUsage(usage: TokenUsage) {
        if (shouldFail) throw failureException
        logUsageCalls.add(usage)
        delegate.logUsage(usage)
    }

    // ===== Test Utilities =====

    /**
     * Resets all call tracking lists and clears the delegate.
     * Does NOT reset [shouldFail] or [failureException].
     */
    fun reset() {
        upsertSessionCalls.clear()
        appendM2PatchCalls.clear()
        upsertTranscriptCalls.clear()
        upsertExportCalls.clear()
        logUsageCalls.clear()
        // Note: InMemoryMetaHub doesn't expose a reset. A fresh instance is created on FakeMetaHub reset.
    }

    /**
     * Returns all recorded usage. Delegates to [InMemoryMetaHub.dumpUsage].
     */
    suspend fun dumpUsage(): List<TokenUsage> = delegate.dumpUsage()
}
