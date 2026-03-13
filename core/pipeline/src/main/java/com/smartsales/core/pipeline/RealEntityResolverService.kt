package com.smartsales.core.pipeline

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService

import com.smartsales.prism.domain.memory.EntityEntry
import org.json.JSONObject
import javax.inject.Inject

/**
 * Implementation of EntityResolverService using a lightweight LLM.
 */
class RealEntityResolverService @Inject constructor(
    private val aiChatService: AiChatService,
    private val activityController: AgentActivityController
) : EntityResolverService {

    private val TAG = "RealEntityResolverService"

    override suspend fun resolve(query: String, candidates: List<EntityEntry>): EntityEntry? {
        if (candidates.isEmpty()) return null
        
        // Fast path: Exact match
        val exactMatch = candidates.find { it.displayName == query }
        if (exactMatch != null) return exactMatch

        activityController.appendTrace("🔍 尝试模糊匹配实体: $query")

        // Format candidates for the prompt
        val candidateStrings = candidates.joinToString("\n") { 
            "- ${it.displayName} (ID: ${it.entityId}, Type: ${it.entityType})" 
        }

        val prompt = "你是一个实体解析助手。用户的查询可能是错别字、简称或别名。\n\n" +
            "[查询实体]: $query\n\n" +
            "[已知候选实体列表]:\n" +
            "$candidateStrings\n\n" +
            "[任务]\n" +
            "判断用户查询最可能指代哪个候选实体。\n\n" +
            "[响应格式 -> 严格JSON]\n" +
            "{\n" +
            "  \"matched_id\": \"最匹配的实体ID（如果没有确定把握的匹配，返回null）\",\n" +
            "  \"confidence\": \"从0到1的置信度\",\n" +
            "  \"reason\": \"简短原因\"\n" +
            "}"

        // Restored LLM logic
        val request = AiChatRequest(
            prompt = prompt,
            model = com.smartsales.core.llm.ModelRegistry.EXTRACTOR.modelId, // Use a fast model
            temperature = com.smartsales.core.llm.ModelRegistry.EXTRACTOR.temperature,
            skillTags = setOf("entity_resolution")
        )

        val result = aiChatService.sendMessage(request)

        return when (result) {
            is Result.Success<com.smartsales.data.aicore.AiChatResponse> -> {
                val content = result.data.displayText
                val sanitized = content.replace("```json", "").replace("```", "").trim()
                try {
                    val json = JSONObject(sanitized)
                    val matchedId = json.optString("matched_id", "")
                    val confidence = json.optDouble("confidence", 0.0)
                    
                    if (matchedId.isNotEmpty() && matchedId != "null" && confidence >= 0.8) {
                        val match = candidates.find { it.entityId == matchedId }
                        if (match != null) {
                            activityController.appendTrace("✅ 匹配成功: $query -> ${match.displayName}")
                            match
                        } else {
                            Log.w(TAG, "LLM returned matched_id $matchedId but it is not in candidates list.")
                            null
                        }
                    } else {
                        activityController.appendTrace("⚠️ 匹配失败或置信度太低: $query")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse EntityResolver JSON", e)
                    null
                }
            }
            is Result.Error -> {
                Log.e(TAG, "Entity resolver LLM request failed", result.throwable)
                null
            }
        }
    }
}
