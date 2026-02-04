package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.theme.*
import java.time.LocalDate

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
    rescheduledDates: Set<Int> = emptySet()  // 改期目标日期
) {
    // 获取今天日期
    val today = remember { LocalDate.now() }
    val todayDayOfMonth = today.dayOfMonth
    val todayMonth = today.monthValue
    val todayYear = today.year
    
    // State: Selected Month (1-12) — 默认当前月
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    
    // TODO: Event dots should come from real calendar data via repository
    // For now, empty until we wire up CalendarRepository query
    val eventDots = remember { emptyMap<Int, Boolean>() }
    
    // Transparent container to let glass sheet show through
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // 1. Header (Month Carousel)
        CalendarHeader(
            year = todayYear,
            selectedMonth = selectedMonth,
            onMonthSelected = { selectedMonth = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. Expandable Grid
        // Calculate selected day of month from offset
        val selectedDayOfMonth = if (selectedMonth == todayMonth) todayDayOfMonth + activeDay else -1
        
        ExpandableCalendarGrid(
            isExpanded = isExpanded, 
            activeDay = activeDay,
            todayDayOfMonth = if (selectedMonth == todayMonth) todayDayOfMonth else -1,
            selectedDayOfMonth = selectedDayOfMonth,
            selectedMonth = selectedMonth,
            onDateSelected = { dayNum ->
                // 计算 offset: dayNum - todayDayOfMonth (同月)
                val offset = if (selectedMonth == todayMonth) dayNum - todayDayOfMonth else 0
                onDateSelected(offset)
            },
            eventDots = eventDots,
            unacknowledgedDates = unacknowledgedDates,
            rescheduledDates = rescheduledDates
        )
        
        // 3. Expansion Handle
        ExpansionHandle(
            isExpanded = isExpanded,
            onExpandChange = onExpandChange
        )
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Year + Month Text
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${year}年",
                fontSize = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "${selectedMonth}月",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Month Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(12) { index ->
                val monthNum = index + 1
                val isSelected = monthNum == selectedMonth
                val haptic = LocalHapticFeedback.current
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) TextPrimary else BackgroundSurfaceMuted)
                        .clickable { 
                            onMonthSelected(monthNum)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${monthNum}月",
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCalendarGrid(
    isExpanded: Boolean, 
    activeDay: Int,
    todayDayOfMonth: Int,
    selectedDayOfMonth: Int,
    selectedMonth: Int,
    onDateSelected: (Int) -> Unit,
    eventDots: Map<Int, Boolean>,
    unacknowledgedDates: Set<Int> = emptySet(),
    rescheduledDates: Set<Int> = emptySet()
) {
    val days = (1..35).toList()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Column {
            // Week Headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { 
                    Text(
                        text = it, 
                        fontSize = 12.sp, 
                        color = TextMuted, 
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Grid Rows
            if (isExpanded) {
                (0 until 5).forEach { rowIndex ->
                    CalendarRow(
                        days = days.subList(rowIndex * 7, (rowIndex + 1) * 7),
                        todayDayOfMonth = todayDayOfMonth,
                        selectedDayOfMonth = selectedDayOfMonth,
                        onDateSelected = onDateSelected,
                        eventDots = eventDots,
                        unacknowledgedDates = unacknowledgedDates,
                        rescheduledDates = rescheduledDates
                    )
                }
            } else {
                // Collapsed: Show only the week containing today (or first week if different month)
                val weekStartIndex = if (todayDayOfMonth > 0) {
                    // 计算今天所在的周 (0-indexed): (day-1) / 7 * 7
                    ((todayDayOfMonth - 1) / 7) * 7
                } else {
                    0 // 非当前月显示第一周
                }
                val weekDays = days.subList(weekStartIndex, minOf(weekStartIndex + 7, days.size))
                CalendarRow(
                    days = weekDays,
                    todayDayOfMonth = todayDayOfMonth,
                    selectedDayOfMonth = selectedDayOfMonth,
                    onDateSelected = onDateSelected,
                    eventDots = eventDots,
                    unacknowledgedDates = unacknowledgedDates,
                    rescheduledDates = rescheduledDates
                )
            }
        }
    }
}

@Composable
private fun CalendarRow(
    days: List<Int>, 
    todayDayOfMonth: Int,
    selectedDayOfMonth: Int,
    onDateSelected: (Int) -> Unit,
    eventDots: Map<Int, Boolean>,
    unacknowledgedDates: Set<Int> = emptySet(),
    rescheduledDates: Set<Int> = emptySet()
) {
    // 呼吸发光动画 (用于未确认日期)
    val infiniteTransition = rememberInfiniteTransition(label = "dateGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { dayVal ->
            val dayNum = if (dayVal > 31) dayVal - 31 else dayVal
            val isToday = dayNum == todayDayOfMonth
            val isSelected = dayNum == selectedDayOfMonth && !isToday // Don't double highlight today if selected
            val hasEvent = eventDots[dayNum] == true
            // 计算 dayOffset 从 todayDayOfMonth
            val dayOffset = dayNum - todayDayOfMonth
            val isRescheduleGlow = dayOffset in rescheduledDates
            val isNewTaskGlow = dayOffset in unacknowledgedDates && !isRescheduleGlow
            val hasGlow = isRescheduleGlow || isNewTaskGlow
            val glowColor = if (isRescheduleGlow) AccentAmber else AccentBlue
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    // 发光效果 (优先显示，独立于其他状态)
                    .then(
                        if (hasGlow) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = glowColor.copy(alpha = glowAlpha),
                                    radius = size.minDimension / 2 + 4.dp.toPx()
                                )
                            }
                        } else Modifier
                    )
                    .then(
                        when {
                            isToday -> Modifier.background(TextPrimary, CircleShape)
                            isSelected -> Modifier.border(2.dp, TextPrimary, CircleShape)
                            else -> Modifier
                        }
                    )
                    .clip(CircleShape)
                    .clickable { onDateSelected(dayNum) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayNum.toString(),
                        color = if (isToday) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    if (!isToday && hasEvent) { 
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(AccentSecondary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpansionHandle(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragOffset += delta
                },
                onDragStopped = { 
                    if (isExpanded && dragOffset < -30) {
                        onExpandChange(false)
                    } else if (!isExpanded && dragOffset > 30) {
                        onExpandChange(true)
                    }
                    dragOffset = 0f
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(BorderSubtle, RoundedCornerShape(2.dp))
        )
    }
}
