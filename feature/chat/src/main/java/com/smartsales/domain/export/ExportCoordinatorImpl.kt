// File: feature/chat/src/main/java/com/smartsales/domain/export/ExportCoordinatorImpl.kt
// Module: :feature:chat
// Summary: Export coordination implementation - gate checking, PDF/CSV export, and share handling
// Author: created on 2026-01-07

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
import com.smartsales.domain.export.ExportGateState
import com.smartsales.domain.export.ExportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export Coordinator implementation.
 */
@Singleton
class ExportCoordinatorImpl @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val exportOrchestrator: ExportOrchestrator,
    private val shareHandler: ChatShareHandler,
) : ExportCoordinator {

    private val _exportState = MutableStateFlow(ExportUiState())
    override val exportState: StateFlow<ExportUiState> = _exportState

    override suspend fun checkExportGate(sessionId: String): ExportGateState {
        val summary = sessionRepository.findById(sessionId)
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        return resolveExportGateState(sessionId, summary, meta)
    }

    override suspend fun performExport(
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

    override fun clearSnackbar() {
        _exportState.update { it.copy(snackbarMessage = null) }
    }

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
}
