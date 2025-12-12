package com.smartsales.data.aicore.params

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt
// 模块：:data:ai-core
// 说明：集中管理 AI 请求参数（Tingwu / DashScope），便于统一调优与测试
// 作者：创建于 2025-12-12

/**
 * 总入口：集中管理各 vendor 的默认参数。
 */
object AiParaSettings {
    val tingwu: TingwuSettings = TingwuSettings()
    val dashScope: DashScopeSettings = DashScopeSettings()
}

data class TingwuSettings(
    // 语音分离与转码等参数
    val transcription: TingwuTranscriptionSettings = TingwuTranscriptionSettings(),
    val summarization: TingwuSummarizationSettings = TingwuSummarizationSettings(),
    val autoChaptersEnabled: Boolean = true,
    val pptExtractionEnabled: Boolean = true,
    val textPolishEnabled: Boolean = true,
    val customPrompt: TingwuCustomPromptSettings = TingwuCustomPromptSettings(),
    val transcoding: TingwuTranscodingSettings = TingwuTranscodingSettings()
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
                输入是一份包含段落和 SpeakerId 的转写文本。仅重命名发言人：
                - SpeakerId "1" -> Dad
                - SpeakerId "2" -> Son
                - 其他 SpeakerId 保持不变
                不要猜测或新增角色，不要改动时间戳/顺序/文本，仅修改显示的发言人名称。
            """.trimIndent(),
            model = "tingwu-turbo",
            transType = "default"
        )
    )
)

data class TingwuCustomPromptContentSettings(
    val name: String,
    val prompt: String,
    val model: String = "tingwu-turbo",
    val transType: String = "default"
)

data class TingwuTranscodingSettings(
    val targetAudioFormat: String = "mp3"
)

// DashScope 预留占位，当前不改动行为
data class DashScopeSettings(
    val temperature: Double = 0.3,
    val incrementalOutput: Boolean = true
)
