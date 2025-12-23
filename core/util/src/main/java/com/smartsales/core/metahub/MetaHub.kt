package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt
// 模块：:core:util
// 说明：元数据中心接口，统一读写会话/转写/导出/用量元数据
// 作者：创建于 2025-12-04

/**
 * 元数据中心，统一管理结构化元数据，不处理原始聊天或转写正文。
 */
interface MetaHub {
    /**
     * 写入或覆盖会话级元数据。
     */
    suspend fun upsertSession(metadata: SessionMetadata)

    /**
     * 按会话读取元数据。
     */
    suspend fun getSession(sessionId: String): SessionMetadata?

    /**
     * 追加 M2 补丁（内部派生结构），用于计算有效的 ConversationDerivedState。
     */
    suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord)

    /**
     * 读取有效的 M2 会话派生状态。
     */
    suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState?

    /**
     * 写入或覆盖转写元数据。
     */
    suspend fun upsertTranscript(metadata: TranscriptMetadata)

    /**
     * 按会话读取转写元数据。
     * 当前假设一对一，如需一对多后续可扩展。
     */
    suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata?

    /**
     * 写入或覆盖导出元数据。
     */
    suspend fun upsertExport(metadata: ExportMetadata)

    /**
     * 读取导出元数据。
     */
    suspend fun getExport(sessionId: String): ExportMetadata?

    /**
     * 记录一次模型用量，供统计或日志使用。
     */
    suspend fun logUsage(usage: TokenUsage)
}
