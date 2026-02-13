package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.CandidateOption
import com.smartsales.prism.domain.model.ClarificationType
import android.util.Log
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.UrgencyLevel
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
    private val reinforcementLearner: com.smartsales.prism.domain.rl.ReinforcementLearner,
    private val coachPipeline: com.smartsales.prism.domain.coach.CoachPipeline,
    private val entityWriter: EntityWriter,
    private val entityRepository: EntityRepository
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
            Mode.SCHEDULER -> createScheduledTask(input, null)
        }
    }
    
    private suspend fun processCoachInput(input: String): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        
        return try {
            // 获取会话历史
            val sessionHistory = contextBuilder.getSessionHistory()
            
            activityController.updateAction(ActivityAction.ASSEMBLING)
            activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.STREAMING)
            
            // 委托给 CoachPipeline 处理
            val response = coachPipeline.process(input, sessionHistory)
            
            when (response) {
                is com.smartsales.prism.domain.coach.CoachResponse.Chat -> {
                    activityController.complete()
                    // 记录会话历史
                    contextBuilder.recordUserMessage(input)
                    contextBuilder.recordAssistantMessage(response.content)
                    UiState.Response(response.content, suggestAnalyst = response.suggestAnalyst)
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
    

    
    override suspend fun createScheduledTask(input: String, replaceItemId: String?): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        
        return try {
            // Wave 8: 注入旧任务上下文 (如果存在)
            var llmInput = input
            if (replaceItemId != null) {
                val oldTask = scheduledTaskRepository.getTask(replaceItemId)
                if (oldTask != null) {
                    llmInput = """
                        【正在修改现有任务，请返回 schedulable 分类】
                        原任务: ${oldTask.title} 在 ${oldTask.dateRange}
                        修改指令: $input
                        
                        请计算新的具体时间并返回 schedulable 格式的任务。
                    """.trimIndent()
                }
            }

            // 构建 Scheduler 上下文
            val context = contextBuilder.build(llmInput, Mode.SCHEDULER)
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
                            // === Entity Resolution (直接查 SSD，不再依赖 buildWithClues) ===
                            val personCandidates = lintResult.parsedClues.person?.let { clue ->
                                entityRepository.findByAlias(clue).map { entry ->
                                    CandidateOption(
                                        entityId = entry.entityId,
                                        displayName = entry.displayName,
                                        description = entry.entityType.name
                                    )
                                }
                            } ?: emptyList()
                            
                            if (personCandidates.size > 1) {
                                activityController.complete()
                                return UiState.AwaitingClarification(
                                    question = "请问是哪位${lintResult.parsedClues.person}？",
                                    clarificationType = ClarificationType.AMBIGUOUS_PERSON,
                                    candidates = personCandidates
                                )
                            }
                            
                            // === Entity Write-Back (EntityWriter 自动 write-through 到 RAM) ===
                            val resolvedId = personCandidates.firstOrNull()?.entityId
                            val enrichedTask = lintResult.parsedClues.person?.let { clue ->
                                val result = entityWriter.upsertFromClue(
                                    clue = clue,
                                    resolvedId = resolvedId,
                                    type = EntityType.PERSON,
                                    source = "scheduler"
                                )
                                lintResult.task.copy(
                                    keyPerson = result.displayName,
                                    keyPersonEntityId = result.entityId
                                )
                            } ?: lintResult.task
                            
                            // 插入任务到日历
                            val taskId = scheduledTaskRepository.insertTask(enrichedTask)
                            
                            // 设置提醒 — 使用 UrgencyLevel 决定的级联
                            alarmScheduler.scheduleCascade(
                                taskId, enrichedTask.title, enrichedTask.startTime, enrichedTask.alarmCascade
                            )
                            
                            activityController.complete()
                            
                            // 计算实际的日期偏移量
                            val zone = ZoneId.systemDefault()
                            val today = LocalDate.now(zone)
                            val taskDate = lintResult.task.startTime.atZone(zone).toLocalDate()
                            val dayOffset = ChronoUnit.DAYS.between(today, taskDate).toInt()

                            // Wave 8: Atomicity — Create succeeded, now delete old task if replacing
                            if (replaceItemId != null) {
                                scheduledTaskRepository.deleteItem(replaceItemId)
                                alarmScheduler.cancelReminder(replaceItemId)
                                scheduleBoard.refresh()
                            }
                            
                            UiState.SchedulerTaskCreated(
                                taskId = taskId,
                                title = lintResult.task.title,
                                dayOffset = dayOffset,
                                scheduledAtMillis = lintResult.task.startTime.toEpochMilli(),
                                durationMinutes = lintResult.task.durationMinutes,
                                isReschedule = replaceItemId != null  // Wave 11: amber glow 信号
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
                            UiState.Idle
                        }
                        
                        is LintResult.Inspiration -> {
                            inspirationRepository.insert(lintResult.content)
                            activityController.complete()
                            UiState.Toast("💡 已存入灵感箱")
                        }
                        
                        // Wave 4.1: Multi-Task Direct Insert
                        is LintResult.MultiTask -> {
                            var anyConflict = false
                            val zone = ZoneId.systemDefault()
                            val today = LocalDate.now(zone)
                            val createdTasks = mutableListOf<UiState.SchedulerTaskCreated>()
                            
                            lintResult.tasks.forEach { task ->
                                // Entity write-back per task
                                val enrichedTask = task.keyPerson?.let { clue ->
                                    val result = entityWriter.upsertFromClue(
                                        clue = clue,
                                        resolvedId = null,
                                        type = EntityType.PERSON,
                                        source = "scheduler"
                                    )
                                    task.copy(
                                        keyPerson = result.displayName,
                                        keyPersonEntityId = result.entityId
                                    )
                                } ?: task
                                
                                // 冲突检测 — 仅对有时长的任务
                                if (enrichedTask.durationMinutes > 0) {
                                    val conflictResult = scheduleBoard.checkConflict(
                                        enrichedTask.startTime.toEpochMilli(),
                                        enrichedTask.durationMinutes
                                    )
                                    if (conflictResult is ConflictResult.Conflict) {
                                        anyConflict = true
                                    }
                                }
                                val taskId = scheduledTaskRepository.insertTask(enrichedTask)
                                
                                // 设置提醒 — 批量任务也使用自身的 alarmCascade (-15m, -1m default)
                                alarmScheduler.scheduleCascade(
                                    taskId, enrichedTask.title, enrichedTask.startTime, enrichedTask.alarmCascade
                                )
                                
                                // 计算日期偏移量
                                val taskDate = enrichedTask.startTime.atZone(zone).toLocalDate()
                                val dayOffset = ChronoUnit.DAYS.between(today, taskDate).toInt()
                                
                                createdTasks.add(UiState.SchedulerTaskCreated(
                                    taskId = taskId,
                                    title = enrichedTask.title,
                                    dayOffset = dayOffset,
                                    scheduledAtMillis = enrichedTask.startTime.toEpochMilli(),
                                    durationMinutes = enrichedTask.durationMinutes
                                ))
                            }
                            
                            scheduleBoard.refresh()
                            activityController.complete()
                            
                            UiState.SchedulerMultiTaskCreated(
                                tasks = createdTasks,
                                hasConflict = anyConflict
                            )
                        }
                        
                        // Wave 7: NL Deletion
                        is LintResult.Deletion -> {
                            // Fast Path: 如果指定了 replaceItemId 且 LLM 确认为删除，直接删除指定 ID
                            if (replaceItemId != null) {
                                scheduledTaskRepository.deleteItem(replaceItemId)
                                alarmScheduler.cancelReminder(replaceItemId)
                                scheduleBoard.refresh()
                                activityController.complete()
                                UiState.Toast("🗑️ 已删除任务")
                            } else {
                                // Normal Path: Fuzzy match
                                val items = scheduleBoard.upcomingItems.value
                                val matches = items.filter { 
                                    it.title.contains(lintResult.targetTitle, ignoreCase = true) 
                                }
                                
                                when (matches.size) {
                                    0 -> {
                                        activityController.complete()
                                        UiState.Toast("未找到匹配'${lintResult.targetTitle}'的任务")
                                    }
                                    1 -> {
                                        val match = matches.first()
                                        scheduledTaskRepository.deleteItem(match.entryId)
                                        scheduleBoard.refresh()
                                        activityController.complete()
                                        UiState.Toast("🗑️ 已删除'${match.title}'")
                                    }
                                    else -> {
                                        activityController.complete()
                                        UiState.Toast("找到 ${matches.size} 个匹配，请更具体地描述要取消的任务")
                                    }
                                }
                            }
                        }
                        
                        // Wave 11: Global Reschedule
                        is LintResult.Reschedule -> {
                            val items = scheduleBoard.upcomingItems.value
                            val matches = items.filter {
                                it.title.contains(lintResult.targetTitle, ignoreCase = true)
                            }

                            when (matches.size) {
                                0 -> {
                                    activityController.complete()
                                    UiState.Toast("未找到匹配'${lintResult.targetTitle}'的任务")
                                }
                                1 -> {
                                    val match = matches.first()
                                    Log.d("PrismOrchestrator", "🔄 Global reschedule: '${match.title}' → '${lintResult.newInstruction}'")
                                    createScheduledTask(lintResult.newInstruction, replaceItemId = match.entryId)
                                }
                                else -> {
                                    activityController.complete()
                                    UiState.Toast("找到 ${matches.size} 个匹配，请更具体地描述要改的任务")
                                }
                            }
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
