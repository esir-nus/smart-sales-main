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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Scheduler Timeline Layout
 * @see prism-ui-ux-contract.md §1.3 "Timeline (Adaptive Stack)"
 * 
 * Adaptive stack: time labels on left, cards on right.
 * No fixed height per hour - cards stack naturally with 16dp spacing.
 */
@Composable
fun SchedulerTimeline(
    items: List<TimelineItem>,
    onInteraction: (String) -> Unit // Generic callback for skeleton clicks
) {


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            TimelineRow(item = item, onInteraction = onInteraction)
        }
        
        // Bottom spacer for overscroll
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    onInteraction: (String) -> Unit
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
            color = Color(0xFF999999),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .width(48.dp)
                .padding(top = 4.dp) // Align with card text top
        )
        
        // Right: Card Content
        Box(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineItem.Task -> {
                    SwipeableCardItem(
                        itemId = item.id,
                        onDelete = { onInteraction("delete_${item.id}") },
                        enabled = !isExpanded // Disable swipe when expanded
                    ) {
                        TaskCard(
                            state = item,
                            isExpanded = isExpanded,
                            onExpandToggle = { isExpanded = !isExpanded },
                            onClick = { onInteraction("task_${item.id}") }
                        )
                    }
                }
                is TimelineItem.Inspiration -> {
                    SwipeableCardItem(
                        itemId = item.id,
                        onDelete = { onInteraction("delete_${item.id}") }
                    ) {
                        InspirationCard(
                            state = item,
                            onAskAI = { onInteraction("ask_ai_${item.id}") },
                            onToggleSelection = { onInteraction("select_${item.id}") }
                        )
                    }
                }
                is TimelineItem.Conflict -> {
                    SwipeableCardItem(
                        itemId = item.id,
                        onDelete = { onInteraction("delete_${item.id}") },
                        enabled = !isExpanded // Disable swipe when expanded
                    ) {
                        ConflictCard(
                            state = item,
                            isExpanded = isExpanded,
                            onExpandToggle = { isExpanded = !isExpanded },
                            onRemove = { onInteraction("conflict_resolve_${item.id}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCardItem(
    itemId: String,
    onDelete: () -> Unit,
    enabled: Boolean = true, // Added enabled param
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }, // 25% Threshold
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) { // Left-to-Right delete
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled, // Respect enabled flag (L->R)
        enableDismissFromEndToStart = false, // Disable R->L
        backgroundContent = {
            val color = Color(0xFFFFCDD2) // Light red
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart // Icon on Left
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        },
        content = { content() }
    )
}
