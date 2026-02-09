package com.smartsales.prism.data.scheduler

import android.util.Log
import com.smartsales.prism.data.real.DashscopeExecutor
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.scheduler.ActionType
import com.smartsales.prism.domain.scheduler.ConflictAction
import com.smartsales.prism.domain.scheduler.ConflictResolution
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实冲突解决器 — 使用 LLM 理解用户意图
 * 
 * 支持复合指令（如"取消A，改期B"）返回多个动作
 */
@Singleton
class RealConflictResolver @Inject constructor(
    private val dashscopeExecutor: DashscopeExecutor
) {
    
    /**
     * 解决冲突 — 调用 LLM 理解用户意图
     * 
     * @param userMessage 用户输入（如 "取消高铁，开会推迟2小时"）
     * @param taskA 冲突任务A
     * @param taskB 冲突任务B
     * @return ConflictResolution 包含一个或多个动作
     */
    suspend fun resolve(
        userMessage: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): ConflictResolution {
        val prompt = buildConflictPrompt(userMessage, taskA, taskB)
        
        val context = EnhancedContext(
            userText = prompt,
            modeMetadata = ModeMetadata(
                currentMode = com.smartsales.prism.domain.model.Mode.SCHEDULER
            )
        )
        
        Log.d("ConflictResolver", "Calling LLM with prompt: $prompt")
        
        val result = dashscopeExecutor.execute(context)
        
        return when (result) {
            is ExecutorResult.Success -> parseConflictResolution(result.content, taskA, taskB)
            is ExecutorResult.Failure -> {
                Log.e("ConflictResolver", "LLM failed: ${result.error}")
                ConflictResolution(
                    actions = listOf(ConflictAction(
                        action = ActionType.NONE,
                        taskToRemove = null,
                        taskToReschedule = null,
                        rescheduleText = null
                    )),
                    reply = "解析失败，请重试"
                )
            }
        }
    }
    
    /**
     * 构建冲突解决提示词 — 支持复合指令
     */
    private fun buildConflictPrompt(
        userMessage: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): String = """
你是日程助手。用户有两个时间重叠的任务（重叠是可以接受的，未必一定要删除一个）：

- 任务A: ${taskA.title} @ ${formatTime(taskA.scheduledAt)}
- 任务B: ${taskB.title} @ ${formatTime(taskB.scheduledAt)}

用户说: "$userMessage"

返回 JSON（只输出JSON，不要有任何其他文字）:
{
  "actions": [
    {"action": "keep_a" | "keep_b" | "reschedule" | "coexist" | "none", "target": "a" | "b", "time": "改期时间"}
  ],
  "reply": "友好的回复文本"
}

actions 是数组，用户可能同时要求多个操作（如"取消A，改期B"）。

每个 action 的规则:
- keep_a: 保留任务A，删除任务B（不需要 target/time）
- keep_b: 保留任务B，删除任务A（不需要 target/time）
- reschedule: 改期某一个任务（需要 target 和 time）
- coexist: 两个都保留（不需要 target/time）
- none: 无法理解

示例1: "取消会议" → {"actions": [{"action": "keep_a"}], "reply": "好的，已取消会议"}
示例2: "取消高铁，开会推迟2小时" → {"actions": [{"action": "keep_a"}, {"action": "reschedule", "target": "b", "time": "推迟2小时"}], "reply": "好的，已取消高铁，开会推迟2小时"}
示例3: "都保留" → {"actions": [{"action": "coexist"}], "reply": "好的，两个都保留"}
    """.trimIndent()
    
    /**
     * 解析 LLM 返回的 JSON → ConflictResolution
     */
    private fun parseConflictResolution(
        llmResponse: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): ConflictResolution {
        return try {
            val json = JSONObject(llmResponse.trim())
            val reply = json.optString("reply", "已处理")
            val actionsArray = json.optJSONArray("actions") ?: JSONArray()
            
            val actions = mutableListOf<ConflictAction>()
            
            for (i in 0 until actionsArray.length()) {
                val actionJson = actionsArray.getJSONObject(i)
                val actionType = when (actionJson.optString("action")) {
                    "keep_a" -> ActionType.KEEP_A
                    "keep_b" -> ActionType.KEEP_B
                    "reschedule" -> ActionType.RESCHEDULE
                    "coexist" -> ActionType.COEXIST
                    else -> ActionType.NONE
                }
                
                // 根据 action 类型解析对应字段
                val taskToRemove = when (actionType) {
                    ActionType.KEEP_A -> taskB.entryId
                    ActionType.KEEP_B -> taskA.entryId
                    else -> null
                }
                
                var taskToReschedule: String? = null
                var rescheduleText: String? = null
                
                if (actionType == ActionType.RESCHEDULE) {
                    val target = actionJson.optString("target")
                    taskToReschedule = if (target.equals("b", ignoreCase = true)) taskB.entryId else taskA.entryId
                    rescheduleText = actionJson.optString("time")
                    if (rescheduleText.isNullOrEmpty()) {
                        rescheduleText = "稍后"
                    }
                }
                
                actions.add(ConflictAction(
                    action = actionType,
                    taskToRemove = taskToRemove,
                    taskToReschedule = taskToReschedule,
                    rescheduleText = rescheduleText
                ))
            }
            
            // 空数组兜底
            if (actions.isEmpty()) {
                actions.add(ConflictAction(
                    action = ActionType.NONE,
                    taskToRemove = null,
                    taskToReschedule = null,
                    rescheduleText = null
                ))
            }
            
            Log.d("ConflictResolver", "Parsed ${actions.size} actions: ${actions.map { it.action }}")
            
            ConflictResolution(actions = actions, reply = reply)
        } catch (e: Exception) {
            Log.e("ConflictResolver", "JSON parse failed: ${e.message}", e)
            ConflictResolution(
                actions = listOf(ConflictAction(
                    action = ActionType.NONE,
                    taskToRemove = null,
                    taskToReschedule = null,
                    rescheduleText = null
                )),
                reply = "解析失败: ${e.message}"
            )
        }
    }
    
    /**
     * 格式化时间戳为可读格式
     */
    private fun formatTime(epochMillis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
