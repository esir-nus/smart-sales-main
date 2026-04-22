package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.mapper.TaskMemoryMapper
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem.CrossedOff
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.data.scheduler.SchedulerTelemetryDispatcher
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class SchedulerViewModelProjectionSupport(
    private val scope: CoroutineScope,
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val inspirationRepository: InspirationRepository,
    private val timeProvider: TimeProvider
) {

    fun buildTimelineItems(
        activeDayOffset: StateFlow<Int>
    ): StateFlow<List<SchedulerTimelineItem>> {
        return combine(
            activeDayOffset.flatMapLatest { offset -> taskRepository.getTimelineItems(offset) },
            activeDayOffset.flatMapLatest(::observeCompletedMemoriesForOffset),
            inspirationRepository.getAll()
        ) { activeTasks, factualMemories, inspirations ->
            val crossedOffItems = factualMemories.map(::mapCompletedMemoryToTimelineItem)
            val crossedOffIds = crossedOffItems.map { it.id }.toSet()
            val filteredActiveTasks = activeTasks.filter { it.id !in crossedOffIds }
            val combinedTasks = (filteredActiveTasks + crossedOffItems).sortedWith(
                compareByDescending<SchedulerTimelineItem> { it is ScheduledTask && it.isVague }
                    .thenBy { it.sortInstantOrMax() }
            )
            val finalResult = inspirations + combinedTasks

            SchedulerTelemetryDispatcher.post(
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

    private fun mapCompletedMemoryToTimelineItem(memory: MemoryEntry): CrossedOff {
        val reminderCascade = TaskMemoryMapper.reminderCascadeFromOutcomeJson(memory.outcomeJson)
        return CrossedOff(
            id = memory.entryId,
            timeDisplay = "已完成",
            title = memory.title ?: memory.content,
            urgencyLevel = TaskMemoryMapper.urgencyLevelFromOutcomeJson(memory.outcomeJson),
            startTime = Instant.ofEpochMilli(memory.scheduledAt ?: memory.createdAt),
            hasAlarm = reminderCascade.isNotEmpty(),
            reminderCascade = reminderCascade
        )
    }

    private fun SchedulerTimelineItem.sortInstantOrMax(): Instant = when (this) {
        is ScheduledTask -> startTime
        is CrossedOff -> startTime
        else -> Instant.MAX
    }
}
