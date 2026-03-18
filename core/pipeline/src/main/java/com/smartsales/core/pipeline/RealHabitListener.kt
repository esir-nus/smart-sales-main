package com.smartsales.core.pipeline

import android.util.Log
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.core.telemetry.PipelineValve
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

如果上下文中出现 Scheduler Pattern Signals，它们只用于推断**用户全局习惯**，
不能单独拿来推断客户/实体习惯。客户/实体习惯必须依赖当前活跃实体上下文。

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
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.RL_LISTENER_TRIGGERED,
            payloadSize = rawInput.length + context.sessionHistory.size + context.entityContext.size,
            summary = "RL listener triggered from latest user input",
            rawDataDump = buildListenerSummary(rawInput, context)
        )

        coroutineScope.launch {
            try {
                context.schedulerPatternContext?.let { schedulerPattern ->
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.RL_SCHEDULER_PATTERN_ATTACHED,
                        payloadSize = schedulerPattern.toPromptSummary().length,
                        summary = "Scheduler pattern context attached to RL packet",
                        rawDataDump = schedulerPattern.toPromptSummary()
                    )
                }

                val fullPrompt = "$SYSTEM_PROMPT\n\n${buildPromptBody(rawInput, context)}"

                val response = when (val result = executor.execute(ModelRegistry.EXTRACTOR, fullPrompt)) {
                    is ExecutorResult.Success -> result.content
                    is ExecutorResult.Failure -> {
                        Log.e(TAG, "analyzeAsync: LLM execution failed: ${result.error}")
                        return@launch
                    }
                }

                val cleanJson = response.replace("```json", "").replace("```", "").trim()
                PipelineValve.tag(
                    checkpoint = PipelineValve.Checkpoint.RL_EXTRACTION_EMITTED,
                    payloadSize = cleanJson.length,
                    summary = "RL extractor emitted payload",
                    rawDataDump = cleanJson
                )
                val jsonInterpreter = Json { 
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
                
                val payload = try {
                    if (cleanJson.startsWith("[")) {
                        val list = jsonInterpreter.decodeFromString<List<RlObservation>>(cleanJson)
                        RlPayload(rlObservations = list)
                    } else {
                        jsonInterpreter.decodeFromString<RlPayload>(cleanJson)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "analyzeAsync: Failed to parse RlPayload JSON, ignoring. Error: ${e.message}\nJSON input: $cleanJson")
                    return@launch
                }
                PipelineValve.tag(
                    checkpoint = PipelineValve.Checkpoint.RL_PAYLOAD_DECODED,
                    payloadSize = payload.rlObservations.size,
                    summary = "RL payload decoded",
                    rawDataDump = payload.rlObservations.joinToString(separator = "\n") { obs ->
                        "entityId=${obs.entityId} key=${obs.key} value=${obs.value} source=${obs.source}"
                    }
                )

                if (payload.rlObservations.isEmpty()) {
                    Log.d(TAG, "analyzeAsync: No learnable habits detected in RlPayload.")
                    return@launch
                }

                val parsedObservations = mutableListOf<RlObservation>()
                for (obs in payload.rlObservations) {
                    val entityIdRaw = obs.entityId
                    val entityId = if (entityIdRaw == "null" || entityIdRaw.isNullOrBlank()) null else entityIdRaw
                    
                    if (obs.key.isBlank() || obs.value.isBlank()) continue
                    if (context.schedulerPatternContext != null && entityId != null) {
                        Log.w(TAG, "analyzeAsync: Dropping entity-bound observation because scheduler pattern context is user-habit-only by default (entityId=$entityId)")
                        continue
                    }
                    if (entityId != null && !isActiveEntity(entityId, context)) {
                        Log.w(TAG, "analyzeAsync: Dropping contextual observation for inactive entityId=$entityId")
                        continue
                    }

                    parsedObservations.add(
                        obs.copy(entityId = entityId)
                    )
                    Log.d(TAG, "analyzeAsync: Parsed observation: key=${obs.key} value=${obs.value} source=${obs.source} entityId=$entityId")
                }

                if (parsedObservations.isNotEmpty()) {
                    Log.d(TAG, "analyzeAsync: Sending ${parsedObservations.size} observations to ReinforcementLearner")
                    reinforcementLearner.processObservations(parsedObservations)
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.RL_HABIT_WRITE_EXECUTED,
                        payloadSize = parsedObservations.size,
                        summary = "RL learner processed observations into habit storage",
                        rawDataDump = parsedObservations.joinToString(separator = "\n") { obs ->
                            "entityId=${obs.entityId} key=${obs.key} value=${obs.value} source=${obs.source}"
                        }
                    )
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.DB_WRITE_EXECUTED,
                        payloadSize = parsedObservations.size,
                        summary = "RL habit write executed",
                        rawDataDump = parsedObservations.joinToString(separator = "\n") { obs ->
                            "entityId=${obs.entityId} key=${obs.key} value=${obs.value}"
                        }
                    )
                    contextBuilder.applyHabitUpdates(parsedObservations)
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.RL_RAM_REFRESH_APPLIED,
                        payloadSize = parsedObservations.size,
                        summary = "RL habit write-through refreshed RAM sections",
                        rawDataDump = parsedObservations.joinToString(separator = "\n") { obs ->
                            "entityId=${obs.entityId} key=${obs.key} value=${obs.value}"
                        }
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "analyzeAsync: Unhandled exception during background assessment", e)
            }
        }
    }

    private fun buildPromptBody(rawInput: String, context: EnhancedContext): String {
        val promptBody = StringBuilder("== CONTEXT ==\n")

        if (context.entityContext.isNotEmpty()) {
            promptBody.append("Active Entities:\n")
            context.entityContext.forEach { (_, entity) ->
                promptBody.append("- ID: ${entity.entityId} Name: ${entity.displayName}\n")
            }
        }

        if (context.sessionHistory.isNotEmpty()) {
            promptBody.append("History:\n")
            context.sessionHistory.takeLast(4).forEach { turn ->
                promptBody.append("[${turn.role}] ${turn.content}\n")
            }
        }

        context.schedulerPatternContext?.let { schedulerPattern ->
            promptBody.append("Scheduler Pattern Signals:\n")
            promptBody.append(schedulerPattern.toPromptSummary())
            promptBody.append('\n')
        }

        promptBody.append("\n== CURRENT USER INPUT ==\n")
        promptBody.append(rawInput)
        return promptBody.toString()
    }

    private fun buildListenerSummary(rawInput: String, context: EnhancedContext): String {
        return buildString {
            appendLine("rawInput=$rawInput")
            appendLine("entityCount=${context.entityContext.size}")
            appendLine("historyTurns=${context.sessionHistory.size}")
            appendLine("schedulerPatternAttached=${context.schedulerPatternContext != null}")
            appendLine("packet=latest_input+bounded_recent_turns+active_entity_context${if (context.schedulerPatternContext != null) "+scheduler_pattern_context" else ""}")
        }
    }

    private fun isActiveEntity(entityId: String, context: EnhancedContext): Boolean {
        return context.entityContext.values.any { it.entityId == entityId }
    }
}
