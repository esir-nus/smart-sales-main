// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt
// 模块：:data:ai-core
// 说明：提供 AI 参数快照与注入式 Provider，避免全局可变状态
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.params

import javax.inject.Inject
import javax.inject.Singleton

data class AiParaSettingsSnapshot(
    val tingwu: TingwuSettings = TingwuSettings(),
    val dashScope: DashScopeSettings = DashScopeSettings()
)

interface AiParaSettingsProvider {
    fun snapshot(): AiParaSettingsSnapshot
}

@Singleton
class DefaultAiParaSettingsProvider @Inject constructor() : AiParaSettingsProvider {
    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
}

data class TingwuSettings(
    // 语音分离与转码等参数
    val transcription: TingwuTranscriptionSettings = TingwuTranscriptionSettings(),
    val summarization: TingwuSummarizationSettings = TingwuSummarizationSettings(),
    val autoChaptersEnabled: Boolean = true,
    val pptExtractionEnabled: Boolean = true,
    val textPolishEnabled: Boolean = true,
    val customPrompt: TingwuCustomPromptSettings = TingwuCustomPromptSettings(),
    val transcoding: TingwuTranscodingSettings = TingwuTranscodingSettings(),
    val postTingwuEnhancer: PostTingwuEnhancerSettings = PostTingwuEnhancerSettings()
)

data class TingwuTranscriptionSettings(
    val diarizationEnabled: Boolean = true,
    val diarizationSpeakerCount: Int = 0,
    val diarizationOutputLevel: Int = 1,
    val audioEventDetectionEnabled: Boolean = true
)

data class TingwuSummarizationSettings(
    val enabled: Boolean = true,
    val types: List<String> = listOf("Paragraph", "Conversational", "QuestionsAnswering")
)

data class TingwuCustomPromptSettings(
    val enabled: Boolean = true,
    val contents: List<TingwuCustomPromptContentSettings> = listOf(
        TingwuCustomPromptContentSettings(
            name = "speaker-role-relabel-v1",
            prompt = """
{
  You will receive a Tingwu Transcription JSON inside {Transcription}. 1. Please Patch the frist 5 lines of waht you receive (raw, not polished). 2. answer this question, did you recieved anything relate or liekly timestamps or speaker/ speakerid? 
}
""".trimIndent(),
            model = "qwen3-max",
            transType = "default"
        )
    )
)

data class TingwuCustomPromptContentSettings(
    val name: String,
    val prompt: String,
    val model: String = "tingwu-max3",
    val transType: String = "default"
)

data class TingwuTranscodingSettings(
    val targetAudioFormat: String = "mp3"
)

data class PostTingwuEnhancerSettings(
    val enabled: Boolean = false,
    val maxUtterances: Int = 120,
    val maxChars: Int = 8000,
    val timeoutMs: Long = 15_000
)

// DashScope 预留占位，当前不改动行为
data class DashScopeSettings(
    val temperature: Double = 0.3,
    val incrementalOutput: Boolean = true
)
