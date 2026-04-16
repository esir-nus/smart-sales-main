package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandSchedulerTarget
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import java.time.ZoneId

internal fun buildSimDynamicIslandItems(
    sessionTitle: String,
    sessionHasAudioContextHistory: Boolean = false,
    orderedTasks: List<ScheduledTask>,
    showIdleTeachingHint: Boolean = false
): List<DynamicIslandItem> {
    val normalizedTitle = sessionTitle.ifBlank { "SIM" }
    val titleItem = if (isSimSessionTitleEligibleForIsland(normalizedTitle)) {
        DynamicIslandItem(
            sessionTitle = normalizedTitle,
            displayText = normalizedTitle,
            visualState = DynamicIslandVisualState.SESSION_TITLE_HIGHLIGHT,
            showsAudioIndicator = sessionHasAudioContextHistory,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
    } else {
        null
    }
    val taskLimit = if (titleItem != null) 2 else 3
    val activeTasks = orderedTasks
        .filterNot { it.isDone }
        .take(taskLimit)
    if (activeTasks.isEmpty()) {
        val schedulerFallback = DynamicIslandItem(
            sessionTitle = normalizedTitle,
            schedulerSummary = if (showIdleTeachingHint) {
                "下滑这里查看日程"
            } else {
                "暂无待办"
            },
            isIdleEntry = true,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
        )
        return listOfNotNull(titleItem, schedulerFallback)
    }
    return buildList {
        titleItem?.let(::add)
        addAll(activeTasks.map { task ->
        DynamicIslandItem(
            sessionTitle = normalizedTitle,
            schedulerSummary = buildSimDynamicIslandSummary(task),
            isConflict = task.hasConflict,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer(
                target = DynamicIslandSchedulerTarget(
                    date = task.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    taskId = task.id,
                    isConflict = task.hasConflict
                )
            )
        )
        })
    }
}

private fun buildSimDynamicIslandSummary(task: ScheduledTask): String {
    return when {
        task.hasConflict && task.isVague -> "冲突：${task.title} · 待定提醒"
        task.hasConflict && task.timeDisplay.isBlank() -> "冲突：${task.title}"
        task.hasConflict -> "冲突：${task.title} · ${task.timeDisplay}"
        task.isVague -> "最近：${task.title} · 待定提醒"
        task.timeDisplay.isBlank() -> "最近：${task.title}"
        else -> "最近：${task.title} · ${task.timeDisplay}"
    }
}
