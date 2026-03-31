package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.drawers.scheduler.components.InspirationCard
import com.smartsales.prism.ui.drawers.scheduler.components.SchedulerTaskCard
import com.smartsales.prism.ui.drawers.scheduler.components.taskCardIndicatorColor
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.AccentDanger
import com.smartsales.prism.ui.theme.GlassCardShape

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
    onReschedule: (String, String) -> Unit,
    onMicRecord: (java.io.File) -> Unit = {},
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictResolution) -> Unit,
    onConflictToggle: (String) -> Unit,
    onCardExpanded: (String, String?) -> Unit,
    onToggleDone: (String) -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    val horizontalPadding = if (isSimVisualMode) visuals.drawerContentHorizontalPadding else 16.dp

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 8.dp,
                bottom = visuals.timelineBottomFadeHeight
            )
        ) {
            itemsIndexed(
                items = items,
                key = { _, item ->
                    when (item) {
                        is TimelineItem.Task -> item.renderKey
                        else -> item.id
                    }
                }
            ) { index, item ->
                TimelineRow(
                    item = item,
                    showTopConnector = isSimVisualMode && index > 0,
                    showBottomConnector = isSimVisualMode && index < items.lastIndex,
                    conflictedTaskIds = conflictedTaskIds,
                    causingTaskId = causingTaskId,
                    onItemClick = onItemClick,
                    onDelete = onDelete,
                    onMultiSelectToggle = onMultiSelectToggle,
                    onEnterMultiSelect = onEnterMultiSelect,
                    onConflictResolve = onConflictResolve,
                    onConflictToggle = onConflictToggle,
                    onCardExpanded = onCardExpanded
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (isSimVisualMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(visuals.timelineBottomFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                visuals.containerColor
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    showTopConnector: Boolean,
    showBottomConnector: Boolean,
    conflictedTaskIds: Set<String>,
    causingTaskId: String?,
    onItemClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMultiSelectToggle: (String) -> Unit,
    onEnterMultiSelect: () -> Unit,
    onConflictResolve: (com.smartsales.prism.domain.scheduler.ConflictResolution) -> Unit,
    onConflictToggle: (String) -> Unit,
    onCardExpanded: (String, String?) -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
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
            isTaskVisible = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = visuals.timelineCardBottomSpacing),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = when {
                item is TimelineItem.Task && isSimVisualMode -> item.simTimelineRailLabel()
                item is TimelineItem.Task && item.isVague -> "待定"
                else -> item.timeDisplay
            },
            fontSize = if (isSimVisualMode) 13.sp else 12.sp,
            color = visuals.timeLabelColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSimVisualMode) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .width(visuals.timelineLabelWidth)
                .padding(top = visuals.timelineTopInset - 2.dp, end = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(visuals.timelineAxisWidth)
                .fillMaxHeight()
                .drawBehind {
                    if (!isSimVisualMode) return@drawBehind
                    val axisX = size.width / 2f
                    val dotCenterY = visuals.timelineTopInset.toPx() +
                        ((visuals.timelineDotSize + 6.dp).toPx() / 2f)
                    val startY = if (showTopConnector) 0f else dotCenterY
                    val endY = if (showBottomConnector) size.height else dotCenterY
                    if (endY > startY) {
                        drawLine(
                            color = visuals.timelineLineColor,
                            start = Offset(axisX, startY),
                            end = Offset(axisX, endY),
                            strokeWidth = visuals.timelineLineWidth.toPx()
                        )
                    }
                }
        ) {
            Box(
                modifier = Modifier.padding(top = visuals.timelineTopInset)
            ) {
                TimelineDot(item = item)
            }
        }

        Spacer(modifier = Modifier.width(visuals.timelineRailGap))

        Column(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineItem.Task -> {
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
                                onMultiSelectToggle(item.id)
                            },
                            onToggleSelection = { onMultiSelectToggle(item.id) }
                        )
                    }
                }

                is TimelineItem.Conflict -> {
                    ConflictCard(
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

@Composable
private fun TimelineDot(item: TimelineItem) {
    val visuals = currentSchedulerDrawerVisuals
    val pulse = rememberInfiniteTransition(label = "timelineDotPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "timelineDotPulseAlpha"
    )
    val dotColor = when (item) {
        is TimelineItem.Task -> taskCardIndicatorColor(item.urgencyLevel, item.isDone)
        is TimelineItem.Conflict -> AccentAmber
        is TimelineItem.Inspiration -> visuals.timelineDotColor
    }
    val showPulse = item is TimelineItem.Task &&
        !item.isDone &&
        item.urgencyLevel == UrgencyLevel.L1_CRITICAL

    Box(
        modifier = Modifier.size(visuals.timelineDotSize + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showPulse) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dotColor.copy(alpha = pulseAlpha * 0.18f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(visuals.timelineDotSize + 4.dp)
                .background(visuals.containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(visuals.timelineDotSize)
                    .background(dotColor, CircleShape)
            )
        }
    }
}

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

    LaunchedEffect(itemId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = false,
        backgroundContent = {
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
