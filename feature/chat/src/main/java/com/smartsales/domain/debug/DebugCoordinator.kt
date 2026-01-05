// File: feature/chat/src/main/java/com/smartsales/domain/debug/DebugCoordinator.kt
// Module: :feature:chat
// Summary: Debug panel data management, HUD snapshots, and trace collection
// Author: created on 2026-01-05

package com.smartsales.domain.debug

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
import com.smartsales.feature.chat.home.debug.DebugSessionMetadata
import com.smartsales.feature.chat.home.debug.DebugUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug Coordinator: handles HUD debug data retrieval and display.
 *
 * Responsibilities:
 * - Manage debug panel visibility
 * - Get session debug metadata
 * - Get HUD debug snapshots
 * - Refresh Xfyun/Tingwu trace data
 *
 * Design:
 * - Stateful coordinator for debug information
 * - Fail-soft on errors to avoid blocking debug panel
 */
@Singleton
class DebugCoordinator @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val debugOrchestrator: DebugOrchestrator,
    private val xfyunTraceStore: XfyunTraceStore,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,
) {

    private val _debugState = MutableStateFlow(DebugUiState())
    val debugState: StateFlow<DebugUiState> = _debugState

    /**
     * Toggle debug panel visibility.
     */
    fun toggleDebugPanel() {
        _debugState.update { it.copy(visible = !it.visible) }
    }

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
    ) {
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        updateSessionMetadata(sessionId, sessionTitle, meta, extraNotes)
    }

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
    ) {
        val snapshot = runCatching {
            debugOrchestrator.getDebugSnapshot(
                sessionId = sessionId,
                jobId = jobId,
                sessionTitle = sessionTitle,
                isTitleUserEdited = isTitleUserEdited
            )
        }.getOrElse { error ->
            // Important: HUD debug snapshot failure must fail-soft to avoid blocking debug panel display.
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
     * Refresh trace snapshots (Xfyun + Tingwu).
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
     * Append debug note.
     */
    fun appendDebugNote(note: String) {
        val current = _debugState.value.sessionMetadata
        val updated = current?.copy(notes = (current.notes + note).distinct())
        _debugState.update { it.copy(sessionMetadata = updated) }
    }

    /**
     * Clear debug state (on session switch).
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
     * Update session debug metadata.
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

        // Important: HUD needs to show transcription lane selection and disabled reason, avoid "looks switched but not actually effective".
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
