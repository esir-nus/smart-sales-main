package com.smartsales.domain.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeDebugCoordinator : DebugCoordinator {
    private val _debugState = MutableStateFlow(DebugUiState())
    override val debugState: StateFlow<DebugUiState> = _debugState

    var toggleCalls = 0
    val refreshMetadataCalls = mutableListOf<Triple<String, String, List<String>>>()
    val refreshSnapshotCalls = mutableListOf<SnapshotParams>()
    var refreshTracesCalls = 0
    val appendNoteCalls = mutableListOf<String>()
    var clearCalls = 0

    data class SnapshotParams(
        val sessionId: String,
        val jobId: String?,
        val sessionTitle: String,
        val isTitleUserEdited: Boolean?
    )

    override fun toggleDebugPanel() {
        toggleCalls++
    }

    override suspend fun refreshSessionMetadata(
        sessionId: String,
        sessionTitle: String,
        extraNotes: List<String>
    ) {
        refreshMetadataCalls.add(Triple(sessionId, sessionTitle, extraNotes))
    }

    override suspend fun refreshDebugSnapshot(
        sessionId: String,
        jobId: String?,
        sessionTitle: String,
        isTitleUserEdited: Boolean?
    ) {
        refreshSnapshotCalls.add(SnapshotParams(sessionId, jobId, sessionTitle, isTitleUserEdited))
    }

    override fun refreshTraces() {
        refreshTracesCalls++
    }

    override fun appendDebugNote(note: String) {
        appendNoteCalls.add(note)
    }

    override fun clear() {
        clearCalls++
    }

    fun reset() {
        toggleCalls = 0
        refreshMetadataCalls.clear()
        refreshSnapshotCalls.clear()
        refreshTracesCalls = 0
        appendNoteCalls.clear()
        clearCalls = 0
    }
}
