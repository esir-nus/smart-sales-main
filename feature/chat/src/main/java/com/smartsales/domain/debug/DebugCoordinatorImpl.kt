// File: feature/chat/src/main/java/com/smartsales/domain/debug/DebugCoordinatorImpl.kt
// Module: :feature:chat
// Summary: Debug panel data management implementation - HUD snapshots and trace collection
// Author: created on 2026-01-07

package com.smartsales.domain.debug

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.core.metahub.SessionMetadataLabelProvider
import com.smartsales.data.aicore.params.TranscriptionLaneSelector
import com.smartsales.domain.debug.DebugSessionMetadata
import com.smartsales.domain.debug.DebugUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug Coordinator implementation.
 */
@Singleton
class DebugCoordinatorImpl @Inject constructor(
    private val metaHub: MetaHub,
    private val sessionRepository: AiSessionRepository,
    private val debugOrchestrator: DebugOrchestrator,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,
) : DebugCoordinator {

    private val _debugState = MutableStateFlow(DebugUiState())
    override val debugState: StateFlow<DebugUiState> = _debugState

    override fun toggleDebugPanel() {
        _debugState.update { it.copy(visible = !it.visible) }
    }

    override suspend fun refreshSessionMetadata(
        sessionId: String,
        sessionTitle: String,
        extraNotes: List<String>
    ) {
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        updateSessionMetadata(sessionId, sessionTitle, meta, extraNotes)
    }

    override suspend fun refreshDebugSnapshot(
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

    override fun refreshTraces() {
        _debugState.update {
            it.copy(
                tingwuTrace = tingwuTraceStore.getSnapshot()
            )
        }
    }

    override fun appendDebugNote(note: String) {
        val current = _debugState.value.sessionMetadata
        val updated = current?.copy(notes = (current.notes + note).distinct())
        _debugState.update { it.copy(sessionMetadata = updated) }
    }

    override fun clear() {
        _debugState.update {
            it.copy(
                sessionMetadata = null,
                snapshot = null
            )
        }
    }

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

        // Important: HUD needs to show transcription lane selection and disabled reason.
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
            notes = mergedNotes
        )

        _debugState.update { it.copy(sessionMetadata = debug) }
    }
}
