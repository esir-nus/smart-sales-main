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
    val isInspirationsExpanded by viewModel.isInspirationsExpanded.collectAsState()
    val tipsLoadingSet by viewModel.tipsLoading.collectAsState()  // Wave 9: Tips loading state
    
    val context = LocalContext.current
    
    // Show Toast when pipeline status changes
    LaunchedEffect(pipelineStatus) {
        if (!pipelineStatus.isNullOrEmpty()) {
            Toast.makeText(context, pipelineStatus, Toast.LENGTH_SHORT).show()
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
                    exitDirection = ExitDirection.RIGHT,
                    // Wave 9: Smart Tips
                    keyPersonEntityId = model.keyPersonEntityId,
                    tips = viewModel.getCachedTips(model.id),
                    tipsLoading = model.id in tipsLoadingSet
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
                    taskA = model.taskA,
                    taskB = model.taskB,
                    isExpanded = expandedConflictIds.contains(model.id)
                )
            }
        }
    }
    
    // 分离灵感和任务用于可折叠灵感架
    val inspirationItems = remember(uiItems) {
        uiItems.filterIsInstance<TimelineItem.Inspiration>()
    }
    val taskItems = remember(uiItems) {
        uiItems.filter { it !is TimelineItem.Inspiration }
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
                        val causingId by viewModel.causingTaskId.collectAsState()
                        
                        SchedulerTimeline(
                            items = taskItems,
                            conflictedTaskIds = conflictedIds,
                            causingTaskId = causingId,
                            onItemClick = { id -> viewModel.onItemClick(id) },
                            onDelete = { id -> viewModel.onDeleteItem(id) },
                            onReschedule = { id, text -> viewModel.onReschedule(id, text) },
                            onMicRecord = { wavFile -> viewModel.simulateFromMic(wavFile) },
                            onMultiSelectToggle = { id -> viewModel.onToggleSelection(id) },
                            onEnterMultiSelect = { viewModel.onEnterSelectionMode() },
                            onConflictResolve = { action -> viewModel.handleConflictResolution(action) },
                            onConflictToggle = { id -> viewModel.toggleConflictExpansion(id) },
                            onCardExpanded = { id, entityId -> viewModel.onCardExpanded(id, entityId) }  // Wave 9
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

                    // 🧪 DEV ONLY: 录音 + 文本模拟 (bypasses hardware)
                    if (com.smartsales.prism.BuildConfig.DEBUG) {
                        val scope = rememberCoroutineScope()
                        val devContext = LocalContext.current
                        
                        // 录音状态
                        var isRecordingMic by remember { mutableStateOf(false) }
                        val recorder = remember { com.smartsales.prism.data.audio.PhoneAudioRecorder(devContext) }
                        
                        // 安全网：Activity 暂停时自动取消录音
                        // 权限对话框、来电、Home键等都会触发 ON_PAUSE
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
                        
                        // 权限请求
                        val permissionLauncher = rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (!granted) {
                                Toast.makeText(devContext, "❌ 需要录音权限", Toast.LENGTH_SHORT).show()
                            }
                            // 权限获得后不自动录音 — 原始 press 手势已被权限对话框中断，
                            // 不存在对应的 tryAwaitRelease() 来停止录音。
                            // 用户下次按住即可正常录音。
                        }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color(0xFF2ECC71).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // 🎙️ Mic Record Button (hold-to-record)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isRecordingMic) Color(0xFFE74C3C).copy(alpha = 0.8f)
                                            else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    // 检查权限
                                                    val hasPermission = androidx.core.content.ContextCompat
                                                        .checkSelfPermission(
                                                            devContext,
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    
                                                    if (!hasPermission) {
                                                        permissionLauncher.launch(
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        )
                                                        return@detectTapGestures
                                                    }
                                                    
                                                    // 开始录音
                                                    recorder.startRecording()
                                                    isRecordingMic = true
                                                    
                                                    // 等松手或取消
                                                    val released = tryAwaitRelease()
                                                    
                                                    // 停止录音 → 提交
                                                    isRecordingMic = false
                                                    if (released) {
                                                        val wavFile = recorder.stopRecording()
                                                        viewModel.simulateFromMic(wavFile)
                                                    } else {
                                                        recorder.cancel()
                                                    }
                                                }
                                            )
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRecordingMic) "🔴 松开结束录音..." else "🎙️ 按住录音",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
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
