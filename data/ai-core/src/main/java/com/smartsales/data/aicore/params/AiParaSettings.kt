// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt
// 模块：:data:ai-core
// 说明：提供 AI 参数快照与注入式 Provider，避免全局可变状态
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.params

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AiParaSettingsSnapshot(
    val tingwu: TingwuSettings = TingwuSettings(),
    val dashScope: DashScopeSettings = DashScopeSettings(),
    val transcription: TranscriptionSettings = TranscriptionSettings(),
)

/**
 * 说明：转写相关运行时开关（MVP：内存态，可被测试壳实时修改）。
 * 重要：V7+ 仅支持 Tingwu，XFyun 已移除。
 */
data class TranscriptionSettings(
    val provider: String = TRANSCRIPTION_PROVIDER_TINGWU,
)

interface AiParaSettingsProvider {
    fun snapshot(): AiParaSettingsSnapshot
}

/**
 * 说明：
 * - MVP：仅内存态，满足运行时切换与单元测试。
 * - 后续如需持久化，再替换为 DataStore/SharedPreferences 实现。
 */
interface AiParaSettingsRepository {
    val flow: StateFlow<AiParaSettingsSnapshot>
    fun snapshot(): AiParaSettingsSnapshot
    fun update(transform: (AiParaSettingsSnapshot) -> AiParaSettingsSnapshot)
}

@Singleton
class InMemoryAiParaSettingsRepository @Inject constructor() : AiParaSettingsRepository {
    private val state = MutableStateFlow(AiParaSettingsSnapshot())

    override val flow: StateFlow<AiParaSettingsSnapshot> = state.asStateFlow()

    override fun snapshot(): AiParaSettingsSnapshot = state.value

    override fun update(transform: (AiParaSettingsSnapshot) -> AiParaSettingsSnapshot) {
        // 重要：以原子方式更新快照，避免并发写入丢失。
        state.update(transform)
    }
}

@Singleton
class DefaultAiParaSettingsProvider @Inject constructor(
    private val repository: AiParaSettingsRepository,
) : AiParaSettingsProvider {
    override fun snapshot(): AiParaSettingsSnapshot = repository.snapshot()
}

data class TingwuSettings(
    // 听悟语音分离与转码等参数
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

// 重要：V7+ 仅支持 Tingwu，XFyun 已移除
const val TRANSCRIPTION_PROVIDER_TINGWU: String = "TINGWU"
