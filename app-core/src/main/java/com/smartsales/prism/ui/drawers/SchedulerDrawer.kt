package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.smartsales.prism.ui.drawers.scheduler.SchedulerCalendar
import com.smartsales.prism.ui.drawers.scheduler.SchedulerTimeline
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.CollapsibleInspirationShelf
import com.smartsales.prism.ui.drawers.scheduler.LocalSchedulerDrawerVisuals
import com.smartsales.prism.ui.drawers.scheduler.LocalSchedulerDrawerVisualMode
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.scheduler.mapper.toUiState
import com.smartsales.prism.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.FakeSchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.schedulerDrawerVisualsFor
import com.smartsales.prism.ui.components.prismMonolithTopInsetPadding
import com.smartsales.prism.ui.components.prismStatusBarPadding
import com.smartsales.prism.ui.sim.SimHomeHeroTokens
import java.time.Instant
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import kotlin.math.abs

internal const val SCHEDULER_DRAWER_HANDLE_TEST_TAG = "scheduler_drawer_handle"

private val SCHEDULER_HANDLE_DISMISS_DISTANCE = 32.dp
private val SCHEDULER_HANDLE_DISMISS_VELOCITY = 900.dp
private val SCHEDULER_HANDLE_TAP_SLOP = 3.dp
private const val SCHEDULER_HANDLE_VERTICAL_DOMINANCE_RATIO = 1.15f

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
    visualMode: SchedulerDrawerVisualMode = SchedulerDrawerVisualMode.STANDARD,
    onInspirationAskAi: ((String) -> Unit)? = null,
    enableInspirationMultiSelect: Boolean = true,
    viewModel: ISchedulerViewModel = hiltViewModel<SchedulerViewModel>(),
    reminderGuideProvider: (android.content.Context) -> ReminderReliabilityAdvisor.ReminderReliabilityGuide? =
        ReminderReliabilityAdvisor::fromContext,
    reminderActionOpener: (android.content.Context, ReminderReliabilityAdvisor.Action) -> Boolean =
        ReminderReliabilityAdvisor::openAction
) {
    val visuals = remember(visualMode) { schedulerDrawerVisualsFor(visualMode) }
    val isSimVisualMode = visualMode == SchedulerDrawerVisualMode.SIM
    val drawerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    
    // UI State: View-specific (Animation/Layout)
    var isCalendarExpanded by remember { mutableStateOf(false) }
    
    // Business State: From ViewModel
    val activeDayOffset by viewModel.activeDayOffset.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedInspirationIds by viewModel.selectedInspirationIds.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val exitingTasks by viewModel.exitingTasks.collectAsState()
    val pipelineStatus by viewModel.pipelineStatus.collectAsState()
    val isInspirationsExpanded by viewModel.isInspirationsExpanded.collectAsState()
    val tipsLoadingSet by viewModel.tipsLoading.collectAsState()  // Wave 9: Tips loading state
    val effectiveIsSelectionMode = enableInspirationMultiSelect && isSelectionMode
    val effectiveSelectedInspirationIds = if (enableInspirationMultiSelect) {
        selectedInspirationIds
    } else {
        emptySet()
    }
    
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
    var reminderGuide by remember { mutableStateOf<ReminderReliabilityAdvisor.ReminderReliabilityGuide?>(null) }
    LaunchedEffect(Unit) {
        viewModel.exactAlarmPermissionNeeded.collect {
            reminderGuide = reminderGuideProvider(context)
        }
    }
    if (reminderGuide != null) {
        val guide = reminderGuide!!
        AlertDialog(
            onDismissRequest = { reminderGuide = null },
            title = { Text(guide.title) },
            text = {
                Text(
                    buildString {
                        append(guide.message)
                        if (guide.checklist.isNotEmpty()) {
                            append("\n\n")
                            guide.checklist.forEach { item ->
                                append("• ")
                                append(item)
                                append('\n')
                            }
                        }
                    }.trim()
                )
            },
            confirmButton = {
                Row {
                    guide.secondaryAction?.let { secondaryAction ->
                        TextButton(onClick = {
                            reminderActionOpener(context, secondaryAction)
                            reminderGuide = null
                        }) {
                            Text(guide.secondaryLabel ?: "更多设置")
                        }
                    }
                    TextButton(onClick = {
                        reminderActionOpener(context, guide.primaryAction)
                        reminderGuide = null
                    }) { Text(guide.primaryLabel) }
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderGuide = null }) { Text("稍后") }
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
    
    val uiItems = remember(
        timelineItems,
        exitingTasks,
        activeDayOffset,
        effectiveIsSelectionMode,
        effectiveSelectedInspirationIds,
        expandedConflictIds,
        tipsLoadingSet
    ) {
        val mappedItems = timelineItems.map { model: SchedulerTimelineItem ->
            model.toUiState(
                isSelectionMode = effectiveIsSelectionMode,
                selectedInspirationIds = effectiveSelectedInspirationIds,
                expandedConflictIds = expandedConflictIds,
                tipsLoadingSet = tipsLoadingSet,
                cachedTips = if (model is ScheduledTask) viewModel.getCachedTips(model.id) else null
            )
        }

        val exitingUiTasks = exitingTasks
            .filter { it.sourceDayOffset == activeDayOffset }
            .map { motion ->
                val snapshot = motion.snapshot
                val base = snapshot.toUiState(
                    tipsLoadingSet = tipsLoadingSet,
                    cachedTips = viewModel.getCachedTips(snapshot.id)
                ) as TimelineItem.Task
                base.copy(
                    id = motion.sourceTaskId,
                    renderKey = motion.renderKey,
                    isInteractive = false,
                    sortInstant = snapshot.startTime,
                    processingStatus = null,
                    isExiting = true,
                    exitDirection = motion.exitDirection
                )
            }

        mappedItems + exitingUiTasks
    }
    
    // 分离灵感和任务用于可折叠灵感架
    val inspirationItems = remember(uiItems) {
        uiItems.filterIsInstance<TimelineItem.Inspiration>()
    }
    val taskItems = remember(uiItems) {
        uiItems
            .filter { item: TimelineItem -> item !is TimelineItem.Inspiration }
            .sortedWith(
                compareBy<TimelineItem> {
                    when (it) {
                        is TimelineItem.Task -> it.sortInstant ?: Instant.MAX
                        else -> Instant.MAX
                    }
                }
            )
    }

    // Drawer container — no internal scrim (AgentShell provides global scrim)
    // Use AnimatedVisibility to slide content in from top
    CompositionLocalProvider(
        LocalSchedulerDrawerVisuals provides visuals,
        LocalSchedulerDrawerVisualMode provides visualMode
    ) {
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
            val containerModifier = if (isSimVisualMode) {
                modifier
                    .fillMaxSize()
                    .prismMonolithTopInsetPadding(SimHomeHeroTokens.HeaderHeight)
                    .padding(
                        bottom = SimHomeHeroTokens.BottomMonolithHeight + 16.dp
                    )
            } else {
                modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .prismStatusBarPadding()
            }

            Surface(
                modifier = containerModifier
                    .shadow(
                        elevation = if (isSimVisualMode) 24.dp else 16.dp,
                        shape = drawerShape,
                        ambientColor = Color.Black.copy(alpha = if (isSimVisualMode) 0.18f else 0.05f),
                        spotColor = Color.Black.copy(alpha = if (isSimVisualMode) 0.18f else 0.05f)
                    )
                    .border(0.5.dp, visuals.containerBorder, drawerShape),
                shape = drawerShape,
                color = visuals.containerColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Consume all pointer events so clicks don't pass through to scrim
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
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
                        onDismiss = if (isSimVisualMode) onDismiss else null,
                        unacknowledgedDates = unacknowledgedDates,
                        rescheduledDates = rescheduledDates
                    )

                    // 2. Conflict Warning (if any)
                    val conflictWarning by viewModel.conflictWarning.collectAsState()
                    AnimatedVisibility(visible = conflictWarning != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = visuals.drawerContentHorizontalPadding)
                                .background(visuals.conflictBannerBackground, RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = conflictWarning ?: "",
                                color = visuals.conflictBannerText,
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
                        onAskAI = onInspirationAskAi,
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
                            onDelete = { id -> viewModel.deleteItem(id) },
                            onReschedule = { id, text -> viewModel.onReschedule(id, text) },
                            onMicRecord = { wavFile -> viewModel.processAudio(wavFile) },
                            onMultiSelectToggle = { id ->
                                if (enableInspirationMultiSelect) {
                                    viewModel.toggleItemSelection(id)
                                }
                            },
                            onEnterMultiSelect = {
                                if (enableInspirationMultiSelect) {
                                    viewModel.toggleSelectionMode(true)
                                }
                            },
                            onConflictResolve = { action -> viewModel.handleConflictResolution(action) },
                            onConflictToggle = { id -> viewModel.toggleConflictExpansion(id) },
                            onCardExpanded = { id, entityId -> /* no-op for now */ },
                            onToggleDone = { id -> viewModel.toggleDone(id) }
                        )

                        if (effectiveIsSelectionMode) {
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

                    if (effectiveIsSelectionMode && effectiveSelectedInspirationIds.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = visuals.multiSelectBarColor,
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
                                    text = "问AI (${effectiveSelectedInspirationIds.size})",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (com.smartsales.prism.BuildConfig.DEBUG) {
                        val devContext = LocalContext.current
                        var isRecordingMic by remember { mutableStateOf(false) }
                        val recorder = remember { com.smartsales.prism.data.audio.PhoneAudioRecorder(devContext) }

                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && isRecordingMic) {
                                    recorder.cancel()
                                    isRecordingMic = false
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                if (isRecordingMic) {
                                    recorder.cancel()
                                }
                            }
                        }

                        val permissionLauncher = rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (granted && isSimVisualMode) {
                                recorder.startRecording()
                                isRecordingMic = true
                            } else if (!granted) {
                                Toast.makeText(devContext, "需要录音权限", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = visuals.debugPanelColor,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isRecordingMic) visuals.debugPanelButtonActiveColor else visuals.debugPanelButtonColor,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .then(
                                            if (isSimVisualMode) {
                                                Modifier.clickable {
                                                    val hasPermission = androidx.core.content.ContextCompat
                                                        .checkSelfPermission(
                                                            devContext,
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                                    if (!hasPermission) {
                                                        permissionLauncher.launch(
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        )
                                                    } else if (isRecordingMic) {
                                                        isRecordingMic = false
                                                        val wavFile = runCatching { recorder.stopRecording() }.getOrNull()
                                                        if (wavFile != null) {
                                                            viewModel.processAudio(wavFile)
                                                        } else {
                                                            Toast.makeText(devContext, "停止录音失败", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        recorder.startRecording()
                                                        isRecordingMic = true
                                                    }
                                                }
                                            } else {
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onPress = {
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

                                                            recorder.startRecording()
                                                            isRecordingMic = true

                                                            val released = tryAwaitRelease()

                                                            isRecordingMic = false
                                                            if (released) {
                                                                val wavFile = recorder.stopRecording()
                                                                viewModel.processAudio(wavFile)
                                                            } else {
                                                                recorder.cancel()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            isSimVisualMode && isRecordingMic -> "停止测试录音"
                                            isSimVisualMode -> "REC 测试录音"
                                            isRecordingMic -> "松开结束录音..."
                                            else -> "按住录音"
                                        },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    DragHandle(
                        onDismiss = onDismiss,
                        dismissDirection = if (isSimVisualMode) {
                            DragHandleDismissDirection.DOWN
                        } else {
                            DragHandleDismissDirection.UP
                        }
                    )
                }
            }
        }
    }
}

private enum class DragHandleDismissDirection {
    UP,
    DOWN
}

@Composable
private fun DragHandle(
    onDismiss: () -> Unit,
    dismissDirection: DragHandleDismissDirection = DragHandleDismissDirection.UP,
    modifier: Modifier = Modifier
) {
    val visuals = LocalSchedulerDrawerVisuals.current
    val density = LocalDensity.current
    val dismissThresholdPx = remember(density) {
        with(density) { SCHEDULER_HANDLE_DISMISS_DISTANCE.toPx() }
    }
    val velocityThresholdPx = remember(density) {
        with(density) { SCHEDULER_HANDLE_DISMISS_VELOCITY.toPx() }
    }
    val tapSlopPx = remember(density) {
        with(density) { SCHEDULER_HANDLE_TAP_SLOP.toPx() }
    }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(28.dp)
                .testTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
                .semantics {
                    onClick {
                        onDismiss()
                        true
                    }
                }
                .pointerInput(dismissDirection, dismissThresholdPx, velocityThresholdPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var activePointerId: PointerId = down.id
                        val startPosition = down.position
                        val velocityTracker = VelocityTracker().apply {
                            addPosition(down.uptimeMillis, down.position)
                        }
                        val touchSlop = viewConfiguration.touchSlop
                        var directionLocked = false
                        var rejected = false
                        var maxAbsDx = 0f
                        var maxAbsDy = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull()
                                ?: break

                            activePointerId = change.id
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            val totalDx = change.position.x - startPosition.x
                            val totalDy = change.position.y - startPosition.y
                            maxAbsDx = maxOf(maxAbsDx, abs(totalDx))
                            maxAbsDy = maxOf(maxAbsDy, abs(totalDy))

                            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                if (!rejected) {
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    if (directionLocked) {
                                        if (
                                            crossedSchedulerHandleDismissDistance(
                                                dismissDirection = dismissDirection,
                                                totalDy = totalDy,
                                                thresholdPx = dismissThresholdPx
                                            ) || shouldDismissSchedulerHandleOnVelocity(
                                                dismissDirection = dismissDirection,
                                                totalDy = totalDy,
                                                velocityY = velocityY,
                                                velocityThresholdPx = velocityThresholdPx
                                            )
                                        ) {
                                            triggerSchedulerHandleDismiss(hapticFeedback, onDismiss)
                                        }
                                    } else if (maxAbsDx <= tapSlopPx && maxAbsDy <= tapSlopPx) {
                                        triggerSchedulerHandleDismiss(hapticFeedback, onDismiss)
                                    }
                                }
                                break
                            }

                            val absDx = abs(totalDx)
                            val absDy = abs(totalDy)
                            if (!directionLocked && !rejected && (absDx > touchSlop || absDy > touchSlop)) {
                                val matchesDirection = when (dismissDirection) {
                                    DragHandleDismissDirection.UP -> totalDy < 0f
                                    DragHandleDismissDirection.DOWN -> totalDy > 0f
                                }
                                val verticalDominant =
                                    absDy >= absDx * SCHEDULER_HANDLE_VERTICAL_DOMINANCE_RATIO

                                if (matchesDirection && verticalDominant) {
                                    directionLocked = true
                                } else {
                                    rejected = true
                                }
                            }

                            if (directionLocked) {
                                change.consume()
                                if (
                                    crossedSchedulerHandleDismissDistance(
                                        dismissDirection = dismissDirection,
                                        totalDy = totalDy,
                                        thresholdPx = dismissThresholdPx
                                    )
                                ) {
                                    triggerSchedulerHandleDismiss(hapticFeedback, onDismiss)
                                    break
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(visuals.handleColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun crossedSchedulerHandleDismissDistance(
    dismissDirection: DragHandleDismissDirection,
    totalDy: Float,
    thresholdPx: Float
): Boolean = when (dismissDirection) {
    DragHandleDismissDirection.UP -> totalDy <= -thresholdPx
    DragHandleDismissDirection.DOWN -> totalDy >= thresholdPx
}

private fun shouldDismissSchedulerHandleOnVelocity(
    dismissDirection: DragHandleDismissDirection,
    totalDy: Float,
    velocityY: Float,
    velocityThresholdPx: Float
): Boolean = when (dismissDirection) {
    DragHandleDismissDirection.UP ->
        totalDy < 0f && velocityY <= -velocityThresholdPx
    DragHandleDismissDirection.DOWN ->
        totalDy > 0f && velocityY >= velocityThresholdPx
}

private fun triggerSchedulerHandleDismiss(
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDismiss: () -> Unit
) {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    onDismiss()
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Vague_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("VAGUE") }
    SchedulerDrawer(isOpen = true, onDismiss = {}, viewModel = fakeViewModel)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Sim_Loaded_Preview() {
    val fakeViewModel = FakeSchedulerViewModel().apply { debugRunScenario("LOADED") }
    SchedulerDrawer(
        isOpen = true,
        onDismiss = {},
        visualMode = SchedulerDrawerVisualMode.SIM,
        enableInspirationMultiSelect = false,
        viewModel = fakeViewModel
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SchedulerDrawer_Sim_StartOnlyTask_Preview() {
    val start = java.time.LocalDate.now()
        .atTime(17, 0)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
    val fakeViewModel = FakeSchedulerViewModel().apply {
        debugSetTimelineItems(
            listOf(
                ScheduledTask(
                    id = "sim_start_only_preview",
                    timeDisplay = "17:00 - ...",
                    title = "约王总开会",
                    startTime = start,
                    endTime = null,
                    durationMinutes = 0
                )
            )
        )
    }
    SchedulerDrawer(
        isOpen = true,
        onDismiss = {},
        visualMode = SchedulerDrawerVisualMode.SIM,
        enableInspirationMultiSelect = false,
        viewModel = fakeViewModel
    )
}
