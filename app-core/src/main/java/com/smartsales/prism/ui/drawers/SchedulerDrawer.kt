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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendar
import com.smartsales.prism.ui.drawers.scheduler.SchedulerTimeline
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.CollapsibleInspirationShelf
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.scheduler.mapper.toUiState
import com.smartsales.prism.ui.theme.*
import com.smartsales.prism.ui.drawers.scheduler.components.CommandInputPill
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.FakeSchedulerViewModel

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
    viewModel: ISchedulerViewModel = hiltViewModel<SchedulerViewModel>()
) {
    
    // UI State: View-specific (Animation/Layout)
    var isCalendarExpanded by remember { mutableStateOf(false) }
    
    // Business State: From ViewModel
    val activeDayOffset by viewModel.activeDayOffset.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedInspirationIds by viewModel.selectedInspirationIds.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val pipelineStatus by viewModel.pipelineStatus.collectAsState()
    val isInspirationsExpanded by viewModel.isInspirationsExpanded.collectAsState()
    val tipsLoadingSet by viewModel.tipsLoading.collectAsState()  // Wave 9: Tips loading state
    val causingId by viewModel.causingTaskId.collectAsState()
    
    val context = LocalContext.current
    
    // Show Toast when pipeline status changes
    LaunchedEffect(pipelineStatus) {
        val status = pipelineStatus
        if (!status.isNullOrEmpty()) {
            Toast.makeText(context, status as CharSequence, Toast.LENGTH_SHORT).show()
        }
    }

    // 每次抽屉打开时清理过期任务
    LaunchedEffect(isOpen) {
        if (isOpen) {
            // Re-fetch timeline or sweep tasks when drawer opens
        }
    }

    // 精确闹钟权限提示 — 一次性对话框
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.exactAlarmPermissionNeeded.collect {
            showExactAlarmDialog = true
        }
    }
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("精确闹钟权限") },
            text = { Text("未授予精确闹钟权限，提醒可能延迟最多1小时。\n\n建议在设置中开启「闹钟和提醒」权限以确保准时提醒。") },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    com.smartsales.prism.data.notification.OemCompat.openExactAlarmSettings(context)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) { Text("稍后") }
            }
        )
    }
    
    // Derived UI State for Timeline
    // Map Domain Models to UI (if needed, but for now they are compatible or shared)
    // Note: Since we defined TimelineItemModel in Domain, we need to ensure UI uses that or maps it.
    // To facilitate compiling without touching every child file, we can map Domain->UI if they differ.
    // Based on previous reads, UI used 'TimelineItem' sealed class in `SchedulerStates.kt`.
    // We should double check if we need to map Domain `TimelineItemModel` -> UI `TimelineItem`.
    // Assuming for now we map them or they are same. 
    // Wait, I created TimelineItemModel in Domain. I must map it to UI TimelineItem here.
    
    val expandedConflictIds by viewModel.expandedConflictIds.collectAsState()
    
    val uiItems = remember(timelineItems, isSelectionMode, selectedInspirationIds, expandedConflictIds, tipsLoadingSet) {
        timelineItems.map { model: SchedulerTimelineItem ->
            model.toUiState(
                isSelectionMode = isSelectionMode,
                selectedInspirationIds = selectedInspirationIds,
                expandedConflictIds = expandedConflictIds,
                tipsLoadingSet = tipsLoadingSet,
                cachedTips = if (model is ScheduledTask) viewModel.getCachedTips(model.id) else null
            )
        }
    }
    
    // 分离灵感和任务用于可折叠灵感架
    val inspirationItems = remember(uiItems) {
        uiItems.filterIsInstance<TimelineItem.Inspiration>()
    }
    val taskItems = remember(uiItems) {
        uiItems.filter { item: TimelineItem -> item !is TimelineItem.Inspiration }
    }

    // Drawer container — no internal scrim (AgentShell provides global scrim)
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F)) // DeepOnyx
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) { awaitPointerEvent() }
                    }
                }
        ) {
            // Ambient Glow (Top Right)
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-20).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(com.smartsales.prism.ui.theme.AccentBlue.copy(alpha = 0.05f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp) // Space for Command Dock
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "日程安排,\nFrank.",
                        fontSize = 32.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 38.sp
                    )
                }

                // 1. Calendar Section (Expandable)
                    val unacknowledgedDates by viewModel.unacknowledgedDates.collectAsState()
                    val rescheduledDates by viewModel.rescheduledDates.collectAsState()
                    SchedulerCalendar(
                        isExpanded = isCalendarExpanded,
                        onExpandChange = { isCalendarExpanded = it },
                        activeDay = activeDayOffset,
                        onDateSelected = { viewModel.onDateSelected(it) },
                        unacknowledgedDates = unacknowledgedDates,
                        rescheduledDates = rescheduledDates
                    )
                    
                    // 2. Conflict Warning (if any)
                    val conflictWarning by viewModel.conflictWarning.collectAsState()
                    AnimatedVisibility(visible = conflictWarning != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f))
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
                    
                    // 3. 灵感箱可折叠面板 (Wave 5)
                    CollapsibleInspirationShelf(
                        items = inspirationItems,
                        isExpanded = isInspirationsExpanded,
                        onToggle = { viewModel.toggleInspirations() },
                        onDelete = { id -> viewModel.deleteInspiration(id) },
                        onAskAI = { id -> /* 等待 Coach Mode */ },
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    
                    // 4. Timeline Section (只显示任务，不显示灵感)
                    Box(modifier = Modifier.weight(1f)) {
                        val conflictedIds by viewModel.conflictedTaskIds.collectAsState()
                        
                        SchedulerTimeline(
                            items = taskItems,
                            conflictedTaskIds = conflictedIds,
                            causingTaskId = causingId,
                            onItemClick = { id -> viewModel.onItemClick(id) },
                            onDelete = { id -> viewModel.deleteItem(id) },
                            onMultiSelectToggle = { id -> viewModel.toggleItemSelection(id) },
                            onEnterMultiSelect = { viewModel.toggleSelectionMode(true) },
                            onConflictResolve = { action -> viewModel.handleConflictResolution(action) },
                            onConflictToggle = { id -> viewModel.toggleConflictExpansion(id) },
                            onCardExpanded = { id, entityId -> /* no-op for now */ },  // Wave 9
                            onToggleDone = { id -> viewModel.toggleDone(id) }  // Wave 12
                        )
                        
                        // Swipe to exit multi-select
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .draggable(
                                        state = rememberDraggableState { delta ->
                                            if (delta > 20) {
                                                viewModel.toggleSelectionMode(false)
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
                                        viewModel.toggleSelectionMode(false)
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

            }

            // --- Unified Command Dock ---
            var inputText by remember { mutableStateOf("") }
            var isRecordingMic by remember { mutableStateOf(false) }
            val recorder = remember { com.smartsales.prism.data.audio.PhoneAudioRecorder(context) }
            
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && isRecordingMic) {
                        recorder.cancel()
                        isRecordingMic = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            if (!isSelectionMode) {
                CommandInputPill(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    onSubmitText = { 
                        viewModel.onReschedule(causingId ?: "", it)
                        inputText = "" 
                    },
                    isRecordingMic = isRecordingMic,
                    onRecordStart = {
                        recorder.startRecording()
                        isRecordingMic = true
                    },
                    onRecordStop = { released ->
                        isRecordingMic = false
                        if (released) {
                            val wavFile = recorder.stopRecording()
                            if (wavFile != null) viewModel.processAudio(wavFile)
                        } else {
                            recorder.cancel()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Empty_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("EMPTY") }
    SchedulerDrawer(isOpen = true, onDismiss = {}, viewModel = fakeViewModel)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Loaded_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("LOADED") }
    SchedulerDrawer(isOpen = true, onDismiss = {}, viewModel = fakeViewModel)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Conflict_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("CONFLICT") }
    SchedulerDrawer(isOpen = true, onDismiss = {}, viewModel = fakeViewModel)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Inspirations_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("INSPIRATIONS") }
    SchedulerDrawer(isOpen = true, onDismiss = {}, viewModel = fakeViewModel)
}
