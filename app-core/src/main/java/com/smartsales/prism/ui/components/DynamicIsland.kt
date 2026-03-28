package com.smartsales.prism.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.prism.domain.scheduler.ScheduledTask
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val DynamicIslandTextColor = Color.White.copy(alpha = 0.95f)
private val DynamicIslandConflictColor = Color(0xFFF59E0B)
private val DynamicIslandImmediateColor = Color(0xFFEF4444)
private val DynamicIslandIdleColor = Color(0xFF38BDF8)

sealed interface DynamicIslandUiState {
    data object Hidden : DynamicIslandUiState
    data class Visible(val item: DynamicIslandItem) : DynamicIslandUiState
}

data class DynamicIslandItem(
    val sessionTitle: String,
    val schedulerSummary: String,
    val isConflict: Boolean = false,
    val isIdleEntry: Boolean = false,
    val tapAction: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
) {
    val displayText: String
        get() = schedulerSummary

    val stableKey: String
        get() = buildString {
            append(sessionTitle)
            append('|')
            append(schedulerSummary)
            append('|')
            append(isConflict)
            append('|')
            append(isIdleEntry)
            append('|')
            when (val action = tapAction) {
                is DynamicIslandTapAction.OpenSchedulerDrawer -> {
                    append(action.target?.date ?: "default")
                    append('|')
                    append(action.target?.taskId.orEmpty())
                    append('|')
                    append(action.target?.isConflict ?: false)
                }
            }
        }
}

data class DynamicIslandSchedulerTarget(
    val date: LocalDate? = null,
    val taskId: String? = null,
    val isConflict: Boolean = false
)

fun DynamicIslandSchedulerTarget.toDayOffset(
    referenceDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
): Int? = date?.let { ChronoUnit.DAYS.between(referenceDate, it).toInt() }

sealed interface DynamicIslandTapAction {
    data class OpenSchedulerDrawer(
        val target: DynamicIslandSchedulerTarget? = null
    ) : DynamicIslandTapAction
}

object DynamicIslandStateMapper {
    fun fromScheduler(
        sessionTitle: String,
        upcoming: List<ScheduledTask>
    ): DynamicIslandUiState {
        val pendingTasks = upcoming
            .filterNot { it.isDone }
            .sortedWith(
                compareByDescending<ScheduledTask> { it.hasConflict }
                    .thenBy { it.startTime }
            )
        val leadingTask = pendingTasks.firstOrNull()
        val schedulerSummary = when {
            leadingTask == null -> "暂无待办"
            leadingTask.hasConflict -> buildDynamicIslandSummary(prefix = "冲突", task = leadingTask)
            else -> buildDynamicIslandSummary(prefix = "最近", task = leadingTask)
        }
        return DynamicIslandUiState.Visible(
            DynamicIslandItem(
                sessionTitle = sessionTitle,
                schedulerSummary = schedulerSummary,
                isConflict = leadingTask?.hasConflict == true,
                isIdleEntry = leadingTask == null,
                tapAction = DynamicIslandTapAction.OpenSchedulerDrawer(
                    target = leadingTask?.toSchedulerTarget()
                )
            )
        )
    }
}

@Composable
fun DynamicIsland(
    state: DynamicIslandUiState,
    modifier: Modifier = Modifier,
    onTap: (DynamicIslandTapAction) -> Unit
) {
    if (state !is DynamicIslandUiState.Visible) return
    val accentColor = when {
        state.item.isIdleEntry -> DynamicIslandIdleColor
        state.item.isConflict -> DynamicIslandConflictColor
        else -> DynamicIslandImmediateColor
    }

    PrismSurface(
        modifier = modifier
            .clickable {
                onTap(state.item.tapAction)
            },
        shape = RoundedCornerShape(18.dp),
        backgroundColor = accentColor.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = state.item.displayText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = DynamicIslandTextColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildDynamicIslandSummary(
    prefix: String,
    task: ScheduledTask
): String {
    return when {
        task.timeDisplay.isBlank() -> "$prefix：${task.title}"
        else -> "$prefix：${task.title} · ${task.timeDisplay}"
    }
}

private fun ScheduledTask.toSchedulerTarget(): DynamicIslandSchedulerTarget {
    return DynamicIslandSchedulerTarget(
        date = startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
        taskId = id,
        isConflict = hasConflict
    )
}
