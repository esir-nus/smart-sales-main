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
    // 重要：声纹辅助分离（roleType=3）。默认关闭；仅在配置齐全时才会生效（fail-soft 回退）。
    val voiceprint: XfyunVoiceprintSettings = XfyunVoiceprintSettings(),
    val result: XfyunResultSettings = XfyunResultSettings(),
    // 能力开关：默认全部关闭，避免误触发 failType=11。
    val capabilities: XfyunCapabilities = XfyunCapabilities(),
    // 重要：讯飞转写后处理（跨说话人边界分词漂移修复），默认关闭，避免“看起来变好了但引入不可控改写”。
    val postXfyun: PostXfyunSettings = PostXfyunSettings(),
)

/**
 * 说明：讯飞声纹相关配置（MVP：内存态，可被测试壳实时修改）。
 *
 * 重要：
 * - 声纹属于敏感能力：任何配置缺失/非法都必须 fail-soft（不启用声纹，继续普通转写）。
 * - featureIds 最多支持 64 个（以 docs/xfyun-asr-rest-api.md 为准）。
 * - 声纹 API baseUrl 必须独立于转写 baseUrlOverride，避免误把转写 host 劫持到声纹域名。
 */
data class XfyunVoiceprintSettings(
    // 默认关闭：需要配置 featureIds 才能生效；避免“看起来开了但实际没生效”与隐私误用。
    val enabled: Boolean = false,
    val featureIds: List<String> = emptyList(),
    val roleNum: Int = 0,
    val baseUrlOverride: String = "",
    // 可选：用于声纹注册接口的 uid；为空则不传。
    val uid: String = "",
) {
    data class Effective(
        val enabledSetting: Boolean,
        val effectiveEnabled: Boolean,
        val featureIds: List<String>,
        val featureIdsTruncated: Boolean,
        val roleNum: Int?,
        val baseUrl: String,
        val disabledReason: String? = null,
    )

    fun resolveEffective(): Effective {
        val normalized = normalizeFeatureIds(featureIds)
        val roleNumValid = roleNum in 0..10
        val baseUrl = baseUrlOverride.trim().takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL

        if (!enabled) {
            return Effective(
                enabledSetting = false,
                effectiveEnabled = false,
                featureIds = normalized.ids,
                featureIdsTruncated = normalized.truncated,
                roleNum = if (roleNumValid) roleNum else null,
                baseUrl = baseUrl,
                disabledReason = "VOICEPRINT_DISABLED_SETTING_OFF",
            )
        }
        if (normalized.ids.isEmpty()) {
            return Effective(
                enabledSetting = true,
                effectiveEnabled = false,
                featureIds = emptyList(),
                featureIdsTruncated = normalized.truncated,
                roleNum = if (roleNumValid) roleNum else null,
                baseUrl = baseUrl,
                disabledReason = "VOICEPRINT_DISABLED_EMPTY_FEATURE_IDS",
            )
        }
        if (!roleNumValid) {
            return Effective(
                enabledSetting = true,
                effectiveEnabled = false,
                featureIds = normalized.ids,
                featureIdsTruncated = normalized.truncated,
                roleNum = null,
                baseUrl = baseUrl,
                disabledReason = "VOICEPRINT_DISABLED_INVALID_ROLE_NUM",
            )
        }
        return Effective(
            enabledSetting = true,
            effectiveEnabled = true,
            featureIds = normalized.ids,
            featureIdsTruncated = normalized.truncated,
            roleNum = roleNum,
            baseUrl = baseUrl,
            disabledReason = null,
        )
    }

    /**
     * 说明：用于调试/实验时将 featureId 追加到配置，并强制 enabled=true。
     *
     * 重要：
     * - 声纹 featureId 属于敏感标识：只存 ID，不存音频。
     * - 列表上限 64，避免配置膨胀导致误配或 UI/HUD 过载。
     */
    fun withEnabledAndAddedFeatureId(featureId: String): XfyunVoiceprintSettings {
        val id = featureId.trim()
        if (id.isBlank()) return copy(enabled = true)
        val normalized = normalizeFeatureIds(featureIds + id)
        return copy(
            enabled = true,
            featureIds = normalized.ids,
        )
    }

    fun withoutFeatureId(featureId: String): XfyunVoiceprintSettings {
        val id = featureId.trim()
        if (id.isBlank()) return this
        val filtered = featureIds.filterNot { it.trim() == id }
        val normalized = normalizeFeatureIds(filtered)
        return copy(featureIds = normalized.ids)
    }

    private data class NormalizedIds(
        val ids: List<String>,
        val truncated: Boolean,
    )

    private fun normalizeFeatureIds(raw: List<String>): NormalizedIds {
        if (raw.isEmpty()) return NormalizedIds(ids = emptyList(), truncated = false)
        val deduped = LinkedHashSet<String>()
        raw.forEach { id ->
            val trimmed = id.trim()
            if (trimmed.isNotBlank()) deduped.add(trimmed)
        }
        val all = deduped.toList()
        val capped = all.take(MAX_FEATURE_IDS)
        return NormalizedIds(ids = capped, truncated = all.size > capped.size)
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://office-api-personal-dx.iflyaisol.com"
        const val MAX_FEATURE_IDS: Int = 64
    }
}

/**
 * 说明：声纹设置写入的集中入口（用于 Debug Lab）。
 *
 * 重要安全要求：
 * - 仅写入 featureId（敏感标识）；绝不写入/保存音频 base64。
 */
fun AiParaSettingsRepository.enableVoiceprintAndAddFeatureId(featureId: String) {
    update { current ->
        val transcription = current.transcription
        val xfyun = transcription.xfyun
        val updatedVoiceprint = xfyun.voiceprint.withEnabledAndAddedFeatureId(featureId)
        current.copy(
            transcription = transcription.copy(
                xfyun = xfyun.copy(voiceprint = updatedVoiceprint)
            )
        )
    }
}

fun AiParaSettingsRepository.removeVoiceprintFeatureId(featureId: String) {
    update { current ->
        val transcription = current.transcription
        val xfyun = transcription.xfyun
        val updatedVoiceprint = xfyun.voiceprint.withoutFeatureId(featureId)
        current.copy(
            transcription = transcription.copy(
                xfyun = xfyun.copy(voiceprint = updatedVoiceprint)
            )
        )
    }
}

/**
 * 说明：一键应用“讯飞声纹调试预置参数”（仅内存态，不持久化）。
 *
 * 重要：
 * - 该 preset 仅用于调试：确保 upload.language 合法且 roleType 不会在未配置 featureIds 时误切到声纹模式。
 * - 声纹是否生效仍以 voiceprint.resolveEffective() 为准（enabled + featureIds 非空）。
 */
fun AiParaSettingsRepository.applyXfyunVoiceprintTestPreset(lastFeatureId: String? = null) {
    update { current ->
        val transcription = current.transcription
        val xfyun = transcription.xfyun
        val upload = xfyun.upload
        val postXfyun = xfyun.postXfyun
        val voiceprint = xfyun.voiceprint

        val updatedVoiceprint = lastFeatureId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { featureId ->
                // 重要：声纹 featureId 属于敏感标识，只写入 ID，不写入音频；列表上限 64。
                voiceprint.withEnabledAndAddedFeatureId(featureId)
            }
            ?: voiceprint

        current.copy(
            transcription = transcription.copy(
                provider = TRANSCRIPTION_PROVIDER_XFYUN,
                xfyun = xfyun.copy(
                    baseUrlOverride = "",
                    upload = upload.copy(
                        language = "autodialect",
                        // 默认走普通说话人分离；声纹必须由 voiceprintEffectiveEnabled（roleType=3 + featureIds）触发。
                        roleType = 1,
                        roleNum = 0,
                    ),
                    postXfyun = postXfyun.copy(enabled = false),
                    voiceprint = updatedVoiceprint,
                )
            )
        )
    }
}

/**
 * 说明：PostXFyun 的运行时配置（MVP：内存态，可被测试壳实时修改）。
 *
 * 重要：
 * - Prompt 必须可调：便于不同账号/口音/场景下快速试验而不改代码。
 * - PostXFyun 属于可行性增强：任何失败必须 fail-soft 回退到原始逐字稿。
 */
data class PostXfyunSettings(
    // 默认关闭：避免线上"看起来变好了但引入不可控改写"。
    val enabled: Boolean = false,
    // 判定可疑边界：gapMs <= threshold 才会触发仲裁（单位毫秒）。
    val suspiciousGapThresholdMs: Long = 200L,
    // 最多保留多少条"可疑边界提示"（按 boundaryIndex 从小到大，确定性截断）。
    val maxSuspiciousHints: Int = 50,
    // 批次切分：每个 batch 最多包含多少条逐字稿行；默认不切分（单批 b1）。
    val maxUtterancesPerBatch: Int = Int.MAX_VALUE,
    // 每份逐字稿最多允许应用多少次"边界两行改写修复"（同时也会限制 LLM 调用次数，避免无限循环）。
    val maxRepairsPerTranscript: Int = 15,
    // 改写模型覆盖（例如：qwen3-max）；为空则使用默认模型配置。
    val model: String = "qwen3-max",
    // 重要：LLM Prompt 模板（仅输入相邻两行 + boundary 信息，不允许输入 raw HTTP JSON）。
    val promptTemplate: String = DEFAULT_POST_XFYUN_REWRITE_PROMPT_TEMPLATE,
    // 置信度阈值：<= 0.0 表示禁用置信度检查；> 0.0 时，只有 decision.confidence >= 此值才应用修复。
    val confidenceThreshold: Double = 0.8,
    // 最大 span 字符数：允许在边界两侧移动的最大字符数（1-200）。
    val maxSpanChars: Int = 24,
)

private val DEFAULT_POST_XFYUN_REWRITE_PROMPT_TEMPLATE: String = """
中文ASR转录校正专家提示

核心目标：将原始ASR转录转化为专业、连贯、符合口语逻辑的对话文本，尤其适用于销售/客服等互动场景。

【执行流程】

第一步：整体语境分析（先理解，再修改）

- 通读全文，识别场景（如销售、会议、访谈）及人物关系（如客户/销售、上级/下属）。
- 提取关键信息：讨论的核心主题、产品/服务名称、价格、技术参数等。
- 分析人物特征：标注每个发言人的说话风格（如“客户爱打岔质疑”、“销售使用专业话术”、“领导做总结”）。

第二步：分阶段校正（重点优化开头）

- 初期（前10-15行）重点审查：ASR开头通常错误率更高。特别注意：
  - 开场白是否完整、自然？不完整的半句（如“在路虎能买”）需根据语境补充或连接。
  - 称呼、问候语是否粘连（如“您好罗总欢迎来到”）？需添加合理标点分隔。
  - 口语填充词（的话、呀、啊）是否错位？将其归位到正确的句子中。
- 中后期校正：基于已建立的语境，处理飘移的从句、修复被错误标点割裂的句子、合并无意义的重复。

第三步：修复具体ASR问题的策略

- 句子边界与标点：忽略原始标点。根据语义和口语节奏重加标点（问号、感叹号、逗号、省略号），使每句话轮完整、自然。
- 飘移的碎片处理：
  - 填充词/语气词：将句尾的“呀”、“啊”、“呢”等移至其语义归属的疑问句或感叹句句尾。
  - 连接词/从句：将孤立的“的话”、“那”、“但是”等与前文或后文连接，形成完整的条件句、转折句。
  - 跨话轮打断：当A的话被B打断时，A的句子可能在中间被切断。确保中断点合理，且B的接话内容符合其人物性格（如客户抢话、揶揄）。
  - 重复与冗余：删除ASR造成的无意义重复（如“400万…400万”），保留有修辞或强调作用的重复。
- 话轮归属：除非有强烈证据，否则不合并说话人。短促的回应（如“是的”、“好”）、笑声（“Haha”）、语气词（“嗯…”）单独成行。

第四步：风格与一致性优化

- 口语化：保留“哥们”、“呀”、“唉哟”等真实口语元素，避免过度书面化。
- 专业性：确保产品名称、技术术语、数字准确无误。
- 互动感：通过合理的标点和分段，体现对话的节奏、打断、追问和幽默。

第五步：最终检查

- 通读校正后的全文，确保上下文连贯、人物行为一致、逻辑通顺。
- 确认所有修正均未引入原文不存在的事实。

【约束条件（必须遵守）】

- 忠于原意：不添加、不删除、不改变事实性内容。
- 最小化移动：只移动修复连贯性所必需的碎片。
- 保持话轮独立：绝不合并不同发言人的内容。
- 修复，不重写：优化表达，但不改变说话人的原始意图和风格。

输入（两行原文，顺序为A、B）：
LINE_A:
{{PREV_LINE_TEXT}}
LINE_B:
{{NEXT_LINE_TEXT}}
相关信号：
sus_id: {{SUS_ID}}
gap_ms: {{GAP_MS}}

输出（JSON，仅支持以下格式，不得输出多余内容）：

{
  "prevText": "", // 修正后的 LINE_A（保留原有说话人前缀标签）
  "nextText": "", // 修正后的 LINE_B（保留原有说话人前缀标签）
  "confidence": 0.0, // 连贯性提升充足时信心高，若仅做标点微调则 confidence <= 0.30
  "reason": "" // 简要说明修正点（不要出现提示词或标记）
}
""".trimIndent()

/**
 * 说明：/v2/upload 的可控参数（排除密钥/签名）。
 * 重要：这是客户端可控的 query 参数集合；会进入签名/URL/HUD，必须一致。
 */
data class XfyunUploadSettings(
    // 文档参数：language（本项目固定使用 autodialect；cn/zh 等旧值会被统一兜底为 autodialect）
    val language: String = "autodialect",
    // 文档参数：pd（领域个性化；为空则不传）
    val pd: String = "",
    // 文档参数：roleType/roleNum（角色分离；默认走普通说话人分离；声纹必须显式开启 roleType=3 + featureIds）
    val roleType: Int = 1,
    val roleNum: Int = 0,
    // 文档参数：顺滑/口语规整/远近场模式
    val engSmoothProc: Boolean = true,
    val engColloqProc: Boolean = false,
    val engVadMdn: Int = 2,
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

// 重要：该常量位于 :data:ai-core，避免依赖 app 层 enum。
const val TRANSCRIPTION_PROVIDER_XFYUN: String = "XFYUN"
const val TRANSCRIPTION_PROVIDER_TINGWU: String = "TINGWU"

const val XFYUN_AUTH_MODE_SIGNATURE: String = "SIGNATURE"

// 文档参数：audioMode
const val XFYUN_AUDIO_MODE_FILE_STREAM: String = "fileStream"
const val XFYUN_AUDIO_MODE_URL_LINK: String = "urlLink"
