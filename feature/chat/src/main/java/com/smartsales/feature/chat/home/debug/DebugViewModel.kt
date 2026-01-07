package com.smartsales.feature.chat.home.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.core.metahub.SessionMetadataLabelProvider
import com.smartsales.data.aicore.params.TranscriptionLaneSelector
import com.smartsales.domain.debug.DebugUiState
import com.smartsales.domain.debug.DebugSessionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/debug/DebugViewModel.kt
// 模块：:feature:chat
// 说明：调试面板 ViewModel，从 HomeViewModel 中提取
// 作者：创建于 2026-01-05

/**
 * 调试面板 ViewModel：负责 HUD 调试数据的获取与展示。
 *
 * 职责：
 * - 管理调试面板可见性
 * - 获取会话调试元数据
 * - 获取 HUD 调试快照
 * - 刷新 Xfyun/Tingwu 追踪数据
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val debugOrchestrator: DebugOrchestrator,
    private val xfyunTraceStore: XfyunTraceStore,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,
) : ViewModel() {

    private val _debugState = MutableStateFlow(DebugUiState())
    val debugState: StateFlow<DebugUiState> = _debugState

    /**
     * 切换调试面板可见性。
     */
    fun toggleDebugPanel() {
        _debugState.update { it.copy(visible = !it.visible) }
    }

    /**
     * 刷新会话调试元数据。
     *
     * @param sessionId 会话 ID
     * @param sessionTitle 会话标题
     * @param extraNotes 额外的调试备注
     */
    suspend fun refreshSessionMetadata(
        sessionId: String,
        sessionTitle: String,
        extraNotes: List<String> = emptyList()
    ) {
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        updateSessionMetadata(sessionId, sessionTitle, meta, extraNotes)
    }

    /**
     * 刷新 HUD 调试快照。
     *
     * @param sessionId 会话 ID
     * @param jobId 转写任务 ID
     * @param sessionTitle 会话标题
     * @param isTitleUserEdited 标题是否用户编辑
     */
    suspend fun refreshDebugSnapshot(
        sessionId: String,
        jobId: String?,
        sessionTitle: String,
        isTitleUserEdited: Boolean?
    ) {
        val snapshot = runCatching {
            debugOrchestrator.getDebugSnapshot(
                sessionId = sessionId,
                jobId = jobId,
                sessionTitle = sessionTitle,
                isTitleUserEdited = isTitleUserEdited
            )
        }.getOrElse { error ->
            // 重要：HUD 的调试快照失败时要 fail-soft，避免阻断调试面板展示。
            DebugSnapshot(
                section1EffectiveRunText = "DebugSnapshot failed: ${error.message ?: "unknown"}",
                section2RawTranscriptionText = "(missing: debug snapshot unavailable)",
                section3PreprocessedText = "(missing: debug snapshot unavailable)",
                sessionId = sessionId,
                jobId = jobId,
            )
        }
        _debugState.update { it.copy(snapshot = snapshot) }
    }

    /**
     * 刷新追踪快照（Xfyun + Tingwu）。
     */
    fun refreshTraces() {
        _debugState.update {
            it.copy(
                xfyunTrace = xfyunTraceStore.getSnapshot(),
                tingwuTrace = tingwuTraceStore.getSnapshot()
            )
        }
    }

    /**
     * 添加调试备注。
     */
    fun appendDebugNote(note: String) {
        val current = _debugState.value.sessionMetadata
        val updated = current?.copy(notes = (current.notes + note).distinct())
        _debugState.update { it.copy(sessionMetadata = updated) }
    }

    /**
     * 清空调试状态（会话切换时）。
     */
    fun clear() {
        _debugState.update {
            it.copy(
                sessionMetadata = null,
                snapshot = null
            )
        }
    }

    /**
     * 更新会话调试元数据。
     */
    private fun updateSessionMetadata(
        sessionId: String,
        sessionTitle: String,
        meta: SessionMetadata?,
        extraNotes: List<String>
    ) {
        val existingNotes = _debugState.value.sessionMetadata
            ?.takeIf { it.sessionId == sessionId }
            ?.notes
            .orEmpty()
        val mergedNotes = (existingNotes + extraNotes).distinct()

        val stageLabel = meta?.stage?.let { SessionMetadataLabelProvider.stageLabel(it) }
        val riskLabel = meta?.riskLevel?.let { SessionMetadataLabelProvider.riskLabel(it) }
        val tagsLabel = meta?.tags
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                SessionMetadataLabelProvider.tagsLabel(
                    tags = it,
                    limit = Int.MAX_VALUE,
                    delimiter = "、",
                    maxLength = Int.MAX_VALUE,
                    sort = true
                )
            }
        val latestSourceLabel = meta?.latestMajorAnalysisSource
            ?.let { SessionMetadataLabelProvider.sourceLabel(it) }
        val latestAtLabel = SessionMetadataLabelProvider
            .timeLabel(meta?.latestMajorAnalysisAt)
            .takeIf { it.isNotBlank() }

        // 重要：HUD 需展示转写链路选择与禁用原因，避免"看起来切了但实际没生效"。
        val laneDecision = TranscriptionLaneSelector.resolve(aiParaSettingsRepository.snapshot())

        val debug = DebugSessionMetadata(
            sessionId = sessionId,
            title = sessionTitle,
            mainPerson = meta?.mainPerson,
            shortSummary = meta?.shortSummary,
            summaryTitle6Chars = meta?.summaryTitle6Chars,
            stageLabel = stageLabel,
            riskLabel = riskLabel,
            tagsLabel = tagsLabel,
            latestSourceLabel = latestSourceLabel,
            latestAtLabel = latestAtLabel,
            transcriptionProviderRequested = laneDecision.requestedProvider,
            transcriptionProviderSelected = laneDecision.selectedProvider,
            transcriptionProviderDisabledReason = laneDecision.disabledReason,
            transcriptionXfyunEnabledSetting = laneDecision.xfyunEnabledSetting,
            notes = mergedNotes
        )

        _debugState.update { it.copy(sessionMetadata = debug) }
    }
}
