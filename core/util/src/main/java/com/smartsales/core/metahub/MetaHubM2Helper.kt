package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/MetaHubM2Helper.kt
// 模块：:core:util
// 说明：M2 补丁应用与有效态合并（确定性）
// 作者：创建于 2025-12-23

/**
 * 说明：
 * - M2PatchRecord 为内部派生结构（schema 未定义 patch type）。
 * - 合并规则：按追加顺序应用，后写覆盖（仅覆盖 patch 中显式提供的字段）。
 * - 读取时不产生新时间戳，确保确定性。
 */
fun applyM2PatchHistory(
    patchHistory: List<M2PatchRecord>
): ConversationDerivedState {
    var effective = ConversationDerivedState()
    for (patch in patchHistory) {
        val delta = patch.payload
        effective = effective.copy(
            rawSignals = delta.rawSignals ?: effective.rawSignals,
            uiSignals = delta.uiSignals ?: effective.uiSignals,
            speakerRegistry = delta.speakerRegistry ?: effective.speakerRegistry,
            memoryBank = delta.memoryBank ?: effective.memoryBank,
            preprocess = delta.preprocess ?: effective.preprocess,
            smartAnalysisRefs = delta.smartAnalysisRefs ?: effective.smartAnalysisRefs,
            externalContextRefs = delta.externalContextRefs ?: effective.externalContextRefs
        )
    }
    val lastPatchAt = patchHistory.lastOrNull()?.createdAt ?: 0L
    return effective.copy(
        updatedAt = lastPatchAt,
        version = patchHistory.size
    )
}
