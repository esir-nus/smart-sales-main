package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendar
import com.smartsales.prism.ui.drawers.scheduler.SchedulerTimeline
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.ui.theme.*

/**
 * Scheduler Drawer — Top-Down Glass Sheet
 * @see prism-ui-ux-contract.md §1.3
 * 
 * Updated: Phase 3 Sleek Glass Purity
 */
@Composable
fun SchedulerDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchedulerViewModel = hiltViewModel()
) {
    // Height: ~85% of screen
    val drawerFraction = 0.85f 
    
    // UI State: View-specific (Animation/Layout)
    var isCalendarExpanded by remember { mutableStateOf(false) }
    
    // Business State: From ViewModel
    val activeDayOffset by viewModel.activeDayOffset.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedInspirationIds by viewModel.selectedInspirationIds.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val pipelineStatus by viewModel.pipelineStatus.collectAsState()
    
    val context = LocalContext.current
    
    // Derived UI State for Timeline
    // Map Domain Models to UI (if needed, but for now they are compatible or shared)
    // Note: Since we defined TimelineItemModel in Domain, we need to ensure UI uses that or maps it.
    // To facilitate compiling without touching every child file, we can map Domain->UI if they differ.
    // Based on previous reads, UI used 'TimelineItem' sealed class in `SchedulerStates.kt`.
    // We should double check if we need to map Domain `TimelineItemModel` -> UI `TimelineItem`.
    // Assuming for now we map them or they are same. 
    // Wait, I created TimelineItemModel in Domain. I must map it to UI TimelineItem here.
    
    val uiItems = remember(timelineItems, isSelectionMode, selectedInspirationIds) {
        timelineItems.map { model ->
            when (model) {
                is TimelineItemModel.Task -> TimelineItem.Task(
                    id = model.id,
                    timeDisplay = model.timeDisplay,
                    title = model.title,
                    isDone = model.isDone,
                    hasAlarm = model.hasAlarm,
                    isSmartAlarm = model.isSmartAlarm,
                    dateRange = model.dateRange,
                    location = model.location,
                    notes = model.notes,
                    keyPerson = model.keyPerson,
                    highlights = model.highlights,
                    alarmCascade = model.alarmCascade,
                    // UI-only animation state (not from Domain)
                    processingStatus = null,
                    isExiting = false,
                    exitDirection = ExitDirection.RIGHT
                )
                is TimelineItemModel.Inspiration -> TimelineItem.Inspiration(
                    id = model.id,
                    timeDisplay = model.timeDisplay,
                    title = model.title,
                    isSelected = selectedInspirationIds.contains(model.id),
                    isSelectionMode = isSelectionMode
                )
                is TimelineItemModel.Conflict -> TimelineItem.Conflict(
                    id = model.id,
                    timeDisplay = model.timeDisplay,
                    conflictText = model.conflictText,
                    isExpanded = false // UI state local to item, or hoisted? ideally hoisted but okay for now
                )
            }
        }
    }

    // Drawer container — no internal scrim (PrismShell provides global scrim)
    // Use AnimatedVisibility to slide content in from top
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        PrismSurface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(drawerFraction),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            backgroundColor = BackgroundSurface.copy(alpha = 0.98f),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Consume all pointer events so clicks don't pass through to scrim
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                // Just consume, don't do anything
                            }
                        }
                    }
            ) {
                    // 1. Calendar Section (Expandable)
                    val unacknowledgedDates by viewModel.unacknowledgedDates.collectAsState()
                    SchedulerCalendar(
                        isExpanded = isCalendarExpanded,
                        onExpandChange = { isCalendarExpanded = it },
                        activeDay = activeDayOffset,
                        onDateSelected = { viewModel.onDateSelected(it) },
                        unacknowledgedDates = unacknowledgedDates
                    )
                    
                    // 2. Conflict Warning (if any)
                    val conflictWarning by viewModel.conflictWarning.collectAsState()
                    AnimatedVisibility(visible = conflictWarning != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                .clickable { viewModel.clearConflictWarning() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = conflictWarning ?: "",
                                color = Color(0xFFFFB74D),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // 3. Timeline Section
                    Box(modifier = Modifier.weight(1f)) {
                        SchedulerTimeline(
                            items = uiItems,
                            onItemClick = { id -> viewModel.onItemClick(id) },
                            onDelete = { id -> viewModel.onDeleteItem(id) },
                            onReschedule = { id, text -> viewModel.onReschedule(id, text) },
                            onMultiSelectToggle = { id -> viewModel.onToggleSelection(id) },
                            onEnterMultiSelect = { viewModel.onEnterSelectionMode() }
                        )
                        
                        // Swipe to exit multi-select
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .draggable(
                                        state = rememberDraggableState { delta ->
                                            if (delta > 20) {
                                                viewModel.onExitSelectionMode()
                                            }
                                        },
                                        orientation = Orientation.Horizontal
                                    )
                            )
                        }
                    }

                    // Multi-Select Action Bar
                    if (isSelectionMode && selectedInspirationIds.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFAF52DE), // iOS Purple
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.onExitSelectionMode()
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

                    // 🧪 DEV ONLY: Flexible Simulation Input (bypasses hardware)
                    if (com.smartsales.prism.BuildConfig.DEBUG) {
                        val scope = rememberCoroutineScope()
                        val devContext = LocalContext.current
                        var simulationText by remember { mutableStateOf("") }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color(0xFF2ECC71).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Input Field
                                androidx.compose.foundation.text.BasicTextField(
                                    value = simulationText,
                                    onValueChange = { simulationText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (simulationText.isEmpty()) {
                                            Text("输入测试语句...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Send Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val textToSend = simulationText.ifEmpty { "我需要明天凌晨3点赶飞机" }
                                            scope.launch {
                                                Toast.makeText(devContext, "🧪 模拟: $textToSend", Toast.LENGTH_SHORT).show()
                                                viewModel.simulateTranscript(textToSend)
                                            }
                                        }
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🧪 发送模拟转录",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                // Scenario Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.debugRunScenario("CLEAN") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("🧹 清除", fontSize = 11.sp, maxLines = 1) }
                                    
                                    Button(
                                        onClick = { viewModel.debugRunScenario("1PM") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("测试 1-2点", fontSize = 11.sp, maxLines = 1) }
                                    
                                    Button(
                                        onClick = { viewModel.debugRunScenario("3PM") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("测试 3-4点", fontSize = 11.sp, maxLines = 1) }
                                }
                            }
                        }
                    }

                    // Drag Handle
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
            .padding(vertical = 12.dp)
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
                .background(BorderSubtle, RoundedCornerShape(2.dp))
        )
    }
}
