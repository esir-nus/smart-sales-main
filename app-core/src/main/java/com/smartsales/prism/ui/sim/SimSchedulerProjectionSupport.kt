package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.RescheduleExitMotion
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class SimSchedulerUiBridge(
    val getActiveDayOffset: () -> Int,
    val getUnacknowledgedDates: () -> Set<Int>,
    val setUnacknowledgedDates: (Set<Int>) -> Unit,
    val getRescheduledDates: () -> Set<Int>,
    val setRescheduledDates: (Set<Int>) -> Unit,
    val setConflictWarning: (String?) -> Unit,
    val setConflictedTaskIds: (Set<String>) -> Unit,
    val setCausingTaskId: (String?) -> Unit,
    val getExitingTasks: () -> List<RescheduleExitMotion>,
    val setExitingTasks: (List<RescheduleExitMotion>) -> Unit,
    val getPipelineStatus: () -> String?,
    val setPipelineStatus: (String?) -> Unit,
    val getPipelineStatusResetJob: () -> Job?,
    val setPipelineStatusResetJob: (Job?) -> Unit,
    val emitExactAlarmPermissionNeeded: () -> Unit
)

internal class SimSchedulerProjectionSupport(
    private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val bridge: SimSchedulerUiBridge
) {

    fun buildTopUrgentTasks(
        taskRepository: ScheduledTaskRepository,
        pipelineStatus: StateFlow<String?>
    ): StateFlow<List<ScheduledTask>> {
        return taskRepository
            .queryByDateRange(timeProvider.today, timeProvider.today.plusDays(30))
            .combine(pipelineStatus) { items, _ ->
                items
                    .filterIsInstance<ScheduledTask>()
                    .filterNot { it.isDone }
                    .sortedWith(
                        compareByDescending<ScheduledTask> { it.hasConflict }
                            .thenBy { it.urgencyLevel.ordinal }
                            .thenBy { it.isVague }
                            .thenBy { it.startTime.truncatedTo(ChronoUnit.MINUTES) }
                    )
                    .take(3)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun buildTimelineItems(
        taskRepository: ScheduledTaskRepository,
        inspirationRepository: InspirationRepository,
        activeDayOffset: StateFlow<Int>
    ): StateFlow<List<SchedulerTimelineItem>> {
        return activeDayOffset
            .flatMapLatest { dayOffset ->
                combine(
                    taskRepository.getTimelineItems(dayOffset),
                    inspirationRepository.getAll()
                ) { tasks, inspirations ->
                    inspirations + tasks
                }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    fun clearFailureState() {
        bridge.setConflictWarning(null)
        bridge.setConflictedTaskIds(emptySet())
        bridge.setCausingTaskId(null)
    }

    fun emitFailure(message: String) {
        bridge.setConflictWarning(message)
        bridge.setPipelineStatus(message)
    }

    fun emitStatus(message: String, autoClear: Boolean = true) {
        bridge.setPipelineStatus(message)
        bridge.getPipelineStatusResetJob()?.cancel()
        if (!autoClear) {
            bridge.setPipelineStatusResetJob(null)
            return
        }

        bridge.setPipelineStatusResetJob(
            scope.launch {
                delay(1600)
                if (bridge.getPipelineStatus() == message) {
                    bridge.setPipelineStatus(null)
                }
            }
        )
    }

    fun markConflict(task: ScheduledTask) {
        if (!task.hasConflict) {
            clearFailureState()
            return
        }

        bridge.setConflictWarning(task.conflictSummary ?: "当前日程存在时间冲突")
        bridge.setConflictedTaskIds(
            buildSet {
                add(task.id)
                task.conflictWithTaskId?.let(::add)
            }
        )
        bridge.setCausingTaskId(task.id)
    }

    fun applyAggregatedConflictState(createdTasks: List<ScheduledTask>) {
        createdTasks.firstOrNull { it.hasConflict }?.let(::markConflict)
        if (createdTasks.none { it.hasConflict }) {
            clearFailureState()
        }
    }

    fun buildMultiTaskStatus(
        createdCount: Int,
        conflictCount: Int,
        failureCount: Int
    ): String {
        val totalCount = createdCount + failureCount
        return when {
            failureCount > 0 -> "❌ 共 $totalCount 项，$failureCount 项失败"
            conflictCount > 0 -> "⚠️  已创建 $createdCount 项，其中 $conflictCount 项有冲突"
            else -> "✅ 已创建 $createdCount 项"
        }
    }

    fun markRescheduledDate(start: Instant) {
        val offset = dayOffsetFor(start)
        bridge.setRescheduledDates(bridge.getRescheduledDates() + offset)
        bridge.setUnacknowledgedDates(bridge.getUnacknowledgedDates() + offset)
    }

    fun markCreatedDates(tasks: List<ScheduledTask>) {
        val warningOffsets = linkedSetOf<Int>()
        val normalOffsets = linkedSetOf<Int>()

        tasks.forEach { task ->
            val offset = dayOffsetFor(task.startTime)
            if (offset == bridge.getActiveDayOffset()) return@forEach

            normalOffsets += offset
            if (task.hasConflict) {
                warningOffsets += offset
            }
        }

        if (normalOffsets.isEmpty()) return

        bridge.setUnacknowledgedDates(bridge.getUnacknowledgedDates() + normalOffsets)
        if (warningOffsets.isNotEmpty()) {
            bridge.setRescheduledDates(bridge.getRescheduledDates() + warningOffsets)
        }
    }

    fun buildRescheduleExitMotion(
        original: ScheduledTask,
        updatedTask: ScheduledTask,
        sourceOffset: Int,
        destinationOffset: Int
    ): RescheduleExitMotion? {
        if (sourceOffset != bridge.getActiveDayOffset()) return null
        if (original.startTime == updatedTask.startTime) return null

        val direction = when {
            destinationOffset > sourceOffset -> ExitDirection.RIGHT
            destinationOffset < sourceOffset -> ExitDirection.LEFT
            updatedTask.startTime > original.startTime -> ExitDirection.RIGHT
            updatedTask.startTime < original.startTime -> ExitDirection.LEFT
            else -> return null
        }

        return RescheduleExitMotion(
            renderKey = "${original.id}:exit:${UUID.randomUUID()}",
            sourceTaskId = original.id,
            sourceDayOffset = sourceOffset,
            snapshot = original,
            exitDirection = direction
        )
    }

    fun armExitMotion(
        motion: RescheduleExitMotion,
        sourceOffset: Int,
        destinationOffset: Int
    ) {
        bridge.setExitingTasks(bridge.getExitingTasks() + motion)
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = 1,
            summary = "SIM scheduler reschedule exit motion armed",
            rawDataDump = "taskId=${motion.sourceTaskId}, sourceDay=$sourceOffset, destinationDay=$destinationOffset, direction=${motion.exitDirection}"
        )
        Log.d(
            "SimSchedulerMotion",
            "taskId=${motion.sourceTaskId} sourceDay=$sourceOffset destinationDay=$destinationOffset direction=${motion.exitDirection}"
        )
        scope.launch {
            delay(420)
            bridge.setExitingTasks(
                bridge.getExitingTasks().filterNot { it.renderKey == motion.renderKey }
            )
        }
    }

    fun dayOffsetFor(start: Instant): Int {
        return java.time.LocalDate.ofInstant(start, timeProvider.zoneId)
            .toEpochDay()
            .minus(timeProvider.today.toEpochDay())
            .toInt()
    }

    fun displayedDateIso(): String {
        return timeProvider.today.plusDays(bridge.getActiveDayOffset().toLong()).toString()
    }

    fun getActiveDayOffset(): Int = bridge.getActiveDayOffset()
}
