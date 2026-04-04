package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.MutationResult
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UrgencyEnum
import com.smartsales.prism.domain.time.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * onboarding quick start 日程暂存与最终提交接口。
 */
interface OnboardingSchedulerQuickStartCommitter {
    fun stage(items: List<OnboardingQuickStartItem>)
    fun clear()
    suspend fun commitIfNeeded(): OnboardingSchedulerQuickStartCommitResult
}

sealed interface OnboardingSchedulerQuickStartCommitResult {
    data class Success(val taskIds: List<String>) : OnboardingSchedulerQuickStartCommitResult
    data object Noop : OnboardingSchedulerQuickStartCommitResult
    data class Failure(val message: String) : OnboardingSchedulerQuickStartCommitResult
}

/**
 * 使用现有 scheduler Path A 变更引擎提交 quick start 结果。
 */
@Singleton
class RealOnboardingSchedulerQuickStartCommitter @Inject constructor(
    private val mutationEngine: FastTrackMutationEngine,
    private val taskRepository: ScheduledTaskRepository,
    private val timeProvider: TimeProvider
) : OnboardingSchedulerQuickStartCommitter {

    private var stagedItems: List<OnboardingQuickStartItem> = emptyList()

    override fun stage(items: List<OnboardingQuickStartItem>) {
        stagedItems = items.map { it.copy() }
    }

    override fun clear() {
        stagedItems = emptyList()
    }

    override suspend fun commitIfNeeded(): OnboardingSchedulerQuickStartCommitResult {
        if (stagedItems.isEmpty()) return OnboardingSchedulerQuickStartCommitResult.Noop

        val commitItems = stagedItems.map { it.copy() }
        val committedTaskIds = mutableListOf<String>()
        for (item in commitItems) {
            val result = if (item.isExact) {
                mutationEngine.execute(
                    FastTrackResult.CreateTasks(
                        CreateTasksParams(
                            unifiedId = item.stableId,
                            tasks = listOf(
                                TaskDefinition(
                                    title = item.title,
                                    startTimeIso = resolveExactStartIso(item),
                                    durationMinutes = 0,
                                    urgency = item.urgencyLevel.toUrgencyEnum()
                                )
                            )
                        )
                    )
                )
            } else {
                mutationEngine.execute(
                    FastTrackResult.CreateVagueTask(
                        CreateVagueTaskParams(
                            unifiedId = item.stableId,
                            title = item.title,
                            anchorDateIso = item.dateIso,
                            urgency = item.urgencyLevel.toUrgencyEnum()
                        )
                    )
                )
            }

            when (result) {
                is MutationResult.Success -> {
                    committedTaskIds += result.taskIds
                }
                is MutationResult.InspirationCreated -> Unit
                is MutationResult.AmbiguousMatch -> {
                    rollbackCommittedTasks(committedTaskIds)
                    return OnboardingSchedulerQuickStartCommitResult.Failure("体验日程暂存失败，请重试。")
                }
                is MutationResult.NoMatch -> {
                    rollbackCommittedTasks(committedTaskIds)
                    return OnboardingSchedulerQuickStartCommitResult.Failure(result.reason)
                }
                is MutationResult.Error -> {
                    rollbackCommittedTasks(committedTaskIds)
                    return OnboardingSchedulerQuickStartCommitResult.Failure("体验日程同步失败，请稍后重试。")
                }
            }
        }

        stagedItems = emptyList()
        return OnboardingSchedulerQuickStartCommitResult.Success(committedTaskIds)
    }

    private fun resolveExactStartIso(item: OnboardingQuickStartItem): String {
        val startDate = LocalDate.parse(item.dateIso)
        val startTime = java.time.LocalTime.of(
            item.startHour ?: 0,
            item.startMinute ?: 0
        )
        return startDate.atTime(startTime).atZone(timeProvider.zoneId).toInstant().toString()
    }

    private suspend fun rollbackCommittedTasks(taskIds: List<String>) {
        for (taskId in taskIds.asReversed()) {
            taskRepository.deleteItem(taskId)
        }
    }
}

private fun com.smartsales.prism.domain.scheduler.UrgencyLevel.toUrgencyEnum(): UrgencyEnum {
    return when (this) {
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL -> UrgencyEnum.L1_CRITICAL
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT -> UrgencyEnum.L2_IMPORTANT
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L3_NORMAL -> UrgencyEnum.L3_NORMAL
        com.smartsales.prism.domain.scheduler.UrgencyLevel.FIRE_OFF -> UrgencyEnum.FIRE_OFF
    }
}
