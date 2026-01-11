// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatMetadataCoordinator.kt
// Module: :feature:chat
// Summary: Metadata processing coordinator - extracts and persists chat metadata
// Author: created on 2026-01-11

package com.smartsales.domain.chat

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult

/**
 * ChatMetadataCoordinator: Domain-layer coordinator for chat metadata extraction.
 *
 * Responsibilities:
 * - Parse metadata from general chat responses
 * - Parse metadata from V1 MachineArtifact (metadataPatch)
 * - Persist to MetaHub (M3 + M2 patches)
 * - Return result for UI state updates (callback pattern)
 *
 * Design:
 * - Returns data, no UI side effects
 * - Stateless
 */
interface ChatMetadataCoordinator {

    /**
     * Process general chat metadata from raw LLM response.
     * Used for legacy/fallback metadata extraction.
     *
     * @return Merged SessionMetadata if meaningful fields found, null otherwise
     */
    suspend fun processGeneralChatMetadata(
        sessionId: String,
        rawFullText: String,
        metadataJson: String?
    ): SessionMetadata?

    /**
     * Process V1 MachineArtifact metadataPatch.
     * Only processes L3 mode with valid artifact.
     *
     * @return Merged SessionMetadata if L3 metadataPatch found, null otherwise
     */
    suspend fun processV1GeneralChatMetadata(
        sessionId: String,
        result: V1FinalizeResult?
    ): SessionMetadata?

    /**
     * Persist latest analysis marker to MetaHub.
     *
     * @return Updated SessionMetadata after persistence
     */
    suspend fun persistLatestAnalysisMarker(
        sessionId: String,
        source: AnalysisSource,
        messageId: String
    ): SessionMetadata?
}
