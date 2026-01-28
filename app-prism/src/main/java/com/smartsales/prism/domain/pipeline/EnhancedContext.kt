package com.smartsales.prism.domain.pipeline

import com.smartsales.prism.domain.model.Mode

/**
 * 增强上下文 — 统一输入载体
 * @see Prism-V1.md §2.2 #1
 */
data class EnhancedContext(
    val userText: String,
    val audioTranscripts: List<TranscriptBlock> = emptyList(),
    val imageAnalysis: List<VisionResult> = emptyList(),
    val memoryHits: List<MemoryHit> = emptyList(),
    val entityContext: Map<String, EntityRef> = emptyMap(),
    val modeMetadata: ModeMetadata = ModeMetadata()
)

/**
 * 转写片段（来自 Tingwu）
 */
data class TranscriptBlock(
    val text: String,
    val speakerId: String? = null,
    val startMs: Long = 0,
    val endMs: Long = 0
)

/**
 * 视觉分析结果（来自 Qwen-VL）
 */
data class VisionResult(
    val description: String,
    val ocrText: String? = null
)

/**
 * 记忆命中项（来自 Hot Zone）
 */
data class MemoryHit(
    val entryId: String,
    val content: String,
    val relevanceScore: Float = 1.0f
)

/**
 * 实体引用（来自 Relevancy Library）
 */
data class EntityRef(
    val entityId: String,
    val displayName: String,
    val entityType: String
)

/**
 * 模式元数据
 */
data class ModeMetadata(
    val currentMode: Mode = Mode.COACH,
    val sessionId: String = "",
    val turnIndex: Int = 0
)
