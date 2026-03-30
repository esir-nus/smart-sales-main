package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val SchedulerDateAttentionKindKey = SemanticsPropertyKey<String>("SchedulerDateAttentionKind")
var SemanticsPropertyReceiver.schedulerDateAttentionKind by SchedulerDateAttentionKindKey

private val SchedulerMonthTitleFormatter = DateTimeFormatter.ofPattern("yyyy年 M月")
private val SchedulerWeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * Scheduler Calendar Component (Sleek Glass Version)
 * @see prism-ui-ux-contract.md §1.3
 */
@Composable
fun SchedulerCalendar(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    activeDay: Int,
    onDateSelected: (Int) -> Unit,
    unacknowledgedDates: Set<Int> = emptySet(),
    rescheduledDates: Set<Int> = emptySet()
) {
    val visuals = LocalSchedulerDrawerVisuals.current
    val today = remember { LocalDate.now() }
    val selectedDate = remember(activeDay, today) { today.plusDays(activeDay.toLong()) }
    val firstOfMonth = remember(selectedDate) { selectedDate.withDayOfMonth(1) }
    val lastOfMonth = remember(selectedDate) { selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()) }
    val gridStart = remember(firstOfMonth) {
        firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
    }
    val gridEnd = remember(lastOfMonth) {
        lastOfMonth.plusDays((7 - lastOfMonth.dayOfWeek.value).toLong() % 7L)
    }
    val monthGrid = remember(gridStart, gridEnd) {
        buildList {
            var cursor = gridStart
            while (!cursor.isAfter(gridEnd)) {
                add(cursor)
                cursor = cursor.plusDays(1)
            }
        }
    }
    val collapsedWeekStart = remember(selectedDate) {
        selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    }
    val collapsedWeek = remember(collapsedWeekStart) {
        List(7) { index -> collapsedWeekStart.plusDays(index.toLong()) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .animateContentSize()
    ) {
        CalendarHeader(
            monthTitle = selectedDate.format(SchedulerMonthTitleFormatter),
            visuals = visuals
        )

        Spacer(modifier = Modifier.height(12.dp))

        WeekdayRow()
        Spacer(modifier = Modifier.height(8.dp))

        if (isExpanded) {
            monthGrid.chunked(7).forEach { week ->
                CalendarDateRow(
                    dates = week,
                    selectedDate = selectedDate,
                    visibleMonth = selectedDate.monthValue,
                    today = today,
                    unacknowledgedDates = unacknowledgedDates,
                    rescheduledDates = rescheduledDates,
                    onDateSelected = { clickedDate ->
                        onDateSelected(ChronoUnit.DAYS.between(today, clickedDate).toInt())
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            CalendarDateRow(
                dates = collapsedWeek,
                selectedDate = selectedDate,
                visibleMonth = selectedDate.monthValue,
                today = today,
                unacknowledgedDates = unacknowledgedDates,
                rescheduledDates = rescheduledDates,
                onDateSelected = { clickedDate ->
                    onDateSelected(ChronoUnit.DAYS.between(today, clickedDate).toInt())
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ExpansionHandle(
            isExpanded = isExpanded,
            onExpandChange = onExpandChange
        )
    }
}

@Composable
private fun CalendarHeader(
    monthTitle: String,
    visuals: SchedulerDrawerVisuals
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = visuals.drawerContentHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}, enabled = false) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = null,
                tint = visuals.calendarChevronColor
            )
        }

        Text(
            text = monthTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = visuals.calendarPrimaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = {}, enabled = false) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = visuals.calendarChevronColor
            )
        }
    }
}

@Composable
private fun WeekdayRow() {
    val visuals = currentSchedulerDrawerVisuals
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = visuals.drawerContentHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SchedulerWeekdayLabels.forEach { label ->
            Text(
                text = label,
                fontSize = 12.sp,
                color = visuals.calendarWeekdayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

@Composable
private fun CalendarDateRow(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    visibleMonth: Int,
    today: LocalDate,
    unacknowledgedDates: Set<Int>,
    rescheduledDates: Set<Int>,
    onDateSelected: (LocalDate) -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = visuals.drawerContentHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dates.forEach { date ->
            CalendarDateCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                isOutOfBounds = date.monthValue != visibleMonth,
                attentionOffset = ChronoUnit.DAYS.between(today, date).toInt(),
                unacknowledgedDates = unacknowledgedDates,
                rescheduledDates = rescheduledDates,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun CalendarDateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isOutOfBounds: Boolean,
    attentionOffset: Int,
    unacknowledgedDates: Set<Int>,
    rescheduledDates: Set<Int>,
    onClick: () -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    val glow = rememberInfiniteTransition(label = "dateAttentionGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "dateAttentionGlowAlpha"
    )
    val attentionKind = when {
        attentionOffset in rescheduledDates -> "warning"
        attentionOffset in unacknowledgedDates -> "normal"
        else -> "none"
    }
    val glowColor = when (attentionKind) {
        "warning" -> visuals.attentionWarning
        "normal" -> visuals.attentionNormal
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .semantics(mergeDescendants = true) {
                    schedulerDateAttentionKind = attentionKind
                }
                .then(
                    if (attentionKind != "none") {
                        Modifier.drawBehind {
                            drawCircle(
                                color = glowColor.copy(alpha = glowAlpha * 0.35f),
                                radius = size.minDimension / 2f + 5.dp.toPx()
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    when {
                        isSelected -> Modifier.background(visuals.calendarSelectedContainer, CircleShape)
                        isToday -> Modifier.background(visuals.calendarTodayContainer, CircleShape)
                        else -> Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isSelected -> visuals.calendarSelectedText
                    isToday -> visuals.calendarTodayText
                    isOutOfBounds -> visuals.calendarOutOfBoundsText
                    else -> visuals.calendarPrimaryText
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    color = when (attentionKind) {
                        "warning" -> visuals.attentionWarning
                        "normal" -> visuals.attentionNormal
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ExpansionHandle(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragOffset += delta
                },
                onDragStopped = {
                    if (!isExpanded && dragOffset > 30f) {
                        onExpandChange(true)
                    } else if (isExpanded && dragOffset < -30f) {
                        onExpandChange(false)
                    }
                    dragOffset = 0f
                }
            )
            .clickable { onExpandChange(!isExpanded) },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(visuals.calendarPillColor, RoundedCornerShape(20.dp))
                .border(0.5.dp, visuals.calendarPillBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = visuals.calendarChevronColor,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (isExpanded) 180f else 0f
                }
            )
        }
    }
}
