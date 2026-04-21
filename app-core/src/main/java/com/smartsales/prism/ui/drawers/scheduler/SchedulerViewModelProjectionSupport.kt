package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.mapper.TaskMemoryMapper
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class SchedulerViewModelProjectionSupport(
    private val scope: CoroutineScope,
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val inspirationRepository: InspirationRepository,
    private val timeProvider: TimeProvider
) {

    fun buildTimelineItems(
        activeDayOffset: StateFlow<Int>,
        refreshTrigger: SharedFlow<Unit>
    ): StateFlow<List<SchedulerTimelineItem>> {
        return combine(
            combine(activeDayOffset, refreshTrigger) { offset, _ -> offset }
                .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) },
            combine(activeDayOffset, refreshTrigger) { offset, _ -> offset }
                .flatMapLatest(::observeCompletedMemoriesForOffset),
            inspirationRepository.getAll()
        ) { activeTasks, factualMemories, inspirations ->
            val crossedOffTasks = factualMemories.map(::mapCompletedMemoryToTask)
            val crossedOffIds = crossedOffTasks.map { it.id }.toSet()
            val filteredActiveTasks = activeTasks.filter { it.id !in crossedOffIds }
            val combinedTasks = (filteredActiveTasks + crossedOffTasks).sortedWith(
                compareByDescending<SchedulerTimelineItem> { it is ScheduledTask && it.isVague }
                    .thenBy { if (it is ScheduledTask) it.startTime else Instant.MAX }
            )
            val finalResult = inspirations + combinedTasks

            PipelineValve.tag(
                checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
                payloadSize = finalResult.size,
                summary = "Scheduler UI State Emitted (Active/Crossed/Insp)",
                rawDataDump = null
            )

            finalResult
        }.stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    fun buildTopUrgentTasks(): StateFlow<List<ScheduledTask>> {
        val today = timeProvider.today
        return taskRepository
            .queryByDateRange(today, today.plusDays(30))
            .map { items ->
                items
                    .filterIsInstance<ScheduledTask>()
                    .filterNot { it.isDone }
                    .sortedWith(
                        compareBy<ScheduledTask> { it.urgencyLevel.ordinal }
                            .thenBy { it.isVague }
                            .thenBy { it.startTime }
                    )
                    .take(3)
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    private fun observeCompletedMemoriesForOffset(offset: Int): Flow<List<MemoryEntry>> {
        val zoneId = timeProvider.zoneId
        val date = timeProvider.today.plusDays(offset.toLong())
        val startOfDayMs = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDayMs = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return memoryRepository.observeByTypeAndDateRange(
            MemoryEntryType.SCHEDULE_ITEM,
            startOfDayMs,
            endOfDayMs
        )
    }

    private fun mapCompletedMemoryToTask(memory: MemoryEntry): ScheduledTask {
        val reminderCascade = TaskMemoryMapper.reminderCascadeFromOutcomeJson(memory.outcomeJson)
        return ScheduledTask(
            id = memory.entryId,
            timeDisplay = "已完成",
            title = memory.title ?: memory.content,
            urgencyLevel = TaskMemoryMapper.urgencyLevelFromOutcomeJson(memory.outcomeJson),
            startTime = Instant.ofEpochMilli(memory.scheduledAt ?: memory.createdAt),
            endTime = null,
            durationMinutes = 60,
            isDone = true,
            hasAlarm = reminderCascade.isNotEmpty(),
            alarmCascade = reminderCascade,
            keyPersonEntityId = null
        )
    }
}
