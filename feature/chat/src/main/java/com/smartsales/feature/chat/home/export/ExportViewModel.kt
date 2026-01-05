package com.smartsales.feature.chat.home.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/export/ExportViewModel.kt
// 模块：:feature:chat
// 说明：导出逻辑，从 HomeScreenViewModel 中提取（简化版：纯执行器）
// 作者：创建于 2026-01-05

/**
 * 导出 ViewModel：负责导出门禁判断、PDF/CSV 导出及分享流程。
 *
 * 职责：
 * - 解析导出门禁状态（基于智能分析完成度）
 * - 执行 PDF/CSV 导出
 * - 调用 ShareHandler 分享导出结果
 *
 * 设计原则：
 * - 纯导出执行器，不处理 markdown 准备逻辑
 * - Markdown 由调用方（HomeScreenViewModel）提供
 * - 与智能分析解耦，由调用方协调
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val exportOrchestrator: ExportOrchestrator,
    private val shareHandler: ChatShareHandler,
) : ViewModel() {

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState

    /**
     * 检查导出门禁状态。
     * 
     * 调用方应在导出前调用此方法判断是否允许导出。
     */
    suspend fun checkExportGate(sessionId: String): ExportGateState {
        val summary = sessionRepository.findById(sessionId)
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        return resolveExportGateState(sessionId, summary, meta)
    }

    /**
     * 执行导出：使用调用方提供的 markdown 内容导出为指定格式。
     *
     * 前置条件：调用方应先调用 checkExportGate() 确认门禁通过。
     * 
     * @param sessionId 会话 ID
     * @param format 导出格式（PDF/CSV）
     * @param userName 用户名
     * @param markdown 导出内容（由调用方准备）
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

        // 重新获取门禁状态以获取 resolved name
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
     * 解析导出门禁状态：基于智能分析完成度判断是否允许导出。
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
     * 清除 snackbar 消息。
     */
    fun clearSnackbar() {
        _exportState.update { it.copy(snackbarMessage = null) }
    }
}
