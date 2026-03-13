package com.smartsales.core.pipeline

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.smartsales.prism.domain.rl.RlPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealHabitListener @Inject constructor(
    private val executor: Executor,
    private val reinforcementLearner: ReinforcementLearner,
    private val contextBuilder: com.smartsales.core.context.ContextBuilder
) : HabitListener {

    companion object {
        private const val TAG = "RealHabitListener"
        private const val SYSTEM_PROMPT = """
你是一个后台强化学习观察员（Reinforcement Learning Observer）。
你的任务是从用户当前的输入以及对话历史中，识别出用户偏好或客户偏好。

查找关于以下方面的规则或习惯倾向：
- 偏好的会议时间（如：喜欢早上开会）
- 偏好的会议时长
- 回复或沟通的倾向
- 任何形式的"别再XXX"（负面反馈）

如果有发现，输出结构化的 JSON 数组 `rl_observations`。如果没有任何偏好信息，输出一个空数组。

REQUIRED JSON SCHEMA:
{
  "rl_observations": [
    {
      "entityId": "String or null", // null 代表用户全局偏好，若针对某个客户，输出该客户的ID（从上下文中寻找）
      "key": "String", // 习惯分类键，如 "preferred_meeting_time", "default_duration"
      "value": "String", // 习惯的值，如 "morning", "30m"
      "source": "USER_POSITIVE|USER_NEGATIVE|INFERRED", // USER_POSITIVE(明确同意), USER_NEGATIVE(明确拒绝), INFERRED(暗含)
      "evidence": "String" // 原始支持这句话的上下文
    }
  ]
}

直接输出符合规范的JSON，不要Markdown标记语言，不要有多余文字。
"""
    }

    override fun analyzeAsync(rawInput: String, context: EnhancedContext, coroutineScope: CoroutineScope) {
        Log.d(TAG, "analyzeAsync: Triggered background assessment for '$rawInput'")

        coroutineScope.launch {
            try {
                // Compile the background prompt
                var promptBody = "== CONTEXT ==\n"
                
                // Inject relevant entities for disambiguation of entityId
                if (context.entityContext.isNotEmpty()) {
                    promptBody += "Active Entities:\n"
                    context.entityContext.forEach { (name, entity) ->
                        promptBody += "- ID: ${entity.entityId} Name: ${entity.displayName}\n"
                    }
                }

                // Inject history for temporal relations
                if (context.sessionHistory.isNotEmpty()) {
                    promptBody += "History:\n"
                    context.sessionHistory.takeLast(4).forEach { turn ->
                        promptBody += "[${turn.role}] ${turn.content}\n"
                    }
                }

                promptBody += "\n== CURRENT USER INPUT ==\n$rawInput"

                val fullPrompt = "$SYSTEM_PROMPT\n\n$promptBody"

                val response = when (val result = executor.execute(ModelRegistry.EXTRACTOR, fullPrompt)) {
                    is ExecutorResult.Success -> result.content
                    is ExecutorResult.Failure -> {
                        Log.e(TAG, "analyzeAsync: LLM execution failed: ${result.error}")
                        return@launch
                    }
                }

                val cleanJson = response.replace("```json", "").replace("```", "").trim()
                val payload = try {
                    Json { ignoreUnknownKeys = true }.decodeFromString<RlPayload>(cleanJson)
                } catch (e: Exception) {
                    Log.w(TAG, "analyzeAsync: Failed to parse RlPayload JSON, ignoring. Error: ${e.message}")
                    return@launch
                }

                if (payload.rlObservations.isEmpty()) {
                    Log.d(TAG, "analyzeAsync: No learnable habits detected in RlPayload.")
                    return@launch
                }

                val parsedObservations = mutableListOf<RlObservation>()
                for (obs in payload.rlObservations) {
                    val entityIdRaw = obs.entityId
                    val entityId = if (entityIdRaw == "null" || entityIdRaw.isNullOrBlank()) null else entityIdRaw
                    
                    if (obs.key.isBlank() || obs.value.isBlank()) continue

                    parsedObservations.add(
                        obs.copy(entityId = entityId)
                    )
                    Log.d(TAG, "analyzeAsync: Parsed observation: key=${obs.key} value=${obs.value} source=${obs.source} entityId=$entityId")
                }

                if (parsedObservations.isNotEmpty()) {
                    Log.d(TAG, "analyzeAsync: Sending ${parsedObservations.size} observations to ReinforcementLearner")
                    reinforcementLearner.processObservations(parsedObservations)
                    contextBuilder.applyHabitUpdates(parsedObservations)
                }

            } catch (e: Exception) {
                Log.e(TAG, "analyzeAsync: Unhandled exception during background assessment", e)
            }
        }
    }
}
