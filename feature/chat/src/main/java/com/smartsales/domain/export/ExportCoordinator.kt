// File: feature/chat/src/main/java/com/smartsales/domain/export/ExportCoordinator.kt
// Module: :feature:chat
// Summary: Export coordination logic - gate checking, PDF/CSV export, and share handling
// Author: created on 2026-01-05

package com.smartsales.domain.export

import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.home.export.ExportGateState
import com.smartsales.feature.chat.home.export.ExportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class ExportCoordinator @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val exportOrchestrator: ExportOrchestrator,
    private val shareHandler: ChatShareHandler,
) {

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState

    /**
     * Check export gate state.
     *
     * Caller should invoke this before export to determine if export is allowed.
     */
    suspend fun checkExportGate(sessionId: String): ExportGateState {
        val summary = sessionRepository.findById(sessionId)
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        return resolveExportGateState(sessionId, summary, meta)
    }

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
    ): Result<Unit> {
        if (_exportState.value.inProgress) {
            return Result.Error(IllegalStateException("导出正在进行中"))
        }

        if (format == ExportFormat.PDF && markdown.isBlank()) {
            _exportState.update {
                it.copy(snackbarMessage = "暂无可导出的内容")
            }
            return Result.Error(IllegalStateException("暂无可导出的内容"))
        }

        _exportState.update { it.copy(inProgress = true, snackbarMessage = null) }

        // Re-fetch gate state to get resolved name
        val gate = checkExportGate(sessionId)
        val sessionTitle = gate.resolvedName

        val result = when (format) {
            ExportFormat.PDF -> exportOrchestrator.exportPdf(
                sessionId,
                markdown,
                sessionTitle,
                userName
            )
            ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId, sessionTitle, userName)
        }

        return when (result) {
            is Result.Success -> {
                when (val share = shareHandler.shareExport(result.data)) {
                    is Result.Success -> {
                        _exportState.update { it.copy(inProgress = false) }
                        Result.Success(Unit)
                    }
                    is Result.Error -> {
                        _exportState.update {
                            it.copy(
                                inProgress = false,
                                snackbarMessage = share.throwable.message ?: "分享失败"
                            )
                        }
                        Result.Error(share.throwable)
                    }
                }
            }
            is Result.Error -> {
                _exportState.update {
                    it.copy(
                        inProgress = false,
                        snackbarMessage = result.throwable.message ?: "导出失败"
                    )
                }
                Result.Error(result.throwable)
            }
        }
    }

    /**
     * Resolve export gate state: determine if export is allowed based on smart analysis completion.
     */
    private fun resolveExportGateState(
        sessionId: String,
        summary: AiSessionSummary?,
        meta: SessionMetadata?
    ): ExportGateState {
        val ready = meta?.latestMajorAnalysisMessageId != null
        val reason = if (ready) "" else "需先完成智能分析"
        val resolution = ExportNameResolver.resolve(
            sessionId = sessionId,
            sessionTitle = summary?.title,
            isTitleUserEdited = summary?.isTitleUserEdited,
            meta = meta
        )
        return ExportGateState(
            ready = ready,
            reason = reason,
            resolvedName = resolution.baseName,
            nameSource = resolution.source
        )
    }

    /**
     * Clear snackbar message.
     */
    fun clearSnackbar() {
        _exportState.update { it.copy(snackbarMessage = null) }
    }
}
