package com.smartsales.prism.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.scheduler.ScheduledTask
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val DynamicIslandConflictColor = Color(0xFFF59E0B)
private val DynamicIslandImmediateColor = Color(0xFFEF4444)
private val DynamicIslandIdleColor = Color(0xFF38BDF8)
private val DynamicIslandConnectedColor = Color(0xFF34C759)
private val DynamicIslandDisconnectedColor = Color(0xFF86868B)
private val DynamicIslandReconnectColor = Color(0xFFFF9F0A)

sealed interface DynamicIslandUiState {
    data object Hidden : DynamicIslandUiState
    data class Visible(val item: DynamicIslandItem) : DynamicIslandUiState
}

enum class DynamicIslandLane {
    SCHEDULER,
    CONNECTIVITY
}

enum class DynamicIslandVisualState {
    SCHEDULER_UPCOMING,
    SCHEDULER_CONFLICT,
    SCHEDULER_IDLE,
    CONNECTIVITY_CONNECTED,
    CONNECTIVITY_DISCONNECTED,
    CONNECTIVITY_RECONNECTING,
    CONNECTIVITY_NEEDS_SETUP
}

data class DynamicIslandItem(
    val sessionTitle: String = "",
    val displayText: String,
    val lane: DynamicIslandLane = DynamicIslandLane.SCHEDULER,
    val visualState: DynamicIslandVisualState = DynamicIslandVisualState.SCHEDULER_UPCOMING,
    val batteryPercentage: Int? = null,
    val tapAction: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
) {
    constructor(
        sessionTitle: String,
        schedulerSummary: String,
        isConflict: Boolean = false,
        isIdleEntry: Boolean = false,
        tapAction: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
    ) : this(
        sessionTitle = sessionTitle,
        displayText = schedulerSummary,
        lane = DynamicIslandLane.SCHEDULER,
        visualState = when {
            isIdleEntry -> DynamicIslandVisualState.SCHEDULER_IDLE
            isConflict -> DynamicIslandVisualState.SCHEDULER_CONFLICT
            else -> DynamicIslandVisualState.SCHEDULER_UPCOMING
        },
        tapAction = tapAction
    )

    val schedulerSummary: String
        get() = displayText

    val isConflict: Boolean
        get() = visualState == DynamicIslandVisualState.SCHEDULER_CONFLICT

    val isIdleEntry: Boolean
        get() = visualState == DynamicIslandVisualState.SCHEDULER_IDLE

    val usesPulse: Boolean
        get() = when (visualState) {
            DynamicIslandVisualState.SCHEDULER_CONFLICT,
            DynamicIslandVisualState.CONNECTIVITY_RECONNECTING,
            DynamicIslandVisualState.CONNECTIVITY_NEEDS_SETUP -> true
            else -> false
        }

    val stableKey: String
        get() = buildString {
            append(sessionTitle)
            append('|')
            append(displayText)
            append('|')
            append(lane)
            append('|')
            append(visualState)
            append('|')
            append(batteryPercentage ?: -1)
            append('|')
            when (val action = tapAction) {
                is DynamicIslandTapAction.OpenSchedulerDrawer -> {
                    append("scheduler")
                    append('|')
                    append(action.target?.date ?: "default")
                    append('|')
                    append(action.target?.taskId.orEmpty())
                    append('|')
                    append(action.target?.isConflict ?: false)
                }
                DynamicIslandTapAction.OpenConnectivityEntry -> {
                    append("connectivity")
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

    data object OpenConnectivityEntry : DynamicIslandTapAction
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
    val chroma = state.item.resolveChroma()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 28.dp)
                .clickable { onTap(state.item.tapAction) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = chroma.dot)
            }
            Box(modifier = Modifier.size(width = 8.dp, height = 1.dp))
            Text(
                text = state.item.displayText,
                style = TextStyle(
                    brush = Brush.linearGradient(chroma.textGradient),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class DynamicIslandChroma(
    val dot: Color,
    val textGradient: List<Color>
)

private fun DynamicIslandItem.resolveChroma(): DynamicIslandChroma {
    return when (visualState) {
        DynamicIslandVisualState.SCHEDULER_IDLE -> DynamicIslandChroma(
            dot = Color.White.copy(alpha = 0.42f),
            textGradient = listOf(Color.White, Color(0xFFA0A0A5))
        )
        DynamicIslandVisualState.SCHEDULER_CONFLICT -> DynamicIslandChroma(
            dot = DynamicIslandConflictColor,
            textGradient = listOf(Color(0xFFC93400), DynamicIslandConflictColor)
        )
        DynamicIslandVisualState.SCHEDULER_UPCOMING -> DynamicIslandChroma(
            dot = Color(0xFFFF453A),
            textGradient = listOf(Color(0xFFFF8A84), Color(0xFFFF453A))
        )
        DynamicIslandVisualState.CONNECTIVITY_CONNECTED -> DynamicIslandChroma(
            dot = DynamicIslandConnectedColor,
            textGradient = listOf(Color(0xFFA4E38A), DynamicIslandConnectedColor)
        )
        DynamicIslandVisualState.CONNECTIVITY_DISCONNECTED -> DynamicIslandChroma(
            dot = Color.White.copy(alpha = 0.30f),
            textGradient = listOf(Color(0xFFA0A0A5), DynamicIslandDisconnectedColor)
        )
        DynamicIslandVisualState.CONNECTIVITY_RECONNECTING,
        DynamicIslandVisualState.CONNECTIVITY_NEEDS_SETUP -> DynamicIslandChroma(
            dot = DynamicIslandReconnectColor,
            textGradient = listOf(Color(0xFFFFD380), DynamicIslandReconnectColor)
        )
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
