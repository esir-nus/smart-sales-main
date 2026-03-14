package com.smartsales.core.context

import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.HabitContext

/**
 * 增强上下文 — 统一输入载体
 * @see Prism-V1.md §2.2 #1
 */
data class EnhancedContext(
    val userText: String,
    val isBadge: Boolean = false,
    val audioTranscripts: List<TranscriptBlock> = emptyList(),
    val imageAnalysis: List<VisionResult> = emptyList(),
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
    // 临时文档上下文 (Transient Payload) — 比如从音频加载的摘要等
    val documentContext: String? = null,
    // Wave 3: 习惯上下文 — 用户和客户偏好（RL Module）
    val habitContext: HabitContext? = null,
    // Sticky Notes: 近期日程摘要（由ContextBuilder从ScheduledTaskRepository读取）
    val scheduleContext: String? = null,
    // Architect override
    val systemPromptOverride: String? = null
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
 * 模式元数据
 */
data class ModeMetadata(
    val currentMode: Mode = Mode.ANALYST,
    val sessionId: String = "",
    val turnIndex: Int = 0
)
