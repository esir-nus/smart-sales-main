package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import com.smartsales.prism.ui.drawers.scheduler.simCardTimeSummary
import com.smartsales.prism.ui.drawers.scheduler.simCollapsedTimeLabel
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals
import com.smartsales.prism.ui.theme.AccentGreen
import com.smartsales.prism.ui.theme.AccentBlue

@Composable
fun TaskCardHeader(
    state: TimelineItem.Task,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    if (isSimVisualMode) {
        SimTaskCardHeader(
            state = state,
            isExpanded = isExpanded,
            modifier = modifier
        )
        return
    }

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
        if (state.hasAlarm && !state.isDone) {
            Icon(
                imageVector = Icons.Outlined.QueryBuilder,
                contentDescription = "Reminder Scheduled",
                tint = AccentBlue,
                modifier = Modifier.size(16.dp).padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SimTaskCardHeader(
    state: TimelineItem.Task,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                if (state.isDone) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AccentGreen.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(top = 2.dp, end = 6.dp)
                            .size(14.dp)
                    )
                }

                Text(
                    text = state.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isDone) visuals.taskDoneTitleColor else visuals.taskTitleColor,
                    textDecoration = if (state.isDone) TextDecoration.LineThrough else null,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            val subtitle = when {
                state.clarificationState != null && !isExpanded -> "需确认以完善日程"
                !isExpanded -> listOfNotNull(state.keyPerson, state.location).joinToString(" · ")
                else -> ""
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (state.clarificationState != null && !isExpanded) AccentBlue else visuals.taskContextColor,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = state.simCardTimeSummary(),
                fontSize = 12.sp,
                color = visuals.taskTimeColor,
                fontWeight = FontWeight.Medium
            )
            if (state.hasAlarm && !state.isDone) {
                Icon(
                    imageVector = Icons.Outlined.QueryBuilder,
                    contentDescription = "Reminder Scheduled",
                    tint = AccentBlue,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
