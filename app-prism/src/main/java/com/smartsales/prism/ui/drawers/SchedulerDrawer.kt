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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource // Added import
import androidx.compose.ui.platform.LocalContext // Added for Toast
import android.widget.Toast // Added for Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    
    // State: Inspiration Multi-Select
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedInspirationIds by remember { mutableStateOf(setOf<String>()) }
    
    // Mock Data (Mutable for Deletion)
    val timelineItems = remember {
        mutableStateListOf(
            TimelineItem.Task(
                "1", 
                "08:00", 
                "与张总会议 (A3项目)", 
                hasAlarm = true,
                dateRange = "08:00 - 09:00",
                location = "会议室 A",
                notes = "讨论Q4预算审核细节，确认最终报价范围。"
            ),
            TimelineItem.Inspiration("2", "10:30", "研究竞品报价策略", isSelected = false),
            TimelineItem.Conflict("3", "12:00", "李总电话 vs 午餐会议", isExpanded = false),
            TimelineItem.Task(
                "4", 
                "14:00", 
                "提交季度报告 (已完成)", 
                isDone = true,
                dateRange = "14:00 - 15:30",
                location = "工位",
                notes = "已通过邮件发送给运营总监。"
            )
        )
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Transform timeline items based on selection mode
    // FIX: Added timelineItems.size/lastOrNull to force refresh when list mutates
    val displayItems = remember(timelineItems, timelineItems.size, timelineItems.lastOrNull(), isSelectionMode, selectedInspirationIds) {
        timelineItems.map { item ->
            if (item is TimelineItem.Inspiration) {
                item.copy(
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedInspirationIds.contains(item.id)
                )
            } else {
                item
            }
        }
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
                    // Intercept clicks to prevent them from passing through to the scrim
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume click */ }
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
                        items = displayItems,
                        onInteraction = { interaction ->
                            when {
                                interaction.startsWith("ask_ai_") -> {
                                    val id = interaction.removePrefix("ask_ai_")
                                    // Enter selection mode and select this inspiration
                                    isSelectionMode = true
                                    selectedInspirationIds = selectedInspirationIds + id
                                }
                                interaction.startsWith("select_") -> {
                                    val id = interaction.removePrefix("select_")
                                    // Toggle selection
                                    selectedInspirationIds = if (selectedInspirationIds.contains(id)) {
                                        val newSet = selectedInspirationIds - id
                                        // Exit selection mode if none selected
                                        if (newSet.isEmpty()) isSelectionMode = false
                                        newSet
                                    } else {
                                        selectedInspirationIds + id
                                    }
                                }

                                interaction.startsWith("delete_") -> {
                                    val id = interaction.removePrefix("delete_")
                                    val item = timelineItems.find { it.id == id }
                                    if (item != null) {
                                        timelineItems.remove(item)
                                        // "Boom, it's gone" - with cleanup confirmation
                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                interaction.startsWith("reschedule_") -> {
                                    // Format: reschedule_<id>_<text>
                                    val parts = interaction.split("_", limit = 3)
                                    if (parts.size == 3) {
                                        val id = parts[1]
                                        // Fake I/O Flow
                                        coroutineScope.launch {
                                            // 1. Parsing
                                            val index = timelineItems.indexOfFirst { it.id == id }
                                            if (index != -1 && timelineItems[index] is TimelineItem.Task) {
                                                val task = timelineItems[index] as TimelineItem.Task
                                                timelineItems[index] = task.copy(processingStatus = "Parsing request...")
                                            }
                                            
                                            delay(800) // Fake parsing delay

                                            // 2. Checking Memory
                                            val index2 = timelineItems.indexOfFirst { it.id == id }
                                            if (index2 != -1 && timelineItems[index2] is TimelineItem.Task) {
                                                val task = timelineItems[index2] as TimelineItem.Task
                                                timelineItems[index2] = task.copy(processingStatus = "Checking availability...")
                                            }

                                            delay(1200) // Fake checking delay

                                            // 3. Success (Move Card)
                                            val index3 = timelineItems.indexOfFirst { it.id == id }
                                            if (index3 != -1 && timelineItems[index3] is TimelineItem.Task) {
                                                val task = timelineItems[index3] as TimelineItem.Task
                                                // Trigger Animation: Slide Out
                                                timelineItems[index3] = task.copy(
                                                    processingStatus = null, // Hide overlay during exit
                                                    isExiting = true 
                                                )
                                            }

                                            delay(300) // Wait for animation

                                            val itemToRemove = timelineItems.find { it.id == id }
                                            if (itemToRemove != null) {
                                                timelineItems.remove(itemToRemove)
                                                // Fade + Toast Feedback
                                                Toast.makeText(context, "Rescheduled to March 3rd, 08:00", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                                else -> { /* Other interactions */ }
                            }
                        }
                    )
                }
                
                // 2.1 Multi-Select Bottom Bar
                if (isSelectionMode && selectedInspirationIds.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF7B1FA2), // Purple accent
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    // TODO: Navigate to Coach with selected inspirations
                                    isSelectionMode = false
                                    selectedInspirationIds = emptySet()
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "问AI (${selectedInspirationIds.size})",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
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
            // Removed clickable { onDismiss() } to prevent accidental dismissals during interaction attempt
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
