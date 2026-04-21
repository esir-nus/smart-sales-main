package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.mapper.TaskMemoryMapper
import com.smartsales.core.pipeline.*

/**
 * Orchestrates backend logic for the Scheduler, decoupling the [ScheduledTaskRepository]
 * and conflict math from the pure [ISchedulerViewModel] UI Skin.
 */
@Singleton
class SchedulerCoordinator @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val scheduleBoard: ScheduleBoard,
    private val alarmScheduler: AlarmScheduler,
    private val unifiedPipeline: UnifiedPipeline,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Observe alarms to trigger background expiration sweeps
        scope.launch {
            SchedulerRefreshBus.events.collect {
                println("SchedulerCoordinator: alarmFired: sweeping expired tasks")
                autoCompleteExpiredTasks()
            }
        }
    }

    /**
     * Cross-Off Lifecycle (Phase 3 Backend): Sweeps the Actionable feed for expired tasks.
     * Migrates them irreversibly to Factual Memory (`MemoryRepository`), then deletes them from Actionable.
     * 
     * Expiration: endTime < now && !isDone
     */
    suspend fun autoCompleteExpiredTasks() {
        val now = timeProvider.now
        val today = timeProvider.today
        // Sweep all active tasks from the beginning of time up to today
        val allItems = taskRepository.queryByDateRange(LocalDate.ofEpochDay(0), today).first()
        val expiredTasks = allItems
            .filterIsInstance<ScheduledTask>()
            .filter { !it.isDone }
            .filter { task ->
                val endTime = task.endTime
                    ?: task.startTime.plusSeconds(task.durationMinutes * 60L)
                endTime.isBefore(now)
            }

        expiredTasks.forEach { task ->
            // Phase 3 Cross-Off: Migrate to Memory, Delete from Task
            val memoryEntry = TaskMemoryMapper.toMemoryEntry(
                task = task,
                completionSource = TaskMemoryMapper.COMPLETION_SOURCE_AUTO_EXPIRED
            )
            memoryRepository.save(memoryEntry)
            taskRepository.deleteItem(task.id)
            alarmScheduler.cancelReminder(task.id)
        }

        if (expiredTasks.isNotEmpty()) {
            println("SchedulerCoordinator: autoComplete: ${expiredTasks.size} tasks swept to Factual Memory")
        }
    }

    /**
     * Recreats a set of conflicting tasks based on user instruction.
     * Example: User is told "There's a conflict between Team Sync and Lunch", they say -> "Cancel Lunch".
     */
    fun resolveConflictGroup(conflictedIds: Set<String>, userText: String) {
        scope.launch {
            val allItems = scheduleBoard.upcomingItems.value
            val conflictedTasks = conflictedIds.mapNotNull { id ->
                allItems.find { it.entryId == id }
            }
            
            val taskContext = conflictedTasks.joinToString("\n") { task ->
                val time = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(task.scheduledAt))
                "- ${task.title} @ $time (${task.durationMinutes}分钟)"
            }

            println("SchedulerCoordinator: resolveConflictGroup: Purging old tasks and dispatching LLM resolution")

            // Nuke the old conflicting tasks
            conflictedIds.forEach { taskRepository.deleteItem(it) }

            // Dispatch to Unified Pipeline to regenerate the resolved tasks
            val input = PipelineInput(
                rawText = "原计划有冲突：\n$taskContext\n\n用户指示：$userText",
                isVoice = false,
                intent = QueryQuality.CRM_TASK,
                unifiedId = java.util.UUID.randomUUID().toString()
            )
            unifiedPipeline.processInput(input).collect { result ->
                // The intent orchestrator normally handles this, but since we are coordinating
                // explicit background conflict reschedules, we emit back up via the bus if needed,
                // or the pipeline will just recreate the tasks natively through the Linter.
            }
        }
    }
}
