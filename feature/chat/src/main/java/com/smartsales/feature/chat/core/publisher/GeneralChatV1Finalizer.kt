package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/GeneralChatV1Finalizer.kt
// Module: :feature:chat
// Summary: V1 finalizer seam for GENERAL chat publishing.
// Author: created on 2025-12-29

data class V1FinalizeResult(
    val visibleMarkdown: String,
    val artifactStatus: ArtifactStatus,
    val artifactJson: String?,
    val failureReason: String? = null,
)

class GeneralChatV1Finalizer(
    private val publisher: ChatPublisher,
) {
    fun finalize(rawFullText: String, isTerminal: Boolean = false): V1FinalizeResult {
        // V1：只渲染 <visible2user>，禁止从其他文本推断正文
        val published = publisher.publish(rawFullText)
        val visible = resolveVisibleMarkdown(
            rawFullText = rawFullText,
            published = published,
            isTerminal = isTerminal
        )
        // V1：机器结构只允许 fenced JSON，不做任何启发式提取
        // 透传 failureReason 供上层重试策略判断，不改变发布内容/校验结果
        return V1FinalizeResult(
            visibleMarkdown = visible,
            artifactStatus = resolveArtifactStatus(published, isTerminal, visible),
            artifactJson = published.machineArtifactJson,
            failureReason = published.failureReason,
        )
    }

    private fun resolveVisibleMarkdown(
        rawFullText: String,
        published: PublishedChatTurnV1,
        isTerminal: Boolean
    ): String {
        if (!isTerminal) {
            // 只在终止时兜底，避免提前掩盖格式错误，影响重试
            return published.displayMarkdown
        }
        if (published.artifactStatus == ArtifactStatus.VALID) {
            return published.displayMarkdown
        }
        if (published.displayMarkdown.isNotBlank()) {
            // 优先使用已提取的可见内容，避免不必要的回退
            return published.displayMarkdown
        }
        // 终止兜底：剥离 ```json 防止泄露到 UI，且保持确定性
        return rawFullText
            .replace(Regex("```json[\\s\\S]*?```", RegexOption.IGNORE_CASE), "")
            .replace("<visible2user>", "")
            .replace("</visible2user>", "")
            .trim()
    }

    private fun resolveArtifactStatus(
        published: PublishedChatTurnV1,
        isTerminal: Boolean,
        visible: String
    ): ArtifactStatus {
        if (!isTerminal) {
            return published.artifactStatus
        }
        if (published.artifactStatus == ArtifactStatus.VALID) {
            return published.artifactStatus
        }
        return if (visible == published.displayMarkdown && visible.isNotBlank()) {
            published.artifactStatus
        } else {
            ArtifactStatus.FAILED
        }
    }
}
