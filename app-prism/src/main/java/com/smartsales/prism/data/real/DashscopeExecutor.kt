package com.smartsales.prism.data.real

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.config.ModelRegistry
import com.smartsales.prism.domain.config.ModelRouter
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实执行器 — 适配 DashScope API
 * 
 * 将 Prism 的 EnhancedContext 映射为 ai-core 的 AiChatRequest，
 * 并将 AiChatResponse 转换为 ExecutorResult。
 * 
 * @see Prism-V1.md §2.2 #3
 */
@Singleton
class DashscopeExecutor @Inject constructor(
    private val aiChatService: AiChatService
) : Executor {
    
    override suspend fun execute(context: EnhancedContext): ExecutorResult {
        // 构建 AiChatRequest
        val request = buildRequest(context)
        
        // 调用 ai-core 服务
        return when (val result = aiChatService.sendMessage(request)) {
            is Result.Success -> mapSuccess(result.data)
            is Result.Error -> mapError(result.throwable)
        }
    }
    
    /**
     * 构建 AiChatRequest
     * 根据任务类型选择模型（通过 ModelRouter）
     */
    private fun buildRequest(context: EnhancedContext): AiChatRequest {
        val mode = context.modeMetadata.currentMode
        
        // 根据任务类型选择模型 (via ModelRouter)
        val model = ModelRouter.forContext(context)
        
        // 根据模式选择技能标签
        val skillTags = when (mode) {
            Mode.COACH -> setOf("sales_coach", "conversational")
            Mode.ANALYST -> setOf("data_analysis", "reasoning", "tool_calling")
            Mode.SCHEDULER -> setOf("scheduling", "structured_output")
        }
        
        // 构建提示词（包含上下文）
        val prompt = buildPrompt(context)
        
        return AiChatRequest(
            prompt = prompt,
            model = model,
            skillTags = skillTags,
            transcriptMarkdown = context.audioTranscripts.firstOrNull()?.text
        )
    }
    
    /**
     * 构建完整的提示词
     * 包含用户输入 + 上下文信息
     */
    private fun buildPrompt(context: EnhancedContext): String = buildString {
        // 基础用户输入
        appendLine(context.userText)
        
        // 添加实体上下文（如果有）
        if (context.entityContext.isNotEmpty()) {
            appendLine()
            appendLine("## 相关实体上下文")
            context.entityContext.forEach { (name, entity) ->
                appendLine("- $name: ${entity.displayName} (${entity.entityType})")
            }
        }
        
        // 添加记忆命中（如果有）
        if (context.memoryHits.isNotEmpty()) {
            appendLine()
            appendLine("## 历史记忆")
            context.memoryHits.take(3).forEach { entry ->
                appendLine("- ${entry.content}")
            }
        }
    }
    
    /**
     * 成功结果映射
     */
    private fun mapSuccess(response: AiChatResponse): ExecutorResult.Success {
        return ExecutorResult.Success(
            content = response.displayText,
            tokenUsage = TokenUsage(
                // 注：ai-core 目前不返回 token 用量，使用默认值
                promptTokens = 0,
                completionTokens = 0
            )
        )
    }
    
    /**
     * 错误结果映射
     */
    private fun mapError(error: Throwable): ExecutorResult.Failure {
        val message = error.message ?: "未知错误"
        val retryable = when {
            message.contains("timeout", ignoreCase = true) -> true
            message.contains("network", ignoreCase = true) -> true
            message.contains("rate limit", ignoreCase = true) -> true
            message.contains("401") -> false  // 认证错误不可重试
            message.contains("403") -> false  // 权限错误不可重试
            else -> true
        }
        return ExecutorResult.Failure(
            error = message,
            retryable = retryable
        )
    }
}

