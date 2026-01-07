// File: feature/chat/src/main/java/com/smartsales/domain/export/ExportCoordinator.kt
// Module: :feature:chat
// Summary: Export coordination interface - gate checking, PDF/CSV export, and share handling
// Author: created on 2026-01-05

package com.smartsales.domain.export

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.feature.chat.home.export.ExportGateState
import com.smartsales.feature.chat.home.export.ExportUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Export Coordinator: handles export gate checking, PDF/CSV export, and share flow.
 *
 * Responsibilities:
 * - Resolve export gate state (based on smart analysis completion)
 * - Execute PDF/CSV export
 * - Invoke ShareHandler for export results
 *
 * Design:
 * - Pure coordinator, no markdown preparation logic
 * - Markdown provided by caller (HomeViewModel)
 * - Decoupled from smart analysis - caller coordinates
 */
interface ExportCoordinator {

    val exportState: StateFlow<ExportUiState>

    /**
     * Check export gate state.
     *
     * Caller should invoke this before export to determine if export is allowed.
     */
    suspend fun checkExportGate(sessionId: String): ExportGateState

    /**
     * Execute export: export to specified format using caller-provided markdown.
     *
     * Precondition: caller should call checkExportGate() first to confirm gate passed.
     *
     * @param sessionId Session ID
     * @param format Export format (PDF/CSV)
     * @param userName User name
     * @param markdown Export content (prepared by caller)
     */
    suspend fun performExport(
        sessionId: String,
        format: ExportFormat,
        userName: String,
        markdown: String
    ): Result<Unit>

    /**
     * Clear snackbar message.
     */
    fun clearSnackbar()
}
