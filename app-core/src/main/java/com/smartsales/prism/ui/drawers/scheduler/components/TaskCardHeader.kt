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
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextPrimary

@Composable
fun TaskCardHeader(
    state: TimelineItem.Task,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time
        Text(
            text = state.timeDisplay.split(" - ").firstOrNull() ?: "",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.width(50.dp).padding(top = 2.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Titles & Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.isDone) TextMuted else TextPrimary,
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
                            color = TextMuted,
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
