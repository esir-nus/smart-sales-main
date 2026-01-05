// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatPublisher.kt
// Module: :feature:chat
// Summary: V1-aligned ChatPublisher - extracts HumanDraft, validates MachineArtifact
// Author: created on 2026-01-05

package com.smartsales.domain.chat

import org.json.JSONObject

/**
 * ChatPublisher: V1-aligned publisher for chat responses.
 *
 * Per Orchestrator-V1 Section 3.2.4:
 * - Extracts HumanDraft from `<visible2user>` tags only
 * - Extracts MachineArtifact from fenced ```json blocks
 * - Validates artifact structure
 * - Decides retry/degrade strategy
 *
 * Design:
 * - No heuristic cleanup (explicitly forbidden by V1 Section 5.2)
 * - Deterministic extraction only
 * - Failure semantics per V1 Section 8
 */
object ChatPublisher {

    /**
     * Artifact validation status per V1 Section 7.1
     */
    enum class ArtifactStatus {
        VALID,      // Artifact parsed and validated successfully
        INVALID,    // Parse/validation failed, retry eligible
        RETRIED,    // After retry attempt
        FAILED      // Retries exhausted
    }

    /**
     * Published chat turn ready for UI rendering.
     * Per V1 Section 7.1 PublishedChatTurn contract.
     */
    data class PublishedChatTurn(
        val displayMarkdown: String,
        val mode: String?,  // L1/L2/L3
        val machineArtifact: JSONObject?,
        val artifactStatus: ArtifactStatus
    )

    /**
     * Extract HumanDraft from raw LLM output.
     *
     * V1 Contract (Section 5.2):
     * - Must be inside `<visible2user>...</visible2user>`
     * - Publisher renders ONLY this part
     *
     * @param raw Raw LLM output
     * @return Extracted visible content, or null if tag not found
     */
    fun extractHumanDraft(raw: String): String? {
        val regex = Regex("<visible2user>([\\s\\S]*?)</visible2user>", RegexOption.IGNORE_CASE)
        return regex.find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Extract MachineArtifact from raw LLM output.
     *
     * V1 Contract (Section 5.2):
     * - Must be in fenced ```json block OUTSIDE visible2user
     * - Explicitly forbidden: heuristic "guess JSON / regex extract JSON" from non-fenced text
     *
     * @param raw Raw LLM output
     * @return Parsed JSON object, or null if not found/invalid
     */
    fun extractMachineArtifact(raw: String): JSONObject? {
        // Remove visible2user content first to avoid extracting JSON from display text
        val withoutVisible = raw.replace(Regex("<visible2user>[\\s\\S]*?</visible2user>", RegexOption.IGNORE_CASE), "")
        
        // V1 Contract: Must be in fenced ```json block
        val fencedRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val jsonContent = fencedRegex.find(withoutVisible)?.groupValues?.getOrNull(1)?.trim()
            ?: return null
        
        return try {
            JSONObject(jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate MachineArtifact structure.
     *
     * V1 Contract (Section 7.2):
     * - Must have schemaVersion
     * - Must have mode (L1/L2/L3)
     *
     * @param artifact Parsed JSON object
     * @return true if valid
     */
    fun validateArtifact(artifact: JSONObject): Boolean {
        return artifact.has("schemaVersion") && artifact.has("mode")
    }

    /**
     * Extract mode from artifact or infer from content.
     */
    fun extractMode(artifact: JSONObject?): String? {
        return artifact?.optString("mode")?.takeIf { it.isNotEmpty() }
    }

    /**
     * Build a published chat turn from raw LLM output.
     *
     * This is the main entry point for publishing chat responses.
     *
     * @param raw Raw LLM output
     * @param fallbackToRaw If true, use raw content when visible2user not found
     * @return PublishedChatTurn ready for UI
     */
    fun publish(raw: String, fallbackToRaw: Boolean = true): PublishedChatTurn {
        val humanDraft = extractHumanDraft(raw)
        val artifact = extractMachineArtifact(raw)
        
        val displayMarkdown = when {
            humanDraft != null -> humanDraft
            fallbackToRaw -> raw.trim()
            else -> ""
        }
        
        val artifactStatus = when {
            artifact == null -> ArtifactStatus.INVALID
            !validateArtifact(artifact) -> ArtifactStatus.INVALID
            else -> ArtifactStatus.VALID
        }
        
        return PublishedChatTurn(
            displayMarkdown = displayMarkdown,
            mode = extractMode(artifact),
            machineArtifact = artifact,
            artifactStatus = artifactStatus
        )
    }

    /**
     * Simple display text extraction for streaming/partial responses.
     * Falls back to raw if no visible2user tag found.
     */
    fun extractDisplayText(raw: String): String {
        return extractHumanDraft(raw) ?: raw.trim()
    }

    // ====== Consolidated Extraction (Wave 8A) ======

    /**
     * Extract Metadata JSON from raw LLM output.
     *
     * Extracts content inside `<Metadata>...</Metadata>` tags.
     * Pattern tolerates whitespace around tag names.
     *
     * @param raw Raw LLM output
     * @return JSON string inside Metadata tags, or null if not found
     */
    fun extractMetadataJson(raw: String): String? {
        val regex = Regex("<\\s*Metadata\\s*>([\\s\\S]*?)<\\s*/\\s*Metadata\\s*>", RegexOption.IGNORE_CASE)
        return regex.find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Combined extraction result for GENERAL chat responses.
     * Consolidates visible text, metadata, and title candidate.
     */
    data class Channels(
        val visibleText: String?,
        val metadataJson: String?
    )

    /**
     * Extract all channels from raw GENERAL chat response.
     *
     * Consolidates: Visible2User + Metadata extraction.
     * Pattern tolerates whitespace around tag names for robustness.
     *
     * @param raw Raw LLM output
     * @return Channels containing extracted content
     */
    fun extractChannels(raw: String): Channels {
        // Use whitespace-tolerant pattern for Visible2User (matches ViewModel behavior)
        val visibleRegex = Regex("<\\s*Visible2User\\s*>([\\s\\S]*?)<\\s*/\\s*Visible2User\\s*>", RegexOption.IGNORE_CASE)
        val visibleText = visibleRegex.find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        val metadataJson = extractMetadataJson(raw)
        return Channels(
            visibleText = visibleText,
            metadataJson = metadataJson
        )
    }

    // ====== Wave 9: Display Resolution ======

    /**
     * Resolve final display text from raw LLM output.
     *
     * Consolidates V1/legacy display text selection logic:
     * - V1 mode: Use visibleMarkdown from V1FinalizeResult
     * - Legacy mode: Extract from channels or fallback to raw
     * - Smart Analysis: Pass through raw text (already final Markdown)
     *
     * @param rawFullText Raw LLM response
     * @param visibleText Extracted visible text from channels (legacy mode)
     * @param isSmartAnalysis Whether this is a SMART_ANALYSIS response
     * @param useV1Publisher Whether to use V1 publishing path
     * @param v1VisibleMarkdown Visible markdown from V1 finalizer (V1 mode)
     * @param onCompletedTransform Optional transformation callback
     * @return Final display text ready for UI
     */
    fun resolveDisplayText(
        rawFullText: String,
        visibleText: String?,
        isSmartAnalysis: Boolean,
        useV1Publisher: Boolean,
        v1VisibleMarkdown: String?,
        onCompletedTransform: ((String) -> String)? = null
    ): String {
        return if (useV1Publisher) {
            v1VisibleMarkdown.orEmpty()
        } else {
            val displaySource = visibleText ?: rawFullText
            val sanitized = if (isSmartAnalysis) {
                rawFullText  // Smart analysis already has final Markdown
            } else {
                extractDisplayText(displaySource)
            }
            
            if (isSmartAnalysis) {
                sanitized
            } else {
                onCompletedTransform?.invoke(sanitized) ?: sanitized
            }
        }
    }
}
