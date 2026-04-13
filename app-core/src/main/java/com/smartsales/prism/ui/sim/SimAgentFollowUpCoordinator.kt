package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.RealFollowUpRescheduleExtractionService
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.SchedulerIntelligenceRouter
import com.smartsales.core.pipeline.SchedulerRescheduleTimeInterpreter
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.bypassesConflictEvaluation
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.normalizedReminderCascade
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class SimAgentFollowUpCoordinator(
    private val sessionCoordinator: SimAgentSessionCoordinator,
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val activeTaskRetrievalIndex: ActiveTaskRetrievalIndex,
    private val alarmScheduler: AlarmScheduler,
    private val uniAExtractionService: RealUniAExtractionService,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService,
    private val followUpRescheduleExtractionService: RealFollowUpRescheduleExtractionService,
    private val timeProvider: TimeProvider,
    private val bridge: SimAgentUiBridge
) {

    private val schedulerRouter = SchedulerIntelligenceRouter(
        timeProvider = timeProvider,
        globalRescheduleExtractionService = globalRescheduleExtractionService,
        followUpRescheduleExtractionService = followUpRescheduleExtractionService
    )

    suspend fun createDebugBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        tasks.forEach { summary ->
            taskRepository.upsertTask(summary.toScheduledTask())
        }
        return createBadgeSchedulerFollowUpSession(
            threadId = threadId,
            transcript = transcript,
            tasks = tasks,
            batchId = batchId
        )
    }

    fun createBadgeSchedulerFollowUpSession(
        threadId: String,
        transcript: String,
        tasks: List<SchedulerFollowUpTaskSummary>,
        batchId: String? = null
    ): String? {
        if (transcript.isBlank() || tasks.isEmpty()) return null

        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val context = SchedulerFollowUpContext(
            sourceBadgeThreadId = threadId,
            boundTaskIds = tasks.map { it.taskId },
            batchId = batchId,
            taskSummaries = tasks,
            createdAt = now,
            updatedAt = now
        )
        val preview = SessionPreview(
            id = sessionId,
            clientName = if (tasks.size == 1) {
                tasks.first().title
            } else {
                "工牌日程跟进"
            },
            summary = if (tasks.size == 1) "跟进" else "批量跟进",
            timestamp = now,
            sessionKind = SessionKind.SCHEDULER_FOLLOW_UP,
            schedulerFollowUpContext = context
        )
        val firstMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            uiState = UiState.Response(
                buildBadgeSchedulerFollowUpSummary(
                    transcript = transcript,
                    taskSummaries = tasks
                )
            )
        )
        sessionCoordinator.createSession(
            preview = preview,
            messages = listOf(firstMessage),
            autoSelect = false
        )
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_SESSION_CREATED_SUMMARY,
            detail = "threadId=$threadId sessionId=$sessionId taskCount=${tasks.size}"
        )
        return sessionId
    }

    fun selectSchedulerFollowUpTask(taskId: String) {
        val context = bridge.getCurrentSchedulerFollowUpContext() ?: return
        if (context.boundTaskIds.contains(taskId)) {
            bridge.setSelectedSchedulerFollowUpTaskId(taskId)
        }
    }

    suspend fun handleSchedulerFollowUpInput(content: String) {
        val sessionId = sessionCoordinator.currentSessionId() ?: return
        sessionCoordinator.appendUserMessage(sessionId, content)
        bridge.setIsSending(true)
        bridge.setUiState(UiState.Thinking("SIM 正在处理当前日程跟进"))

        val normalized = content.lowercase()
        when {
            normalized.contains("删除") ||
                normalized.contains("取消") ||
                normalized.contains("删掉") ||
                normalized.contains("delete") ||
                normalized.contains("remove") -> {
                performSchedulerFollowUpQuickActionInternal(SimSchedulerFollowUpQuickAction.DELETE)
            }
            normalized.contains("完成") ||
                normalized.contains("done") ||
                normalized.contains("mark done") -> {
                performSchedulerFollowUpQuickActionInternal(SimSchedulerFollowUpQuickAction.MARK_DONE)
            }
            normalized.contains("说明") ||
                normalized.contains("explain") ||
                normalized.contains("是什么") -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpExplanation())
            }
            normalized.contains("状态") ||
                normalized.contains("什么时候") ||
                normalized.contains("提醒") ||
                normalized.contains("status") -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpStatus())
            }
            else -> {
                handleSchedulerFollowUpReschedule(content)
            }
        }

        bridge.setIsSending(false)
        if (bridge.getUiState() !is UiState.Error) {
            bridge.setUiState(UiState.Idle)
        }
    }

    suspend fun performSchedulerFollowUpQuickAction(action: SimSchedulerFollowUpQuickAction) {
        performSchedulerFollowUpQuickActionInternal(action)
    }

    private suspend fun performSchedulerFollowUpQuickActionInternal(
        action: SimSchedulerFollowUpQuickAction
    ) {
        when (action) {
            SimSchedulerFollowUpQuickAction.EXPLAIN -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpExplanation())
            }
            SimSchedulerFollowUpQuickAction.STATUS -> {
                appendSchedulerFollowUpResponse(buildSchedulerFollowUpStatus())
            }
            SimSchedulerFollowUpQuickAction.PREFILL_RESCHEDULE -> {
                val target = resolveSelectedSchedulerFollowUpTask()
                if (target == null) {
                    blockSchedulerFollowUpAction("请先选择要改期的日程")
                } else {
                    bridge.setInputText("把“${target.title}”改到")
                }
            }
            SimSchedulerFollowUpQuickAction.MARK_DONE -> {
                runSchedulerFollowUpTaskMutation("mark_done") { task ->
                    val updated = task.copy(isDone = !task.isDone)
                    taskRepository.updateTask(updated)
                    if (updated.isDone) {
                        cancelReminderSafely(task.id)
                    } else {
                        scheduleReminderIfExact(updated)
                    }
                    val actionLabel = if (updated.isDone) "已标记完成" else "已恢复为待办"
                    appendSchedulerFollowUpResponse(
                        "$actionLabel：${updated.title}\n\n${formatSchedulerTaskSummary(updated)}"
                    )
                }
            }
            SimSchedulerFollowUpQuickAction.DELETE -> {
                runSchedulerFollowUpTaskMutation("delete") { task ->
                    taskRepository.deleteItem(task.id)
                    cancelReminderSafely(task.id)
                    updateSchedulerFollowUpContext { current ->
                        current.removeTask(task.id)
                    }
                    appendSchedulerFollowUpResponse("已删除：${task.title}")
                }
            }
        }
    }

    private suspend fun handleSchedulerFollowUpReschedule(content: String) {
        val context = bridge.getCurrentSchedulerFollowUpContext()
            ?: run {
                blockSchedulerFollowUpAction("当前没有可用于改期的跟进上下文。")
                return
            }
        val shortlist = activeTaskRetrievalIndex.buildShortlist(
            transcript = content
        )
        emitSchedulerFollowUpTelemetry(
            summary = SIM_SCHEDULER_GLOBAL_SHORTLIST_BUILT_SUMMARY,
            detail = "shortlistSize=${shortlist.size}"
        )
        val selectedTask = resolveSelectedSchedulerFollowUpTask()
        when (
            val decision = schedulerRouter.routeFollowUp(
                SchedulerIntelligenceRouter.FollowUpContext(
                    transcript = content,
                    selectedTask = selectedTask,
                    activeTaskShortlist = shortlist
                )
            )
        ) {
            is SchedulerIntelligenceRouter.Decision.GlobalReschedule -> {
                emitSchedulerFollowUpTelemetry(
                    summary = SIM_SCHEDULER_GLOBAL_SUGGESTION_RECEIVED_SUMMARY,
                    detail = "suggestedTaskId=${decision.extracted.suggestedTaskId ?: "null"}"
                )
                val task = when (
                    val resolution = activeTaskRetrievalIndex.resolveTarget(
                        target = decision.extracted.target,
                        suggestedTaskId = decision.extracted.suggestedTaskId
                    )
                ) {
                    is ActiveTaskResolveResult.Resolved -> {
                        taskRepository.getTask(resolution.taskId)
                            ?: run {
                                blockSchedulerFollowUpAction("找不到要改期的日程。")
                                return
                            }
                    }
                    is ActiveTaskResolveResult.Ambiguous -> {
                        blockSchedulerFollowUpAction("目标不明确，未执行改动。")
                        return
                    }
                    is ActiveTaskResolveResult.NoMatch -> {
                        blockSchedulerFollowUpAction("未找到匹配的日程，请更具体一些。")
                        return
                    }
                }

                val v1ResolvedTime = SimRescheduleTimeInterpreter.resolve(
                    originalTask = task,
                    transcript = decision.extracted.timeInstruction,
                    displayedDateIso = task.startTime.atZone(timeProvider.zoneId).toLocalDate().toString(),
                    timeProvider = timeProvider,
                    uniAExtractionService = uniAExtractionService
                )
                val v2ShadowResult = SimFollowUpRescheduleShadowInterpreter.resolve(
                    originalTask = task,
                    transcript = decision.extracted.timeInstruction,
                    timeProvider = timeProvider,
                    extractionService = followUpRescheduleExtractionService
                )
                emitFollowUpRescheduleShadowTelemetry(
                    task = task,
                    transcript = decision.extracted.timeInstruction,
                    v1Result = v1ResolvedTime,
                    v2Result = v2ShadowResult
                )

                val resolved = when (v1ResolvedTime) {
                    is SimRescheduleTimeInterpreter.Result.Success -> v1ResolvedTime
                    SimRescheduleTimeInterpreter.Result.Unsupported -> {
                        blockSchedulerFollowUpAction("当前跟进只支持明确时间改期，请直接输入新的具体时间。")
                        return
                    }
                    SimRescheduleTimeInterpreter.Result.InvalidExactTime -> {
                        blockSchedulerFollowUpAction("改期时间格式无法解析，请换一种明确说法。")
                        return
                    }
                }

                applyFollowUpReschedule(
                    task = task,
                    newStart = resolved.startTime,
                    newDuration = resolved.durationMinutes ?: task.durationMinutes
                )
            }

            is SchedulerIntelligenceRouter.Decision.FollowUpReschedule -> {
                val targetTask = decision.selectedTask
                val v1ResolvedTime = SimRescheduleTimeInterpreter.resolve(
                    originalTask = targetTask,
                    transcript = content,
                    displayedDateIso = targetTask.startTime.atZone(timeProvider.zoneId).toLocalDate().toString(),
                    timeProvider = timeProvider,
                    uniAExtractionService = uniAExtractionService
                )
                val v2ShadowResult = SimFollowUpRescheduleShadowInterpreter.resolve(
                    originalTask = targetTask,
                    transcript = content,
                    timeProvider = timeProvider,
                    extractionService = followUpRescheduleExtractionService
                )
                emitFollowUpRescheduleShadowTelemetry(
                    task = targetTask,
                    transcript = content,
                    v1Result = v1ResolvedTime,
                    v2Result = v2ShadowResult
                )
                applyFollowUpReschedule(
                    task = targetTask,
                    newStart = SchedulerRescheduleTimeInterpreter.resolveFollowUpOperand(
                        originalTask = targetTask,
                        operand = decision.extracted.operand,
                        timeProvider = timeProvider
                    ),
                    newDuration = targetTask.durationMinutes
                )
            }

            is SchedulerIntelligenceRouter.Decision.Reject -> {
                blockSchedulerFollowUpAction(decision.message)
                return
            }

            is SchedulerIntelligenceRouter.Decision.NotMatched,
            is SchedulerIntelligenceRouter.Decision.Create -> {
                blockSchedulerFollowUpAction("当前跟进只支持明确时间改期，请直接输入新的具体时间。")
                return
            }
        }
    }

    private suspend fun applyFollowUpReschedule(
        task: ScheduledTask,
        newStart: Instant,
        newDuration: Int
    ) {
        val conflict = if (bypassesConflictEvaluation(task.urgencyLevel)) {
            null
        } else {
            scheduleBoard.checkConflict(
                proposedStart = newStart.toEpochMilli(),
                durationMinutes = newDuration,
                excludeId = task.id
            ) as? ConflictResult.Conflict
        }

        val updatedTask = task.copy(
            startTime = newStart,
            durationMinutes = newDuration,
            hasConflict = conflict != null,
            conflictWithTaskId = conflict?.overlaps?.firstOrNull()?.entryId,
            conflictSummary = conflict?.overlaps?.firstOrNull()?.let { "与「${it.title}」时间冲突" },
            isVague = false
        )

        taskRepository.rescheduleTask(task.id, updatedTask)
        cancelReminderSafely(task.id)
        scheduleReminderIfExact(updatedTask)
        updateSchedulerFollowUpContext { current ->
            current.updateTask(
                taskId = task.id,
                dayOffset = dayOffsetFor(updatedTask.startTime),
                scheduledAtMillis = updatedTask.startTime.toEpochMilli(),
                durationMinutes = updatedTask.durationMinutes
            )
        }
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_COMPLETED_SUMMARY,
            detail = "action=reschedule taskId=${task.id}"
        )
        appendSchedulerFollowUpResponse(
            buildString {
                append("已改期：")
                append(updatedTask.title)
                append("\n\n")
                append(formatSchedulerTaskSummary(updatedTask))
                if (updatedTask.hasConflict) {
                    append("\n\n注意：")
                    append(updatedTask.conflictSummary ?: "当前存在时间冲突")
                }
            }
        )
    }

    private suspend fun runSchedulerFollowUpTaskMutation(
        action: String,
        block: suspend (ScheduledTask) -> Unit
    ) {
        val task = resolveSelectedSchedulerFollowUpTask()
        if (task == null) {
            blockSchedulerFollowUpAction("请先选择要跟进的日程。")
            return
        }
        block(task)
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_COMPLETED_SUMMARY,
            detail = "action=$action taskId=${task.id}"
        )
    }

    private suspend fun resolveSelectedSchedulerFollowUpTask(): ScheduledTask? {
        val context = bridge.getCurrentSchedulerFollowUpContext() ?: return null
        val selectedTaskId = bridge.getSelectedSchedulerFollowUpTaskId()
            ?: defaultSelectedFollowUpTaskId(context)
            ?: return null
        return taskRepository.getTask(selectedTaskId)
            ?: context.taskSummaries.firstOrNull { it.taskId == selectedTaskId }?.let { stale ->
                appendSchedulerFollowUpResponse("未找到「${stale.title}」的当前日程记录，请重新选择其他任务。")
                bridge.setSelectedSchedulerFollowUpTaskId(null)
                null
            }
    }

    private fun buildSchedulerFollowUpExplanation(): String {
        val context = bridge.getCurrentSchedulerFollowUpContext() ?: return "当前没有工牌日程跟进上下文。"
        if (context.taskSummaries.isEmpty()) {
            return "当前跟进会话已没有可继续操作的绑定日程。"
        }
        return buildString {
            append("这是工牌创建后的日程跟进会话，会优先参考当前这批最新任务，但改期解析不会被点选状态偷偷带偏。\n\n")
            context.taskSummaries.forEachIndexed { index, task ->
                append(index + 1)
                append(". ")
                append(formatSchedulerTaskSummary(task))
                if (index != context.taskSummaries.lastIndex) append('\n')
            }
        }
    }

    private suspend fun buildSchedulerFollowUpStatus(): String {
        val context = bridge.getCurrentSchedulerFollowUpContext() ?: return "当前没有工牌日程跟进上下文。"
        val selectedTaskId = bridge.getSelectedSchedulerFollowUpTaskId()
        val selectedSummaries = if (selectedTaskId != null) {
            context.taskSummaries.filter { it.taskId == selectedTaskId }
        } else {
            context.taskSummaries
        }
        if (selectedSummaries.isEmpty()) {
            return "请先选择要查看状态的日程。"
        }
        return buildString {
            selectedSummaries.forEachIndexed { index, summary ->
                val liveTask = taskRepository.getTask(summary.taskId)
                append(
                    if (liveTask != null) {
                        formatSchedulerTaskSummary(liveTask)
                    } else {
                        "「${summary.title}」当前已不在日程库中。"
                    }
                )
                if (index != selectedSummaries.lastIndex) append("\n")
            }
        }
    }

    private fun buildBadgeSchedulerFollowUpSummary(
        transcript: String,
        taskSummaries: List<SchedulerFollowUpTaskSummary>
    ): String {
        return buildString {
            append("工牌已完成日程创建，并生成了一个任务级跟进会话。\n\n")
            append("原始指令：")
            append(transcript)
            append("\n\n")
            taskSummaries.forEachIndexed { index, task ->
                append(index + 1)
                append(". ")
                append(formatSchedulerTaskSummary(task))
                if (index != taskSummaries.lastIndex) append('\n')
            }
            append("\n\n可继续执行：说明、状态、改期、完成、删除。改期时请直接说出目标日程和时间。")
        }
    }

    private fun formatSchedulerTaskSummary(task: ScheduledTask): String {
        val status = if (task.isDone) "已完成" else if (task.hasConflict) "有冲突" else "待办"
        return "${task.title} · ${formatDayOffset(dayOffsetFor(task.startTime))} · ${task.timeDisplay} · ${status}"
    }

    private fun formatSchedulerTaskSummary(task: SchedulerFollowUpTaskSummary): String {
        return "${task.title} · ${formatDayOffset(task.dayOffset)} · ${formatTimeMillis(task.scheduledAtMillis)}"
    }

    private fun formatDayOffset(dayOffset: Int): String {
        return when (dayOffset) {
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            else -> if (dayOffset > 0) "${dayOffset}天后" else "${-dayOffset}天前"
        }
    }

    private fun formatTimeMillis(millis: Long): String {
        return runCatching {
            val local = Instant.ofEpochMilli(millis).atZone(timeProvider.zoneId).toLocalTime()
            "%02d:%02d".format(local.hour, local.minute)
        }.getOrDefault("--:--")
    }

    private fun appendSchedulerFollowUpResponse(content: String) {
        val sessionId = sessionCoordinator.currentSessionId() ?: return
        sessionCoordinator.appendAiMessage(sessionId, UiState.Response(content))
    }

    private fun blockSchedulerFollowUpAction(message: String) {
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_ACTION_BLOCKED_SUMMARY,
            detail = message
        )
        appendSchedulerFollowUpResponse(message)
        bridge.setUiState(UiState.Error(message))
    }

    private fun defaultSelectedFollowUpTaskId(context: SchedulerFollowUpContext?): String? {
        if (context == null) return null
        return context.boundTaskIds.singleOrNull()
    }

    private fun SchedulerFollowUpTaskSummary.toScheduledTask(): ScheduledTask {
        return ScheduledTask(
            id = taskId,
            timeDisplay = formatTimeMillis(scheduledAtMillis),
            title = title,
            urgencyLevel = UrgencyLevel.L3_NORMAL,
            startTime = Instant.ofEpochMilli(scheduledAtMillis),
            durationMinutes = durationMinutes
        )
    }

    private fun updateSchedulerFollowUpContext(
        transform: (SchedulerFollowUpContext) -> SchedulerFollowUpContext
    ) {
        val sessionId = sessionCoordinator.currentSessionId() ?: return
        sessionCoordinator.updateSession(sessionId) { record ->
            val currentContext = record.preview.schedulerFollowUpContext ?: return@updateSession record
            val updatedContext = transform(currentContext).copy(updatedAt = System.currentTimeMillis())
            val updatedPreview = record.preview.copy(schedulerFollowUpContext = updatedContext)
            bridge.setCurrentSchedulerFollowUpContext(updatedContext)
            bridge.setSelectedSchedulerFollowUpTaskId(
                when {
                    updatedContext.boundTaskIds.size == 1 -> updatedContext.boundTaskIds.single()
                    bridge.getSelectedSchedulerFollowUpTaskId() in updatedContext.boundTaskIds ->
                        bridge.getSelectedSchedulerFollowUpTaskId()
                    else -> null
                }
            )
            record.copy(preview = updatedPreview)
        }
    }

    private fun SchedulerFollowUpContext.removeTask(taskId: String): SchedulerFollowUpContext {
        return copy(
            boundTaskIds = boundTaskIds.filterNot { it == taskId },
            taskSummaries = taskSummaries.filterNot { it.taskId == taskId }
        )
    }

    private fun SchedulerFollowUpContext.updateTask(
        taskId: String,
        dayOffset: Int,
        scheduledAtMillis: Long,
        durationMinutes: Int
    ): SchedulerFollowUpContext {
        return copy(
            taskSummaries = taskSummaries.map { summary ->
                if (summary.taskId == taskId) {
                    summary.copy(
                        dayOffset = dayOffset,
                        scheduledAtMillis = scheduledAtMillis,
                        durationMinutes = durationMinutes
                    )
                } else {
                    summary
                }
            }
        )
    }

    private fun dayOffsetFor(start: Instant): Int {
        return LocalDate.ofInstant(start, timeProvider.zoneId)
            .toEpochDay()
            .minus(timeProvider.today.toEpochDay())
            .toInt()
    }

    private suspend fun scheduleReminderIfExact(task: ScheduledTask) {
        if (task.isVague || task.isDone) return
        val cascade = task.normalizedReminderCascade()
        if (cascade.isEmpty()) return
        runCatching {
            alarmScheduler.scheduleCascade(
                taskId = task.id,
                taskTitle = task.title,
                eventTime = task.startTime,
                cascade = cascade
            )
        }
    }

    private suspend fun cancelReminderSafely(taskId: String) {
        runCatching {
            alarmScheduler.cancelReminder(taskId)
        }
    }

    private fun emitFollowUpRescheduleShadowTelemetry(
        task: ScheduledTask,
        transcript: String,
        v1Result: SimRescheduleTimeInterpreter.Result,
        v2Result: SimFollowUpRescheduleShadowInterpreter.Result
    ) {
        val startedSnapshot = SimFollowUpRescheduleShadowMetrics.onStarted()
        emitSchedulerFollowUpTelemetry(
            summary = SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_STARTED_SUMMARY,
            detail = buildFollowUpRescheduleShadowDetail(
                task = task,
                transcript = transcript,
                v1Result = v1Result,
                v2Result = v2Result,
                metrics = startedSnapshot
            )
        )

        val classified = when (v2Result) {
            is SimFollowUpRescheduleShadowInterpreter.Result.Invalid -> {
                SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_INVALID_SUMMARY to
                    SimFollowUpRescheduleShadowMetrics.onInvalid()
            }

            is SimFollowUpRescheduleShadowInterpreter.Result.Failure -> {
                SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_FAILURE_SUMMARY to
                    SimFollowUpRescheduleShadowMetrics.onFailure()
            }

            is SimFollowUpRescheduleShadowInterpreter.Result.Success -> {
                val v1Success = v1Result as? SimRescheduleTimeInterpreter.Result.Success
                if (v1Success == null) {
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_SUPPORT_SUMMARY to
                        SimFollowUpRescheduleShadowMetrics.onMismatchSupport()
                } else if (v1Success.startTime == v2Result.startTime) {
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_PARITY_SUMMARY to
                        SimFollowUpRescheduleShadowMetrics.onParity()
                } else {
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_TIME_SUMMARY to
                        SimFollowUpRescheduleShadowMetrics.onMismatchTime()
                }
            }

            is SimFollowUpRescheduleShadowInterpreter.Result.Unsupported -> {
                val v1Success = v1Result as? SimRescheduleTimeInterpreter.Result.Success
                if (v1Success != null) {
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_SUPPORT_SUMMARY to
                        SimFollowUpRescheduleShadowMetrics.onMismatchSupport()
                } else {
                    null
                }
            }
        }

        val (summary, snapshot) = classified ?: return
        emitSchedulerFollowUpTelemetry(
            summary = summary,
            detail = buildFollowUpRescheduleShadowDetail(
                task = task,
                transcript = transcript,
                v1Result = v1Result,
                v2Result = v2Result,
                metrics = snapshot
            )
        )
    }

    private fun buildFollowUpRescheduleShadowDetail(
        task: ScheduledTask,
        transcript: String,
        v1Result: SimRescheduleTimeInterpreter.Result,
        v2Result: SimFollowUpRescheduleShadowInterpreter.Result,
        metrics: SimFollowUpRescheduleShadowMetrics.Snapshot
    ): String {
        return buildString {
            append("taskId=")
            append(task.id)
            append(" transcriptClass=")
            append(v2Result.transcriptClassLabel())
            append(" transcript=")
            append(transcript)
            append(" v1=")
            append(v1Result.shadowLabel())
            append(" v1Start=")
            append(v1Result.shadowStartTimeIso())
            append(" v2=")
            append(v2Result.shadowLabel())
            append(" v2Start=")
            append(v2Result.shadowStartTimeIso())
            append(" counters=")
            append(
                "started=${metrics.started},parity=${metrics.parity},mismatchTime=${metrics.mismatchTime}," +
                    "mismatchSupport=${metrics.mismatchSupport},invalid=${metrics.invalid},failure=${metrics.failure}"
            )
        }
    }

    private fun SimRescheduleTimeInterpreter.Result.shadowLabel(): String = when (this) {
        is SimRescheduleTimeInterpreter.Result.Success -> "success"
        SimRescheduleTimeInterpreter.Result.Unsupported -> "unsupported"
        SimRescheduleTimeInterpreter.Result.InvalidExactTime -> "invalid"
    }

    private fun SimRescheduleTimeInterpreter.Result.shadowStartTimeIso(): String = when (this) {
        is SimRescheduleTimeInterpreter.Result.Success -> startTime.toString()
        SimRescheduleTimeInterpreter.Result.Unsupported -> "null"
        SimRescheduleTimeInterpreter.Result.InvalidExactTime -> "null"
    }

    private fun SimFollowUpRescheduleShadowInterpreter.Result.shadowLabel(): String = when (this) {
        is SimFollowUpRescheduleShadowInterpreter.Result.Success -> "success"
        is SimFollowUpRescheduleShadowInterpreter.Result.Unsupported -> "unsupported"
        is SimFollowUpRescheduleShadowInterpreter.Result.Invalid -> "invalid"
        is SimFollowUpRescheduleShadowInterpreter.Result.Failure -> "failure"
    }

    private fun SimFollowUpRescheduleShadowInterpreter.Result.shadowStartTimeIso(): String = when (this) {
        is SimFollowUpRescheduleShadowInterpreter.Result.Success -> startTime.toString()
        is SimFollowUpRescheduleShadowInterpreter.Result.Unsupported -> "null"
        is SimFollowUpRescheduleShadowInterpreter.Result.Invalid -> "null"
        is SimFollowUpRescheduleShadowInterpreter.Result.Failure -> "null"
    }

    private fun SimFollowUpRescheduleShadowInterpreter.Result.transcriptClassLabel(): String = when (this) {
        is SimFollowUpRescheduleShadowInterpreter.Result.Success -> transcriptClass.name
        is SimFollowUpRescheduleShadowInterpreter.Result.Unsupported -> transcriptClass.name
        is SimFollowUpRescheduleShadowInterpreter.Result.Invalid -> transcriptClass.name
        is SimFollowUpRescheduleShadowInterpreter.Result.Failure -> transcriptClass.name
    }

    private fun emitSchedulerFollowUpTelemetry(summary: String, detail: String) {
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = detail.length,
            summary = summary,
            rawDataDump = detail
        )
        Log.d(SIM_BADGE_FOLLOW_UP_CHAT_LOG_TAG, "$summary: $detail")
    }
}
