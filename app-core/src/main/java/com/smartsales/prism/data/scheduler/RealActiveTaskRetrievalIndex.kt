package com.smartsales.prism.data.scheduler

import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 活跃任务检索索引实现。
 * 说明：从持久任务真相派生 follow-up 检索候选，不新增第二持久层。
 */
@Singleton
class RealActiveTaskRetrievalIndex @Inject constructor(
    private val taskRepository: ScheduledTaskRepository
) : ActiveTaskRetrievalIndex {

    override suspend fun buildShortlist(
        transcript: String,
        limit: Int
    ): List<ActiveTaskContext> {
        return rankedTasks(
            tasks = taskRepository.getActiveTasks(),
            query = transcript,
            person = null,
            location = null
        ).take(limit)
            .map { (task, _) -> task.toContext() }
    }

    override suspend fun resolveTarget(
        target: TargetResolutionRequest,
        suggestedTaskId: String?
    ): ActiveTaskResolveResult {
        val ranked = rankedTasks(
            tasks = taskRepository.getActiveTasks(),
            query = target.targetQuery,
            person = target.targetPerson,
            location = target.targetLocation
        )
        val top = ranked.firstOrNull() ?: return ActiveTaskResolveResult.NoMatch(target.describeForFailure())
        val topScore = top.second
        val runnerUpScore = ranked.getOrNull(1)?.second ?: 0
        if (topScore < TaskRetrievalScoring.MIN_RESOLUTION_SCORE) {
            return ActiveTaskResolveResult.NoMatch(target.describeForFailure())
        }

        if (suggestedTaskId != null) {
            val suggested = ranked.firstOrNull { (task, _) -> task.id == suggestedTaskId }
                ?: return ActiveTaskResolveResult.NoMatch(target.describeForFailure())
            val suggestedScore = suggested.second
            if (suggestedScore < TaskRetrievalScoring.MIN_RESOLUTION_SCORE) {
                return ActiveTaskResolveResult.NoMatch(target.describeForFailure())
            }
            if (top.first.id != suggestedTaskId) {
                return if (topScore - suggestedScore < TaskRetrievalScoring.MIN_MARGIN_SCORE) {
                    ActiveTaskResolveResult.Ambiguous(
                        query = target.describeForFailure(),
                        candidateIds = ranked.take(3).map { it.first.id }
                    )
                } else {
                    ActiveTaskResolveResult.NoMatch(target.describeForFailure())
                }
            }
        }

        if (runnerUpScore > 0 && topScore - runnerUpScore < TaskRetrievalScoring.MIN_MARGIN_SCORE) {
            return ActiveTaskResolveResult.Ambiguous(
                query = target.describeForFailure(),
                candidateIds = ranked.take(3).map { it.first.id }
            )
        }

        return ActiveTaskResolveResult.Resolved(top.first.id)
    }

    private fun rankedTasks(
        tasks: List<ScheduledTask>,
        query: String,
        person: String?,
        location: String?
    ): List<Pair<ScheduledTask, Int>> {
        val normalizedQuery = TaskRetrievalScoring.normalize(query)
        val normalizedPerson = TaskRetrievalScoring.normalize(person)
        val normalizedLocation = TaskRetrievalScoring.normalize(location)
        return tasks
            .map { task ->
                task to scoreTask(
                    task = task,
                    query = normalizedQuery,
                    person = normalizedPerson,
                    location = normalizedLocation
                )
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
    }

    private fun scoreTask(
        task: ScheduledTask,
        query: String,
        person: String,
        location: String
    ): Int = TaskRetrievalScoring.scoreCandidate(
        query = query,
        person = person,
        location = location,
        candidate = TaskRetrievalCandidate(
            id = task.id,
            title = task.title,
            participants = task.keyPerson?.let(::listOf).orEmpty(),
            location = task.location,
            notes = task.notes
        )
    )

    private fun ScheduledTask.toContext(): ActiveTaskContext {
        return ActiveTaskContext(
            taskId = id,
            title = title,
            timeSummary = when {
                isVague -> dateRange.ifBlank { timeDisplay }
                else -> timeDisplay
            },
            isVague = isVague,
            keyPerson = keyPerson,
            location = location,
            notesDigest = notes?.trim()?.takeIf { it.isNotBlank() }?.take(80)
        )
    }

}
