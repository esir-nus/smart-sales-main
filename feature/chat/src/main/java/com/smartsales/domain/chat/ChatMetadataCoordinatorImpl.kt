// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatMetadataCoordinatorImpl.kt
// Module: :feature:chat
// Summary: Implementation of ChatMetadataCoordinator - extracts metadata and writes M2/M3 patches
// Author: created on 2026-01-11

package com.smartsales.domain.chat

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.ConversationDerivedStateDelta
import com.smartsales.core.metahub.DerivedEnumWithProv
import com.smartsales.core.metahub.DerivedTextList
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.RawSignals
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatMetadataCoordinatorImpl: Extracts and persists chat metadata.
 *
 * Extracted from HomeViewModel for:
 * - Clean architecture alignment
 * - Reduced agent cognitive burden
 * - Single responsibility
 *
 * Writes to both M3 (SessionMetadata) and M2 (ConversationDerivedState patches).
 */
@Singleton
class ChatMetadataCoordinatorImpl @Inject constructor(
    private val metaHub: MetaHub
) : ChatMetadataCoordinator {

    override suspend fun processGeneralChatMetadata(
        sessionId: String,
        rawFullText: String,
        metadataJson: String?
    ): SessionMetadata? {
        val candidates = buildList {
            metadataJson?.takeIf { it.isNotBlank() }?.let { add(it) }
            MetadataParser.findLastJsonBlock(rawFullText)?.text?.let { tail ->
                if (tail != metadataJson) add(tail)
            }
        }
        val parsed = candidates.firstNotNullOfOrNull { 
            MetadataParser.parseGeneralChatMetadata(it, sessionId) 
        }
        val metadata = parsed?.takeIf { it.hasMeaningfulGeneralFields() } ?: return null
        val patch = metadata.copy(
            latestMajorAnalysisSource = AnalysisSource.GENERAL_FIRST_REPLY,
            latestMajorAnalysisAt = System.currentTimeMillis()
        )
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(patch) ?: patch
        runCatching { metaHub.upsertSession(merged) }
        // Also write M2 patch for chat-derived signals
        appendChatM2Patch(sessionId, merged)
        return merged
    }

    override suspend fun processV1GeneralChatMetadata(
        sessionId: String,
        result: V1FinalizeResult?
    ): SessionMetadata? {
        if (result == null) return null
        // Only L3 with VALID artifact allowed to write metadata
        if (result.artifactStatus != ArtifactStatus.VALID) return null
        val artifactJson = result.artifactJson?.takeIf { it.isNotBlank() } ?: return null
        val obj = runCatching { JSONObject(artifactJson) }.getOrNull() ?: return null
        if (obj.optString("mode") != "L3") return null
        // V1 only allows MachineArtifact.metadataPatch, no heuristic/legacy extraction
        val patchObj = obj.optJSONObject("metadataPatch") ?: return null
        val metadata = MetadataParser.parseGeneralChatMetadata(patchObj.toString(), sessionId)
            ?.takeIf { it.hasMeaningfulGeneralFields() }
            ?: return null
        val patch = metadata.copy(
            latestMajorAnalysisSource = AnalysisSource.GENERAL_FIRST_REPLY,
            latestMajorAnalysisAt = System.currentTimeMillis()
        )
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(patch) ?: patch
        runCatching { metaHub.upsertSession(merged) }
        // Also write M2 patch for chat-derived signals
        appendChatM2Patch(sessionId, merged)
        return merged
    }

    override suspend fun persistLatestAnalysisMarker(
        sessionId: String,
        source: AnalysisSource,
        messageId: String
    ): SessionMetadata? {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = sessionId)
        val updated = base.copy(
            latestMajorAnalysisMessageId = messageId,
            latestMajorAnalysisAt = System.currentTimeMillis(),
            latestMajorAnalysisSource = source
        )
        runCatching { metaHub.upsertSession(updated) }
        return updated
    }

    /**
     * Append M2 patch for chat-derived signals (stage, risk, highlights).
     * Per V1 §4: M2 stores tags, highlights, action items, key people.
     */
    private suspend fun appendChatM2Patch(sessionId: String, metadata: SessionMetadata) {
        val now = System.currentTimeMillis()
        val prov = Provenance(
            source = "llm.chat.metadataPatch",
            updatedAt = now
        )
        val rawSignals = RawSignals(
            stage = metadata.stage?.let { DerivedEnumWithProv(it.name, prov) },
            dealRiskLevel = metadata.riskLevel?.let { DerivedEnumWithProv(it.name, prov) },
            highlights = metadata.tags.takeIf { it.isNotEmpty() }?.let {
                DerivedTextList(it.toList(), prov)
            }
        )
        // Only write if there are signals to persist
        if (rawSignals.stage == null && rawSignals.dealRiskLevel == null && rawSignals.highlights == null) {
            return
        }
        val patch = M2PatchRecord(
            patchId = "chat_${sessionId}_$now",
            createdAt = now,
            prov = prov,
            payload = ConversationDerivedStateDelta(rawSignals = rawSignals)
        )
        runCatching { metaHub.appendM2Patch(sessionId, patch) }
    }

    private fun SessionMetadata.hasMeaningfulGeneralFields(): Boolean =
        !mainPerson.isNullOrBlank() ||
            !shortSummary.isNullOrBlank() ||
            !summaryTitle6Chars.isNullOrBlank() ||
            !location.isNullOrBlank()
}
