package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import com.smartsales.prism.ui.theme.*
import com.smartsales.prism.ui.drawers.scheduler.ConflictVisual
import com.smartsales.prism.ui.drawers.scheduler.components.SchedulerTaskCard
import com.smartsales.prism.ui.drawers.scheduler.components.InspirationCard
import androidx.compose.ui.text.style.TextAlign

/**
 * Scheduler Timeline Layout (Sleek Glass Version)
 * @see prism-ui-ux-contract.md §1.3
 */
@Composable
fun SchedulerTimeline(
    items: List<TimelineItem>,
    conflictedTaskIds: Set<String> = emptySet(),
    causingTaskId: String? = null,
    onItemClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReschedule: (String, String) -> Unit, // id, userText
    onMicRecord: (java.io.File) -> Unit = {},
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictResolution) -> Unit,
    onConflictToggle: (String) -> Unit,
    onCardExpanded: (String, String?) -> Unit,  // Wave 9: (taskId, keyPersonEntityId)
    onToggleDone: (String) -> Unit  // Wave 12: Task Completion
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(items, key = {
            when (it) {
                is TimelineItem.Task -> it.renderKey
                else -> it.id
            }
        }) { item ->
            TimelineRow(
                item = item,
                conflictedTaskIds = conflictedTaskIds,
                causingTaskId = causingTaskId,
                onItemClick = onItemClick,
                onDelete = onDelete,
                onReschedule = onReschedule,
                onMicRecord = onMicRecord,
                onMultiSelectToggle = onMultiSelectToggle,
                onEnterMultiSelect = onEnterMultiSelect,
                onConflictResolve = onConflictResolve,
                onConflictToggle = onConflictToggle,
                onCardExpanded = onCardExpanded,  // Wave 9
                onToggleDone = onToggleDone  // Wave 12
            )
        }
        
        // Bottom spacer for overscroll
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    conflictedTaskIds: Set<String>,
    causingTaskId: String?,
    onItemClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReschedule: (String, String) -> Unit,
    onMicRecord: (java.io.File) -> Unit,
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictResolution) -> Unit,
    onConflictToggle: (String) -> Unit,
    onCardExpanded: (String, String?) -> Unit,  // Wave 9
    onToggleDone: (String) -> Unit  // Wave 12
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    // Local expansion state for this row item
    var isExpanded by remember { mutableStateOf(false) }
    var isTaskVisible by remember(
        when (item) {
            is TimelineItem.Task -> item.renderKey
            else -> item.id
        }
    ) { mutableStateOf(true) }

    LaunchedEffect(item) {
        isTaskVisible = true
        if (item is TimelineItem.Task && item.isExiting) {
            // 先渲染源卡片，再切换为退出动画
            isTaskVisible = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp)
    ) {
        // Left: Time Label
        Text(
            text = when {
                item is TimelineItem.Task && isSimVisualMode -> item.simTimelineRailLabel()
                item is TimelineItem.Task && item.isVague -> "待定"
                else -> item.timeDisplay
            },
            fontSize = 12.sp,
            color = visuals.timeLabelColor,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(visuals.timelineLabelWidth)
                .padding(top = 2.dp)
        )
        
        // Middle: Timeline Axis (Dot + Line)
        Column(
            modifier = Modifier
                .width(visuals.timelineAxisWidth)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(visuals.timelineTopInset))
            Box(
                modifier = Modifier
                    .size(visuals.timelineDotSize)
                    .background(visuals.timelineDotColor, androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(visuals.timelineLineWidth)
                    .fillMaxHeight()
                    .background(visuals.timelineLineColor)
            )
        }
        
        Spacer(modifier = Modifier.width(visuals.timelineRailGap))
        
        // Right: Card Content
        Column(modifier = Modifier.weight(1f).padding(bottom = visuals.timelineCardBottomSpacing)) {
            when (item) {
                is TimelineItem.Task -> {
                    // 映射冲突视觉状态
                    val conflictVisual = when {
                        item.id == causingTaskId -> ConflictVisual.CAUSING
                        item.id in conflictedTaskIds -> ConflictVisual.IN_GROUP
                        else -> ConflictVisual.NONE
                    }
                    val taskWithVisual = item.copy(conflictVisual = conflictVisual)
                    
                    val slideOffset: (Int) -> Int = if (item.exitDirection == ExitDirection.LEFT) {
                        { -it }
                    } else {
                        { it }
                    }
                    
                    AnimatedVisibility(
                        visible = isTaskVisible,
                        exit = slideOutHorizontally(
                            targetOffsetX = slideOffset,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                    ) {
                        SwipeableCardItem(
                            itemId = item.id,
                            onDelete = { onDelete(item.id) },
                            enabled = !isExpanded && item.isInteractive
                        ) {
                            SchedulerTaskCard(
                                state = taskWithVisual,
                                isExpanded = isExpanded,
                                enabled = item.isInteractive,
                                onClick = {
                                    if (!item.isInteractive) return@SchedulerTaskCard
                                    val wasExpanded = isExpanded
                                    isExpanded = !isExpanded
                                    if (!wasExpanded) {
                                        onCardExpanded(item.id, item.keyPersonEntityId)
                                    }
                                    onItemClick(item.id)
                                }
                            )
                        }
                    }
                }
                is TimelineItem.Inspiration -> {
                    SwipeableCardItem(
                        itemId = item.id,
                        onDelete = { onDelete(item.id) }
                    ) {
                        InspirationCard(
                            state = item,
                            onAskAI = {
                                onEnterMultiSelect()
                                onMultiSelectToggle(item.id) // Select this one
                            },
                            onToggleSelection = { onMultiSelectToggle(item.id) }
                        )
                    }
                }
                is TimelineItem.Conflict -> {
                    com.smartsales.prism.ui.drawers.scheduler.ConflictCard(
                        taskA = item.taskA,
                        taskB = item.taskB,
                        isExpanded = item.isExpanded,
                        onExpandToggle = { onConflictToggle(item.id) },
                        onResolve = onConflictResolve
                    )
                }
            }
        }
    }
}


// Keeping SwipeableCardItem and TaskCard as they were in the original document,
// assuming SchedulerTaskCard is a new composable that replaces their combined functionality for tasks.
// The InspirationCard still uses SwipeableCardItem.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCardItem(
    itemId: String,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.25f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                currentOnDelete()
                true
            } else {
                false
            }
        }
    )
    
    // Reset dismiss state when item changes
    LaunchedEffect(itemId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            // Only show background when actively swiping (not settled)
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isSwiping) {
                            Modifier.background(AccentDanger.copy(alpha = 0.1f), GlassCardShape)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentDanger
                    )
                }
            }
        },
        content = { content() }
    )
}
