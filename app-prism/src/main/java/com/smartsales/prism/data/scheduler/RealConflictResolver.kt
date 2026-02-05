package com.smartsales.prism.data.scheduler

import android.util.Log
import com.smartsales.prism.data.real.DashscopeExecutor
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.scheduler.ActionType
import com.smartsales.prism.domain.scheduler.ConflictAction
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实冲突解决器 — 使用 LLM 理解用户意图
 * 
 * 对接 DashscopeExecutor，解析用户对冲突的回复（如 "保留第一个"、"取消午餐"）
 * 返回结构化动作供 ViewModel 执行
 */
@Singleton
class RealConflictResolver @Inject constructor(
    private val dashscopeExecutor: DashscopeExecutor
) {
    
    /**
     * 解决冲突 — 调用 LLM 理解用户意图
     * 
     * @param userMessage 用户输入（如 "保留第一个"、"取消午餐"）
     * @param taskA 冲突任务A
     * @param taskB 冲突任务B
     * @return ConflictAction 包含动作类型和要删除的任务ID
     */
    suspend fun resolve(
        userMessage: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): ConflictAction {
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
            is ExecutorResult.Success -> parseConflictAction(result.content, taskA, taskB)
            is ExecutorResult.Failure -> {
                Log.e("ConflictResolver", "LLM failed: ${result.error}")
                ConflictAction(
                    action = ActionType.NONE,
                    taskToRemove = null,
                    taskToReschedule = null,
                    rescheduleText = null,
                    reply = "解析失败，请重试"
                )
            }
        }
    }
    
    /**
     * 构建冲突解决提示词
     */
    private fun buildConflictPrompt(
        userMessage: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): String = """
你是日程冲突解决助手。用户有以下冲突：

- 任务A: ${taskA.title} @ ${formatTime(taskA.scheduledAt)}
- 任务B: ${taskB.title} @ ${formatTime(taskB.scheduledAt)}

用户说: "$userMessage"

返回 JSON:
{
  "action": "keep_a" | "keep_b" | "reschedule" | "none", 
  "target": "a" | "b" (仅当 action="reschedule"),
  "time": "明天下午3点" (仅当 action="reschedule", 提取用户说的时间),
  "reply": "回复用户"
}

action 规则:
- keep_a: 保留任务A，删除任务B
- keep_b: 保留任务B，删除任务A  
- reschedule: 用户明确说要改某一个任务的时间
- none: 不确定用户意图

只输出 JSON，不要有任何其他文字。
    """.trimIndent()
    
    /**
     * 解析 LLM 返回的 JSON
     */
    private fun parseConflictAction(
        llmResponse: String,
        taskA: ScheduleItem,
        taskB: ScheduleItem
    ): ConflictAction {
        return try {
            val json = JSONObject(llmResponse.trim())
            val action = when (json.optString("action")) {
                "keep_a" -> ActionType.KEEP_A
                "keep_b" -> ActionType.KEEP_B
                "reschedule" -> ActionType.RESCHEDULE
                else -> ActionType.NONE
            }
            val reply = json.optString("reply", "已处理")
            
            // Logic for KEEP actions
            val taskToRemove = when (action) {
                ActionType.KEEP_A -> taskB.entryId
                ActionType.KEEP_B -> taskA.entryId
                else -> null
            }
            
            // Logic for RESCHEDULE action
            var taskToReschedule: String? = null
            var rescheduleText: String? = null
            
            if (action == ActionType.RESCHEDULE) {
                val target = json.optString("target")
                taskToReschedule = if (target.equals("b", ignoreCase = true)) taskB.entryId else taskA.entryId
                rescheduleText = json.optString("time")
                
                // If time is missing, fallback to user intent in reply or just generic prompt
                if (rescheduleText.isNullOrEmpty()) {
                    rescheduleText = "稍后" // Fallback parsing needed downstream or re-prompt
                }
            }
            
            Log.d("ConflictResolver", "Parsed action: $action, reply: $reply")
            
            ConflictAction(
                action = action, 
                taskToRemove = taskToRemove,
                taskToReschedule = taskToReschedule,
                rescheduleText = rescheduleText,
                reply = reply
            )
        } catch (e: Exception) {
            Log.e("ConflictResolver", "JSON parse failed: ${e.message}", e)
            ConflictAction(
                action = ActionType.NONE,
                taskToRemove = null,
                taskToReschedule = null,
                rescheduleText = null,
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
