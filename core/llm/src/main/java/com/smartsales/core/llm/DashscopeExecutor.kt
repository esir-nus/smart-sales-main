package com.smartsales.core.llm

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import android.util.Log
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
    
    override suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult {
        // 构建 AiChatRequest
        val request = buildRequest(profile, prompt)
        
        // 记录 API 请求
        Log.d("DashscopeExecutor", "🔌 API: ${request.model}")
        
        // 使用非流式调用 — 输出格式更稳定（列表换行、markdown 结构）
        val result = aiChatService.sendMessage(request)
        
        return when (result) {
            is com.smartsales.core.util.Result.Success -> {
                val response = result.data
                // 记录思考痕迹（如有）
                response.thinkingTrace?.let { trace ->
                    if (trace.isNotBlank()) Log.d("DashscopeExecutor", trace)
                }
                Log.d("DashscopeExecutor", "✅ 分析完成")
                mapSuccess(response)
            }
            is com.smartsales.core.util.Result.Error -> {
                Log.e("DashscopeExecutor", "❌ API Error: ${result.throwable.message}")
                mapError(result.throwable)
            }
        }
    }
    
    /**
     * 构建 AiChatRequest
     */
    private fun buildRequest(profile: LlmProfile, prompt: String): AiChatRequest {
        return AiChatRequest(
            prompt = prompt,
            model = profile.modelId,
            temperature = profile.temperature,
            skillTags = profile.skillTags,
            transcriptMarkdown = null
        )
    }
    
    /**
     * 成功结果映射
     */
    private fun mapSuccess(response: AiChatResponse): ExecutorResult.Success {
        return ExecutorResult.Success(
            content = com.smartsales.core.util.MarkdownSanitizer.strip(response.displayText),
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

