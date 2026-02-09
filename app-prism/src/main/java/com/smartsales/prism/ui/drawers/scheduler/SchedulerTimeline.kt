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
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictAction) -> Unit,
    onConflictToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items, key = { it.id }) { item ->
            TimelineRow(
                item = item,
                conflictedTaskIds = conflictedTaskIds,
                causingTaskId = causingTaskId,
                onItemClick = onItemClick,
                onDelete = onDelete,
                onReschedule = onReschedule,
                onMultiSelectToggle = onMultiSelectToggle,
                onEnterMultiSelect = onEnterMultiSelect,
                onConflictResolve = onConflictResolve,
                onConflictToggle = onConflictToggle
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
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictAction) -> Unit,
    onConflictToggle: (String) -> Unit
) {
    // Local expansion state for this row item
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Left: Time Label
        Text(
            text = item.timeDisplay,
            fontSize = 12.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .width(48.dp)
                .padding(top = 4.dp)
        )
        
        // Right: Card Content
        Column(modifier = Modifier.weight(1f)) {
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
                        visible = !item.isExiting,
                        exit = slideOutHorizontally(
                            targetOffsetX = slideOffset,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                    ) {
                        SwipeableCardItem(
                            itemId = item.id,
                            onDelete = { onDelete(item.id) },
                            enabled = !isExpanded
                        ) {
                            TaskCard(
                                state = taskWithVisual,  // 传递带视觉状态的任务
                                isExpanded = isExpanded,
                                onExpandToggle = { isExpanded = !isExpanded },
                                onClick = { onItemClick(item.id) },
                                onReschedule = { text -> onReschedule(item.id, text) }
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
