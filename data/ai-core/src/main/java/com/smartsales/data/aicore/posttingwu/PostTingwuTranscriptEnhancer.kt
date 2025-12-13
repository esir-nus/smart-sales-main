// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/posttingwu/PostTingwuTranscriptEnhancer.kt
// 模块：:data:ai-core
// 说明：定义 Tingwu 后置转写增强的接口与模型
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.posttingwu

/**
 * 单条转写话语，保持 Tingwu 原始顺序和时间戳。
 */
data class EnhancerUtterance(
    val index: Int,
    val startMs: Long?,
    val endMs: Long?,
    val speakerId: String?,
    val text: String
)

data class EnhancerInput(
    val jobId: String,
    val language: String?,
    val utterances: List<EnhancerUtterance>
)

data class SpeakerLabel(
    val sourceSpeakerId: String,
    val label: String,
    val confidence: Double?
)

data class SplitLine(
    val speakerLabel: String?,
    val text: String
)

data class UtteranceEdit(
    val index: Int,
    val newSpeakerLabel: String? = null,
    val newText: String? = null,
    val split: List<SplitLine>? = null,
    val confidence: Double? = null
)

data class EnhancerOutput(
    val speakerRoster: List<SpeakerLabel>? = null,
    val utteranceEdits: List<UtteranceEdit>? = null
)

/**
 * 后置转写增强入口，返回结构化补丁，失败返回 null 触发回退。
 */
interface PostTingwuTranscriptEnhancer {
    suspend fun enhance(input: EnhancerInput): EnhancerOutput?
}
