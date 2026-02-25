package com.smartsales.prism.data.real.session

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.domain.session.TitleResult
import org.json.JSONObject
import javax.inject.Inject

/**
 * 真实会话标题生成器 — 基于 LLM 分析
 * 
 * 使用 AiChatService (轻量级) 而非完整 Orchestrator 管道，避免资源竞争。
 */
class LlmSessionTitleGenerator @Inject constructor(
    private val aiChatService: AiChatService
) : SessionTitleGenerator {

    companion object {
        private const val TAG = "LlmTitleGen"
    }

    override suspend fun generateTitle(history: List<ChatTurn>): TitleResult {
        // 构建提示词
        val conversationText = history.joinToString("\n") { turn ->
            "${if (turn.role == "user") "用户" else "AI"}: ${turn.content}"
        }

        val prompt = """
            任务：分析以下销售对话，提取客户称呼和会话摘要。
            
            要求：
            1. clientName: 客户的称呼（如"王总"、"李经理"）。如果未知，返回"客户"。
            2. summary: 6个字以内的摘要，概括对话核心意图。
            3. 返回纯 JSON 格式。
            
            对话内容：
            $conversationText
            
            返回格式示例：
            {"clientName": "张总", "summary": "询问产品报价"}
        """.trimIndent()

        Log.d(TAG, "Requesting title generation for history length: ${history.size}")

        val request = AiChatRequest(
            prompt = prompt,
            model = "qwen-turbo", // 使用快速模型
            skillTags = setOf("summarization")
        )

        return when (val result = aiChatService.sendMessage(request)) {
            is Result.Success -> {
                try {
                    val jsonStr = result.data.structuredMarkdown ?: result.data.displayText
                    // 尝试提取 JSON (简单的容错处理)
                    val jsonStart = jsonStr.indexOf("{")
                    val jsonEnd = jsonStr.lastIndexOf("}")
                    if (jsonStart != -1 && jsonEnd != -1) {
                        val cleanJson = jsonStr.substring(jsonStart, jsonEnd + 1)
                        val jsonObj = JSONObject(cleanJson)
                        
                        val client = jsonObj.optString("clientName", "客户")
                        val summary = jsonObj.optString("summary", "新会话")
                        
                        Log.d(TAG, "Generated: client=$client, summary=$summary")
                        TitleResult(client, summary)
                    } else {
                        Log.w(TAG, "Failed to find JSON in response: $jsonStr")
                        TitleResult("客户", "对话摘要")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing failed", e)
                    TitleResult("客户", "对话摘要") // Fallback
                }
            }
            is Result.Error -> {
                Log.e(TAG, "LLM request failed", result.throwable)
                TitleResult("客户", "对话摘要") // Fallback
            }
        }
    }
}
