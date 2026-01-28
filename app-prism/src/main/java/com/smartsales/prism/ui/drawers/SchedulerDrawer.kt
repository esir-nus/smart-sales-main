package com.smartsales.prism.ui.drawers

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendar
import com.smartsales.prism.ui.drawers.scheduler.SchedulerTimeline
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem

/**
 * Scheduler Drawer — Top-Down Sheet
 * @see prism-ui-ux-contract.md §1.3
 * 
 * Updated: Phase 2 Expansion & Visuals (Light Theme)
 */
@Composable
fun SchedulerDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Height: ~85% of screen
    val drawerFraction = 0.85f 
    
    // State: Calendar Expansion
    var isCalendarExpanded by remember { mutableStateOf(false) }
    
    // Mock Data
    val timelineItems = remember {
        listOf(
            TimelineItem.Task("1", "08:00", "与张总会议 (A3项目)", hasAlarm = true),
            TimelineItem.Inspiration("2", "10:30", "研究竞品报价策略", isSelected = false),
            TimelineItem.Conflict("3", "12:00", "李总电话 vs 午餐会议", isExpanded = false),
            TimelineItem.Task("4", "14:00", "提交季度报告 (已完成)", isDone = true)
        )
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isOpen,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if(isOpen) 0.3f else 0f))
                    .clickable { onDismiss() }
            )

            // Drawer Content (White Background - Light Theme)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(drawerFraction)
                    .background(
                        Color.White, // Light Theme per Spec
                        RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                // 1. Calendar Section (Expandable)
                SchedulerCalendar(
                    isExpanded = isCalendarExpanded,
                    onExpandChange = { isCalendarExpanded = it }
                )
                
                // 2. Timeline Section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFAFAFA)) // Very light gray for timeline area
                ) {
                    SchedulerTimeline(
                        items = timelineItems,
                        onInteraction = { /* Handle interactions */ }
                    )
                }

                // 3. Drag Handle (Bottom of sheet)
                DragHandle(onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun DragHandle(onDismiss: () -> Unit) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp)
            .clickable { onDismiss() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    accumulatedDrag += dragAmount
                    // Drag UP to close
                    if (accumulatedDrag < -50) {
                        onDismiss()
                        accumulatedDrag = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFFDDDDDD), RoundedCornerShape(2.dp))
        )
    }
}
