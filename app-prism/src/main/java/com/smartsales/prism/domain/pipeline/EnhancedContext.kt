package com.smartsales.prism.domain.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.HabitContext

/**
 * 增强上下文 — 统一输入载体
 * @see Prism-V1.md §2.2 #1
 */
data class EnhancedContext(
    val userText: String,
    val audioTranscripts: List<TranscriptBlock> = emptyList(),
    val imageAnalysis: List<VisionResult> = emptyList(),
    val memoryHits: List<MemoryHit> = emptyList(),
    val entityKnowledge: String? = null,
    val entityContext: Map<String, EntityRef> = emptyMap(),
    val modeMetadata: ModeMetadata = ModeMetadata(),
    // Phase 4: Session history for context-aware refinement
    val sessionHistory: List<ChatTurn> = emptyList(),
    val lastToolResult: ToolArtifact? = null,
    val executedTools: Set<String> = emptySet(),
    // 日期上下文 — LLM 需要知道今天才能正确解析 "明天"、"下周" 等
    val currentDate: String? = null,
    // 当前时刻 (epoch millis) — 用于 RelativeTimeResolver 计算绝对时间
    val currentInstant: Long = 0,
    // Wave 3: 习惯上下文 — 用户和客户偏好（RL Module）
    val habitContext: HabitContext? = null
)

/**
 * 会话轮次（用于历史上下文）
 */
data class ChatTurn(
    val role: String,  // "user" | "assistant"
    val content: String
)

/**
 * 工具执行结果（用于上下文传递）
 */
data class ToolArtifact(
    val toolId: String,
    val title: String,
    val preview: String
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
