package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/PublishedChatTurnV1.kt
// Module: :feature:chat
// Summary: Published chat turn payload for V1 rendering.
// Author: created on 2025-12-29

data class PublishedChatTurnV1(
    val displayMarkdown: String,
    val machineArtifactJson: String?,
    val artifactStatus: ArtifactStatus,
    val retryCount: Int,
    val failureReason: String?
)
