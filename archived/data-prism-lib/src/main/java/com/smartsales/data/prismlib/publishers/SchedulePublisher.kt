package com.smartsales.data.prismlib.publishers

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartsales.domain.prism.core.ExecutorResult
import com.smartsales.domain.prism.core.UiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler 模式发布器 — 解析 JSON 命令并展示日程确认
 * @see Prism-V1.md §2.2 #4, §4.3 Schedule Mode
 */
@Singleton
class SchedulePublisher @Inject constructor() : BaseModePublisher() {

    private val gson = Gson()

    override suspend fun publish(result: ExecutorResult) {
        // 1. 设置 Thinking 状态
        setThinking("正在处理日程...")

        // 2. 尝试解析结构化 JSON
        val schedulerCommand = result.structuredJson?.let { json ->
            parseSchedulerCommand(json)
        }

        // 3. 验证解析结果
        if (schedulerCommand == null && result.structuredJson != null) {
            // JSON 解析失败
            setError("日程格式错误，请重试", retryable = true)
            return
        }

        // 4. 模拟流式输出确认消息
        val confirmationMessage = buildConfirmationMessage(result.displayContent, schedulerCommand)
        simulateStreaming(confirmationMessage)

        // 5. 发布最终响应
        _uiState.value = UiState.Response(
            content = confirmationMessage,
            structuredJson = result.structuredJson
        )
    }

    /**
     * 解析 Scheduler 命令 JSON
     */
    private fun parseSchedulerCommand(json: String): SchedulerCommand? {
        return try {
            gson.fromJson(json, SchedulerCommand::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * 构建确认消息
     */
    private fun buildConfirmationMessage(
        displayContent: String,
        command: SchedulerCommand?
    ): String {
        if (command == null) {
            return displayContent
        }

        val builder = StringBuilder()
        builder.append(displayContent)
        
        // 添加智能提示（如有冲突）
        command.conflicts?.takeIf { it.isNotEmpty() }?.let { conflicts ->
            builder.append("\n\n⚠️ 注意事项：")
            conflicts.forEach { conflict ->
                builder.append("\n• $conflict")
            }
        }

        return builder.toString()
    }

    /**
     * Scheduler 命令数据类（内部使用）
     */
    private data class SchedulerCommand(
        val action: String?,           // CREATE, MODIFY, DELETE
        val title: String?,
        val scheduledAt: Long?,
        val priority: String?,
        val reminder: Boolean?,
        val conflicts: List<String>?   // 冲突提示
    )
}
