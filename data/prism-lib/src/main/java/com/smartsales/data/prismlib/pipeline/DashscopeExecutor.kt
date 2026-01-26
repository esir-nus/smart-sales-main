package com.smartsales.data.prismlib.pipeline

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.smartsales.domain.prism.core.EnhancedContext
import com.smartsales.domain.prism.core.Executor
import com.smartsales.domain.prism.core.ExecutorResult
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.TokenUsage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DashScope 执行器 — 调用阿里云 LLM API
 * @see Prism-V1.md §2.2 #3
 */
@Singleton
class DashscopeExecutor @Inject constructor() : Executor {

    private val generation = Generation()

    override suspend fun execute(context: EnhancedContext): ExecutorResult {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = buildSystemPrompt(context)
                val userMessage = context.userText

                val messages = listOf(
                    Message.builder()
                        .role(Role.SYSTEM.value)
                        .content(systemPrompt)
                        .build(),
                    Message.builder()
                        .role(Role.USER.value)
                        .content(userMessage)
                        .build()
                )

                val param = GenerationParam.builder()
                    .model("qwen-turbo")
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build()

                val result: GenerationResult = generation.call(param)
                val content = result.output?.choices?.firstOrNull()?.message?.content ?: ""

                val usage = result.usage?.let {
                    TokenUsage(
                        promptTokens = it.inputTokens ?: 0,
                        completionTokens = it.outputTokens ?: 0
                    )
                }

                ExecutorResult(
                    displayContent = content,
                    structuredJson = extractStructuredJson(content, context.mode),
                    usage = usage
                )
            } catch (e: Exception) {
                ExecutorResult(
                    displayContent = "执行失败: ${e.message}",
                    structuredJson = null
                )
            }
        }
    }

    private fun buildSystemPrompt(context: EnhancedContext): String {
        val template = when (context.mode) {
            Mode.COACH -> Prompts.COACH_SYSTEM
            Mode.ANALYST -> Prompts.ANALYST_SYSTEM
            Mode.SCHEDULER -> Prompts.SCHEDULER_SYSTEM
        }

        val replacements = mutableMapOf<String, String>()
        
        context.userProfile?.let { profile ->
            replacements["displayName"] = profile.displayName
            replacements["industry"] = profile.industry ?: "未设置"
            replacements["role"] = profile.role ?: "未设置"
            replacements["experienceLevel"] = profile.experienceLevel ?: "INTERMEDIATE"
        }

        val memorySnippets = context.memoryHits.joinToString("\n") { "- ${it.snippet}" }
        replacements["memoryHits"] = memorySnippets.ifEmpty { "(无历史上下文)" }

        return Prompts.format(template, replacements)
    }

    /**
     * 从 LLM 输出中提取结构化 JSON
     * 仅用于 Scheduler 模式
     */
    private fun extractStructuredJson(content: String, mode: Mode): String? {
        if (mode != Mode.SCHEDULER) return null

        // 简单提取 JSON 块
        val jsonPattern = Regex("\\{[^}]+\\}")
        return jsonPattern.find(content)?.value
    }
}
