// File: feature/chat/src/main/java/com/smartsales/domain/debug/DebugCoordinator.kt
// Module: :feature:chat
// Summary: Debug panel data management interface - HUD snapshots and trace collection
// Author: created on 2026-01-05

package com.smartsales.domain.debug

import com.smartsales.domain.debug.DebugUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Debug Coordinator: handles HUD debug data retrieval and display.
 *
 * Responsibilities:
 * - Manage debug panel visibility
 * - Get session debug metadata
 * - Get HUD debug snapshots
 * - Refresh Tingwu trace data
 *
 * Design:
 * - Stateful coordinator for debug information
 * - Fail-soft on errors to avoid blocking debug panel
 */
interface DebugCoordinator {

    val debugState: StateFlow<DebugUiState>

    /**
     * Toggle debug panel visibility.
     */
    fun toggleDebugPanel()

    /**
     * Refresh session debug metadata.
     *
     * @param sessionId Session ID
     * @param sessionTitle Session title
     * @param extraNotes Extra debug notes
     */
    suspend fun refreshSessionMetadata(
        sessionId: String,
        sessionTitle: String,
        extraNotes: List<String> = emptyList()
    )

    /**
     * Refresh HUD debug snapshot.
     *
     * @param sessionId Session ID
     * @param jobId Transcription job ID
     * @param sessionTitle Session title
     * @param isTitleUserEdited Whether title is user-edited
     */
    suspend fun refreshDebugSnapshot(
        sessionId: String,
        jobId: String?,
        sessionTitle: String,
        isTitleUserEdited: Boolean?
    )

    /**
     * Refresh trace snapshots (Tingwu).
     */
    fun refreshTraces()

    /**
     * Append debug note.
     */
    fun appendDebugNote(note: String)

    /**
     * Clear debug state (on session switch).
     */
    fun clear()
}
