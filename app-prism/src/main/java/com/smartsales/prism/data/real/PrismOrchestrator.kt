package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.CandidateOption
import com.smartsales.prism.domain.model.ClarificationType
import android.util.Log
import com.smartsales.prism.domain.model.Mode

import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.SchedulerActionResult
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.ReminderType
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val schedulerLinter: SchedulerLinter,
    private val scheduleBoard: com.smartsales.prism.domain.memory.ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val reinforcementLearner: com.smartsales.prism.domain.rl.ReinforcementLearner
) : Orchestrator {
    
    // Fire-and-forget scope for RL learning
    private val rlScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
    
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
                    // === Wave 2: Fire-and-forget RL learning ===
                    val observations = parseRlObservations(result.content)
                    if (observations.isNotEmpty()) {
                        rlScope.launch {
                            reinforcementLearner.processObservations(observations)
                        }
                    }
                    
                    // 验证并解析 LLM 输出
                    when (val lintResult = schedulerLinter.lint(result.content)) {
                        is LintResult.Success -> {
                            // === Phase 2: Entity Resolution ===
                            // Query RelevancyLib with Phase 1 clues
                            val phase2Context = (contextBuilder as? RealContextBuilder)
                                ?.buildWithClues(input, Mode.SCHEDULER, lintResult.parsedClues)
                            
                            // Check for ambiguous person (multiple candidates)
                            val personCandidates = phase2Context?.entityContext
                                ?.filter { it.key.startsWith("person_candidate_") }
                                ?.values?.toList() ?: emptyList()
                            
                            if (personCandidates.size > 1) {
                                // Phase 2: Ask user to disambiguate
                                activityController.complete()
                                return UiState.AwaitingClarification(
                                    question = "请问是哪位${lintResult.parsedClues.person}？",
                                    clarificationType = ClarificationType.AMBIGUOUS_PERSON,
                                    candidates = personCandidates.map { ref ->
                                        CandidateOption(
                                            entityId = ref.entityId,
                                            displayName = ref.displayName,
                                            description = ref.entityType
                                        )
                                    }
                                )
                            }
                            
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
                        
                        // Phase 1 Loop: Missing mandatory fields
                        is LintResult.Incomplete -> {
                            activityController.complete()
                            UiState.AwaitingClarification(
                                question = lintResult.question,
                                clarificationType = when (lintResult.missingField) {
                                    "startTime" -> ClarificationType.MISSING_TIME
                                    "duration" -> ClarificationType.MISSING_DURATION
                                    else -> ClarificationType.MISSING_TIME
                                }
                            )
                        }
                        
                        is LintResult.Error -> {
                            activityController.error(lintResult.message)
                            UiState.Error(lintResult.message, retryable = true)
                        }
                        
                        // Wave 4.0: Classification Results
                        is LintResult.NonIntent -> {
                            activityController.complete()
                            UiState.Idle  // Silently ignore non-scheduling input
                        }
                        
                        is LintResult.Inspiration -> {
                            // Wave 5: 存储到灵感仓库
                            inspirationRepository.insert(lintResult.content)
                            activityController.complete()
                            UiState.Toast("💡 已存入灵感箱")
                        }
                        
                        // Wave 4.1: Multi-Task Direct Insert
                        is LintResult.MultiTask -> {
                            // Insert all tasks directly (no confirmation)
                            var anyConflict = false
                            
                            lintResult.tasks.forEach { task ->
                                // Check conflict first (informational)
                                val conflictResult = scheduleBoard.checkConflict(
                                    task.startTime.toEpochMilli(),
                                    task.durationMinutes
                                )
                                if (conflictResult is ConflictResult.Conflict) {
                                    anyConflict = true
                                }
                                
                                // Insert task
                                scheduledTaskRepository.insertTask(task)
                            }
                            
                            scheduleBoard.refresh()
                            activityController.complete()
                            
                            val warning = if (anyConflict) " (部分任务有冲突)" else ""
                            UiState.Response("✅ 已创建 ${lintResult.tasks.size} 个任务${warning}")
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
            val prompt = buildReschedulePrompt(itemId, userText)
            val context = contextBuilder.build(prompt, Mode.SCHEDULER)
            
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    when (val lintResult = schedulerLinter.lint(result.content)) {
                        is LintResult.Success -> {
                            // 更新已有任务
                            var updatedTask = lintResult.task.copy(id = itemId)
                            
                            // 防御性检查: 保护日期不被意外修改
                            val originalTask = scheduledTaskRepository.getTask(itemId)
                            if (originalTask != null) {
                                val originalDate = originalTask.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                                val newDate = updatedTask.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                                
                                // 检测日期变化
                                val hasDateKeyword = userText.contains(Regex("明天|后天|今天|周|星期|号|日"))
                                if (originalDate != newDate && !hasDateKeyword) {
                                    Log.w("PrismOrch", "⚠️ Date changed without date keyword: $originalDate → $newDate, input='$userText', preserving original date")
                                    // 保留原日期,只改时间
                                    val newTime = updatedTask.startTime.atZone(ZoneId.systemDefault()).toLocalTime()
                                    updatedTask = updatedTask.copy(
                                        startTime = originalDate.atTime(newTime).atZone(ZoneId.systemDefault()).toInstant()
                                    )
                                }
                            }
                            
                            // 检查冲突 BEFORE 更新
                            val conflictResult = scheduleBoard.checkConflict(
                                updatedTask.startTime.toEpochMilli(),
                                updatedTask.durationMinutes,
                                excludeId = itemId
                            )
                            val warning = if (conflictResult is ConflictResult.Conflict) " (与其他任务冲突)" else ""
                            
                            scheduledTaskRepository.updateTask(updatedTask)
                            
                            // 计算新日期偏移量 (用于 UI 高亮)
                            val zone = ZoneId.systemDefault()
                            val today = LocalDate.now(zone)
                            val newDate = updatedTask.startTime.atZone(zone).toLocalDate()
                            val newDayOffset = ChronoUnit.DAYS.between(today, newDate).toInt()
                            
                            // 更新提醒
                            alarmScheduler.cancelReminder(itemId)
                            lintResult.reminderType?.let { reminderType ->
                                val startTime = parseStartTime(lintResult.task.dateRange)
                                if (startTime != null) {
                                    alarmScheduler.scheduleReminder(itemId, startTime, reminderType)
                                }
                            }
                            
                            activityController.complete()
                            SchedulerActionResult.Success("已更新任务时间${warning}", newDayOffset = newDayOffset)
                        }
                        is LintResult.Error -> {
                            activityController.error(lintResult.message)
                            SchedulerActionResult.Failure(lintResult.message)
                        }
                        is LintResult.Incomplete -> {
                            activityController.complete()
                            SchedulerActionResult.Failure("请提供更具体的时间信息")
                        }
                        is LintResult.NonIntent -> {
                            activityController.complete()
                            SchedulerActionResult.Failure("未识别到有效的改期请求")
                        }
                        is LintResult.Inspiration -> {
                            activityController.complete()
                            SchedulerActionResult.Failure("这看起来不是改期请求")
                        }
                        
                        // Wave 4.1: MultiTask not supported in reschedule action
                        is LintResult.MultiTask -> {
                            activityController.complete()
                            SchedulerActionResult.Failure("改期操作不支持多任务")
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
    
    private suspend fun buildReschedulePrompt(itemId: String, userText: String): String {
        val task = scheduledTaskRepository.getTask(itemId)
        val context = if (task != null) {
            "当前任务: ${task.title} 在 ${task.dateRange}"
        } else {
            Log.w("PrismOrch", "Reschedule: task $itemId not found, using generic prompt")
            "改期任务"
        }
        return """
            $context
            用户请求: $userText
            
            解析规则:
            1. 日期基准(Anchor)判断:
               - 绝对日期词(如"下周五"、"明天"、"2月10号"): 以【今天】为基准计算新日期
               - 相对延后词(如"推迟2天"、"延后1周"): 以【原任务日期】为基准计算
            2. 仅时间调整:
               - 如"提前1小时"、"改到下午3点", 且未提及日期: 保持【原任务日期】不变
            3. 输出 JSON 格式的新时间安排
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
    
    /**
     * Parse rl_observations from LLM JSON
     * 
     * Wave 2: Extracts learning signals for preference tracking
     */
    private fun parseRlObservations(llmOutput: String): List<com.smartsales.prism.domain.rl.RlObservation> {
        return try {
            val json = org.json.JSONObject(llmOutput)
            val observations = json.optJSONArray("rl_observations") ?: return emptyList()
            
            (0 until observations.length()).mapNotNull { i ->
                try {
                    val obs = observations.getJSONObject(i)
                    com.smartsales.prism.domain.rl.RlObservation(
                        entityId = obs.optString("entityId").takeIf { it.isNotBlank() },
                        key = obs.getString("key"),
                        value = obs.getString("value"),
                        source = when (obs.optString("source", "INFERRED")) {
                            "USER_POSITIVE" -> com.smartsales.prism.domain.rl.ObservationSource.USER_POSITIVE
                            "USER_NEGATIVE" -> com.smartsales.prism.domain.rl.ObservationSource.USER_NEGATIVE
                            else -> com.smartsales.prism.domain.rl.ObservationSource.INFERRED
                        },
                        evidence = obs.optString("evidence").takeIf { it.isNotBlank() }
                    )
                } catch (e: Exception) {
                    Log.w("PrismOrchestrator", "Skipping malformed observation: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("PrismOrchestrator", "Failed to parse rl_observations: ${e.message}")
            emptyList()
        }
    }
}
