// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/posttingwu/RealPostTingwuTranscriptEnhancer.kt
// 模块：:data:ai-core
// 说明：使用通用 AiChatService 调用 LLM，对 Tingwu 转写进行结构化增强
// 作者：创建于 2025-12-13
package com.smartsales.data.aicore.posttingwu

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 真实实现：调用已有 LLM 客户端，要求输出严格 JSON。
 */
@Singleton
class RealPostTingwuTranscriptEnhancer @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val aiChatService: AiChatService,
    private val gson: Gson,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
    optionalConfig: Optional<AiCoreConfig>
) : PostTingwuTranscriptEnhancer {

    private val config = optionalConfig.orElse(AiCoreConfig())

    override suspend fun enhance(input: EnhancerInput): EnhancerOutput? = withContext(dispatchers.io) {
        val settings = aiParaSettingsProvider.snapshot().tingwu.postTingwuEnhancer
        if (!settings.enabled) return@withContext null
        val truncated = truncate(input, settings.maxUtterances, settings.maxChars)
        if (truncated.utterances.isEmpty()) return@withContext null
        val prompt = buildPrompt(truncated)
        val result = withTimeout(settings.timeoutMs.coerceAtLeast(1_000L)) {
            aiChatService.sendMessage(
                AiChatRequest(
                    prompt = prompt
                )
            )
        }
        val content = when (result) {
            is Result.Success -> result.data.displayText
            is Result.Error -> return@withContext null
        }
        parseOutput(content)
    }

    private fun buildPrompt(input: EnhancerInput): String {
        val builder = StringBuilder()
        builder.appendLine("你是转写增强助手，请仅输出 JSON，不要输出 Markdown 或解释。")
        builder.appendLine("规则：")
        builder.appendLine("1) 不要生成新的时间戳。")
        builder.appendLine("2) 保持顺序，不要重排。")
        builder.appendLine("3) 可以拆分一句为多句，首句保留原时标，其余时标为 null。")
        builder.appendLine("4) 可以修正口头禅、同音字，低置信度时保持原文。")
        builder.appendLine("5) speakerId 仅作为参考，可重命名为角色/姓名，并返回置信度。")
        builder.appendLine("6) 输出 JSON 必须符合 EnhancerOutput 结构。")
        builder.appendLine()
        builder.appendLine("输入示例：")
        builder.appendLine(gson.toJson(input))
        builder.appendLine()
        builder.append("请输出严格 JSON（无多余前后缀），字段：speakerRoster、utteranceEdits。")
        return builder.toString()
    }

    private fun truncate(
        input: EnhancerInput,
        maxUtterances: Int,
        maxChars: Int
    ): EnhancerInput {
        if (input.utterances.isEmpty()) return input
        val kept = mutableListOf<EnhancerUtterance>()
        var chars = 0
        input.utterances.forEachIndexed { index, utterance ->
            if (index >= maxUtterances) return@forEachIndexed
            val next = utterance.copy(index = kept.size)
            val length = next.text.length
            if (chars + length > maxChars) return@forEachIndexed
            chars += length
            kept += next
        }
        return input.copy(utterances = kept)
    }

    private fun parseOutput(content: String): EnhancerOutput? {
        val root = runCatching { JsonParser.parseString(content) }.getOrNull()
            ?: return null
        val obj: JsonElement = when {
            root.isJsonObject -> root
            root.isJsonArray && root.asJsonArray.size() > 0 -> root.asJsonArray[0]
            else -> return null
        }
        return runCatching { gson.fromJson(obj, EnhancerOutput::class.java) }
            .onFailure {
                AiCoreLogger.w(TAG, "转写增强结果解析失败：${it.message}")
            }
            .getOrNull()
            ?.takeIf { output ->
                val hasRoster = output.speakerRoster?.isNotEmpty() == true
                val hasEdits = output.utteranceEdits?.isNotEmpty() == true
                hasRoster || hasEdits
            }
    }

    companion object {
        private const val TAG = "Tingwu/PostEnhancer"
    }
}
