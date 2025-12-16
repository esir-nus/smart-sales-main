// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt
// 模块：:data:ai-core
// 说明：提供 AI 参数快照与注入式 Provider，避免全局可变状态
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.params

import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AiParaSettingsSnapshot(
    val tingwu: TingwuSettings = TingwuSettings(),
    val dashScope: DashScopeSettings = DashScopeSettings(),
    // 重要：第三方能力开关统一入口（避免散落在 app/feature 内部造成“看起来切了但实际没生效”）。
    val transcription: TranscriptionSettings = TranscriptionSettings(),
)

/**
 * 说明：转写相关运行时开关（MVP：内存态，可被测试壳实时修改）。
 */
data class TranscriptionSettings(
    val provider: String = TRANSCRIPTION_PROVIDER_XFYUN,
    // 重要：这是客户端可控的 query 参数集合；会进入签名/URL/HUD，必须一致。
    val xfyun: XfyunAsrSettings = XfyunAsrSettings(),
)

/**
 * 说明：讯飞 ASR 参数总入口。
 * 重要：这是客户端可控的 query 参数集合；会进入签名/URL/HUD，必须一致。
 */
data class XfyunAsrSettings(
    val baseUrlOverride: String = "",
    val authMode: String = XFYUN_AUTH_MODE_SIGNATURE,
    val upload: XfyunUploadSettings = XfyunUploadSettings(),
    val result: XfyunResultSettings = XfyunResultSettings(),
    // 能力开关：默认全部关闭，避免误触发 failType=11。
    val capabilities: XfyunCapabilities = XfyunCapabilities(),
)

/**
 * 说明：/v2/upload 的可控参数（排除密钥/签名）。
 * 重要：这是客户端可控的 query 参数集合；会进入签名/URL/HUD，必须一致。
 */
data class XfyunUploadSettings(
    // 文档参数：language（autodialect/autominor）
    val language: String = "autodialect",
    // 文档参数：pd（领域个性化；为空则不传）
    val pd: String = "",
    // 文档参数：roleType/roleNum（角色分离；MVP 不开放 roleType=3 声纹）
    val roleType: Int = 1,
    val roleNum: Int = 0,
    // 文档参数：顺滑/口语规整/远近场模式
    val engSmoothProc: Boolean = true,
    val engColloqProc: Boolean = false,
    val engVadMdn: Int = 1,
    // 文档参数：audioMode（本项目固定走 fileStream，不走 urlLink）
    val audioMode: String = XFYUN_AUDIO_MODE_FILE_STREAM,
)

/**
 * 说明：resultType 相关开关（MVP：默认只允许 transfer）。
 */
data class XfyunResultSettings(
    val resultType: XfyunResultType = XfyunResultType.TRANSFER,
) {
    /**
     * 说明：
     * - Option 1：默认仅转写(transfer)，避免触发 failType=11。
     * - translate/predict/analysis 如未明确开通能力，必须在发请求前阻断（不要“试试看”）。
     */
    fun resolveApiValueOrThrow(capabilities: XfyunCapabilities): String {
        return when (resultType) {
            XfyunResultType.TRANSFER -> "transfer"
            XfyunResultType.TRANSLATE -> {
                if (!capabilities.allowTranslate) throw unsupported("translate")
                "translate"
            }
            XfyunResultType.PREDICT -> {
                if (!capabilities.allowPredict) throw unsupported("predict")
                "predict"
            }
            XfyunResultType.ANALYSIS -> {
                if (!capabilities.allowAnalysis) throw unsupported("analysis")
                "analysis"
            }
        }
    }

    private fun unsupported(type: String): AiCoreException =
        AiCoreException(
            source = AiCoreErrorSource.XFYUN,
            reason = AiCoreErrorReason.UNSUPPORTED_CAPABILITY,
            message = "讯飞账号未开通 $type 能力，为避免触发 failType=11 已阻止请求",
            suggestion = "请先在讯飞控制台确认开通该能力，再打开对应 capability 开关"
        )
}

enum class XfyunResultType {
    TRANSFER,
    TRANSLATE,
    PREDICT,
    ANALYSIS,
}

data class XfyunCapabilities(
    val allowTranslate: Boolean = false,
    val allowPredict: Boolean = false,
    val allowAnalysis: Boolean = false,
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

// 重要：该常量位于 :data:ai-core，避免依赖 app 层 enum。
const val TRANSCRIPTION_PROVIDER_XFYUN: String = "XFYUN"
const val TRANSCRIPTION_PROVIDER_TINGWU: String = "TINGWU"

const val XFYUN_AUTH_MODE_SIGNATURE: String = "SIGNATURE"

// 文档参数：audioMode
const val XFYUN_AUDIO_MODE_FILE_STREAM: String = "fileStream"
const val XFYUN_AUDIO_MODE_URL_LINK: String = "urlLink"
