// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshot.kt
// 模块：:data:ai-core
// 说明：HUD 调试快照模型（Orchestrator 统一生成三段 copy 文本）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

data class DebugSnapshot(
    val section1EffectiveRunText: String,
    val section2RawTranscriptionText: String,
    val section3PreprocessedText: String,
    val generatedAtMs: Long = System.currentTimeMillis(),
    val sessionId: String,
    val jobId: String? = null,
)
