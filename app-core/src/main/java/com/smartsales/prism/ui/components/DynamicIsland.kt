package com.smartsales.prism.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.smartsales.prism.domain.scheduler.ScheduledTask
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val DynamicIslandTextColor = Color.White.copy(alpha = 0.95f)
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

    val showsBattery: Boolean
        get() = visualState == DynamicIslandVisualState.CONNECTIVITY_CONNECTED && batteryPercentage != null

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
    val accentColor = state.item.resolveAccentColor()
    val icon = when (state.item.lane) {
        DynamicIslandLane.SCHEDULER -> Icons.Filled.Schedule
        DynamicIslandLane.CONNECTIVITY -> Icons.Filled.Bluetooth
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
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = state.item.displayText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = DynamicIslandTextColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            state.item.batteryPercentage?.takeIf { state.item.showsBattery }?.let { percent ->
                DynamicIslandBatteryIndicator(
                    percentage = percent,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun DynamicIslandBatteryIndicator(
    percentage: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val boundedPercentage = percentage.coerceIn(0, 100)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 22.dp)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp)
                )
                .drawBehind {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.20f),
                        size = Size(width = size.width * (boundedPercentage / 100f), height = size.height),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = boundedPercentage.toString(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .size(width = 2.dp, height = 6.dp)
                .background(
                    color = accentColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                )
        )
    }
}

private fun DynamicIslandItem.resolveAccentColor(): Color {
    return when (visualState) {
        DynamicIslandVisualState.SCHEDULER_IDLE -> DynamicIslandIdleColor
        DynamicIslandVisualState.SCHEDULER_CONFLICT -> DynamicIslandConflictColor
        DynamicIslandVisualState.SCHEDULER_UPCOMING -> DynamicIslandImmediateColor
        DynamicIslandVisualState.CONNECTIVITY_CONNECTED -> DynamicIslandConnectedColor
        DynamicIslandVisualState.CONNECTIVITY_DISCONNECTED -> DynamicIslandDisconnectedColor
        DynamicIslandVisualState.CONNECTIVITY_RECONNECTING,
        DynamicIslandVisualState.CONNECTIVITY_NEEDS_SETUP -> DynamicIslandReconnectColor
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
