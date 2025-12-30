package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/GeneralChatV1Finalizer.kt
// Module: :feature:chat
// Summary: V1 finalizer seam for GENERAL chat publishing.
// Author: created on 2025-12-29

data class V1FinalizeResult(
    val visibleMarkdown: String,
    val artifactStatus: ArtifactStatus,
    val artifactJson: String?,
)

class GeneralChatV1Finalizer(
    private val publisher: ChatPublisher,
) {
    fun finalize(rawFullText: String): V1FinalizeResult {
        // V1：只渲染 <visible2user>，禁止从其他文本推断正文
        val published = publisher.publish(rawFullText)
        val visible = published.displayMarkdown.ifBlank { publisher.fallbackMessage() }
        // V1：机器结构只允许 fenced JSON，不做任何启发式提取
        return V1FinalizeResult(
            visibleMarkdown = visible,
            artifactStatus = published.artifactStatus,
            artifactJson = published.machineArtifactJson,
        )
    }
}
