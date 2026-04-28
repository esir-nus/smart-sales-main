package com.smartsales.prism.ui.sim

import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.bypassesConflictEvaluation
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.MutationResult
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.withNormalizedReminderMetadata
import com.smartsales.prism.domain.time.TimeProvider

internal data class SimSchedulerCreateExecutionSummary(
    val createdTasks: List<ScheduledTask>,
    val unresolvedReasons: List<String>
)

internal class SimSchedulerMutationCoordinator(
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val fastTrackMutationEngine: FastTrackMutationEngine,
    private val uniAExtractionService: RealUniAExtractionService,
    private val timeProvider: TimeProvider,
    private val projectionSupport: SimSchedulerProjectionSupport,
    private val reminderSupport: SimSchedulerReminderSupport,
    private val taskCreationBadgeSignal: TaskCreationBadgeSignal = TaskCreationBadgeSignal.NoOp
) {

    suspend fun handleMutation(result: FastTrackResult) {
        when (val mutation = fastTrackMutationEngine.execute(result)) {
            is MutationResult.Success -> {
                projectionSupport.clearFailureState()
                val createdTasks = mutation.taskIds.mapNotNull { taskRepository.getTask(it) }
                when (result) {
                    is FastTrackResult.CreateTasks,
                    is FastTrackResult.CreateVagueTask -> {
                        taskCreationBadgeSignal.onTasksCreated()
                        projectionSupport.markCreatedDates(createdTasks)
                        if (createdTasks.isNotEmpty()) {
                            reminderSupport.emitReminderReliabilityPromptIfNeeded()
                        }
                    }
                    else -> Unit
                }
                if (result is FastTrackResult.CreateTasks) {
                    createdTasks.forEach { reminderSupport.scheduleReminderIfExact(it) }
                }
                createdTasks.firstOrNull { it.hasConflict }?.let(projectionSupport::markConflict)
                if (createdTasks.none { it.hasConflict }) {
                    projectionSupport.clearFailureState()
                }
                projectionSupport.emitStatus(
                    when {
                        createdTasks.any { it.hasConflict } -> "已创建日程，存在冲突"
                        createdTasks.any { it.isVague } -> "已加入待定日程"
                        else -> "已创建日程"
                    }
                )
            }

            is MutationResult.InspirationCreated -> {
                projectionSupport.clearFailureState()
                projectionSupport.emitStatus("已加入灵感箱")
            }

            is MutationResult.AmbiguousMatch -> {
                projectionSupport.emitFailure("目标不明确，未执行改动")
            }

            is MutationResult.NoMatch -> {
                projectionSupport.emitFailure(mutation.reason)
            }

            is MutationResult.Error -> {
                projectionSupport.emitFailure(mutation.exception.message ?: "日程执行失败")
            }
        }
    }

    suspend fun executeCreateIntent(result: FastTrackResult): SimSchedulerCreateExecutionSummary {
        return when (val mutation = fastTrackMutationEngine.execute(result)) {
            is MutationResult.Success -> {
                val createdTasks = mutation.taskIds.mapNotNull { taskRepository.getTask(it) }
                when (result) {
                    is FastTrackResult.CreateTasks,
                    is FastTrackResult.CreateVagueTask -> {
                        taskCreationBadgeSignal.onTasksCreated()
                        projectionSupport.markCreatedDates(createdTasks)
                        if (createdTasks.isNotEmpty()) {
                            reminderSupport.emitReminderReliabilityPromptIfNeeded()
                        }
                    }
                    else -> Unit
                }
                if (result is FastTrackResult.CreateTasks) {
                    createdTasks.forEach { reminderSupport.scheduleReminderIfExact(it) }
                }
                SimSchedulerCreateExecutionSummary(
                    createdTasks = createdTasks,
                    unresolvedReasons = emptyList()
                )
            }

            is MutationResult.InspirationCreated -> {
                SimSchedulerCreateExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf("多任务 create 不应落入灵感分支")
                )
            }

            is MutationResult.AmbiguousMatch -> {
                SimSchedulerCreateExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf("目标不明确，未执行改动")
                )
            }

            is MutationResult.NoMatch -> {
                SimSchedulerCreateExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf(mutation.reason)
                )
            }

            is MutationResult.Error -> {
                SimSchedulerCreateExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf(mutation.exception.message ?: "日程执行失败")
                )
            }
        }
    }

    suspend fun executeResolvedReschedule(
        original: ScheduledTask,
        timeInstruction: String,
        newTitle: String? = null
    ) {
        newTitle?.trim()?.takeIf { it.isNotBlank() }?.let { title ->
            val updatedTask = original.copy(
                title = title,
                startTime = original.startTime,
                durationMinutes = original.durationMinutes
            ).withNormalizedReminderMetadata()

            taskRepository.rescheduleTask(original.id, updatedTask)
            reminderSupport.cancelReminderSafely(original.id)
            reminderSupport.scheduleReminderIfExact(updatedTask)
            projectionSupport.emitStatus("已改名")
            return
        }
        val resolvedTime = SimRescheduleTimeInterpreter.resolve(
            originalTask = original,
            transcript = timeInstruction,
            displayedDateIso = projectionSupport.displayedDateIso(),
            timeProvider = timeProvider,
            uniAExtractionService = uniAExtractionService
        )
        val resolved = when (resolvedTime) {
            is SimRescheduleTimeInterpreter.Result.Success -> resolvedTime
            SimRescheduleTimeInterpreter.Result.Unsupported -> {
                projectionSupport.emitFailure("SIM 当前仅支持明确时间改期")
                return
            }
            SimRescheduleTimeInterpreter.Result.InvalidExactTime -> {
                projectionSupport.emitFailure("改期时间格式无法解析")
                return
            }
        }

        val newStart = resolved.startTime
        val newDuration = resolved.durationMinutes ?: original.durationMinutes
        val sourceOffset = projectionSupport.dayOffsetFor(original.startTime)
        val destinationOffset = projectionSupport.dayOffsetFor(newStart)
        val conflict = if (bypassesConflictEvaluation(original.urgencyLevel)) {
            null
        } else {
            scheduleBoard.checkConflict(
                proposedStart = newStart.toEpochMilli(),
                durationMinutes = newDuration,
                excludeId = original.id
            ) as? ConflictResult.Conflict
        }

        val updatedTask = original.copy(
            startTime = newStart,
            durationMinutes = newDuration,
            hasConflict = conflict != null,
            conflictWithTaskId = conflict?.overlaps?.firstOrNull()?.entryId,
            conflictSummary = conflict?.overlaps?.firstOrNull()?.let { "与「${it.title}」时间冲突" },
            isVague = false
        ).withNormalizedReminderMetadata()

        taskRepository.rescheduleTask(original.id, updatedTask)
        reminderSupport.cancelReminderSafely(original.id)
        reminderSupport.scheduleReminderIfExact(updatedTask)
        projectionSupport.markConflict(updatedTask)
        if (destinationOffset != projectionSupport.getActiveDayOffset()) {
            projectionSupport.markRescheduledDate(newStart)
        }
        projectionSupport.buildRescheduleExitMotion(
            original = original,
            updatedTask = updatedTask,
            sourceOffset = sourceOffset,
            destinationOffset = destinationOffset
        )?.let { projectionSupport.armExitMotion(it, sourceOffset, destinationOffset) }
        projectionSupport.emitStatus(if (updatedTask.hasConflict) "已改期，存在冲突" else "已改期")
    }
}
