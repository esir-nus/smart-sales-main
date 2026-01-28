package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                    TaskCard(
                        state = item,
                        onClick = { onInteraction("task_${item.id}") }
                    )
                }
                is TimelineItem.Inspiration -> {
                    InspirationCard(
                        state = item,
                        onAskAI = { onInteraction("ask_ai_${item.id}") }
                    )
                }
                is TimelineItem.Conflict -> {
                    ConflictCard(
                        state = item,
                        onRemove = { onInteraction("conflict_resolve_${item.id}") }
                    )
                }
            }
        }
    }
}
