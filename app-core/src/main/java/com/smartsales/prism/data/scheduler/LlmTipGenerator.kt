package com.smartsales.prism.data.scheduler

import android.util.Log
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.crm.ClientProfileHub
import com.smartsales.prism.domain.crm.FocusedContext
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.core.util.MarkdownSanitizer
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * LLM 驱动的智能提示生成器
 * 
 * 使用 ClientProfileHub 获取实体上下文，调用 LLM 生成行动提示
 * 
 * Wave 9: Smart Tips
 * 来源: docs/cerb/scheduler/spec.md L439-508
 */
@Singleton
class LlmTipGenerator @Inject constructor(
    private val clientProfileHub: ClientProfileHub,
    private val aiChatService: AiChatService
) : TipGenerator {

    override suspend fun generate(task: TimelineItemModel.Task): List<String> {
        val entityId = task.keyPersonEntityId
        if (entityId == null) {
            Log.d(TAG, "🔕 No keyPersonEntityId, skipping tip generation")
            return emptyList()
        }
        
        // Phase 2: Query memory bank for entity context
        val context = try {
            clientProfileHub.getFocusedContext(entityId)
        } catch (e: Exception) {
            Log.d(TAG, "❌ getFocusedContext failed: ${e.message}")
            return emptyList()
        }
        
        // DEBUG: dump context to diagnose 0-tips issue
        Log.d(TAG, "🔍 FocusedContext for entity=$entityId: " +
            "displayName=${context.entity.displayName}, " +
            "attrsLen=${context.entity.attributesJson.length}, " +
            "demeanor=${context.entity.demeanorJson.take(50)}, " +
            "relatedContacts=${context.relatedContacts.size}, " +
            "timeline=${context.activityState.factualItems.size}, " +
            "habits=${context.habitContext.clientHabits.size}")
        context.activityState.factualItems.forEachIndexed { i, a ->
            Log.d(TAG, "🔍 timeline[$i]: ${a.content.take(80)}")
        }
        
        // Build prompt from spec L477-491
        val prompt = buildTipPrompt(task, context)
        Log.d(TAG, "📝 Prompt built (${prompt.length} chars) for task=${task.id}")
        Log.d(TAG, "📝 Full prompt:\n$prompt")
        
        // LLM call → parse JSON array
        val request = AiChatRequest(
            prompt = prompt,
            model = ModelRegistry.COACH.modelId,  // qwen-plus (fast/competent for tips)
            temperature = ModelRegistry.COACH.temperature
        )
        
        val result = aiChatService.sendMessage(request)
        
        return when (result) {
            is com.smartsales.core.util.Result.Success -> {
                Log.d(TAG, "🔍 Raw LLM response: ${result.data.displayText}")
                val tips = parseTipsJson(result.data.displayText)
                Log.d(TAG, "✅ LLM returned ${tips.size} tips for task=${task.id}")
                tips
            }
            is com.smartsales.core.util.Result.Error -> {
                Log.e(TAG, "❌ LLM failure for task=${task.id}", result.throwable)
                emptyList()
            }
        }
    }
    
    /**
     * 构建提示提示词（根据 spec L477-491）
     */
    private fun buildTipPrompt(task: TimelineItemModel.Task, ctx: FocusedContext): String {
        val taskTime = formatTime(task.startTime)
        val contextStr = ctx.toPromptString()
        
        return """
你是销售助手。用户即将参加以下日程：
任务: "${task.title}" ($taskTime)
关键人物: ${task.keyPerson ?: "未知"}

以下是关于该关键人物的上下文信息：
$contextStr

请生成 2-5 条简短的事实提醒，帮助用户回忆可能忽略的关键信息。
每条提示一行，JSON 数组格式：["提示1", "提示2", ...]

规则：
- 只返回有实际价值的信息，不要说废话
- 基于已知数据，绝不编造
- 只陈述事实，不给建议或话术
- 语气像备忘录，不像教练
- 如果没有足够上下文，返回空数组 []
        """.trimIndent()
    }
    
    /**
     * 解析 LLM 返回的 JSON 数组
     */
    private fun parseTipsJson(raw: String): List<String> {
        return try {
            // 提取 JSON 数组（可能包含在 markdown 代码块中）
            val jsonStr = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            if (jsonStr.isBlank() || jsonStr == "[]") {
                return emptyList()
            }
            
            val tipsArray = JSONArray(jsonStr)
            val result = mutableListOf<String>()
            for (i in 0 until tipsArray.length()) {
                val rawTip = tipsArray.getString(i)
                // Sanitize tip content
                val cleanTip = MarkdownSanitizer.strip(rawTip)
                if (cleanTip.isNotBlank()) {
                    result.add(cleanTip)
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON parsing failed: ${e.message}, raw=$raw")
            emptyList()
        }
    }
    
    /**
     * 格式化时间为人类可读格式
     */
    private fun formatTime(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
    
    companion object {
        private const val TAG = "TipGenerator"
    }
}

/**
 * FocusedContext 序列化为 LLM 提示字符串
 */
private fun FocusedContext.toPromptString(): String = buildString {
    appendLine("人物: ${entity.displayName} (${entity.entityType})")
    
    if (entity.attributesJson.isNotBlank() && entity.attributesJson != "{}") {
        appendLine("属性: ${entity.attributesJson}")
    }
    if (entity.demeanorJson.isNotBlank() && entity.demeanorJson != "{}") {
        appendLine("沟通风格: ${entity.demeanorJson}")
    }
    
    if (relatedContacts.isNotEmpty()) {
        appendLine("关联联系人: ${relatedContacts.joinToString { it.displayName }}")
    }
    
    if (activityState.factualItems.isNotEmpty()) {
        appendLine("最近活动:")
        activityState.factualItems.take(5).forEach { appendLine("  - ${it.content}") }
    }
    
    if (habitContext.clientHabits.isNotEmpty()) {
        appendLine("客户偏好:")
        habitContext.clientHabits.forEach { 
            appendLine("  - ${it.habitKey}: ${it.habitValue}")
        }
    }
}
