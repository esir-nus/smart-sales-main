package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.domain.mapper.TaskMemoryMapper
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerCoordinator

internal class SchedulerViewModelLegacyActions(
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val inspirationRepository: InspirationRepository,
    private val coordinator: SchedulerCoordinator,
    private val getTipsLoading: () -> Set<String>,
    private val setTipsLoading: (Set<String>) -> Unit,
    private val tipsCache: MutableMap<String, List<String>>,
    private val getConflictedTaskIds: () -> Set<String>,
    private val clearConflictState: () -> Unit
) {

    suspend fun deleteItem(id: String) {
        taskRepository.deleteItem(id)
        tipsCache.remove(id)
        setTipsLoading(getTipsLoading() - id)
    }

    suspend fun toggleDone(taskId: String) {
        val activeTask = taskRepository.getTask(taskId)

        if (activeTask != null) {
            val memoryEntry = TaskMemoryMapper.toMemoryEntry(activeTask)
            memoryRepository.save(memoryEntry)
            taskRepository.deleteItem(taskId)
            android.util.Log.d("SchedulerVM", "toggleDone: id=$taskId migrated to Factual Memory.")
        } else {
            android.util.Log.d(
                "SchedulerVM",
                "toggleDone: Task $taskId not found in active tasks. Unchecking completed memories is not supported by design."
            )
        }
    }

    suspend fun deleteInspiration(id: String) {
        inspirationRepository.delete(id)
    }

    fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
        clearConflictState()
    }

    fun onReschedule(taskId: String, text: String) {
        val conflictGroup = getConflictedTaskIds()
        if (taskId in conflictGroup && conflictGroup.size > 1) {
            coordinator.resolveConflictGroup(conflictGroup, text)
            clearConflictState()
        } else {
            android.util.Log.d("SchedulerVM", "onReschedule: Pipeline routing decoupled from UI.")
        }
    }
}
