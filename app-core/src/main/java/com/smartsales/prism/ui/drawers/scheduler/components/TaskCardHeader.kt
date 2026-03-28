package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueryBuilder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals
import com.smartsales.prism.ui.drawers.scheduler.simCollapsedTimeLabel
import com.smartsales.prism.ui.theme.AccentBlue

@Composable
fun TaskCardHeader(
    state: TimelineItem.Task,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time
        Text(
            text = if (isSimVisualMode) {
                state.simCollapsedTimeLabel()
            } else {
                state.timeDisplay.split(" - ").firstOrNull() ?: ""
            },
            fontSize = 13.sp,
            color = visuals.taskTimeColor,
            fontWeight = if (isSimVisualMode) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.width(visuals.cardHeaderTimeWidth).padding(top = 2.dp)
        )
        
        Spacer(modifier = Modifier.width(visuals.cardHeaderGap))
        
        // Titles & Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.isDone) visuals.taskDoneTitleColor else visuals.taskTitleColor,
                textDecoration = if (state.isDone) TextDecoration.LineThrough else null,
                lineHeight = 20.sp
            )
            
            if (!isExpanded) {
                // If there's an active clarification state, we hint it here in the collapsed view
                if (state.clarificationState != null) {
                    Text(
                        text = "需确认以完善日程",
                        fontSize = 12.sp,
                        color = AccentBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    val subtitleParts = listOfNotNull(state.keyPerson, state.location)
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString(" • "),
                            fontSize = 12.sp,
                            color = visuals.taskContextColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Smart Badge Icon
        if (state.hasAlarm && state.isSmartAlarm && !state.isDone) {
            Icon(
                imageVector = Icons.Outlined.QueryBuilder,
                contentDescription = "Smart Alarm",
                tint = AccentBlue,
                modifier = Modifier.size(16.dp).padding(top = 4.dp)
            )
        }
    }
}
