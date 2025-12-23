package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/ConversationDerivedState.kt
// 模块：:core:util
// 说明：V7 M2 会话派生状态模型（严格对齐 schema）
// 作者：创建于 2025-12-23

/**
 * 说明：M2 字段与 docs/metahub-schema-v7.json 对齐，不自行扩展字段。
 */
data class ConversationDerivedState(
    val schemaVersion: String = M2_SCHEMA_VERSION,
    val updatedAt: Long = 0L,
    val version: Int = 0,
    val rawSignals: RawSignals = RawSignals(),
    val uiSignals: UiSignals = UiSignals(),
    val speakerRegistry: SpeakerRegistry = SpeakerRegistry(),
    val memoryBank: MemoryBank = MemoryBank(),
    val preprocess: PreprocessSnapshot = PreprocessSnapshot(),
    val smartAnalysisRefs: List<ArtifactRef> = emptyList(),
    val externalContextRefs: List<ExternalContextRef> = emptyList()
)

data class DerivedEnumWithProv(
    val value: String,
    val prov: Provenance
)

data class DerivedTextList(
    val items: List<String> = emptyList(),
    val prov: Provenance
)

data class RawSignals(
    val dealRiskLevel: DerivedEnumWithProv? = null,
    val stage: DerivedEnumWithProv? = null,
    val intent: DerivedEnumWithProv? = null,
    val highlights: DerivedTextList? = null,
    val painPoints: DerivedTextList? = null,
    val nextSteps: DerivedTextList? = null
)

data class UiSignals(
    val dealRiskLevel: String = "UNKNOWN",
    val stage: String = "",
    val progressPercent: Double = 0.0,
    val intentBadge: String = "",
    val lastStabilizedAt: Long? = null
)

data class SpeakerProfile(
    val speakerKey: String,
    val labelCandidate: String? = null,
    val labelAccepted: String? = null,
    val prov: Provenance? = null
)

data class SpeakerRegistry(
    val speakers: List<SpeakerProfile> = emptyList()
)

data class MemoryItem(
    val key: String,
    val summary: String,
    val prov: Provenance
)

data class MemoryBank(
    val people: List<MemoryItem> = emptyList(),
    val topics: List<MemoryItem> = emptyList()
)

data class SuspiciousBoundary(
    val index: Int,
    val reason: String,
    val detail: String? = null
)

data class IndexRange(
    val start: Int,
    val endInclusive: Int
)

data class BatchPlanItem(
    val batchId: String,
    val editableRange: IndexRange,
    val examineRange: IndexRange,
    val tailLookaheadEnabled: Boolean = false
)

data class PreprocessSnapshot(
    val first20Rendered: List<String> = emptyList(),
    val suspiciousBoundaries: List<SuspiciousBoundary> = emptyList(),
    val batchPlan: List<BatchPlanItem> = emptyList(),
    val prov: Provenance? = null
)

data class ArtifactRef(
    val artifactId: String? = null,
    val hash: String? = null,
    val createdAt: Long? = null
)

data class ExternalContextRef(
    val provider: String? = null,
    val purpose: String? = null,
    val queryHash: String? = null,
    val traceId: String? = null,
    val itemIds: List<String> = emptyList(),
    val totalTokens: Int? = null
)

const val M2_SCHEMA_VERSION: String = "7.0.0"
