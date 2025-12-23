package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/M2PatchRecord.kt
// 模块：:core:util
// 说明：M2 内部补丁记录（schema 未定义 patch type）
// 作者：创建于 2025-12-23

/**
 * 说明：M2PatchRecord 为内部派生结构（schema 未定义 patch type），仅用于存储与确定性合并。
 */
data class M2PatchRecord(
    val patchId: String,
    val createdAt: Long,
    val prov: Provenance,
    val payload: ConversationDerivedStateDelta
)

/**
 * 说明：M2 的增量补丁（字段与 schema 对齐，只允许整字段替换）。
 * - 所有字段可空，表示“未提供，不更新”。
 */
data class ConversationDerivedStateDelta(
    val rawSignals: RawSignals? = null,
    val uiSignals: UiSignals? = null,
    val speakerRegistry: SpeakerRegistry? = null,
    val memoryBank: MemoryBank? = null,
    val preprocess: PreprocessSnapshot? = null,
    val smartAnalysisRefs: List<ArtifactRef>? = null,
    val externalContextRefs: List<ExternalContextRef>? = null
)
