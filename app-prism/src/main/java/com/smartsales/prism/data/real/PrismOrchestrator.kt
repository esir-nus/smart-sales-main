package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.SchedulerActionResult
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.ReminderType
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生产环境 Orchestrator — 真实 LLM 调用
 * 
 * @see Prism-V1.md §2.2 Core Components
 */
@Singleton
class PrismOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val activityController: AgentActivityController,
    private val analystController: AnalystFlowControllerV2,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val alarmScheduler: AlarmScheduler,
    private val schedulerLinter: SchedulerLinter
) : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    /** Analyst 状态流 — UI 可观察 */
    val analystState: StateFlow<AnalystState> = analystController.state
    
    override suspend fun processInput(input: String): UiState {
        return when (_currentMode.value) {
            Mode.COACH -> processCoachInput(input)
            Mode.ANALYST -> processAnalystInput(input)
            Mode.SCHEDULER -> processSchedulerInput(input)
        }
    }
    
    private suspend fun processCoachInput(input: String): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        
        return try {
            activityController.updateAction(ActivityAction.ASSEMBLING)
            val context = contextBuilder.build(input, Mode.COACH)
            activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
            
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    activityController.complete()
                    UiState.Response(result.content)
                }
                is ExecutorResult.Failure -> {
                    activityController.error(result.error)
                    UiState.Error(result.error, result.retryable)
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "未知错误"
            activityController.error(errorMessage)
            UiState.Error(errorMessage, retryable = true)
        }
    }
    
    private suspend fun processAnalystInput(input: String): UiState {
        analystController.handleInput(input)
        val structuredState = analystController.state.first { it is AnalystState.Structured }
        return UiState.PlannerTableState((structuredState as AnalystState.Structured).table)
    }
    
    override suspend fun createScheduledTask(input: String): UiState {
        return processSchedulerInput(input)
    }
    
    private suspend fun processSchedulerInput(input: String): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        
        return try {
            // 构建 Scheduler 上下文
            val context = contextBuilder.build(input, Mode.SCHEDULER)
            activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
            
            // 调用 LLM 解析
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    // 验证并解析 LLM 输出
                    when (val lintResult = schedulerLinter.lint(result.content)) {
                        is LintResult.Success -> {
                            // 插入任务到日历
                            val taskId = scheduledTaskRepository.insertTask(lintResult.task)
                            
                            // 设置提醒
                            lintResult.reminderType?.let { reminderType ->
                                val startTime = parseStartTime(lintResult.task.dateRange)
                                if (startTime != null) {
                                    when (reminderType) {
                                        ReminderType.SMART_CASCADE -> {
                                            alarmScheduler.scheduleSmartCascade(
                                                taskId, startTime, lintResult.taskTypeHint
                                            )
                                        }
                                        ReminderType.SINGLE -> {
                                            alarmScheduler.scheduleReminder(
                                                taskId, startTime, reminderType
                                            )
                                        }
                                    }
                                }
                            }
                            
                            activityController.complete()
                            
                            // 计算实际的日期偏移量
                            val zone = ZoneId.systemDefault()
                            val today = LocalDate.now(zone)
                            val taskDate = lintResult.task.startTime.atZone(zone).toLocalDate()
                            val dayOffset = ChronoUnit.DAYS.between(today, taskDate).toInt()
                            
                            UiState.SchedulerTaskCreated(
                                taskId = taskId,
                                title = lintResult.task.title,
                                dayOffset = dayOffset,
                                scheduledAtMillis = lintResult.task.startTime.toEpochMilli(),
                                durationMinutes = lintResult.task.durationMinutes
                            )
                        }
                        is LintResult.Error -> {
                            activityController.error(lintResult.message)
                            UiState.Error(lintResult.message, retryable = true)
                        }
                    }
                }
                is ExecutorResult.Failure -> {
                    activityController.error(result.error)
                    UiState.Error(result.error, result.retryable)
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "未知错误"
            activityController.error(errorMessage)
            UiState.Error(errorMessage, retryable = true)
        }
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
        if (newMode != Mode.ANALYST) {
            analystController.reset()
        }
    }
    
    /**
     * 处理日程操作（改期、取消等）
     */
    override suspend fun processSchedulerAction(itemId: String, userText: String): SchedulerActionResult {
        activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
        
        return try {
            // 构建改期上下文
            val prompt = buildReschedulePrompt(userText)
            val context = contextBuilder.build(prompt, Mode.SCHEDULER)
            
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    when (val lintResult = schedulerLinter.lint(result.content)) {
                        is LintResult.Success -> {
                            // 更新已有任务
                            val updatedTask = lintResult.task.copy(id = itemId)
                            scheduledTaskRepository.updateTask(updatedTask)
                            
                            // 更新提醒
                            alarmScheduler.cancelReminder(itemId)
                            lintResult.reminderType?.let { reminderType ->
                                val startTime = parseStartTime(lintResult.task.dateRange)
                                if (startTime != null) {
                                    alarmScheduler.scheduleReminder(itemId, startTime, reminderType)
                                }
                            }
                            
                            activityController.complete()
                            SchedulerActionResult.Success("已更新任务时间")
                        }
                        is LintResult.Error -> {
                            activityController.error(lintResult.message)
                            SchedulerActionResult.Failure(lintResult.message)
                        }
                    }
                }
                is ExecutorResult.Failure -> {
                    activityController.error(result.error)
                    SchedulerActionResult.Failure(result.error)
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "未知错误"
            activityController.error(errorMessage)
            SchedulerActionResult.Failure(errorMessage)
        }
    }
    
    private fun buildReschedulePrompt(userText: String): String {
        return """
            用户想要改期任务: $userText
            请解析用户意图并输出 JSON 格式的新时间安排。
        """.trimIndent()
    }
    
    private fun parseStartTime(dateRange: String): Instant? {
        return try {
            val parts = dateRange.split("~").map { it.trim() }
            if (parts.isEmpty()) return null
            
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            java.time.LocalDateTime.parse(parts[0], formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (e: Exception) {
            null
        }
    }
}
