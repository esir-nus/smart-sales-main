package com.smartsales.domain.prism.core

/**
 * Context Builder 输出 — 统一的多模态上下文载荷
 * @see Prism-V1.md §2.2 #1
 */
data class EnhancedContext(
    val userText: String,
    val audioTranscripts: List<TranscriptBlock> = emptyList(),
    val imageAnalysis: List<VisionResult> = emptyList(),
    val memoryHits: List<MemoryHit> = emptyList(),
    val entityContext: Map<String, EntityRef> = emptyMap(),
    val userProfile: UserProfileSnapshot? = null,
    val userHabits: List<UserHabitSnapshot> = emptyList(),
    val sessionCacheSnapshot: SessionCacheSnapshot? = null,
    val mode: Mode
)

// ============================================================
// 占位类型 — Chunk D (Input Tools) 会定义完整版本
// TODO: Move to dedicated files in Chunk D
// ============================================================

/**
 * Tingwu 转写输出块
 * @see Prism-V1.md §2.2 #1 Input Normalization
 */
data class TranscriptBlock(
    val text: String,
    val speakerId: String? = null,
    val startMs: Long = 0,
    val endMs: Long = 0
)

/**
 * Qwen-VL 视觉分析结果
 * @see Prism-V1.md §2.2 #1 Input Normalization
 */
data class VisionResult(
    val description: String,
    val ocrText: String? = null
)

// ============================================================
// 内存引用类型
// ============================================================

/**
 * 内存命中结果 — Context Builder 检索返回
 */
data class MemoryHit(
    val entryId: String,
    val snippet: String,
    val score: Float
)

/**
 * 实体引用 — 来自 Relevancy Library
 */
data class EntityRef(
    val entityId: String,
    val displayName: String
)

/**
 * 用户档案快照 — 来自 UserProfile Room 实体
 * @see Prism-V1.md §5.8
 */
data class UserProfileSnapshot(
    val displayName: String,
    val preferredLanguage: String,
    val experienceLevel: String? = null,
    val industry: String? = null,
    val role: String? = null
)

/**
 * 用户习惯快照 — 来自 UserHabit Room 实体
 * @see Prism-V1.md §5.9
 */
data class UserHabitSnapshot(
    val habitKey: String,
    val habitValue: String,
    val confidence: Float = 1.0f
)

/**
 * Session Cache 快照 — 任务内快速上下文
 * @see Prism-V1.md §2.2 #1b
 */
data class SessionCacheSnapshot(
    val entries: Map<String, String>
)
